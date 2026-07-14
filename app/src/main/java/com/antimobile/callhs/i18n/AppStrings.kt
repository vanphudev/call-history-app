package com.antimobile.callhs.i18n

import java.time.DayOfWeek

/**
 * HỢP ĐỒNG CHUỖI HIỂN THỊ của toàn app — nền tảng ĐA NGÔN NGỮ (tiếng Việt / English).
 *
 * Vì sao TABLE KIỂU KOTLIN (không dùng strings.xml trong res/values theo locale): app 100% Compose và đã tự quản các
 * cài đặt hiển thị bằng singleton + snapshot-state (xem [com.antimobile.callhs.util.FontScaleSettings]).
 * Bảng chuỗi có KIỂU cho phép ĐỔI NGÔN NGỮ TỨC THÌ (recompose, không khởi động lại Activity) y như cách
 * đổi cỡ chữ, kèm an toàn biên dịch (thiếu chuỗi = lỗi compile) và tham số hoá tự nhiên bằng hàm.
 *
 * Tách theo NHÓM để MIGRATE DẦN từng màn: mỗi màn/mảng dùng một interface con. Hiện đã phủ CallListScreen
 * cùng các thành phần & tiện ích trong luồng render của nó ([common]/[datetime]/[callStatus]/[emergency]/
 * [callList]) và màn chọn ngôn ngữ ([language]). Màn khác bổ sung interface con mới rồi hiện thực ở
 * [ViStrings]/[EnStrings].
 *
 * Đọc chuỗi hiện hành qua [appStrings] (đọc snapshot-state ở [LanguageSettings]): gọi TRONG @Composable sẽ
 * TỰ recompose khi đổi ngôn ngữ; gọi ngoài compose (vd toast) trả về giá trị hiện tại.
 */
interface AppStrings {
    val common: CommonStrings
    val datetime: DateTimeStrings
    val callStatus: CallStatusStrings
    val emergency: EmergencyStrings
    val callList: CallListStrings
    val callDetail: CallDetailStrings
    val allCalls: AllCallsStrings
    val timeline: TimelineStrings
    val contacts: ContactsStrings
    val settings: SettingsStrings
    val fontSize: FontSizeStrings
    val repeatStats: RepeatStatsStrings
    val detailedStats: DetailedStatsStrings
    val costStats: CostStatsStrings
    val phoneStats: PhoneStatsStrings
    val myNumber: MyNumberStrings
    val qr: QrStrings
    val qrHistory: QrHistoryStrings
    val templates: TemplatesStrings
    val templateEditor: TemplateEditorStrings
    val legal: LegalStrings
    val agency: AgencyStrings
    val shareSheet: ShareSheetStrings
    val qrScanner: QrScannerStrings
    val qrAction: QrActionStrings
    val permission: PermissionStrings
    val updateNotice: UpdateNoticeStrings
    val actions: ActionStrings
    val language: LanguageStrings
    val theme: ThemeStrings
    val category: CategoryStrings
    val donate: DonateStrings
}

/** Chuỗi DÙNG CHUNG nhiều màn: nhãn điều hướng, nút quyền, và toast của [com.antimobile.callhs.util.CallActions]. */
interface CommonStrings {
    val back: String
    val settings: String
    val contacts: String
    val search: String
    val openSettings: String
    val grantPermission: String
    val allowAccess: String
    val dismiss: String
    val cancel: String
    /** Nhãn thay cho SỐ bị ẩn/giấu (private/restricted). */
    val hiddenNumber: String

    // Toast (mở app hệ thống thất bại / clipboard)
    val dialerOpenFailed: String
    val featureComingSoon: String
    val messagingOpenFailed: String
    val emailOpenFailed: String
    val wifiSettingsOpenFailed: String
    val mapsOpenFailed: String
    val appSettingsOpenFailed: String
    val numberCopied: String
    val contentCopied: String

    // Nhãn clipboard / tiêu đề hộp thoại chia sẻ
    val phoneNumberLabel: String
    val contentLabel: String
    val shareNumberTitle: String
}

/** Chuỗi NGÀY–GIỜ có chữ (nhãn nhóm ngày, thời lượng). Định dạng SỐ (HH:mm, dd/MM) giữ nguyên, không dịch. */
interface DateTimeStrings {
    val today: String
    val yesterday: String
    /** Tên thứ NGẮN cho nhãn nhóm ("Thứ 4" / "Wed"; Chủ nhật / Sun). */
    fun weekdayShort(dow: DayOfWeek): String
    /** Thời lượng đàm thoại: "3 phút 5 giây" / "3 min 5 sec"; rỗng nếu 0. */
    fun duration(minutes: Long, seconds: Long): String

    // Đơn vị thời lượng — logic ghép nằm ở TimeFormat, chỉ TỪ đơn vị đổi theo ngôn ngữ.
    val unitDay: String         // "ngày" / "day" — dùng cho khoảng cách giữa 2 cuộc gọi (không lên tháng)
    val unitHour: String        // "giờ" / "hr"
    val unitMinute: String      // "phút" / "min"
    val unitSecond: String      // "giây" / "sec"
    val unitHourShort: String   // "g" / "h"
    val unitMinuteShort: String // "p" / "m"
    val unitSecondShort: String // "s" / "s"
}

