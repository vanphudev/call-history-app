package com.antimobile.mcas.util

import java.net.URLDecoder
import java.nio.charset.Charset

/**
 * Phân loại nội dung MỘT mã QR đã quét thành các dạng CÓ HÀNH ĐỘNG (link web / điện thoại / SMS / email)
 * hoặc [Text] (văn bản thuần).
 *
 * Vì sao: mẫu tin nhắn dùng `{contextqr}` để chèn "mã đơn" (một chuỗi văn bản). Nhưng người dùng có thể vô
 * tình quét mã QR loại khác (link web, danh thiếp, tel…) — khi đó KHÔNG nên chèn cả URL vào tin nhắn mà nên
 * mở đúng hành động tương ứng. Nơi gọi kiểm tra: [Text] → theo luồng mẫu cũ; loại khác → mở sheet xử lý.
 *
 * THUẦN Kotlin/JVM (không phụ thuộc Android) để unit-test được; giải mã %xx/`+` bằng [URLDecoder].
 */
sealed interface QrContent {
    /** Chuỗi GỐC đọc được từ mã QR (giữ nguyên để hiển thị & sao chép). */
    val raw: String

    data class Web(override val raw: String, val url: String) : QrContent
    data class Phone(override val raw: String, val number: String) : QrContent
    data class Sms(override val raw: String, val number: String, val body: String?) : QrContent
    data class Email(override val raw: String, val address: String, val subject: String?, val body: String?) : QrContent

    /** Mạng Wi‑Fi: [security] = kiểu bảo mật thô (WPA/WEP/nopass…), [hidden] = SSID ẩn. */
    data class Wifi(override val raw: String, val ssid: String, val password: String?, val security: String?, val hidden: Boolean) : QrContent

    /** Danh thiếp (vCard/MECARD) — các trường có thể rỗng nhưng KHÔNG rỗng tất cả. */
    data class Contact(override val raw: String, val name: String?, val phone: String?, val email: String?, val org: String?) : QrContent

    /**
     * Toạ độ bản đồ; [lat]/[lng] là chuỗi số hợp lệ, [label] nhãn tuỳ chọn.
     * [query] = chuỗi ĐỊA CHỈ/tìm kiếm từ tham số `q=` khi mã KHÔNG kèm toạ độ thật (dạng `geo:0,0?q=địa chỉ`)
     * — để app bản đồ tự geocode thay vì mở đảo 0,0; null khi đã có toạ độ thật.
     */
    data class Geo(override val raw: String, val lat: String, val lng: String, val label: String?, val query: String?) : QrContent

    data class Text(override val raw: String) : QrContent

    /** true nếu là loại CÓ HÀNH ĐỘNG (không phải văn bản thuần) → nơi gọi mở sheet xử lý thay vì gửi mẫu. */
    val isActionable: Boolean get() = this !is Text

