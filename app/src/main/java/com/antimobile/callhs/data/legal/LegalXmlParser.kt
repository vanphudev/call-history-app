package com.antimobile.callhs.data.legal

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Đọc `legal.xml` (nội dung Chính sách quyền riêng tư + Điều khoản) từ luồng `.xml`. Cùng file này website
 * cũng đọc bằng JS → app & web hiển thị đồng nhất.
 *
 * Cấu trúc:
 * ```
 * <legal updated="2026-07-07">
 *   <document id="privacy" eyebrow="Privacy Policy" title="Chính sách bảo mật" updated="2026-07-06">
 *     <intro>...</intro>
 *     <section title="1. Tóm tắt">
 *       <p>...</p>
 *       <h3>...</h3>
 *       <callout>...</callout>
 *       <list><item>...</item><item>...</item></list>
 *     </section>
 *   </document>
 *   <document id="terms" ...>...</document>
 * </legal>
 * ```
 */
object LegalXmlParser {

    fun parse(input: InputStream): List<LegalDocument> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, "UTF-8")
        return read(parser)
    }

    private fun read(parser: XmlPullParser): List<LegalDocument> {
        val documents = ArrayList<LegalDocument>()

        // Trạng thái document đang đọc.
        var id = ""
        var eyebrow = ""
        var title = ""
        var updated = ""
        var intro = ""
        var sections = ArrayList<LegalSection>()
        // Trạng thái section đang đọc.
        var heading = ""
        var blocks = ArrayList<LegalBlock>()
        // Trạng thái list đang đọc.
        var items = ArrayList<String>()

        val text = StringBuilder()

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "document" -> {
                        id = parser.getAttributeValue(null, "id").orEmpty().trim()
                        eyebrow = parser.getAttributeValue(null, "eyebrow").orEmpty().trim()
                        title = parser.getAttributeValue(null, "title").orEmpty().trim()
                        updated = parser.getAttributeValue(null, "updated").orEmpty().trim()
                        intro = ""
                        sections = ArrayList()
                    }
                    "section" -> {
                        heading = parser.getAttributeValue(null, "title").orEmpty().trim()
                        blocks = ArrayList()
                    }
                    "list" -> items = ArrayList()
                    "intro", "p", "h3", "callout", "item" -> text.setLength(0)
                }
                XmlPullParser.TEXT -> text.append(parser.text)
                XmlPullParser.END_TAG -> when (parser.name) {
                    "intro" -> intro = text.toString().trim()
                    "p" -> text.toString().trim().let { if (it.isNotEmpty()) blocks.add(LegalBlock.Paragraph(it)) }
                    "h3" -> text.toString().trim().let { if (it.isNotEmpty()) blocks.add(LegalBlock.SubHeading(it)) }
                    "callout" -> text.toString().trim().let { if (it.isNotEmpty()) blocks.add(LegalBlock.Callout(it)) }
                    "item" -> text.toString().trim().let { if (it.isNotEmpty()) items.add(it) }
                    "list" -> if (items.isNotEmpty()) blocks.add(LegalBlock.Bullets(items.toList()))
                    "section" -> if (heading.isNotEmpty() || blocks.isNotEmpty()) {
                        sections.add(LegalSection(heading, blocks.toList()))
                    }
                    "document" -> if (id.isNotEmpty()) {
                        documents.add(LegalDocument(id, eyebrow, title, intro, updated, sections.toList()))
                    }
                }
            }
            event = parser.next()
        }
        return documents
    }
}