/** Nhãn TRẠNG THÁI ngắn của một cuộc gọi (dùng ở dòng thứ 2 mỗi item). */
interface CallStatusStrings {
    val connected: String
    val noResponse: String
    val received: String
    val noAnswer: String
    val rejected: String
    val blocked: String
    val voicemail: String
    val answeredElsewhere: String
    val unknown: String
    /** Từ "Video" để ghép hậu tố "$trạngThái (Video)". */
    val videoLabel: String

    // Câu KẾT QUẢ + đàm thoại đầy đủ (dropdown chi tiết cuộc gọi / màn "Tất cả").
    fun connectionSuccess(duration: String): String
    val connectionFailed: String
    val connectionNoAnswer: String
    val connectionRejected: String
    val connectionBlocked: String
    val connectionVoicemail: String
    val connectionExternal: String
    val connectionNotConnected: String
}

/** Tên hiển thị các SỐ KHẨN CẤP VN khi số chưa lưu danh bạ. */
interface EmergencyStrings {
    val police: String
    val fire: String
    val medical: String
    fun of(kind: EmergencyKind): String
}

/** Loại số khẩn cấp — tách khỏi TÊN để tên đổi theo ngôn ngữ mà icon giữ nguyên. */
enum class EmergencyKind { POLICE, FIRE, MEDICAL }

/** Toàn bộ chuỗi riêng của màn DANH SÁCH CUỘC GỌI (CallListScreen) và các thành phần con của nó. */
interface CallListStrings {
    val title: String
    val searchHint: String
    val voice: String
    val close: String
    val voicePrompt: String
    val voiceUnsupported: String
    val dialpad: String
    val scrollToTop: String
    val chooseFilter: String
    val selected: String

    // Lọc theo LOẠI
    val filterAll: String
    val filterMissed: String
    val filterOutgoing: String
    val filterIncoming: String
    val typeSheetTitle: String

    // Chế độ xem
    val viewByTime: String
    val viewByPhone: String

    // Trạng thái "đang xem theo SIM" khi phạm vi SIM TOÀN APP đang bật (thay cho thanh lọc SIM)
    fun simViewing(label: String): String

    // Chip lọc NGÀY
    val dateToday: String
    val dateYesterday: String
    val dateWeek: String
    val dateMonth: String
    val pickDate: String

    // Sheet chọn ngày (lịch)
    val prevMonth: String
    val nextMonth: String
    fun monthYear(month: Int, year: Int): String
    /** Nhãn 7 thứ trong tuần (ĐẦU TUẦN thứ 2): [T2..CN] / [Mon..Sun]. */
    val weekdayHeaders: List<String>
    fun selectedDay(label: String): String
    val dateRangeNote: String
    val apply: String
    val clearDateFilter: String

    // Trạng thái rỗng / đang tải
    val emptyNoResults: String
    val emptyNoCalls: String
    val loadingCalls: String

    // Màn xin quyền nhật ký cuộc gọi
    val permTitle: String
    val permBody: String
    val permBullet1: String
    val permBullet2: String
    val permBullet3: String
    val permRevoke: String

    // Banner mời cấp quyền Danh bạ
    val bannerTitle: String
    val bannerBody: String

    // Lịch sử tìm kiếm
    val searchRecentEmpty: String
    val searchRecentTitle: String
    val clearAll: String
    val delete: String

    // Menu nhấn giữ item
    val menuMessage: String
    val menuCall: String
    val menuZalo: String
    val menuCopy: String
}

/** Toàn bộ chuỗi riêng của màn CHI TIẾT CUỘC GỌI (CallDetailScreen + DetailCallItem + StatsSheet). */
interface CallDetailStrings {
    // Top bar + trạng thái
    val loading: String
    val emptyNoCalls: String
    val costStats: String
    val copyNumber: String

    // Thẻ hành động
    val numberTypeMobile: String   // loại số MOBILE + fallback khi CallLog không có/không rõ nhãn loại số
    val numberTypeHome: String
    val numberTypeWork: String
    val numberTypeMain: String
    val numberTypeOther: String
    val numberTypeGeneric: String  // TYPE_CUSTOM nhưng không kèm nhãn riêng
    val call: String
    val message: String
    val sendTemplate: String
    val viewInContacts: String
    val addToContacts: String
    val searchZalo: String
    val searchGoogle: String

    // Lịch sử + công cụ
    val historyTitle: String
    val showMore: String
    val toolsTitle: String
    val shareContact: String
    val shareContactSubtitle: String
    val notSavedContact: String    // CallModels.displayName khi số lạ

    // DetailCallItem — tag HƯỚNG gọi + nhãn phụ
    val dirOutgoing: String
    val dirIncoming: String
    val dirMissed: String
    val dirVoicemail: String
    val dirOther: String
    val callNormal: String         // pill "Thường" (không VoLTE)

