package com.antimobile.callhs.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * BỘ MÀU (palette) cho hai chế độ SÁNG / TỐI.
 *
 * Toàn app đọc màu qua các token top-level trong [Color.kt] (vd `Primary`, `TextPrimary`…). Mỗi token đó là
 * một property getter trả về trường tương ứng của palette ĐANG áp ([ThemeSettings.colors]) — nên KHÔNG cần
 * đổi ~278 chỗ dùng: chỉ cần palette đổi là cả app tự tô lại (đọc snapshot-state ở [ThemeSettings] → recompose).
 *
 * ── Nguyên tắc thiết kế bản TỐI (đối lập & hài hoà với bản SÁNG đang rất đẹp) ──
 *  1. KHÔNG dùng đen tuyền: nền trang [appBackground] là xám-đen trung tính (#0E1113); THẺ [cardSurface]
 *     SÁNG HƠN nền (#1B1E20) để "nổi khối" — đúng cách Material dark thể hiện độ cao (ngược bản sáng: thẻ trắng
 *     nổi nhờ bóng, ở tối thì nổi nhờ bề mặt sáng hơn).
 *  2. Chữ KHÔNG trắng tuyền (giảm chói): chính [textPrimary] #E8EAEB (~13:1), phụ [textSecondary] #A3A9AF (~7:1).
 *  3. Màu NHẤN (xanh lá/xanh dương/đỏ/hổ phách) được LÀM SÁNG cho nền tối vì chúng CHỈ dùng làm chữ/icon trên nền
 *     tối hoặc trên nền-nhạt *_Bg (đã kiểm: không có nút ĐẶC màu-nhấn chữ trắng) → sáng lên cho đủ tương phản
 *     (chữ ≥4.5:1, đồ hoạ ≥3:1) mà không chói. Các nền-nhạt *_Bg thành SẮC TỐI cùng tông (vd xanh lá rất nhạt →
 *     xanh lá đậm tối) để icon/chữ nhấn nổi rõ.
 *  4. [primary] xanh lá thương hiệu #34A853 GIỮ NGUYÊN ở cả hai chế độ: nó là màu ĐẶC của FAB/pill-tab-chọn/
 *     avatar-có-tên/Switch với chữ-icon TRẮNG — giữ đúng nhận diện; tương phản trắng/nền y như bản sáng.
 *
 * Số liệu tương phản (xấp xỉ, nền thẻ tối #1B1E20): textPrimary ~13:1, textSecondary ~7:1, accentGreen ~8.5:1,
 * accentRed ~6:1, accentBlue ~6.4:1, softGreen ~6.2:1, softRed ~6.1:1 — đều đạt WCAG AA cho chữ.
 */
class Palette(
    // Nền & bề mặt
    val appBackground: Color,
    val cardSurface: Color,
    val cardFill: Color,
    val cardShadow: Color,
    val fieldSurface: Color,
    val dividerColor: Color,
    val scrollTopScrim: Color,
    val scrollTopSolid: Color,
    val hairlineBorder: Color,
    // Chữ
    val textPrimary: Color,
    val textSecondary: Color,
    // Chủ đạo (xanh lá) + liên kết
    val primary: Color,
    val linkColor: Color,
    val brandSoft: Color,
    // Loại cuộc gọi
    val callOutgoing: Color,
    val callIncoming: Color,
    val callMissed: Color,
    // Accent + nền nhạt
    val accentGreen: Color,
    val accentGreenBg: Color,
    val accentBlue: Color,
    val accentBlueBg: Color,
    val accentRed: Color,
    val accentRedBg: Color,
    val accentAmber: Color,
    val accentAmberBg: Color,
    val accentGray: Color,
    val accentGrayBg: Color,
    // Tone nhạt cho line 2 / chi tiết
    val softGreen: Color,
    val softRed: Color,
    // Huy hiệu SIM
    val simBadgeBg: Color,
    val simBadgeText: Color,
    // Tab lọc hàng 1
    val tabSelectedBg: Color,
    val tabSelectedText: Color,
    val tabText: Color,
    // Chip lọc hàng 2
    val chipSelectedBg: Color,
    val chipSelectedText: Color,
    val chipBg: Color,
    val chipText: Color,
    // Ánh xạ Material
    val onPrimaryContainer: Color,
    // Token thành phần dùng chung (avatar chưa lưu, tay cầm bottom sheet)
    val avatarGray: Color,
    val sheetHandle: Color
)

/**
 * Bản SÁNG — GIỮ NGUYÊN đúng bộ màu gốc đang được người dùng khen đẹp (chỉ chuyển từ hằng số sang palette).
 */
val LightPalette = Palette(
    appBackground = Color(0xFFFFFFFF),
    cardSurface = Color(0xFFFFFFFF),
    cardFill = Color(0xFFF4F6FA),
    cardShadow = Color(0x22101828),
    fieldSurface = Color(0xFFF1F3F4),
    dividerColor = Color(0xFFEDEFF2),
    scrollTopScrim = Color(0x262E3033), // kính mờ rất nhẹ khi Android 12+ hỗ trợ blur
    scrollTopSolid = Color(0x4D3C4043), // fallback trong suốt 30% cho Android 10–11
    hairlineBorder = Color(0xFFE6E8EB),
    textPrimary = Color(0xFF1F1F1F),
    textSecondary = Color(0xFF5F6368),
    primary = Color(0xFF34A853),
    linkColor = Color(0xFF1E8E3E),
    brandSoft = Color(0xFFE7F6EB),
    callOutgoing = Color(0xFF1E8E3E),
    callIncoming = Color(0xFF1A73E8),
    callMissed = Color(0xFFEA4335),
    accentGreen = Color(0xFF1E8E3E),
    accentGreenBg = Color(0xFFE7F6EB),
    accentBlue = Color(0xFF1A73E8),
    accentBlueBg = Color(0xFFE8F0FE),
    accentRed = Color(0xFFEA4335),
    accentRedBg = Color(0xFFFDECEA),
    accentAmber = Color(0xFFE8710A),
    accentAmberBg = Color(0xFFFEF3E0),
    accentGray = Color(0xFF5F6368),
    accentGrayBg = Color(0xFFEDEFF2),
    softGreen = Color(0xFF34A853),
    softRed = Color(0xFFF0705F),
    simBadgeBg = Color(0xFFE7F6EB),
    simBadgeText = Color(0xFF1E8E3E),
    tabSelectedBg = Color(0xFF34A853),
    tabSelectedText = Color(0xFFFFFFFF),
    tabText = Color(0xFF5F6368),
    chipSelectedBg = Color(0xFFE7F6EB),
    chipSelectedText = Color(0xFF1E8E3E),
    chipBg = Color(0xFFF4F6FA),
    chipText = Color(0xFF5F6368),
    onPrimaryContainer = Color(0xFF0B3D1E),
    avatarGray = Color(0xFFCFD8DC),
    sheetHandle = Color(0xFFDADCE0)
)

/**
 * Bản TỐI — thiết kế đối lập & hài hoà (xem chú thích đầu file). Nền xám-đen, thẻ sáng hơn để nổi khối,
 * chữ ngà, màu nhấn làm sáng cho đủ tương phản, các nền *_Bg thành sắc tối cùng tông. Xanh lá thương hiệu
 * [primary] giữ nguyên #34A853.
 */
val DarkPalette = Palette(
    appBackground = Color(0xFF0E1113),   // nền trang: xám-đen trung tính (không đen tuyền)
    cardSurface = Color(0xFF1B1E20),     // thẻ: SÁNG HƠN nền → nổi khối
    cardFill = Color(0xFF282C2F),        // chip nhỏ tô nhẹ trên thẻ
    cardShadow = Color(0x33000000),      // bóng gần như vô hình trên nền tối (độ cao do bề mặt sáng thể hiện)
    fieldSurface = Color(0xFF2A2E31),    // ô tìm kiếm / nút tròn trung tính
    dividerColor = Color(0xFF2E3236),    // vạch chia mảnh, đủ thấy trên thẻ tối
    scrollTopScrim = Color(0x302A2D30),  // kính mờ nhẹ, không tạo mảng tối đặc
    scrollTopSolid = Color(0x4D2A2D30),  // fallback trong suốt 30% cho Android 10–11
    hairlineBorder = Color(0xFF343A3F),
    textPrimary = Color(0xFFE8EAEB),     // chữ chính: ngà (không trắng tuyền) ~13:1
    textSecondary = Color(0xFFA3A9AF),   // chữ phụ ~7:1
    primary = Color(0xFF34A853),         // GIỮ NGUYÊN xanh lá thương hiệu (nút đặc + chữ trắng)
    linkColor = Color(0xFF5FD07E),       // liên kết/chữ nhấn: xanh lá SÁNG cho nền tối
    brandSoft = Color(0xFF16351F),       // nền xanh lá đậm-tối (primaryContainer)
    callOutgoing = Color(0xFF5FD07E),
    callIncoming = Color(0xFF5AA2FF),
    callMissed = Color(0xFFFF6B60),
    accentGreen = Color(0xFF5FD07E),     // kết nối (line1) — xanh lá sáng, đậm sắc
    accentGreenBg = Color(0xFF16351F),
    accentBlue = Color(0xFF5AA2FF),      // nhắn tin/đến/video/VoLTE — xanh dương sáng
    accentBlueBg = Color(0xFF152D49),
    accentRed = Color(0xFFFF6B60),       // nhỡ/thất bại (line1) — đỏ san hô sáng
    accentRedBg = Color(0xFF3A1E1B),
    accentAmber = Color(0xFFF2A24B),     // ưu tiên/cảnh báo — hổ phách sáng
    accentAmberBg = Color(0xFF3A2A12),
    accentGray = Color(0xFF9BA1A7),
    accentGrayBg = Color(0xFF2A2E31),
    softGreen = Color(0xFF49B26E),       // kết nối (line2) — xanh lá DỊU hơn accentGreen
    softRed = Color(0xFFE0837A),         // không phản hồi (line2) — đỏ san hô DỊU hơn accentRed
    simBadgeBg = Color(0xFF16351F),
    simBadgeText = Color(0xFF5FD07E),
    tabSelectedBg = Color(0xFF34A853),   // pill tab chọn = xanh lá đặc (như bản sáng)
    tabSelectedText = Color(0xFFFFFFFF),
    tabText = Color(0xFFA3A9AF),
    chipSelectedBg = Color(0xFF16351F),
    chipSelectedText = Color(0xFF5FD07E),
    chipBg = Color(0xFF282C2F),
    chipText = Color(0xFFA3A9AF),
    onPrimaryContainer = Color(0xFF9FE7B6), // chữ xanh lá SÁNG trên nền primaryContainer tối
    avatarGray = Color(0xFF3B444B),      // avatar số chưa lưu: xám xanh tối, icon/chữ trắng nổi rõ
    sheetHandle = Color(0xFF3C4247)      // tay cầm bottom sheet, thấy rõ trên nền sheet tối
)
