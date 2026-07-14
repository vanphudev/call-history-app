package com.antimobile.callhs.data.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.antimobile.callhs.data.model.CallEntry
import com.antimobile.callhs.data.model.CallNumberDetail
import com.antimobile.callhs.data.model.CallType
import com.antimobile.callhs.util.CallResults
import com.antimobile.callhs.util.Carrier
import com.antimobile.callhs.util.SimRegistry
import com.antimobile.callhs.util.SimScope

/**
 * Đọc lịch sử cuộc gọi từ máy người dùng. CHỈ ĐỌC — không ghi/sửa/xoá.
 * Caller phải đảm bảo đã có quyền READ_CALL_LOG trước khi gọi.
 */
class CallLogRepository(private val context: Context) {

    private val resolver = context.contentResolver

    private val projection = arrayOf(
        CallLog.Calls._ID,
        CallLog.Calls.NUMBER,
        CallLog.Calls.TYPE,
        CallLog.Calls.DATE,
        CallLog.Calls.DURATION,
        CallLog.Calls.CACHED_NAME,
        CallLog.Calls.CACHED_PHOTO_URI,
        CallLog.Calls.GEOCODED_LOCATION,
        CallLog.Calls.CACHED_NUMBER_TYPE,
        CallLog.Calls.CACHED_NUMBER_LABEL,
        CallLog.Calls.PHONE_ACCOUNT_ID,
        CallLog.Calls.FEATURES
    )