    // StatsSheet — tay cầm + nội dung thống kê
    val statsTitle: String
    fun totalCalls(n: Int): String
    fun lastCallAt(time: String): String
    /** Ghi chú "gồm N SIM" ở tay cầm thống kê khi số này gọi qua nhiều SIM. */
    fun inclSims(n: Int): String
    /** "Gọi cuối (SIM 1) lúc <giờ>" — kèm SIM của cuộc gần nhất khi có nhiều SIM. */
    fun lastCallAtSim(sim: String, time: String): String
    val metricCalls: String
    val metricDuration: String
    val metricMissed: String
    val statOutgoing: String
    val statIncoming: String
    val statTotalTime: String
    val statActiveHours: String
    val statLastCall: String
}

/**
 * Chuỗi RIÊNG của màn XEM TẤT CẢ cuộc gọi (AllCallsScreen). Item/tag/menu tái dùng [CallDetailStrings] và
 * [CallListStrings] (cùng bố cục), nên đây chỉ giữ vài chuỗi độc quyền của màn.
 */
interface AllCallsStrings {
    val loading: String
    val title: String
    val expandAll: String
    val collapseAll: String
}

/**
 * Chuỗi RIÊNG của màn DÒNG THỜI GIAN (TimelineScreen) — cùng dữ liệu với màn XEM TẤT CẢ nhưng bố cục theo
 * trục thời gian, chia hai bên theo hướng gọi và có "chip" khoảng cách giữa hai cuộc. Item/tag/menu tái dùng
 * [CallDetailStrings]/[CallListStrings]; đây chỉ giữ vài chuỗi độc quyền.
 */
interface TimelineStrings {
    val title: String
    val openTimeline: String        // contentDescription nút mở màn timeline
    val dirOutgoingSide: String     // nhãn cột "Gọi đi" (bên trái)
    val dirIncomingSide: String     // nhãn cột "Gọi đến / Nhỡ" (bên phải)
    fun apart(gap: String): String  // "Cách nhau {gap}" — nhãn/đọc màn hình cho chip khoảng cách
}

/** Toàn bộ chuỗi riêng của màn DANH BẠ (ContactsScreen). */
interface ContactsStrings {
    fun count(n: Int): String
    fun countFiltered(shown: Int, total: Int): String
    val loading: String
    val emptyNoContacts: String
    val emptyNoResults: String
    fun morePhones(n: Int): String
    val sheetTitle: String
    val phonesSection: String
    val openInContactsApp: String
    val createContact: String   // mô tả nút "tạo liên hệ mới" trên thanh tiêu đề
    val editContact: String     // mô tả nút "sửa liên hệ" trong menu nhấn giữ
    val deleteContact: String   // mô tả nút "xoá liên hệ" trong menu nhấn giữ
    val copyContactInfo: String // mô tả nút "sao chép cả thông tin liên hệ" trong menu nhấn giữ
    val searchHint: String
    val permTitle: String
    val permBody: String
    val noName: String

    // Nhãn LOẠI số (Di động/Nhà riêng…). Ánh xạ enum→chuỗi làm ở UI để i18n không phụ thuộc tầng data.
    val phoneTypeMobile: String
    val phoneTypeHome: String
    val phoneTypeWork: String
    val phoneTypeMain: String
    val phoneTypeWorkMobile: String
    val phoneTypeFax: String
    val phoneTypePager: String
    val phoneTypeOther: String
}

/** Toàn bộ chuỗi riêng của màn CÀI ĐẶT (SettingsScreen) — tiêu đề nhóm, các thẻ, dòng chính sách, sheet chọn danh bạ. */
interface SettingsStrings {
    /** Slogan dưới tên app ở đầu màn Cài đặt (trước đây là res/values strings.xml, nay đa ngôn ngữ). */
    val brandTagline: String
    // Tiêu đề nhóm
    val sectionStats: String
    val sectionMessaging: String
    val sectionAgency: String
    val sectionDisplay: String
    val sectionPolicy: String
    val sectionApp: String

    // Thẻ PHẠM VI SIM (đầu màn) — chọn xem theo SIM áp dụng cho TOÀN app
    val simScopeSection: String
    val simScopeTitle: String
    val simScopeDesc: String
    val simScopeAll: String

    // Thẻ Thống kê
    val statsTitle: String
    val statsSubtitle: String
    val statsOpen: String
    val repeatTitle: String
    val repeatSubtitle: String
    val repeatOpen: String

    // Thẻ Nhắn tin
    val templatesTitle: String
    val templatesSubtitle: String
    val templatesOpen: String
    val myNumberTitle: String
    val myNumberSubtitle: String
    val myNumberOpen: String
    val qrHistoryTitle: String
    val qrHistorySubtitle: String
    val qrHistoryOpen: String
    val smsStripTitle: String
    val smsStripSubtitle: String

    // Thẻ Tra cứu cơ quan
    val agencyTitle: String
    val agencySubtitle: String
    val agencyOpen: String

