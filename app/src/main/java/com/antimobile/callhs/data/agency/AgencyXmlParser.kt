package com.antimobile.callhs.data.agency

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Đọc danh bạ cơ quan từ luồng `.xml` (bản tải từ GitHub hoặc bản cache tại máy) bằng
 * [XmlPullParser] (không biên dịch, đọc lúc chạy). Gọi ở luồng nền (parse ~vài trăm mục).
 *
 * Cấu trúc file:
 * ```
 * <agencies province="TP. Hồ Chí Minh" key="hcm" branch="police"
 *           category="Cơ quan công an (Công an phường/xã)" updated="2026-07-06" count="168">
 *   <emergency police="113" fire="114" medical="115" note="..."/>
 *   <source>...</source>
 *   <disclaimer>...</disclaimer>
 *   <agency id="DV001" ward="Phường Tân Định" name="Công an Phường Tân Định">
 *     <address>62 Bà Lê Chân, phường Tân Định</address>
 *     <phone confidence="high" source="...">02838298927</phone>   <!-- có thể nhiều số / rỗng + fallback="113" -->
 *     <facebook>https://facebook.com/...</facebook>
 *     <maps>https://www.google.com/maps/search/?api=1&amp;query=...</maps>
 *   </agency>
 * </agencies>
 * ```
 */
object AgencyXmlParser {

    fun parse(dataset: AgencyDataset, input: InputStream): AgencyData {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, "UTF-8")
        return read(dataset, parser)
    }

    private fun read(dataset: AgencyDataset, parser: XmlPullParser): AgencyData {
        var provinceName = dataset.province.displayName
        var category = dataset.category
        var updated: String? = null
        var source: String? = null
        var disclaimer: String? = null
        var emergency: Emergency? = null
        val agencies = ArrayList<Agency>()
        // Số thứ tự tăng dần → ghép vào id để đảm bảo id DUY NHẤT + KHÔNG rỗng (dữ liệu tải về có thể
        // có id gốc trùng hoặc rỗng; nếu để nguyên sẽ làm key của LazyColumn/rememberSaveable đụng nhau → crash).
        var seq = 0

        // Trạng thái cơ quan đang đọc.
        var id = ""
        var name = ""
        var ward = ""
        var address = ""
        var fallback: String? = null
        var maps: String? = null
        var facebook: String? = null
        val phones = ArrayList<String>()
        // Bộ đệm gom text cho các thẻ dạng text (text có thể tách nhiều mảnh).
        val text = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "agencies" -> {
                        parser.getAttributeValue(null, "province")?.let { if (it.isNotBlank()) provinceName = it.trim() }
                        parser.getAttributeValue(null, "category")?.let { if (it.isNotBlank()) category = it.trim() }
                        updated = parser.getAttributeValue(null, "updated")?.trim()
                    }
                    "emergency" -> emergency = Emergency(
                        police = parser.getAttributeValue(null, "police")?.trim(),
                        fire = parser.getAttributeValue(null, "fire")?.trim(),
                        medical = parser.getAttributeValue(null, "medical")?.trim(),
                        note = parser.getAttributeValue(null, "note")?.trim()
                    )
                    "agency" -> {
                        id = parser.getAttributeValue(null, "id").orEmpty().trim()
                        name = parser.getAttributeValue(null, "name").orEmpty().trim()
                        ward = parser.getAttributeValue(null, "ward").orEmpty().trim()
                        address = ""
                        fallback = null
                        maps = null
                        facebook = null
                        phones.clear()
                    }
                    "phone" -> {
                        text.setLength(0)
                        parser.getAttributeValue(null, "fallback")?.trim()?.let { if (it.isNotEmpty()) fallback = it }
                    }
                    "source", "disclaimer", "address", "maps", "facebook" -> text.setLength(0)
                }
                XmlPullParser.TEXT -> text.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "source" -> source = text.toString().trim().ifBlank { null }
                    "disclaimer" -> disclaimer = text.toString().trim().ifBlank { null }
                    "address" -> address = text.toString().trim()
                    "maps" -> maps = text.toString().trim().ifBlank { null }
                    "facebook" -> facebook = text.toString().trim().ifBlank { null }
                    "phone" -> text.toString().trim().let { if (it.isNotEmpty()) phones.add(it) }
                    "agency" -> if (name.isNotEmpty() || ward.isNotEmpty() || phones.isNotEmpty()) {
                        val display = name.ifBlank { ward }
                        agencies.add(
                            Agency(
                                id = "${seq++}:$id",
                                name = display,
                                ward = ward,
                                address = address,
                                phones = phones.toList(),
                                fallbackPhone = if (phones.isEmpty()) fallback else null,
                                maps = maps,
                                facebook = facebook,
                                searchText = normalizeForSearch("$display $ward $address")
                            )
                        )
                    }
                }
            }
            event = parser.next()
        }
        return AgencyData(dataset, provinceName, category, updated, source, disclaimer, emergency, agencies)
    }
}