    /** Toàn bộ cuộc gọi gần đây, mới → cũ. (Provider không nhận LIMIT trong sortOrder nên cắt trong code.) */
    fun loadRecent(limit: Int = 1000): List<CallEntry> {
        // Đồng bộ các SIM ĐANG LẮP (rẻ) TRƯỚC khi gán nhãn / lọc → nhãn "SIM n" và phạm vi bám đúng SIM hiện tại,
        // cuộc của SIM đã tháo (account-id lạ) sẽ KHÔNG được gán nhãn (ẩn khỏi tab SIM), đúng như máy.
        SimRegistry.refresh(context)
        // Phạm vi SIM toàn app: lọc NGAY trong truy vấn theo account-id của SIM đó (khớp CẢ subId lẫn ICCID) →
        // lấy đúng `limit` cuộc GẦN NHẤT CỦA SIM đó, không phải giao với "1000 gần nhất toàn bộ" (tránh rỗng khi
        // SIM ít dùng, cuộc nằm ngoài cửa sổ 1000). scopeIds = null (xem tất cả) → không thêm điều kiện.
        val scopeIds = SimScope.scopeAccountIds()
        val selection = scopeIds?.let { ids ->
            "${CallLog.Calls.PHONE_ACCOUNT_ID} IN (${ids.joinToString(",") { "?" }})"
        }
        val selectionArgs = scopeIds?.toTypedArray()
        val result = ArrayList<CallEntry>()
        resolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CallLog.Calls.DATE} DESC"
        )?.use { c ->
            val idx = ColumnIndex(c)
            while (c.moveToNext() && result.size < limit) result.add(idx.read(c))
        }
        // Truy vấn ĐÃ lọc theo SIM (nếu có scope) nên chỉ cần gán nhãn theo SIM đang lắp.
        return withContacts(assignSimLabels(result), contactDirectory())
    }

    /** Tất cả cuộc gọi với một số cụ thể + số liệu tổng hợp cho màn chi tiết. */
    fun loadDetail(number: String): CallNumberDetail {
        val raw = ArrayList<CallEntry>()
        resolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            "${CallLog.Calls.NUMBER} = ?",
            arrayOf(number),
            "${CallLog.Calls.DATE} DESC"
        )?.use { c ->
            val idx = ColumnIndex(c)
            while (c.moveToNext()) raw.add(idx.read(c))
        }
        // Nhãn SIM theo các SIM ĐANG LẮP (đồng bộ trước) → cuộc của SIM đã tháo không bị gán nhãn; rồi lọc theo
        // phạm vi app. Số liệu tổng hợp bên dưới tính trên tập ĐÃ lọc.
        SimRegistry.refresh(context)
        val entries = withContacts(SimScope.filter(assignSimLabels(raw)), contactDirectory())

        val incoming = entries.count {
            it.type == CallType.INCOMING || it.type == CallType.ANSWERED_EXTERNALLY
        }
        val outgoing = entries.count { it.type == CallType.OUTGOING }
        val missed = entries.count {
            it.type == CallType.MISSED || it.type == CallType.REJECTED || it.type == CallType.BLOCKED
        }
        val contact = lookupContact(number)
        val sims = entries.mapNotNull { it.simLabel }.distinct().sorted()
        return CallNumberDetail(
            number = number,
            // Danh tính (tên + ảnh + URI) lấy từ MỘT nguồn DUY NHẤT là PhoneLookup sống → header và
            // hàng "Thêm/Xem danh bạ" không bao giờ mâu thuẫn; liên hệ đã xoá → null ("Chưa lưu danh bạ").
            cachedName = contact?.displayName,
            photoUri = contact?.photoUri,
            geocodedLocation = entries.firstNotNullOfOrNull { it.geocodedLocation?.takeIf(String::isNotBlank) },
            carrier = Carrier.of(number),
            simLabels = sims,
            entries = entries,
            totalCalls = entries.size,
            incomingCount = incoming,
            outgoingCount = outgoing,
            missedCount = missed,
            // Chỉ cộng thời lượng của cuộc gọi ĐÃ đàm thoại — loại ĐỔ CHUÔNG của cuộc gọi nhỡ/từ chối/chặn.
            totalDurationSeconds = entries.sumOf {
                if (CallResults.isConnected(it.type, it.durationSeconds)) it.durationSeconds else 0L
            },
            contactLookupUri = contact?.lookupUri
        )
    }

    /** Danh tính liên hệ SỐNG (tên + ảnh + lookup URI) đọc CÙNG một dòng PhoneLookup. */
    private data class ContactRef(val displayName: String?, val photoUri: String?, val lookupUri: String)

    /**
     * Tra liên hệ đã lưu cho [number] bằng PhoneLookup (cơ chế khớp số của hệ thống, tự xử lý
     * +84/0/khoảng trắng) — chính xác theo THỜI GIAN THỰC. Trả `null` khi số CHƯA lưu (kể cả khi
     * CACHED_NAME cũ còn sót sau khi xoá liên hệ) hoặc chưa cấp READ_CONTACTS.
     *
     * Lấy tên + ảnh + URI trong CÙNG một dòng cursor để chúng luôn trỏ về ĐÚNG một liên hệ (khi
     * nhiều liên hệ trùng số, header và nút "Xem trong danh bạ" nhất quán mở cùng liên hệ đó).
     */
    private fun lookupContact(number: String): ContactRef? {
        val clean = number.trim()
        if (clean.isEmpty() || clean.startsWith("-")) return null
        return try {
            val filterUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(clean)
            )
            resolver.query(
                filterUri,
                arrayOf(
                    ContactsContract.PhoneLookup._ID,
                    ContactsContract.PhoneLookup.LOOKUP_KEY,
                    ContactsContract.PhoneLookup.DISPLAY_NAME,
                    ContactsContract.PhoneLookup.PHOTO_URI
                ),
                null, null, null
            )?.use { c ->
                if (!c.moveToFirst()) return@use null
                val id = c.getLong(0)
                val key = c.getString(1) ?: return@use null
                val lookup = ContactsContract.Contacts.getLookupUri(id, key)?.toString() ?: return@use null
                ContactRef(
                    displayName = c.getString(2)?.takeIf { it.isNotBlank() },
                    photoUri = c.getString(3),
                    lookupUri = lookup
                )
            }
        } catch (_: SecurityException) {
            null // Chưa cấp READ_CONTACTS.
        } catch (_: Exception) {
            null // Provider lỗi/không có → coi như chưa lưu.
        }
    }

    /**
     * Gán "SIM 1"/"SIM 2"… từ PHONE_ACCOUNT_ID theo các SIM ĐANG LẮP ([SimRegistry], suy từ [SimInfo.activeSims],
     * cần READ_PHONE_STATE) — GIỐNG màn Cước & trình gọi mặc định của máy. Cuộc gọi có account-id KHỚP một SIM
     * đang lắp → nhãn SIM đó; account-id LẠ (SIM đã tháo, khay/eSIM cũ) → KHÔNG gán (simLabel = null), tự ẩn khỏi
     * mọi tab SIM. Registry rỗng (chưa cấp quyền / không SIM) → giữ nguyên tất cả (simLabel = null).
     */
    private fun assignSimLabels(entries: List<CallEntry>): List<CallEntry> {
        val labelOf = SimRegistry.labelMap()
        if (labelOf.isEmpty()) return entries
        return entries.map { e ->
            val label = e.phoneAccountId?.let { labelOf[it] }
            if (label == null) e else e.copy(simLabel = label)
        }
    }

    /**
     * Ảnh chụp danh bạ hiện tại: map "9 số cuối" → tên/ảnh liên hệ. Khoá theo 9 số cuối để khớp
     * bất kể +84/0/khoảng trắng. Chưa cấp READ_CONTACTS → map RỖNG (tự fallback CACHED_NAME).
     */
    private fun contactDirectory(): Map<String, ContactInfo> {
        val map = HashMap<String, ContactInfo>()
        try {
            resolver.query(
                Phone.CONTENT_URI,
                arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME, Phone.PHOTO_URI),
                null,
                null,
                null
            )?.use { c ->
                val numIdx = c.getColumnIndexOrThrow(Phone.NUMBER)
                val nameIdx = c.getColumnIndexOrThrow(Phone.DISPLAY_NAME)
                val photoIdx = c.getColumnIndexOrThrow(Phone.PHOTO_URI)
                while (c.moveToNext()) {
                    val key = phoneKey(c.getString(numIdx).orEmpty())
                    if (key.isNotEmpty()) {
                        map.putIfAbsent(key, ContactInfo(c.getString(nameIdx), c.getString(photoIdx)))
                    }
                }
            }
        } catch (_: SecurityException) {
            // Chưa cấp READ_CONTACTS → giữ map rỗng, dùng CACHED_NAME sẵn có.
        }
        return map
    }

    /** Gộp tên/ảnh danh bạ SỐNG vào entries: số đã lưu → ưu tiên tên danh bạ; chưa lưu → giữ CACHED_NAME. */
    private fun withContacts(entries: List<CallEntry>, dir: Map<String, ContactInfo>): List<CallEntry> {
        if (dir.isEmpty()) return entries
        return entries.map { e ->
            val hit = dir[phoneKey(e.number)] ?: return@map e
            e.copy(cachedName = hit.name ?: e.cachedName, photoUri = hit.photoUri ?: e.photoUri)
        }
    }

    private data class ContactInfo(val name: String?, val photoUri: String?)

    /** Cache vị trí cột để đọc cursor nhanh. */
    private class ColumnIndex(c: Cursor) {
        val id = c.getColumnIndexOrThrow(CallLog.Calls._ID)
        val number = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
        val type = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)
        val date = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
        val duration = c.getColumnIndexOrThrow(CallLog.Calls.DURATION)
        val name = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
        val photo = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_PHOTO_URI)
        val geo = c.getColumnIndexOrThrow(CallLog.Calls.GEOCODED_LOCATION)
        val numType = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NUMBER_TYPE)
        val numLabel = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NUMBER_LABEL)
        val account = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
        val features = c.getColumnIndex(CallLog.Calls.FEATURES)

        fun read(c: Cursor): CallEntry {
            val number = c.getString(number)?.trim().orEmpty()
            val feats = if (features >= 0 && !c.isNull(features)) c.getInt(features) else 0
            return CallEntry(
                id = c.getLong(id),
                number = number,
                cachedName = c.getString(name),
                type = mapType(c.getInt(type)),
                dateMillis = c.getLong(date),
                durationSeconds = c.getLong(duration),
                photoUri = c.getString(photo),
                geocodedLocation = c.getString(geo),
                numberType = c.getInt(numType),
                numberTypeCustom = c.getString(numLabel),
                phoneAccountId = if (account >= 0) c.getString(account) else null,
                simLabel = null,
                carrier = Carrier.of(number),
                isVideo = (feats and CallLog.Calls.FEATURES_VIDEO) != 0,
                isVolte = (feats and CallLog.Calls.FEATURES_VOLTE) != 0
            )
        }
    }

    private companion object {
        /** Khoá so khớp số điện thoại: chỉ giữ chữ số, lấy 9 số cuối (để +84… khớp 0…). */
        fun phoneKey(raw: String): String {
            val digits = raw.filter(Char::isDigit)
            return if (digits.length >= 9) digits.takeLast(9) else digits
        }

        fun mapType(value: Int): CallType = when (value) {
            CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
            CallLog.Calls.VOICEMAIL_TYPE -> CallType.VOICEMAIL
            CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
            CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
            CallLog.Calls.ANSWERED_EXTERNALLY_TYPE -> CallType.ANSWERED_EXTERNALLY
            else -> CallType.UNKNOWN
        }
    }
}