    // Thẻ Cỡ chữ (nhãn mức lấy từ [FontSizeStrings])
    val fontSizeCardNote: String
    val fontSizeOpen: String

    // Chính sách & Hỗ trợ
    val privacyTitle: String
    val privacySubtitle: String
    val termsTitle: String
    val termsSubtitle: String
    val websiteTitle: String
    val contactTitle: String

    // Ứng dụng
    fun version(name: String): String
    val appInfoDesc: String

    // Bottom sheet chọn danh bạ cơ quan
    val pickerTitle: String
    val pickerNote: String
}

/**
 * Chuỗi màn ỦNG HỘ NHÀ PHÁT TRIỂN (DonateScreen) + thẻ mở màn ở Cài đặt.
 *
 * Tinh thần: hoàn toàn TỰ NGUYỆN, minh bạch — nêu rõ đây là đóng góp cho nhà phát triển, không bắt buộc,
 * không đổi lấy tính năng, ứng dụng vẫn miễn phí. Không dùng từ ngữ gây áp lực.
 */
interface DonateStrings {
    // Thẻ ở màn Cài đặt (nằm NGAY TRÊN thẻ "Thông tin ứng dụng")
    val settingsSection: String
    val cardTitle: String
    val cardSubtitle: String
    val open: String

    // Khung màn
    val screenTitle: String

    // Hero (đầu màn)
    val heroTitle: String
    val heroMessage: String

    // Chọn số tiền
    val amountSection: String
    val amountOpen: String        // "Tuỳ tâm" — mã QR mở, người chuyển tự nhập số tiền
    val amountOpenHint: String
    val amountCustom: String      // "Số khác…" — nhập số tiền tuỳ ý
    val customDialogTitle: String
    val customFieldLabel: String
    val customInvalid: String
    val customMax: String
    val confirm: String

    // Mã QR
    val qrSection: String
    val qrHint: String
    val qrOpenAmountNote: String
    val qrLoading: String
    val qrError: String
    val qrRetry: String
    val qrOfflineFallback: String
    val saveQr: String
    val shareQr: String
    val qrSaved: String
    val qrSaveFailed: String
    val shareSubject: String

    // Thông tin chuyển khoản
    val accountSection: String
    val bankLabel: String
    val accountNoLabel: String
    val accountNameLabel: String
    val amountRowLabel: String
    val messageLabel: String
    val amountOpenValue: String   // giá trị hiển thị khi mã mở (chưa cố định số tiền)
    val copied: String

    // Mở nhanh app ngân hàng
    val bankAppsSection: String
    val bankAppsHint: String
    val bankAppsShowAll: String
    val bankAppsShowLess: String
    val bankAppOpenFailed: String
    val bankAppPrefill: String
    val bankAppNeedNetwork: String

    // Chân trang: tinh thần tự nguyện
    val footerTitle: String
    val footerMessage: String
    val thankYou: String
}

/** Nhãn CỠ CHỮ (mức + màn chọn) — mức đổi theo ngôn ngữ, resolve từ [com.antimobile.callhs.util.FontScaleSettings.Tier]. */
interface FontSizeStrings {
    val small: String
    val default: String
    val large: String
    val xlarge: String
    val screenTitle: String
    val previewSection: String
    val chooseSection: String
    val note: String
    val sampleSubtitle: String
}

/** Toàn bộ chuỗi riêng của màn "GỌI LẠI & TRÙNG SỐ" (RepeatStatsScreen). Tiêu đề màn tái dùng [SettingsStrings.repeatTitle]. */
interface RepeatStatsStrings {
    val permNeeded: String
    val analyzing: String
    val emptyNoCalls: String
    val distTitle: String
    val cycleTitle: String
    fun listTitle(n: Int): String
    fun intro(windowDays: Int, start: String, end: String): String

    // Thẻ tổng quan (6 ô số liệu)
    val metricTotalCalls: String
    val metricDistinctNumbers: String
    val metricRepeatNumbers: String
    val metricRepeatCalls: String
    val metricMultiDay: String
    val metricMaxCalls: String
    fun timesCount(n: Int): String      // "3 lần" (ô "Gọi nhiều nhất")

    // Biểu đồ phân bố / chu kỳ
    val barUnitNumbers: String          // đơn vị cột: "số"
    val cycleEmpty: String
    val cycleDesc: String

    // Bộ lọc + chú giải nhiệt
    val sortLabel: String
    val legend: String

    // Hàng một SỐ
    val callsUnit: String               // "lần gọi"
    val collapse: String
    val expand: String
    fun maxPerDay(n: Int): String       // "Tối đa 4 lần/ngày"
    fun detailDates(first: String, last: String): String
    val daysCalledLabel: String
    val noMatch: String

    // Nhãn phân loại
    val classReturning: String
    val classSameDay: String

    // Dòng phụ / chi tiết
    fun daysCount(n: Int): String       // "5 ngày"
    fun outgoing(n: Int): String
    fun incoming(n: Int): String
    fun missed(n: Int): String
    fun talkTime(duration: String): String
    val onlyOneDay: String
    fun avgCycle(gap: String, span: Int): String

