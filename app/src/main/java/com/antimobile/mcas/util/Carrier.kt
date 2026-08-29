package com.antimobile.mcas.util

/**
 * Suy ra nhà mạng từ đầu số điện thoại di động Việt Nam (chuẩn 10 số sau 2018).
 *
 * Bảng [table] đã được tra cứu & đối chiếu nhiều nguồn (Wikipedia "Viễn thông Việt Nam",
 * fptshop, cellphones, genk, vietnamnet — cập nhật 07/2026). Gồm 5 nhà mạng chính + các
 * mạng di động ảo (MVNO). BẠN CÓ THỂ TỰ BỔ SUNG đầu số mới vào [table] — hỗ trợ cả đầu số
 * 3 chữ số (nhà mạng chính) lẫn 4 chữ số (mạng ảo dùng chung dải, ví dụ 0775/0777 nằm trong
 * dải 077 của MobiFone). Khi tra cứu, đầu số DÀI hơn (4 số) được ưu tiên khớp trước đầu số 3 số.
 *
 * Lưu ý mạng ảo (MVNO) và mạng chủ:
 * - iTel (087) chạy trên hạ tầng VinaPhone.
 * - Wintel (055, trước là Reddi/Mobicast → Masan) chạy trên VinaPhone.
 * - VNSKY (0777, VNPAY) và FPT Mobile (0775, FPT Retail) chạy trên MobiFone (dải 077).
 * - Local (ASIM Telecom) dùng dải 089 của MobiFone → tra theo 3 số sẽ ra "MobiFone" (chấp nhận được,
 *   vì không có dải con 4 số công bố để tách riêng Local).
 */
object Carrier {

    private val table: List<Pair<String, List<String>>> = listOf(
        // ----- Nhà mạng chính (MNO) -----
        "Viettel" to listOf("032", "033", "034", "035", "036", "037", "038", "039", "086", "096", "097", "098"),
        "MobiFone" to listOf("070", "076", "077", "078", "079", "089", "090", "093"),
        "VinaPhone" to listOf("081", "082", "083", "084", "085", "088", "091", "094"),
        "Vietnamobile" to listOf("052", "056", "058", "092"),
        "Gmobile" to listOf("059", "099"),
        // ----- Mạng di động ảo (MVNO) -----
        "iTel" to listOf("087"),        // trên VinaPhone
        "Wintel" to listOf("055"),      // trên VinaPhone (trước là Reddi)
        "VNSKY" to listOf("0777"),      // trên MobiFone (dải con của 077)
        "FPT" to listOf("0775")         // FPT Mobile, trên MobiFone (dải con của 077)
        // Bổ sung đầu số/nhà mạng mới ở đây, ví dụ: "Tên mạng" to listOf("0xx", ...)
    )

    /** Danh sách nhà mạng mà app có dữ liệu nhận diện; dùng chung cho bộ lọc/chặn theo nhà mạng. */
    val names: List<String> = table.map { it.first }

    // Phẳng hoá thành (đầu số -> nhà mạng), ưu tiên đầu số DÀI hơn khi khớp (4 số trước 3 số).
    private val byPrefix: List<Pair<String, String>> = table
        .flatMap { (name, prefixes) -> prefixes.map { it to name } }
        .sortedByDescending { it.first.length }

    /** Trả về tên nhà mạng, hoặc null nếu không nhận diện được (số lạ/cố định/quốc tế). */
    fun of(number: String?): String? {
        if (number.isNullOrBlank()) return null
        val d = normalize(number)
        if (d.length < 3) return null
        return byPrefix.firstOrNull { d.startsWith(it.first) }?.second
    }

    /** Đưa về dạng nội địa "0xxxxxxxxx": bỏ ký tự thừa, bỏ "00" quốc tế, đổi +84/84 → 0, thêm 0 cho NSN trần. */
    private fun normalize(number: String): String {
        var d = number.filter(Char::isDigit)
        if (d.length > 2 && d.startsWith("00")) d = d.substring(2)           // mã truy cập quốc tế
        when {
            d.startsWith("84") && d.length >= 11 -> d = "0" + d.substring(2) // +84/84 → 0
            d.length == 9 -> d = "0$d"                                       // NSN di động trần (thiếu số 0)
        }
        return d
    }
}
