package com.antimobile.mcas.data.legal

/** Một khối nội dung trong 1 mục pháp lý. */
sealed interface LegalBlock {
    data class SubHeading(val text: String) : LegalBlock
    data class Paragraph(val text: String) : LegalBlock
    data class Callout(val text: String) : LegalBlock
    data class Bullets(val items: List<String>) : LegalBlock
}

/** Một MỤC (section) đánh số, gồm tiêu đề + danh sách khối nội dung. */
data class LegalSection(
    val heading: String,
    val blocks: List<LegalBlock>
)

/**
 * Một TÀI LIỆU pháp lý ([id] = "privacy" | "terms") — nội dung tải từ `legal.xml` trên GitHub, dùng CHUNG
 * cho cả app lẫn website để hai bên luôn đồng nhất.
 */
data class LegalDocument(
    val id: String,
    val eyebrow: String,
    val title: String,
    val intro: String,
    val updated: String,
    val sections: List<LegalSection>
)

/** Id tài liệu — khớp với thuộc tính `id` trong `legal.xml` và route trong app. */
const val LEGAL_ID_PRIVACY = "privacy"
const val LEGAL_ID_TERMS = "terms"