    // Nhãn 4 enum (RepeatBucket/RecallCycle/RepeatFilter/RepeatSort) — ánh xạ enum→chuỗi làm ở UI.
    val bucketOnce: String
    val bucketTwice: String
    val bucket3to5: String
    val bucket6to10: String
    val bucketOver10: String
    val cycleWithinWeek: String
    val cycle1to2w: String
    val cycle2to3w: String
    val cycleOver3w: String
    val filterRepeat: String
    val filterMultiDay: String
    val filterAll: String
    val sortTotal: String
    val sortDays: String
    val sortRecent: String
}

/** Toàn bộ chuỗi riêng của màn "THỐNG KÊ CHI TIẾT" (DetailedStatsScreen) — 3 tab: Theo số / Khung giờ / Cước phí. */
interface DetailedStatsStrings {
    val title: String
    val tabByNumber: String
    val tabHourly: String
    val tabCost: String

    val rankTitle: String
    fun byNumberTitle(n: Int): String
    fun emptyNoCallsInPeriod(period: String): String

    // Ô số liệu tổng quan
    val metricTotalCalls: String
    val metricOutgoing: String
    val metricIncoming: String
    val metricMissed: String
    val metricDuration: String
    val metricNumbers: String

    // Xếp hạng / đỉnh giờ
    val rankTopOutgoing: String
    val rankTopIncoming: String
    val rankTopMissed: String

    // Đơn vị cuộc gọi
    fun calls(n: Int): String        // "3 cuộc"
    fun callsFull(n: Int): String    // "3 cuộc gọi"
    val callsUnit: String            // "cuộc" (đứng riêng dưới tổng)

    // Tab khung giờ
    fun hourlyIntro(days: Int): String
    val emptyNoActivity: String
    val legendOutgoing: String
    val legendIncoming: String
    val legendMissed: String
    fun deltaVsPrev(value: String): String
    val deltaSame: String

    // Tab cước phí
    val costPhonePermNote: String
    val costByNetworkTitle: String
    fun costTopTitle(n: Int): String
    fun emptyNoBilledInPeriod(period: String): String
    val costDisclaimer: String
    val costTotalLabel: String
    fun costChargeableLine(n: Int, duration: String): String
    val networkOnNet: String
    val networkOffNet: String
    val networkOther: String
    fun billedOutgoing(n: Int): String

    // Nhãn khoảng (StatsPeriod) — ánh xạ enum→chuỗi ở UI.
    val periodDay: String
    val periodWeek: String
    val periodMonth: String
}

/** Toàn bộ chuỗi riêng của màn "CƯỚC CUỘC GỌI" (CostStatsScreen). Tiêu đề màn/tổng cước/nội-ngoại mạng tái dùng [CallDetailStrings]/[DetailedStatsStrings]. */
interface CostStatsStrings {
    val calculating: String
    val infoDesc: String
    val detailTitle: String
    val estimateTag: String
    fun chargeableCallsLine(n: Int): String
    fun billedSuffix(duration: String): String

    // Thẻ phân tích (dòng phụ)
    val freeCallsLabel: String
    fun freeCallsValue(n: Int): String
    val unknownSimLabel: String
    fun unknownSimValue(n: Int): String

    // Thẻ SIM
    val simCardTitle: String
    val simNoneNote: String
    val fromEnteredNumber: String
    val unknownCarrier: String
    val unknownSimCallsNote: String

    // Dòng CÁCH TÍNH (mở rộng mỗi cuộc)
    val calcSim: String
    val calcCalled: String
    val calledFallback: String
    val calcDuration: String
    val calcBilling: String
    val calcRate: String
    val rateUnitSlash: String        // "/phút"
    val calcTotal: String

    // Huy hiệu loại cuộc
    val badgeOther: String
    val badgeUnknownSim: String
    val badgeNoConnect: String
    val badgeFree: String

    // Gợi ý trạng thái
    val hintUnknownSim: String
    val hintNoConnect: String
    val hintFreeIncoming: String
    val noOutgoing: String
    val disclaimer: String

    // Màn xin quyền Điện thoại
    val permTitle: String
    val permBody: String

    // Bottom sheet bảng giá
    val infoSheetTitle: String
    val infoSheetDesc: String
    val tableCarrier: String
    fun tariffRate(amount: Int): String   // "1590 đ/p"
    val infoNote1: String
    val infoNote2: String

    // Nhãn block tính cước (BillingBlock) — ánh xạ enum→chuỗi ở UI.
    val block6: String
    val block60: String
    val blockPerMinute: String
}

/**
 * Chuỗi RIÊNG của màn "PHÂN TÍCH CUỘC GỌI" (PhoneStatsScreen) — thống kê CHUYÊN SÂU cho MỘT số, mở từ tay cầm
 * bottom sheet ở [CallDetailStrings]/[AllCallsStrings]. Nhiều nhãn tái dùng [DetailedStatsStrings]/[CallDetailStrings]/
 * [CallListStrings]/[DateTimeStrings] (đơn vị cuộc, gọi đi/đến/nhỡ, "Tất cả", tên thứ) — đây chỉ giữ chuỗi độc quyền.
 */