    companion object {
        /** Nhận diện theo scheme/tiền tố (không phân biệt hoa/thường). Không khớp gì → [Text] (giữ nguyên raw). */
        fun parse(raw: String): QrContent {
            val t = raw.trim()
            val lower = t.lowercase()
            return when {
                lower.startsWith("http://") || lower.startsWith("https://") ->
                    Web(raw, t)
                lower.startsWith("www.") ->
                    Web(raw, "https://$t")
                lower.startsWith("tel:") ->
                    t.substring(4).trim().ifBlankNull()?.let { Phone(raw, it) } ?: Text(raw)
                lower.startsWith("smsto:") ->
                    parseSmsto(raw, t.substring(6))
                lower.startsWith("sms:") ->
                    parseSms(raw, t.substring(4))
                lower.startsWith("mailto:") ->
                    parseMailto(raw, t.substring(7))
                lower.startsWith("matmsg:") ->
                    parseMatmsg(raw, t)
                lower.startsWith("wifi:") ->
                    parseWifi(raw, t.substring(5))
                lower.startsWith("begin:vcard") ->
                    parseVCard(raw, t)
                lower.startsWith("mecard:") ->
                    parseMecard(raw, t.substring(7))
                lower.startsWith("geo:") ->
                    parseGeo(raw, t.substring(4))
                else -> Text(raw)
            }
        }

        /** `SMSTO:number:message` — phần message (sau dấu `:` đầu tiên) là tuỳ chọn. */
        private fun parseSmsto(raw: String, rest: String): QrContent {
            val idx = rest.indexOf(':')
            val number = (if (idx >= 0) rest.substring(0, idx) else rest).trim()
            val body = if (idx >= 0) rest.substring(idx + 1).ifBlankNull() else null
            return number.ifBlankNull()?.let { Sms(raw, it, body) } ?: Text(raw)
        }

        /** `sms:number`, `sms:number?body=...`, hoặc `sms:number:body`. Bỏ tiền tố `//` nếu có. */
        private fun parseSms(raw: String, restIn: String): QrContent {
            val rest = restIn.removePrefix("//")
            val qIdx = rest.indexOf('?')
            val cIdx = rest.indexOf(':')
            // Dạng query (?body=) CHỈ khi '?' đứng TRƯỚC ':' (nếu có ':'), để body dạng `number:body` chứa dấu
            // '?' (vd câu hỏi cuối tin) không bị nuốt số điện thoại.
            if (qIdx >= 0 && (cIdx < 0 || qIdx < cIdx)) {
                val number = rest.substring(0, qIdx).trim()
                val body = parseQuery(rest.substring(qIdx + 1))["body"]
                return number.ifBlankNull()?.let { Sms(raw, it, body) } ?: Text(raw)
            }
            // Dạng `number:body` (body tuỳ chọn, giữ nguyên mọi ký tự kể cả '?').
            val number = (if (cIdx >= 0) rest.substring(0, cIdx) else rest).trim()
            val body = if (cIdx >= 0) rest.substring(cIdx + 1).ifBlankNull() else null
            return number.ifBlankNull()?.let { Sms(raw, it, body) } ?: Text(raw)
        }

        /** `mailto:addr?subject=..&body=..` (addr có thể rỗng nếu chỉ có tham số). */
        private fun parseMailto(raw: String, rest: String): QrContent {
            val qIdx = rest.indexOf('?')
            // ĐỊA CHỈ: giải mã %xx nhưng GIỮ '+' (trong addr-spec RFC 6068 '+' là ký tự thật, vd Gmail
            // plus-addressing john+news@gmail.com) — KHÔNG dùng quy tắc '+'→dấu-cách của query.
            val address = decodeAddress(if (qIdx >= 0) rest.substring(0, qIdx) else rest).trim()
            val query = if (qIdx >= 0) parseQuery(rest.substring(qIdx + 1)) else emptyMap()
            val subject = query["subject"]
            val body = query["body"]
            // Không có địa chỉ lẫn tham số → coi như văn bản thường.
            return if (address.isBlank() && subject == null && body == null) Text(raw)
            else Email(raw, address, subject, body)
        }

        /** Danh thiếp email `MATMSG:TO:addr;SUB:subject;BODY:body;;`. */
        private fun parseMatmsg(raw: String, t: String): QrContent {
            val to = matField(t, "TO").orEmpty().trim()
            val subject = matField(t, "SUB")
            val body = matField(t, "BODY")
            return if (to.isBlank() && subject == null && body == null) Text(raw)
            else Email(raw, to, subject, body)
        }

        /** Đọc giá trị trường `NAME:` trong MATMSG cho tới dấu `;` chưa được escape (`\;`). */
        private fun matField(t: String, name: String): String? {
            val marker = "$name:"
            val start = t.indexOf(marker, ignoreCase = true).takeIf { it >= 0 } ?: return null
            var i = start + marker.length
            val sb = StringBuilder()
            while (i < t.length) {
                val c = t[i]
                if (c == '\\' && i + 1 < t.length) { sb.append(t[i + 1]); i += 2; continue }
                if (c == ';') break
                sb.append(c); i++
            }
            return sb.toString().ifBlankNull()
        }

        /** `WIFI:S:ssid;T:WPA;P:pass;H:true;;` — thứ tự trường tuỳ ý; giá trị escape bằng `\`. Không có SSID → văn bản. */
        private fun parseWifi(raw: String, body: String): QrContent {
            val f = parseSemicolonFields(body)
            val ssid = f["S"]?.trim().orEmpty()
            if (ssid.isBlank()) return Text(raw)
            return Wifi(
                raw = raw,
                ssid = ssid,
                password = f["P"]?.ifBlankNull(),
                security = f["T"]?.ifBlankNull(),
                hidden = f["H"]?.equals("true", ignoreCase = true) == true
            )
        }

        /** `MECARD:N:Họ,Tên;TEL:...;EMAIL:...;ORG:...;;` — giá trị escape bằng `\`. Rỗng hết → văn bản. */
        private fun parseMecard(raw: String, body: String): QrContent {
            val f = parseSemicolonFields(body)
            val name = f["N"]?.let(::mecardName)?.ifBlankNull()
            val phone = f["TEL"]?.ifBlankNull()
            val email = f["EMAIL"]?.ifBlankNull()
            val org = f["ORG"]?.ifBlankNull()
            return if (name == null && phone == null && email == null && org == null) Text(raw)
            else Contact(raw, name, phone, email, org)
        }

        /** MECARD `N:Họ,Tên` → "Tên Họ"; không có dấu phẩy thì giữ nguyên. */
        private fun mecardName(n: String): String {
            val parts = n.split(',')
            return if (parts.size >= 2) "${parts[1].trim()} ${parts[0].trim()}".trim() else n.trim()
        }

        /** vCard theo DÒNG: lấy FN (hoặc dựng từ N), TEL/EMAIL/ORG đầu tiên. Rỗng hết → văn bản. */
        private fun parseVCard(raw: String, t: String): QrContent {
            var fn: String? = null
            var n: String? = null
            var phone: String? = null
            var email: String? = null
            var org: String? = null
            for (line in unfoldVCardLines(t)) {
                val ci = line.indexOf(':')
                if (ci <= 0) continue
                val head = line.substring(0, ci)
                val rawValue = line.substring(ci + 1).trim()
                if (rawValue.isBlank()) continue
                // Tên thuộc tính: bỏ tham số (`;…`) VÀ tiền tố nhóm (`group.NAME`, vd Apple `item1.TEL`).
                val base = head.substringBefore(';').substringAfterLast('.').uppercase().trim()
                val value = decodeVCardValue(rawValue, head)
                when (base) {
                    "FN" -> if (fn == null) fn = value
                    // N: Họ;Tên;Đệm;… → ghép "Tên … Họ" (đảo để ra thứ tự đọc tự nhiên).
                    "N" -> if (n == null) n = value.split(';').map { it.trim() }.filter { it.isNotEmpty() }.reversed().joinToString(" ").ifBlank { null }
                    "TEL" -> if (phone == null) phone = value
                    "EMAIL" -> if (email == null) email = value
                    "ORG" -> if (org == null) org = value.substringBefore(';').trim()
                }
            }
            val name = fn ?: n
            return if (name == null && phone == null && email == null && org == null) Text(raw)
            else Contact(raw, name, phone, email, org)
        }

        /**
         * Gộp DÒNG vCard thành các dòng LOGIC:
         *  - Dòng bắt đầu bằng khoảng trắng → gấp dòng RFC 6350 (nối tiếp dòng trước).
         *  - Dòng logic trước là QUOTED-PRINTABLE và kết thúc `=` → ngắt mềm QP (nối, bỏ `=`). Giới hạn ở QP để
         *    không nuốt nhầm dòng sau phần base64 (PHOTO) kết thúc bằng `=` đệm.
         */
        private fun unfoldVCardLines(t: String): List<String> {
            val out = ArrayList<String>()
            for (physRaw in t.split('\n')) {
                val phys = physRaw.trimEnd('\r')
                when {
                    out.isNotEmpty() && (phys.startsWith(" ") || phys.startsWith("\t")) ->
                        out[out.lastIndex] = out.last() + phys.substring(1)
                    out.isNotEmpty() && out.last().endsWith("=") &&
                        out.last().contains("QUOTED-PRINTABLE", ignoreCase = true) ->
                        out[out.lastIndex] = out.last().dropLast(1) + phys.trim()
                    else -> out.add(phys.trim())
                }
            }
            return out
        }

        /** Giải mã giá trị theo tham số ENCODING/CHARSET trong [head] (chỉ QUOTED-PRINTABLE); khác → giữ nguyên. */
        private fun decodeVCardValue(value: String, head: String): String {
            if (!head.contains("QUOTED-PRINTABLE", ignoreCase = true)) return value
            val charsetName = Regex("""CHARSET=([^;:]+)""", RegexOption.IGNORE_CASE).find(head)?.groupValues?.get(1)?.trim()
            val charset = runCatching { if (charsetName != null) charset(charsetName) else Charsets.UTF_8 }
                .getOrDefault(Charsets.UTF_8)
            return decodeQuotedPrintable(value, charset)
        }

        /** Giải mã Quoted-Printable: `=XX` → byte (hex), còn lại giữ; rồi dựng chuỗi theo [charset]. */
        private fun decodeQuotedPrintable(s: String, charset: Charset): String {
            val bytes = ArrayList<Byte>(s.length)
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '=' && i + 2 < s.length) {
                    val v = s.substring(i + 1, i + 3).toIntOrNull(16)
                    if (v != null) { bytes.add(v.toByte()); i += 3; continue }
                }
                bytes.add(c.code.toByte()); i++
            }
            return runCatching { String(bytes.toByteArray(), charset) }.getOrDefault(s)
        }

        /**
         * `geo:lat,lng`, `geo:lat,lng?q=…`, `geo:0,0?q=lat,lng(nhãn)`, `geo:0,0?q=địa chỉ`, `geo:lat,lng;u=…`.
         * Ưu tiên toạ độ trong `q=` (chuẩn Google/Android hay đặt toạ độ THẬT ở đó với đế `geo:0,0`), rồi mới
         * tới toạ độ ở phần đường dẫn `geo:`. Không có toạ độ số ở đâu cả:
         *  - `q=` là địa chỉ chữ → giữ làm [Geo.query] (để app bản đồ geocode), toạ độ đế `0,0`.
         *  - không có gì dùng được → [Text].
         */
        private fun parseGeo(raw: String, body: String): QrContent {
            val qVal = when {
                body.contains("?q=") -> body.substringAfter("?q=").substringBefore('&')
                body.contains("&q=") -> body.substringAfter("&q=").substringBefore('&')
                else -> ""
            }
            val label = Regex("""\(([^)]*)\)""").find(qVal)?.groupValues?.get(1)?.let { decode(it) }?.ifBlankNull()
            val qCoords = decode(qVal.substringBefore('(')).trim()
            val path = firstLatLng(body.substringBefore('?').substringBefore(';'))

            // 1) Toạ độ SỐ trong q= (dạng `geo:0,0?q=lat,lng(nhãn)`) → tin cậy nhất.
            firstLatLng(qCoords)?.let { (lat, lng) -> return Geo(raw, lat, lng, label, null) }
            // 2) Toạ độ path THẬT (khác đảo 0,0) → dùng path, nhãn lấy từ q nếu có.
            if (path != null && !isNullIsland(path.first, path.second)) {
                return Geo(raw, path.first, path.second, label, null)
            }
            // 3) q là ĐỊA CHỈ chữ (path chỉ là 0,0 đế) → giữ để app bản đồ geocode.
            qCoords.ifBlankNull()?.let { return Geo(raw, "0", "0", label, it) }
            // 4) Chỉ có path 0,0 (mã đúng là 0,0) → dùng luôn; không gì dùng được → văn bản.
            return if (path != null) Geo(raw, path.first, path.second, label, null) else Text(raw)
        }

        /** Lấy cặp lat,lng ĐẦU của chuỗi nếu cả hai là số hợp lệ; ngược lại null (bỏ qua thành phần độ cao nếu có). */
        private fun firstLatLng(s: String): Pair<String, String>? {
            val parts = s.split(',')
            if (parts.size < 2) return null
            val lat = parts[0].trim()
            val lng = parts[1].trim()
            return if (lat.toDoubleOrNull() != null && lng.toDoubleOrNull() != null) lat to lng else null
        }

        /** true nếu toạ độ là đảo 0,0 — đế placeholder của dạng `geo:0,0?q=…`, không phải vị trí thật. */
        private fun isNullIsland(lat: String, lng: String): Boolean =
            lat.toDoubleOrNull() == 0.0 && lng.toDoubleOrNull() == 0.0

        /** Tách các trường `KEY:value` phân tách bởi `;` (WIFI/MECARD), tôn trọng escape `\`; khoá viết HOA, lấy lần đầu. */
        private fun parseSemicolonFields(body: String): Map<String, String> {
            val result = LinkedHashMap<String, String>()
            for (seg in splitUnescaped(body, ';')) {
                if (seg.isEmpty()) continue
                val ci = indexOfUnescaped(seg, ':')
                if (ci < 0) continue
                val key = seg.substring(0, ci).trim().uppercase()
                if (key.isEmpty() || key in result) continue
                result[key] = unescapeBackslash(seg.substring(ci + 1))
            }
            return result
        }

        /** Tách [s] theo [sep] KHÔNG bị escape bởi `\` (giữ nguyên `\` trong mảnh để giải escape sau). */
        private fun splitUnescaped(s: String, sep: Char): List<String> {
            val out = ArrayList<String>()
            val sb = StringBuilder()
            var i = 0
            while (i < s.length) {
                val c = s[i]
                when {
                    c == '\\' && i + 1 < s.length -> { sb.append(c).append(s[i + 1]); i += 2 }
                    c == sep -> { out.add(sb.toString()); sb.setLength(0); i++ }
                    else -> { sb.append(c); i++ }
                }
            }
            out.add(sb.toString())
            return out
        }

        /** Vị trí [ch] đầu tiên KHÔNG bị escape; -1 nếu không có. */
        private fun indexOfUnescaped(s: String, ch: Char): Int {
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) { i += 2; continue }
                if (c == ch) return i
                i++
            }
            return -1
        }

        /** Bỏ dấu `\` escape: `\x` → `x`. */
        private fun unescapeBackslash(s: String): String {
            if ('\\' !in s) return s
            val sb = StringBuilder()
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) { sb.append(s[i + 1]); i += 2 } else { sb.append(c); i++ }
            }
            return sb.toString()
        }

        /** Tách chuỗi query `k=v&k2=v2` → map (khoá viết thường), giá trị đã giải mã. Bỏ khoá rỗng. */
        private fun parseQuery(q: String): Map<String, String> =
            q.split('&').mapNotNull { part ->
                val i = part.indexOf('=')
                if (i <= 0) null else part.substring(0, i).lowercase() to decode(part.substring(i + 1))
            }.toMap()

        /** Giải mã %xx/`+` theo UTF-8 (dùng cho THAM SỐ query subject/body, nơi '+' = dấu cách). Hỏng → nguyên chuỗi. */
        private fun decode(s: String): String =
            runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)

        /** Giải mã %xx cho ĐỊA CHỈ email: bảo toàn '+' (đổi tạm sang %2B trước khi decode). Hỏng → nguyên chuỗi. */
        private fun decodeAddress(s: String): String =
            runCatching { URLDecoder.decode(s.replace("+", "%2B"), "UTF-8") }.getOrDefault(s)

        private fun String.ifBlankNull(): String? = ifBlank { null }
    }
}