interface PhoneStatsStrings {
    val title: String
    val openDesc: String                       // contentDescription icon mở màn ở tay cầm sheet

    // Biểu đồ tròn (đi / đến / nhỡ)
    val breakdownTitle: String
    val centerCallsLabel: String               // nhãn nhỏ dưới số tổng ở TÂM donut ("cuộc gọi")
    fun countPercent(count: Int, percent: Int): String   // "12 · 34%"

    // Hàng chỉ số nhanh
    val metricAnswerRate: String

    // Biểu đồ ngang: số cuộc theo ngày
    val dailyTitle: String
    fun dailyIntro(days: Int): String

    // Thời lượng & kết nối
    val talkTitle: String
    val talkTotal: String
    val talkAverage: String
    val talkLongest: String
    val connectRate: String
    val missedRate: String
    val rejectedBlocked: String

    // Khung giờ trong ngày
    val hourlyTitle: String
    val peakHourLabel: String
    fun hourRange(from: Int, to: Int): String  // "09:00–10:00"
    val partMorning: String                    // Sáng (5–10h)
    val partAfternoon: String                  // Chiều (11–17h)
    val partEvening: String                    // Tối (18–22h)
    val partNight: String                      // Đêm (23–4h)

    // Theo ngày trong tuần
    val weekdayTitle: String
    val peakWeekdayLabel: String

    // Tính năng cuộc gọi
    val featuresTitle: String
    val featureVideo: String
    val featureVolte: String
    val featuresNone: String

    // Mối quan hệ theo thời gian
    val spanTitle: String
    val spanFirst: String
    val spanLast: String
    val spanDistinctDays: String
    fun distinctDaysValue(days: Int): String   // "12 ngày"
    val spanAvgGap: String

    // Theo SIM (chỉ khi máy nhiều SIM và đang xem "Tất cả")
    val simTitle: String

    // Rỗng
    val empty: String
}

/** Chuỗi của màn "SỐ ĐIỆN THOẠI CỦA TÔI" (Settings → nhập số theo SIM). Tiêu đề tái dùng [SettingsStrings.myNumberTitle]. */
interface MyNumberStrings {
    val errorInvalid: String
    val saved: String
    val save: String
    val done: String
    val introTitle: String
    val introBody: String
    val simPresent: String       // "Đang lắp" (có SIM, chưa rõ nhà mạng)
    val simAbsent: String        // "Chưa lắp SIM"
    val autoRead: String
    val inputHint: String        // "VD: 0987654321"
    fun carrierHint(carrier: String): String
    fun enterHint(slotLabel: String): String
    val checkingSim: String
}

/** Chuỗi QR DÙNG CHUNG: nhãn theo LOẠI mã (qrTypePresentation, dùng cho lịch sử quét + sheet xử lý) + vài chuỗi chung. */
interface QrStrings {
    val typeWeb: String
    val typePhone: String
    val typeSms: String
    val typeEmail: String
    val typeWifi: String
    val typeContact: String
    val typeGeo: String
    val typeText: String
    val empty: String     // "(trống)"
    val scan: String      // "Quét mã QR"
}

/** Chuỗi riêng màn LỊCH SỬ QUÉT QR (QrScanHistoryScreen). Tiêu đề tái dùng [SettingsStrings.qrHistoryTitle]. */
interface QrHistoryStrings {
    val clearAllTitle: String
    val clearAllMessage: String
    val hint: String
    val emptyTitle: String
    val emptyBody: String
    val resultSheetTitle: String
    val pickTemplateForText: String
    val copyContent: String
    fun noQrTemplates(token: String): String
    val pickTemplate: String
}

/** Chuỗi riêng màn QUẢN LÝ MẪU TIN NHẮN (MessageTemplateManagerScreen). Tiêu đề tái dùng [SettingsStrings.templatesTitle]. */
interface TemplatesStrings {
    fun noQrTemplatesToast(token: String): String
    fun maxReached(max: Int): String
    val menuEdit: String
    val deleteTitle: String
    fun deleteMessage(title: String): String
    val hint: String
    val createButton: String
    val emptyTitle: String
    val emptyBody: String
    val pickToSend: String
    val qrScannedPickTemplate: String
}

/** Chuỗi màn SOẠN MẪU tin nhắn (TemplateEditorScreen + TemplatePatternSheets). */
interface TemplateEditorStrings {
    val editTitle: String
    val createTitle: String
    val save: String
    val update: String
    val fieldTitle: String
    val titlePlaceholder: String
    val fieldContent: String
    val contentPlaceholder: String
    val quickInsert: String
    val patternInfoDesc: String
    val discardTitle: String
    val discardMessage: String
    val stay: String
    val exit: String
    val qrPlaceholder: String     // "[nội dung quét QR]"
    val preview: String
    // Sheet giải thích pattern + xem trước
    val qrSampleText: String      // "(nội dung mã QR quét được)"
    val patternHelpTitle: String
    val patternHelpIntro: String
    val patternExampleEmpty: String
    val previewTitle: String
    val previewIntro: String
    val previewNote: String
    // Mô tả các pattern (TemplateFill.hints) hiện trong sheet giải thích
    val hintDate: String
    val hintDatetime: String
    val hintTimedate: String
    val hintWeekdate: String
    val hintPhonesim1: String
    val hintPhonesim2: String
    val hintContextqr: String
}

/** Chuỗi màn XEM VĂN BẢN PHÁP LÝ (LegalScreen). Tiêu đề tái dùng [SettingsStrings.privacyTitle]/[SettingsStrings.termsTitle]. */
interface LegalStrings {
    fun lastUpdated(date: String): String
    fun contactLine(email: String, author: String): String
    val offlineNote: String
    val openFullWeb: String
}

/** Chuỗi màn DANH BẠ CƠ QUAN (AgencyDirectoryScreen). */
interface AgencyStrings {
    val loading: String
    val needNetwork: String
    val noNetworkTitle: String
    val noNetworkMessage: String
    val retry: String
    val refresh: String
    val infoTitle: String         // icon desc + tiêu đề sheet lưu ý
    val disclaimerShort: String
    val disclaimerSub: String
    val searchHint: String
    val clearSearch: String
    fun categoryTotal(category: String, total: Int): String
    fun sourceUpdated(updated: String): String
    fun sourceNetwork(suffix: String): String
    fun sourceCache(suffix: String): String
    fun phonesMore(primary: String, more: Int): String
    fun noPrivatePhoneEmergency(fallback: String): String
    val noPhone: String
    val unnamed: String
    val noAddress: String
    fun emergencyCallNote(fallback: String): String
    val mapChip: String
    val prevPage: String
    val nextPage: String
    fun pageOf(current: Int, count: Int): String
    fun pageCompact(current: Int, count: Int): String
    val emptyNone: String
    fun emptySearch(query: String): String
    val loadFailed: String
    val note1Title: String
    val note1Body: String
    val note2Title: String
    val note2Body: String
    val note3Title: String
    val note3Body: String
    fun emergencyPolice(number: String): String
    fun emergencyFire(number: String): String
    fun emergencyMedical(number: String): String
    val emergencyTitle: String
    fun emergencyCallBody(numbers: String): String
    fun metaUpdated(updated: String): String
}

/** Chuỗi sheet CHIA SẺ LIÊN HỆ (ShareContactSheet). Tiêu đề tái dùng [CallDetailStrings.shareContact]. */
interface ShareSheetStrings {
    val invalidNumber: String
    val asTextTitle: String
    val asTextSubtitle: String
    val asQrTitle: String
    val asQrSubtitle: String
    val myQrTitle: String
    val myQrSubtitle: String
    val contactQrTitle: String
    val qrUnavailable: String
    fun contactQrHelper(number: String): String
    val myQr: String
    val chooseSim: String
    val noSim: String
    val noNumberEnter: String     // khe có SIM nhưng chưa có số
    val chooseSimHint: String
    fun mobileLine(number: String): String
    fun carrierLine(carrier: String): String
    val unknown: String
    fun noNumberForSlot(slotLabel: String): String
    val myQrHelper: String
    val qrImageDesc: String
    val saving: String
    val saveToDevice: String
    val savedToGallery: String
    val saveFailed: String
    val opening: String
    val share: String
    val shareFailed: String
}

/** Chuỗi màn QUÉT MÃ QR (QrScannerDialog). Tái dùng qr.scan + callList.close. */
interface QrScannerStrings {
    val noQrInImage: String
    val torchOn: String
    val torchOff: String
    val instruction: String
    val decoding: String
    val pickImage: String
    val noCameraPerm: String
}

/** Chuỗi sheet XỬ LÝ mã QR có hành động (QrActionSheet). Tái dùng qr.empty + qrHistory.resultSheetTitle/copyContent + callDetail.call/message. */
interface QrActionStrings {
    val scannedContent: String
    val openLink: String
    val sendEmail: String
    val openWifiSettings: String
    val ssid: String
    val password: String
    val security: String
    val hiddenNetwork: String
    val yes: String
    val addToContacts: String
    val name: String
    val phone: String
    val org: String
    val openMap: String
    val label: String
    val address: String
    val latitude: String
    val longitude: String
    val wifiNoPassword: String
}

/** Chuỗi màn ONBOARDING XIN QUYỀN (PermissionOnboarding: 3 quyền bắt buộc + bước đồng ý điều khoản). */
interface PermissionStrings {
    val callLogStep: String
    val callLogHeadline: String
    val callLogDesc: String
    val callLogBullet1: String
    val callLogBullet2: String
    val callLogBullet3: String
    val simStep: String
    val simHeadline: String
    val simDesc: String
    val simBullet1: String
    val simBullet2: String
    val simBullet3: String
    val contactsStep: String
    val contactsHeadline: String
    val contactsDesc: String
    val contactsBullet1: String
    val contactsBullet2: String
    val contactsBullet3: String
    fun stepIndicator(current: Int, total: Int, title: String): String
    val deniedMessage: String
    // Bước đồng ý điều khoản
    val consentStepTitle: String
    val consentTitle: String
    val consentIntro: String
    val consentPoint1: String
    val consentPoint2: String
    val consentPoint3: String
    val consentPoint4: String
    val readTerms: String
    val readPrivacy: String
    val consentCheckLabel: String
    val consentAccept: String
    val consentRequired: String
    val consentFooter: String
    val openInBrowser: String
}

/**
 * Chuỗi cho các HÀNH ĐỘNG util chạy NGOÀI @Composable (toast báo lỗi + tiêu đề hộp chia sẻ):
 * [com.antimobile.callhs.util.AppLinks], [com.antimobile.callhs.util.ContactActions],
 * [com.antimobile.callhs.util.PhoneActions], [com.antimobile.callhs.util.ShareContact].
 */
interface ActionStrings {
    val linkOpenFailed: String
    val browserOpenFailed: String
    val contactsAppOpenFailed: String
    val contactOpenFailed: String
    val invalidPhone: String
    val invalidPhoneSearch: String
    val zaloAndBrowserUnavailable: String
    val feedbackEmailSubject: String
    val shareContactChooser: String
    val shareQrChooser: String
}

/** Chuỗi khung của modal CẬP NHẬT CHÍNH SÁCH / CÓ GÌ MỚI (nội dung policy/feature là data từ WhatsNewStore, giữ nguyên). */
interface UpdateNoticeStrings {
    val policyTitle: String
    val whatsNewTitle: String
    val gotIt: String
    val seeMore: String
}

/** Chuỗi của tính năng CHỌN NGÔN NGỮ (thẻ trong Cài đặt + màn chọn). */
interface LanguageStrings {
    val cardTitle: String
    val sectionChoose: String
    val optionSystem: String
    /** Tên NGÔN NGỮ hiển thị dạng ENDONYM — giữ NGUYÊN ở mọi ngôn ngữ giao diện. */
    val vietnamese: String
    val english: String
    /** Phụ đề mục "Theo hệ thống": ngôn ngữ máy đang áp. */
    fun currentlyUsing(name: String): String
    val note: String
}

/** Chuỗi của tính năng CHỌN GIAO DIỆN (Sáng/Tối) — thẻ trong Cài đặt + màn chọn. */
interface ThemeStrings {
    val cardTitle: String
    val sectionChoose: String
    val optionSystem: String
    val optionLight: String
    val optionDark: String
    /** Phụ đề mục "Theo hệ thống": chế độ máy đang áp (Sáng/Tối). */
    fun currentlyUsing(name: String): String
    val note: String
}

/** Chuỗi của tính năng PHÂN LOẠI NHÓM (Settings + màn danh sách/soạn nhóm + picker + badge). Tên nhóm do
 *  người dùng tự đặt KHÔNG nằm ở đây (là dữ liệu người dùng, không dịch). */
interface CategoryStrings {
    // Settings + danh sách
    val settingsTitle: String
    val settingsSubtitle: String
    val settingsSection: String
    val open: String
    val listTitle: String
    fun memberCount(n: Int): String
    // Tên 2 nhóm mặc định
    val builtinWork: String
    val builtinFavorite: String
    // Ghi chú nhóm mặc định (không đổi tên / không xoá được)
    val builtinLocked: String
    // Tạo / sửa
    val createTitle: String
    val editTitle: String
    val nameLabel: String
    val nameHint: String
    val descLabel: String
    val descHint: String
    val iconLabel: String
    val pickIconTitle: String
    // Tên các nhóm icon (trang trong picker)
    val iconGroupBasic: String
    val iconGroupDelivery: String
    val iconGroupWork: String
    val iconGroupIssue: String
    val iconGroupSocial: String
    val save: String
    val update: String
    // Tab trong màn sửa
    val tabInfo: String
    fun tabNumbers(n: Int): String
    // Danh sách số trong nhóm
    val emptyMembers: String
    val removeMember: String
    fun addedAt(time: String): String
    // Context menu
    val menuEdit: String
    val menuDelete: String
    // Hộp thoại xoá
    val deleteTitle: String
    fun deleteWithMembers(name: String, n: Int): String
    fun deleteEmpty(name: String): String
    val deleteConfirm: String
    val cancel: String
    // Xác nhận thoát khi có thay đổi
    val discardTitle: String
    val discardMessage: String
    val discardStay: String
    val discardExit: String
    // Sheet "thêm vào nhóm"
    val addToCategoryTitle: String
    val createNew: String
    val noCategories: String
    // Nhãn/desc + toast
    val newCategory: String
    val addToCategory: String
    val maxCategories: String
    val maxMembers: String
    val alreadyAdded: String
    fun addedTo(name: String): String
    fun removedFrom(name: String): String
}

/** Chuỗi hiện hành theo ngôn ngữ đang chọn. Dùng được TRONG và NGOÀI @Composable (xem [AppStrings]). */
fun appStrings(): AppStrings = LanguageSettings.strings
