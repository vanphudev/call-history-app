package com.antimobile.callhs.i18n

import java.time.DayOfWeek

/** Bảng chuỗi TIẾNG VIỆT — nguyên văn thiết kế gốc của app (nguồn dịch cho [EnStrings]). */
object ViStrings : AppStrings {

    override val common: CommonStrings = Common
    override val datetime: DateTimeStrings = DateTime
    override val callStatus: CallStatusStrings = CallStatus
    override val emergency: EmergencyStrings = Emergency
    override val callList: CallListStrings = CallList
    override val callDetail: CallDetailStrings = CallDetail
    override val allCalls: AllCallsStrings = AllCalls
    override val timeline: TimelineStrings = Timeline
    override val contacts: ContactsStrings = Contacts
    override val settings: SettingsStrings = Settings
    override val fontSize: FontSizeStrings = FontSize
    override val repeatStats: RepeatStatsStrings = RepeatStats
    override val detailedStats: DetailedStatsStrings = DetailedStats
    override val costStats: CostStatsStrings = CostStats
    override val phoneStats: PhoneStatsStrings = PhoneStats
    override val myNumber: MyNumberStrings = MyNumber
    override val qr: QrStrings = Qr
    override val qrHistory: QrHistoryStrings = QrHistory
    override val templates: TemplatesStrings = Templates
    override val templateEditor: TemplateEditorStrings = TemplateEditor
    override val legal: LegalStrings = Legal
    override val agency: AgencyStrings = Agency
    override val shareSheet: ShareSheetStrings = ShareSheet
    override val qrScanner: QrScannerStrings = QrScanner
    override val qrAction: QrActionStrings = QrAction
    override val permission: PermissionStrings = Permission
    override val updateNotice: UpdateNoticeStrings = UpdateNotice
    override val actions: ActionStrings = Actions
    override val language: LanguageStrings = Language
    override val theme: ThemeStrings = Theme
    override val category: CategoryStrings = Category
    override val donate: DonateStrings = Donate

    private object Common : CommonStrings {
        override val back = "Quay lại"
        override val settings = "Cài đặt"
        override val contacts = "Danh bạ"
        override val search = "Tìm kiếm"
        override val openSettings = "Mở Cài đặt"
        override val grantPermission = "Cấp quyền"
        override val allowAccess = "Cho phép truy cập"
        override val dismiss = "Bỏ qua"
        override val cancel = "Huỷ"
        override val hiddenNumber = "Số ẩn"

        override val dialerOpenFailed = "Không mở được trình quay số"
        override val featureComingSoon = "Tính năng sẽ sớm được cập nhật"
        override val messagingOpenFailed = "Không mở được ứng dụng nhắn tin"
        override val emailOpenFailed = "Không mở được ứng dụng email"
        override val wifiSettingsOpenFailed = "Không mở được cài đặt Wi-Fi"
        override val mapsOpenFailed = "Không mở được ứng dụng bản đồ"
        override val appSettingsOpenFailed = "Không mở được Cài đặt ứng dụng"
        override val numberCopied = "Đã sao chép số"
        override val contentCopied = "Đã sao chép nội dung"

        override val phoneNumberLabel = "Số điện thoại"
        override val contentLabel = "Nội dung"
        override val shareNumberTitle = "Chia sẻ số"
    }

    private object DateTime : DateTimeStrings {
        override val today = "Hôm nay"
        override val yesterday = "Hôm qua"
        override fun weekdayShort(dow: DayOfWeek): String =
            if (dow == DayOfWeek.SUNDAY) "Chủ nhật" else "Thứ ${dow.value + 1}"
        override fun duration(minutes: Long, seconds: Long): String = when {
            minutes > 0 && seconds > 0 -> "$minutes phút $seconds giây"
            minutes > 0 -> "$minutes phút"
            else -> "$seconds giây"
        }
        override val unitDay = "ngày"
        override val unitHour = "giờ"
        override val unitMinute = "phút"
        override val unitSecond = "giây"
        override val unitHourShort = "g"
        override val unitMinuteShort = "p"
        override val unitSecondShort = "s"
    }

    private object CallStatus : CallStatusStrings {
        override val connected = "Đã kết nối"
        override val noResponse = "Không phản hồi"
        override val received = "Đã nhận"
        override val noAnswer = "Không trả lời"
        override val rejected = "Đã từ chối"
        override val blocked = "Bị chặn"
        override val voicemail = "Thư thoại"
        override val answeredElsewhere = "Trả lời nơi khác"
        override val unknown = "Không rõ"
        override val videoLabel = "Video"

        override fun connectionSuccess(duration: String) = "Kết nối thành công · $duration"
        override val connectionFailed = "Kết nối thất bại · không phản hồi"
        override val connectionNoAnswer = "Không trả lời · Không phản hồi"
        override val connectionRejected = "Bạn đã từ chối · không đàm thoại"
        override val connectionBlocked = "Bị chặn · không đàm thoại"
        override val connectionVoicemail = "Lời nhắn thoại"
        override val connectionExternal = "Trả lời ở thiết bị khác"
        override val connectionNotConnected = "Không kết nối"
    }

    private object Emergency : EmergencyStrings {
        override val police = "Cảnh sát"
        override val fire = "Cứu hoả"
        override val medical = "Cấp cứu"
        override fun of(kind: EmergencyKind): String = when (kind) {
            EmergencyKind.POLICE -> police
            EmergencyKind.FIRE -> fire
            EmergencyKind.MEDICAL -> medical
        }
    }

    private object CallList : CallListStrings {
        override val title = "Lịch sử cuộc gọi"
        override val searchHint = "Tìm tên, số điện thoại"
        override val voice = "Giọng nói"
        override val close = "Đóng"
        override val voicePrompt = "Nói để tìm kiếm"
        override val voiceUnsupported = "Thiết bị không hỗ trợ tìm bằng giọng nói"
        override val dialpad = "Bàn phím"
        override val scrollToTop = "Lên đầu danh sách"
        override val chooseFilter = "Chọn bộ lọc"
        override val selected = "Đang chọn"

        override val filterAll = "Tất cả"
        override val filterMissed = "Nhỡ"
        override val filterOutgoing = "Đã gọi"
        override val filterIncoming = "Đã nhận"
        override val typeSheetTitle = "Lọc theo loại cuộc gọi"

        override val viewByTime = "Thời gian"
        override val viewByPhone = "Số điện thoại"

        override fun simViewing(label: String) = "Đang xem $label"

        override val dateToday = "Hôm nay"
        override val dateYesterday = "Hôm qua"
        override val dateWeek = "Tuần này"
        override val dateMonth = "Tháng này"
        override val pickDate = "Chọn ngày"

        override val prevMonth = "Tháng trước"
        override val nextMonth = "Tháng sau"
        override fun monthYear(month: Int, year: Int): String = "Tháng $month, $year"
        override val weekdayHeaders = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        override fun selectedDay(label: String): String = "Đã chọn: $label"
        override val dateRangeNote = "Chỉ có thể chọn trong vòng 3 tháng gần đây."
        override val apply = "Áp dụng"
        override val clearDateFilter = "Bỏ lọc theo ngày"

        override val emptyNoResults = "Không tìm thấy kết quả."
        override val emptyNoCalls = "Chưa có cuộc gọi nào trên máy."
        override val loadingCalls = "Đang tải nhật ký cuộc gọi…"

        override val permTitle = "Xem lịch sử cuộc gọi"
        override val permBody =
            "Cấp quyền đọc nhật ký cuộc gọi để xem lại lịch sử cùng chi tiết SIM, nhà mạng và thời lượng — tất cả ngay trên máy bạn."
        override val permBullet1 = "Chỉ đọc — không sửa, không xoá cuộc gọi"
        override val permBullet2 = "Dữ liệu ở lại trên máy, không gửi đi đâu"
        override val permBullet3 = "Xem loại cuộc gọi, SIM, nhà mạng, thời lượng"
        override val permRevoke = "Bạn có thể thu hồi quyền bất cứ lúc nào trong Cài đặt."

        override val bannerTitle = "Hiện tên liên hệ đã lưu"
        override val bannerBody = "Cấp quyền Danh bạ để thay số bằng tên người gọi."

        override val searchRecentEmpty = "Chưa có tìm kiếm gần đây"
        override val searchRecentTitle = "Tìm kiếm gần đây"
        override val clearAll = "Xoá tất cả"
        override val delete = "Xoá"

        override val menuMessage = "Gửi tin nhắn"
        override val menuCall = "Gọi điện thoại"
        override val menuZalo = "Tìm qua Zalo"
        override val menuCopy = "Sao chép số"
    }

    private object CallDetail : CallDetailStrings {
        override val loading = "Đang tải chi tiết…"
        override val emptyNoCalls = "Không có cuộc gọi nào với số này."
        override val costStats = "Cước cuộc gọi"
        override val copyNumber = "Sao chép số"

        override val numberTypeMobile = "Di động"
        override val numberTypeHome = "Nhà riêng"
        override val numberTypeWork = "Cơ quan"
        override val numberTypeMain = "Chính"
        override val numberTypeOther = "Khác"
        override val numberTypeGeneric = "Điện thoại"
        override val call = "Gọi"
        override val message = "Nhắn tin"
        override val sendTemplate = "Gửi tin nhắn theo mẫu"
        override val viewInContacts = "Xem trong danh bạ"
        override val addToContacts = "Thêm vào liên hệ"
        override val searchZalo = "Tìm kiếm qua Zalo"
        override val searchGoogle = "Tìm kiếm qua Google"

        override val historyTitle = "Lịch sử cuộc gọi"
        override val showMore = "Hiển thị thêm"
        override val toolsTitle = "Công cụ"
        override val shareContact = "Chia sẻ liên hệ"
        override val shareContactSubtitle = "Qua văn bản hoặc mã QR"
        override val notSavedContact = "Chưa lưu danh bạ"

        override val dirOutgoing = "Cuộc gọi đi"
        override val dirIncoming = "Cuộc gọi đến"
        override val dirMissed = "Cuộc gọi nhỡ"
        override val dirVoicemail = "Cuộc gọi thoại"
        override val dirOther = "Khác"
        override val callNormal = "Thường"

        override val statsTitle = "Thống kê"
        override fun totalCalls(n: Int) = "Tổng $n cuộc gọi"
        override fun lastCallAt(time: String) = "Lần cuối lúc $time"
        override fun inclSims(n: Int) = "(Gồm $n SIM)"
        override fun lastCallAtSim(sim: String, time: String) = "Lần cuối ($sim) lúc $time"
        override val metricCalls = "Cuộc gọi"
        override val metricDuration = "Thời lượng"
        override val metricMissed = "Nhỡ"
        override val statOutgoing = "Đã gọi đi"
        override val statIncoming = "Đã nhận"
        override val statTotalTime = "Tổng thời gian"
        override val statActiveHours = "Khung giờ hoạt động"
        override val statLastCall = "Gọi gần nhất"
    }

    private object AllCalls : AllCallsStrings {
        override val loading = "Đang tải cuộc gọi…"
        override val title = "Tất cả cuộc gọi"
        override val expandAll = "Mở tất cả"
        override val collapseAll = "Đóng tất cả"
    }

    private object Timeline : TimelineStrings {
        override val title = "Dòng thời gian"
        override val openTimeline = "Xem dạng dòng thời gian"
        override val dirOutgoingSide = "Gọi đi"
        override val dirIncomingSide = "Gọi đến"
        override fun apart(gap: String) = "Cách nhau $gap"
    }

    private object Contacts : ContactsStrings {
        override fun count(n: Int) = "$n liên hệ"
        override fun countFiltered(shown: Int, total: Int) = "$shown/$total liên hệ"
        override val loading = "Đang tải danh bạ…"
        override val emptyNoContacts = "Chưa có liên hệ nào trong danh bạ."
        override val emptyNoResults = "Không tìm thấy liên hệ phù hợp."
        override fun morePhones(n: Int) = "+$n số"
        override val sheetTitle = "Thông tin liên hệ"
        override val phonesSection = "Số điện thoại"
        override val openInContactsApp = "Mở trong ứng dụng Danh bạ"
        override val createContact = "Tạo liên hệ mới"
        override val editContact = "Sửa liên hệ"
        override val deleteContact = "Xoá liên hệ"
        override val copyContactInfo = "Sao chép thông tin"
        override val searchHint = "Tìm tên, số điện thoại, email"
        override val permTitle = "Xem danh bạ của bạn"
        override val permBody =
            "Cấp quyền Danh bạ để xem đầy đủ liên hệ cùng số điện thoại, nhà mạng và thông tin đi kèm — tất cả ngay trên máy bạn."
        override val noName = "Không tên"

        override val phoneTypeMobile = "Di động"
        override val phoneTypeHome = "Nhà riêng"
        override val phoneTypeWork = "Cơ quan"
        override val phoneTypeMain = "Số chính"
        override val phoneTypeWorkMobile = "DĐ cơ quan"
        override val phoneTypeFax = "Fax"
        override val phoneTypePager = "Máy nhắn"
        override val phoneTypeOther = "Khác"
    }

    private object Settings : SettingsStrings {
        override val brandTagline = "Lịch sử cuộc gọi Shipper"
        override val sectionStats = "Thống kê"
        override val sectionMessaging = "Nhắn tin"
        override val sectionAgency = "Tra cứu cơ quan"
        override val sectionDisplay = "Hiển thị"
        override val sectionPolicy = "Chính sách & Hỗ trợ"
        override val sectionApp = "Ứng dụng"

        override val simScopeSection = "Xem theo SIM"
        override val simScopeTitle = "Phạm vi SIM cho toàn ứng dụng"
        override val simScopeDesc =
            "Chọn một SIM để CẢ ứng dụng (danh sách, thống kê, chi tiết, cước) chỉ tính và hiển thị cuộc gọi của SIM đó. Chọn \"Tất cả\" để xem mọi SIM — khi đó bạn vẫn lọc nhanh theo SIM ngay ở màn danh sách."
        override val simScopeAll = "Tất cả"

        override val statsTitle = "Thống kê & phân tích chi tiết"
        override val statsSubtitle = "Theo số · Khung giờ hoạt động · Cước phí"
        override val statsOpen = "Mở thống kê"
        override val repeatTitle = "Gọi lại & trùng số"
        override val repeatSubtitle = "Số gọi nhiều lần · Chu kỳ quay lại · 30 ngày"
        override val repeatOpen = "Mở thống kê gọi lại"

        override val templatesTitle = "Mẫu tin nhắn"
        override val templatesSubtitle = "Soạn sẵn nội dung · Gửi nhanh · Chèn mã QR"
        override val templatesOpen = "Mở mẫu tin nhắn"
        override val myNumberTitle = "Số điện thoại của tôi"
        override val myNumberSubtitle = "Nhập số theo SIM · Tự điền {phonesim} · QR của tôi"
        override val myNumberOpen = "Mở số điện thoại của tôi"
        override val qrHistoryTitle = "Lịch sử quét mã QR"
        override val qrHistorySubtitle = "Xem lại mã đã quét · Mở nhanh · Chọn mẫu nhắn tin"
        override val qrHistoryOpen = "Mở lịch sử quét mã QR"
        override val smsStripTitle = "Gửi SMS không dấu"
        override val smsStripSubtitle =
            "Bỏ dấu tiếng Việt & ký tự đặc biệt khi chuyển sang ứng dụng nhắn tin để tiết kiệm phí. Nội dung trong ứng dụng vẫn giữ nguyên."

        override val agencyTitle = "Danh bạ cơ quan hành chính / công an"
        override val agencySubtitle = "Hành chính công & Công an · Hà Nội, TP.HCM"
        override val agencyOpen = "Chọn danh bạ"

        override val fontSizeCardNote = "Không đổi theo cỡ chữ hệ thống"
        override val fontSizeOpen = "Mở cài đặt cỡ chữ"

        override val privacyTitle = "Chính sách quyền riêng tư"
        override val privacySubtitle = "Cách ứng dụng xử lý dữ liệu của bạn"
        override val termsTitle = "Điều khoản sử dụng"
        override val termsSubtitle = "Điều khoản, miễn trừ trách nhiệm"
        override val websiteTitle = "Trang web chính thức"
        override val contactTitle = "Liên hệ nhà phát triển"

        override fun version(name: String) = "Phiên bản $name"
        override val appInfoDesc = "Xem thông tin ứng dụng trong Cài đặt"

        override val pickerTitle = "Chọn danh bạ cơ quan"
        override val pickerNote =
            "Dữ liệu tải từ trang chính thức trên GitHub, lưu tại máy và làm mới tối đa 7 ngày/lần. Các tỉnh/thành khác sẽ được bổ sung sau."
    }

    private object FontSize : FontSizeStrings {
        override val small = "Nhỏ"
        override val default = "Mặc định"
        override val large = "Lớn"
        override val xlarge = "Rất lớn"
        override val screenTitle = "Cỡ chữ"
        override val previewSection = "Xem trước"
        override val chooseSection = "Chọn cỡ chữ"
        override val note =
            "Ứng dụng dùng cỡ chữ bạn chọn ở đây và KHÔNG thay đổi theo cài đặt “Cỡ chữ / Cỡ hiển thị” của hệ thống, giúp giao diện luôn hiển thị đúng thiết kế."
        override val sampleSubtitle = "Cuộc gọi đi · 5 phút trước"
    }

    private object RepeatStats : RepeatStatsStrings {
        override val permNeeded = "Cần quyền đọc nhật ký cuộc gọi để xem thống kê."
        override val analyzing = "Đang phân tích lịch sử cuộc gọi…"
        override val emptyNoCalls = "Chưa có cuộc gọi nào trong 30 ngày gần nhất."
        override val distTitle = "Phân bố số lần gọi"
        override val cycleTitle = "Chu kỳ quay lại"
        override fun listTitle(n: Int) = "Danh sách số ($n)"
        override fun intro(windowDays: Int, start: String, end: String) =
            "Phân tích số bị gọi lặp lại trong $windowDays ngày gần nhất — từ $start đến $end."

        override val metricTotalCalls = "Tổng cuộc"
        override val metricDistinctNumbers = "Số máy"
        override val metricRepeatNumbers = "Số gọi ≥2 lần"
        override val metricRepeatCalls = "Cuộc gọi lại"
        override val metricMultiDay = "Quay lại nhiều ngày"
        override val metricMaxCalls = "Gọi nhiều nhất"
        override fun timesCount(n: Int) = "$n lần"

        override val barUnitNumbers = "số"
        override val cycleEmpty = "Chưa có số nào được gọi lại vào nhiều ngày khác nhau trong khoảng này."
        override val cycleDesc = "Khoảng cách trung bình giữa các ngày gọi lại cùng một số."

        override val sortLabel = "Sắp xếp"
        override val legend = "Mỗi ô = 1 ngày (cũ → hôm nay); đậm hơn = gọi nhiều hơn"

        override val callsUnit = "lần gọi"
        override val collapse = "Thu gọn"
        override val expand = "Xem chi tiết"
        override fun maxPerDay(n: Int) = "Tối đa $n lần/ngày"
        override fun detailDates(first: String, last: String) = "Lần đầu $first · Lần cuối $last"
        override val daysCalledLabel = "Các ngày đã gọi"
        override val noMatch = "Không có số nào phù hợp bộ lọc."

        override val classReturning = "Khách quay lại"
        override val classSameDay = "Gọi lại trong ngày"

        override fun daysCount(n: Int) = "$n ngày"
        override fun outgoing(n: Int) = "Gọi đi $n"
        override fun incoming(n: Int) = "Nhận $n"
        override fun missed(n: Int) = "Nhỡ $n"
        override fun talkTime(duration: String) = "Đàm thoại $duration"
        override val onlyOneDay = "Chỉ gọi trong 1 ngày"
        override fun avgCycle(gap: String, span: Int) = "Quay lại trung bình mỗi ~$gap ngày · trải $span ngày"

        override val bucketOnce = "1 lần"
        override val bucketTwice = "2 lần"
        override val bucket3to5 = "3–5 lần"
        override val bucket6to10 = "6–10 lần"
        override val bucketOver10 = ">10 lần"
        override val cycleWithinWeek = "Trong tuần"
        override val cycle1to2w = "1–2 tuần"
        override val cycle2to3w = "2–3 tuần"
        override val cycleOver3w = "Trên 3 tuần"
        override val filterRepeat = "Gọi ≥2 lần"
        override val filterMultiDay = "Nhiều ngày"
        override val filterAll = "Tất cả"
        override val sortTotal = "Tổng cuộc"
        override val sortDays = "Số ngày"
        override val sortRecent = "Gần đây"
    }

    private object DetailedStats : DetailedStatsStrings {
        override val title = "Thống kê chi tiết"
        override val tabByNumber = "Theo số"
        override val tabHourly = "Khung giờ"
        override val tabCost = "Cước phí"

        override val rankTitle = "Xếp hạng"
        override fun byNumberTitle(n: Int) = "Chi tiết theo số ($n)"
        override fun emptyNoCallsInPeriod(period: String) = "Không có cuộc gọi trong $period."

        override val metricTotalCalls = "Tổng cuộc"
        override val metricOutgoing = "Gọi đi"
        override val metricIncoming = "Đã nhận"
        override val metricMissed = "Nhỡ"
        override val metricDuration = "Thời lượng"
        override val metricNumbers = "Số máy"

        override val rankTopOutgoing = "Gọi đi nhiều nhất"
        override val rankTopIncoming = "Nhận nhiều nhất"
        override val rankTopMissed = "Nhỡ nhiều nhất"

        override fun calls(n: Int) = "$n cuộc"
        override fun callsFull(n: Int) = "$n cuộc gọi"
        override val callsUnit = "cuộc"

        override fun hourlyIntro(days: Int) =
            "Phân bố cuộc gọi theo 24 giờ của $days ngày gần nhất — kèm khung giờ đỉnh và chênh lệch so với hôm trước."
        override val emptyNoActivity = "Chưa có dữ liệu hoạt động."
        override val legendOutgoing = "Gọi đi"
        override val legendIncoming = "Nhận"
        override val legendMissed = "Nhỡ"
        override fun deltaVsPrev(value: String) = "$value so với hôm trước"
        override val deltaSame = "Bằng hôm trước"

        override val costPhonePermNote =
            "Chưa cấp quyền Điện thoại — một số cuộc gọi đi chưa xác định được nhà mạng SIM nên có thể chưa tính cước."
        override val costByNetworkTitle = "Theo loại mạng"
        override fun costTopTitle(n: Int) = "Tốn cước nhất ($n số)"
        override fun emptyNoBilledInPeriod(period: String) = "Không có cuộc gọi đi tính cước trong $period."
        override val costDisclaimer =
            "Cước là ƯỚC TÍNH theo bảng giá phổ thông (ngoài gói), chỉ tính cuộc gọi ĐI đã kết nối. Cước thực tế có thể thấp hơn nếu bạn dùng gói cước."
        override val costTotalLabel = "Tổng cước ước tính"
        override fun costChargeableLine(n: Int, duration: String) = "$n cuộc tính cước · $duration"
        override val networkOnNet = "Nội mạng"
        override val networkOffNet = "Ngoại mạng"
        override val networkOther = "Cố định / Khác"
        override fun billedOutgoing(n: Int) = "$n cuộc gọi đi"

        override val periodDay = "Hôm nay"
        override val periodWeek = "Tuần này"
        override val periodMonth = "Tháng này"
    }

    private object PhoneStats : PhoneStatsStrings {
        override val title = "Phân tích cuộc gọi"
        override val openDesc = "Mở phân tích chi tiết"

        override val breakdownTitle = "Tỉ lệ cuộc gọi"
        override val centerCallsLabel = "cuộc gọi"
        override fun countPercent(count: Int, percent: Int) = "$count · $percent%"

        override val metricAnswerRate = "Tỉ lệ bắt máy"

        override val dailyTitle = "Số cuộc theo ngày"
        override fun dailyIntro(days: Int) = "$days ngày gần nhất có cuộc gọi"

        override val talkTitle = "Thời lượng & kết nối"
        override val talkTotal = "Tổng đàm thoại"
        override val talkAverage = "Trung bình mỗi cuộc"
        override val talkLongest = "Cuộc dài nhất"
        override val connectRate = "Tỉ lệ kết nối"
        override val missedRate = "Tỉ lệ nhỡ"
        override val rejectedBlocked = "Từ chối / chặn"

        override val hourlyTitle = "Khung giờ trong ngày"
        override val peakHourLabel = "Giờ cao điểm"
        override fun hourRange(from: Int, to: Int) = "%02d:00–%02d:00".format(from, to)
        override val partMorning = "Sáng"
        override val partAfternoon = "Chiều"
        override val partEvening = "Tối"
        override val partNight = "Đêm"

        override val weekdayTitle = "Theo ngày trong tuần"
        override val peakWeekdayLabel = "Ngày gọi nhiều nhất"

        override val featuresTitle = "Tính năng cuộc gọi"
        override val featureVideo = "Cuộc gọi video"
        override val featureVolte = "VoLTE (gọi HD)"
        override val featuresNone = "Không có cuộc gọi video hay VoLTE với số này."

        override val spanTitle = "Theo dòng thời gian"
        override val spanFirst = "Lần gọi đầu tiên"
        override val spanLast = "Lần gọi gần nhất"
        override val spanDistinctDays = "Số ngày có gọi"
        override fun distinctDaysValue(days: Int) = "$days ngày"
        override val spanAvgGap = "Khoảng cách trung bình"

        override val simTitle = "Theo SIM"

        override val empty = "Chưa có cuộc gọi nào với số này."
    }

    private object CostStats : CostStatsStrings {
        override val calculating = "Đang tính cước…"
        override val infoDesc = "Thông tin cước"
        override val detailTitle = "Chi tiết từng cuộc gọi đi"
        override val estimateTag = "ƯỚC TÍNH"
        override fun chargeableCallsLine(n: Int) = "$n cuộc gọi đi tính phí"
        override fun billedSuffix(duration: String) = "$duration tính cước"

        override val freeCallsLabel = "Cuộc gọi không tính phí"
        override fun freeCallsValue(n: Int) = "$n cuộc · miễn phí"
        override val unknownSimLabel = "Chưa xác định SIM"
        override fun unknownSimValue(n: Int) = "$n cuộc · chưa tính"

        override val simCardTitle = "Nhà mạng SIM trên máy"
        override val simNoneNote =
            "Chưa phát hiện SIM và bạn chưa nhập số. Vào Cài đặt › “Số điện thoại của tôi” để nhập số — dùng suy ra nhà mạng & ước tính cước."
        override val fromEnteredNumber = "theo số bạn nhập"
        override val unknownCarrier = "Không rõ nhà mạng"
        override val unknownSimCallsNote =
            "Một số cuộc gọi đi không khớp SIM nào đang lắp (có thể đã đổi/tháo SIM) nên chưa tính được cước."

        override val calcSim = "SIM gọi đi"
        override val calcCalled = "Gọi đến"
        override val calledFallback = "số cố định / khác"
        override val calcDuration = "Thời lượng thực"
        override val calcBilling = "Tính cước"
        override val calcRate = "Đơn giá"
        override val rateUnitSlash = "/phút"
        override val calcTotal = "Thành tiền"

        override val badgeOther = "Khác"
        override val badgeUnknownSim = "Chưa rõ SIM"
        override val badgeNoConnect = "Không kết nối"
        override val badgeFree = "Miễn phí"

        override val hintUnknownSim =
            "Chưa xác định được SIM/nhà mạng đã dùng cho cuộc gọi này (có thể SIM đã tháo). Không tính được cước."
        override val hintNoConnect =
            "Cuộc gọi đi không kết nối (không nghe máy / máy bận…) nên không phát sinh cước."
        override val hintFreeIncoming = "Cuộc gọi này không tính cước với bạn."
        override val noOutgoing = "Không có cuộc gọi đi nào với số này — bạn không mất cước."
        override val disclaimer =
            "Số tiền chỉ là ƯỚC TÍNH theo bảng giá phổ thông (ngoài gói). Nếu bạn dùng gói cước có phút miễn phí hoặc thuê bao trả sau, cước thực tế thường thấp hơn — nhiều khi bằng 0 trong hạn mức."

        override val permTitle = "Tính cước cuộc gọi"
        override val permBody =
            "Để biết mỗi cuộc gọi là nội mạng hay ngoại mạng, ứng dụng cần đọc nhà mạng của SIM bạn dùng để gọi. Thông tin chỉ dùng để tính cước trên máy, không gửi đi đâu."

        override val infoSheetTitle = "Bảng giá cước tham khảo"
        override val infoSheetDesc =
            "Đơn giá phổ thông (ngoài gói), đã gồm VAT — tra cứu 07/2026 từ trang nhà mạng & đại lý viễn thông."
        override val tableCarrier = "Nhà mạng"
        override fun tariffRate(amount: Int) = "$amount đ/p"
        override val infoNote1 =
            "Cước thực tế phụ thuộc gói cước, khuyến mãi và loại thuê bao (trả trước/trả sau) của bạn, nên số tiền chỉ mang tính ước tính tham khảo."
        override val infoNote2 =
            "SIM mạng ảo (iTel, Wintel, VNSKY, FPT…) có thể bị nhận nhầm thành nhà mạng chủ (VinaPhone/MobiFone), nên cuộc gọi nội mạng của các mạng này đôi khi bị tính như ngoại mạng."

        override val block6 = "block 6 giây + 1"
        override val block60 = "block 60 giây + 1"
        override val blockPerMinute = "tính tròn phút"
    }

    private object MyNumber : MyNumberStrings {
        override val errorInvalid = "Số điện thoại chưa đúng (9–11 chữ số)"
        override val saved = "Đã lưu số điện thoại của bạn"
        override val save = "Lưu"
        override val done = "Xong"
        override val introTitle = "Số máy dùng cho mẫu tin & QR"
        override val introBody =
            "Nhiều SIM tại Việt Nam không lưu số thuê bao nên ứng dụng không đọc được tự động. " +
                "Bạn nhập số của mình ở đây (lưu ngay trên máy) để tự điền pattern {phonesim1}/{phonesim2} " +
                "trong mẫu tin nhắn và tạo “QR của tôi”. Nếu máy đọc được số tự động thì ô sẽ khoá, không cần nhập."
        override val simPresent = "Đang lắp"
        override val simAbsent = "Chưa lắp SIM"
        override val autoRead = "Đã đọc tự động từ SIM — không cần nhập."
        override val inputHint = "VD: 0987654321"
        override fun carrierHint(carrier: String) = "Nhà mạng: $carrier — dùng để ước tính cước cuộc gọi."
        override fun enterHint(slotLabel: String) = "Nhập số của bạn cho $slotLabel (chỉ chữ số)."
        override val checkingSim = "Đang kiểm tra SIM…"
    }

    private object Qr : QrStrings {
        override val typeWeb = "Liên kết web"
        override val typePhone = "Số điện thoại"
        override val typeSms = "Tin nhắn SMS"
        override val typeEmail = "Email"
        override val typeWifi = "Mạng Wi‑Fi"
        override val typeContact = "Danh thiếp"
        override val typeGeo = "Vị trí bản đồ"
        override val typeText = "Văn bản"
        override val empty = "(trống)"
        override val scan = "Quét mã QR"
    }

    private object QrHistory : QrHistoryStrings {
        override val clearAllTitle = "Xoá toàn bộ lịch sử?"
        override val clearAllMessage = "Toàn bộ lịch sử quét mã QR sẽ bị xoá."
        override val hint = "Chạm để mở · Nhấn nút xoá để xoá mục."
        override val emptyTitle = "Chưa có lịch sử quét mã QR"
        override val emptyBody = "Nhấn biểu tượng quét ở góc trên để quét mã QR đầu tiên."
        override val resultSheetTitle = "Kết quả quét mã QR"
        override val pickTemplateForText = "Chọn mẫu tin nhắn để chèn nội dung này"
        override val copyContent = "Sao chép nội dung"
        override fun noQrTemplates(token: String) =
            "Chưa có mẫu tin nhắn nào dùng mã QR ($token). Vào \"Mẫu tin nhắn\" trong Cài đặt để tạo mẫu."
        override val pickTemplate = "Chọn mẫu để nhắn tin"
    }

    private object Templates : TemplatesStrings {
        override fun noQrTemplatesToast(token: String) = "Chưa có mẫu nào dùng mã QR ($token)"
        override fun maxReached(max: Int) = "Chỉ tạo tối đa $max mẫu tin nhắn"
        override val menuEdit = "Sửa"
        override val deleteTitle = "Xoá mẫu tin nhắn?"
        override fun deleteMessage(title: String) = "Mẫu \"$title\" sẽ bị xoá."
        override val hint = "Chạm vào mẫu để gửi tin nhắn · Nhấn giữ để sửa, xoá hoặc quét mã QR."
        override val createButton = "Tạo mẫu"
        override val emptyTitle = "Chưa có mẫu tin nhắn nào"
        override val emptyBody = "Nhấn \"Tạo mẫu\" để soạn nội dung gửi sẵn."
        override val pickToSend = "Chọn mẫu để gửi"
        override val qrScannedPickTemplate = "Đã quét xong mã QR. Chọn mẫu để chèn mã và mở tin nhắn:"
    }

    private object TemplateEditor : TemplateEditorStrings {
        override val editTitle = "Sửa mẫu tin nhắn"
        override val createTitle = "Tạo mẫu tin nhắn"
        override val save = "Lưu"
        override val update = "Cập nhật"
        override val fieldTitle = "Tiêu đề"
        override val titlePlaceholder = "Tiêu đề mẫu"
        override val fieldContent = "Nội dung"
        override val contentPlaceholder = "Nội dung tin nhắn…"
        override val quickInsert = "Chèn nhanh — tự điền khi gửi"
        override val patternInfoDesc = "Giải thích pattern"
        override val discardTitle = "Thoát mà không lưu?"
        override val discardMessage = "Nội dung bạn vừa nhập sẽ không được lưu."
        override val stay = "Ở lại"
        override val exit = "Thoát"
        override val qrPlaceholder = "[nội dung quét QR]"
        override val preview = "Xem trước"
        override val qrSampleText = "(nội dung mã QR quét được)"
        override val patternHelpTitle = "Giải thích các pattern"
        override val patternHelpIntro = "Các pattern này TỰ thay bằng giá trị thật khi gửi. Ví dụ theo thời điểm hiện tại:"
        override val patternExampleEmpty = "(chưa đọc được / để trống)"
        override val previewTitle = "Xem trước tin nhắn"
        override val previewIntro = "Nội dung sẽ điền sẵn vào ứng dụng nhắn tin:"
        override val previewNote = "Lưu ý: [nội dung quét QR] sẽ được thay bằng kết quả quét mã QR khi bạn gửi."
        override val hintDate = "Ngày hiện tại"
        override val hintDatetime = "Ngày và giờ"
        override val hintTimedate = "Giờ và ngày"
        override val hintWeekdate = "Thứ và ngày"
        override val hintPhonesim1 = "Số SIM 1"
        override val hintPhonesim2 = "Số SIM 2"
        override val hintContextqr = "Kết quả quét mã QR"
    }

    private object Legal : LegalStrings {
        override fun lastUpdated(date: String) = "Cập nhật lần cuối: $date · Áp dụng cho ứng dụng CallHS"
        override fun contactLine(email: String, author: String) = "Liên hệ: $email · Nhà phát triển $author"
        override val offlineNote =
            "Nội dung cần kết nối mạng để tải lần đầu.\nBạn có thể xem bản đầy đủ trên trang web chính thức."
        override val openFullWeb = "Mở bản đầy đủ trên web"
    }

    private object Agency : AgencyStrings {
        override val loading = "Đang tải danh bạ…"
        override val needNetwork = "Cần kết nối mạng để tải danh bạ."
        override val noNetworkTitle = "Không có kết nối mạng"
        override val noNetworkMessage = "Cần kết nối mạng để tải danh bạ lần đầu. Vui lòng bật Wi-Fi hoặc dữ liệu di động rồi thử lại."
        override val retry = "Thử lại"
        override val refresh = "Làm mới dữ liệu"
        override val infoTitle = "Lưu ý quan trọng"
        override val disclaimerShort = "Thông tin chỉ để THAM KHẢO — bạn tự chịu rủi ro khi liên hệ."
        override val disclaimerSub = "Số/địa chỉ có thể chưa chính xác hoặc đã thay đổi. Nghiêm cấm sao chép, làm giả. Nhấn để xem chi tiết."
        override val searchHint = "Tìm theo tên, phường/xã…"
        override val clearSearch = "Xoá tìm kiếm"
        override fun categoryTotal(category: String, total: Int) = "$category · $total cơ quan"
        override fun sourceUpdated(updated: String) = " · cập nhật $updated"
        override fun sourceNetwork(suffix: String) = "Nguồn: GitHub (vừa tải mới)$suffix"
        override fun sourceCache(suffix: String) = "Nguồn: bản lưu tại máy$suffix"
        override fun phonesMore(primary: String, more: Int) = "$primary  ·  +$more số khác"
        override fun noPrivatePhoneEmergency(fallback: String) = "Chưa có số riêng · khẩn cấp $fallback"
        override val noPhone = "Chưa có số điện thoại"
        override val unnamed = "(Không tên)"
        override val noAddress = "Chưa có địa chỉ"
        override fun emergencyCallNote(fallback: String) = "Chưa có số riêng. Khi khẩn cấp gọi $fallback."
        override val mapChip = "Bản đồ"
        override val prevPage = "Trang trước"
        override val nextPage = "Trang sau"
        override fun pageOf(current: Int, count: Int) = "Trang $current / $count"
        override fun pageCompact(current: Int, count: Int) = "Trang $current/$count"
        override val emptyNone = "Chưa có cơ quan nào."
        override fun emptySearch(query: String) = "Không tìm thấy \"$query\"."
        override val loadFailed = "Không tải được danh bạ.\nKiểm tra kết nối mạng rồi thử lại."
        override val note1Title = "Chỉ mang tính tham khảo"
        override val note1Body = "Số điện thoại và địa chỉ trong danh bạ chỉ để THAM KHẢO. Dữ liệu có thể CHƯA chính xác hoặc đã được cơ quan nhà nước cập nhật, thay đổi (đặc biệt sau sáp nhập đơn vị hành chính 01/7/2025)."
        override val note2Title = "Nghiêm cấm sao chép, làm giả"
        override val note2Body = "Nghiêm cấm mọi hành vi sao chép, làm giả, chỉnh sửa thông tin nhằm mạo danh cơ quan chức năng dưới mọi hình thức."
        override val note3Title = "Người dùng tự chịu rủi ro"
        override val note3Body = "Bạn TỰ CHỊU mọi rủi ro khi liên hệ tới các số điện thoại trong danh bạ. Hãy xác minh lại thông tin trước khi cung cấp dữ liệu cá nhân hay thực hiện giao dịch."
        override fun emergencyPolice(number: String) = "$number (an ninh)"
        override fun emergencyFire(number: String) = "$number (cứu hoả)"
        override fun emergencyMedical(number: String) = "$number (cấp cứu)"
        override val emergencyTitle = "Số khẩn cấp"
        override fun emergencyCallBody(numbers: String) = "Khi khẩn cấp hãy gọi: $numbers."
        override fun metaUpdated(updated: String) = "\nCập nhật: $updated"
    }

    private object ShareSheet : ShareSheetStrings {
        override val invalidNumber = "Số điện thoại không hợp lệ để chia sẻ"
        override val asTextTitle = "Chia sẻ dạng văn bản"
        override val asTextSubtitle = "Gửi tên và số điện thoại"
        override val asQrTitle = "Chia sẻ mã QR"
        override val asQrSubtitle = "Hiển thị và tải mã QR về máy"
        override val myQrTitle = "Chia sẻ QR của Tôi"
        override val myQrSubtitle = "Mã QR số của bạn theo SIM"
        override val contactQrTitle = "Mã QR liên hệ"
        override val qrUnavailable = "Không tạo được mã QR cho số này."
        override fun contactQrHelper(number: String) = "Đưa mã QR này cho người khác quét bằng camera để gọi tới số $number."
        override val myQr = "QR của Tôi"
        override val chooseSim = "Chọn SIM"
        override val noSim = "Không tìm thấy SIM nào trên máy để tạo mã QR của bạn."
        override val noNumberEnter = "Chưa có số — nhập trong Cài đặt"
        override val chooseSimHint = "Chọn SIM để tạo mã QR số của bạn"
        override fun mobileLine(number: String) = "Di động: $number"
        override fun carrierLine(carrier: String) = "Nhà mạng: $carrier"
        override val unknown = "Không rõ"
        override fun noNumberForSlot(slotLabel: String) = "Chưa có số cho $slotLabel. Vào Cài đặt › “Số điện thoại của tôi” để nhập số."
        override val myQrHelper = "Người khác quét mã QR này để lưu và gọi tới số của bạn."
        override val qrImageDesc = "Mã QR"
        override val saving = "Đang lưu…"
        override val saveToDevice = "Tải về máy"
        override val savedToGallery = "Đã lưu ảnh QR vào thư viện"
        override val saveFailed = "Không lưu được ảnh QR"
        override val opening = "Đang mở…"
        override val share = "Chia sẻ"
        override val shareFailed = "Không chia sẻ được ảnh QR"
    }

    private object QrScanner : QrScannerStrings {
        override val noQrInImage = "Không tìm thấy mã QR trong ảnh"
        override val torchOn = "Bật đèn"
        override val torchOff = "Tắt đèn"
        override val instruction = "Đưa mã QR vào khung · Chạm để lấy nét, chụm hai ngón để phóng to."
        override val decoding = "Đang đọc ảnh…"
        override val pickImage = "Chọn ảnh từ thư viện"
        override val noCameraPerm = "Chưa cấp quyền camera. Bạn vẫn có thể chọn ảnh từ thư viện để quét mã QR."
    }

    private object QrAction : QrActionStrings {
        override val scannedContent = "Nội dung mã QR đã quét"
        override val openLink = "Mở liên kết"
        override val sendEmail = "Gửi email"
        override val openWifiSettings = "Mở cài đặt Wi‑Fi"
        override val ssid = "Tên mạng (SSID)"
        override val password = "Mật khẩu"
        override val security = "Bảo mật"
        override val hiddenNetwork = "Mạng ẩn"
        override val yes = "Có"
        override val addToContacts = "Thêm vào danh bạ"
        override val name = "Tên"
        override val phone = "Điện thoại"
        override val org = "Tổ chức"
        override val openMap = "Mở bản đồ"
        override val label = "Nhãn"
        override val address = "Địa chỉ"
        override val latitude = "Vĩ độ"
        override val longitude = "Kinh độ"
        override val wifiNoPassword = "Không mật khẩu"
    }

    private object Permission : PermissionStrings {
        override val callLogStep = "Nhật ký cuộc gọi"
        override val callLogHeadline = "Xem lịch sử cuộc gọi"
        override val callLogDesc =
            "CallHS đọc nhật ký cuộc gọi trên máy để hiển thị lại đầy đủ: loại cuộc (đi / đến / nhỡ), " +
                "thời gian, thời lượng, SIM và nhà mạng của từng cuộc. Đây là quyền bắt buộc để ứng dụng hoạt động."
        override val callLogBullet1 = "Chỉ ĐỌC — không sửa, không xoá cuộc gọi"
        override val callLogBullet2 = "Dữ liệu ở lại trên máy, không gửi đi đâu"
        override val callLogBullet3 = "Xem loại cuộc gọi, SIM, nhà mạng, thời lượng"
        override val simStep = "Thông tin SIM"
        override val simHeadline = "Đọc thông tin SIM"
        override val simDesc =
            "CallHS đọc nhà mạng của SIM đang lắp (SIM 1 / SIM 2) để xác định mỗi cuộc gọi là nội mạng " +
                "hay ngoại mạng, từ đó ước tính cước phí chính xác hơn. Không đọc danh bạ và không tự thực hiện cuộc gọi."
        override val simBullet1 = "Nhận biết nhà mạng của từng SIM trên máy"
        override val simBullet2 = "Tính nội / ngoại mạng để ước tính cước gọi"
        override val simBullet3 = "Chỉ dùng để tính toán trên máy, không gửi đi"
        override val contactsStep = "Danh bạ"
        override val contactsHeadline = "Hiện tên người liên hệ"
        override val contactsDesc =
            "CallHS đọc danh bạ để thay số điện thoại bằng TÊN và ảnh đại diện đã lưu, giúp bạn nhận ra " +
                "ngay ai đã gọi. Ứng dụng chỉ đọc, không thêm / sửa / xoá danh bạ."
        override val contactsBullet1 = "Hiện tên & ảnh liên hệ thay cho số lạ"
        override val contactsBullet2 = "Dễ nhận ra ai đã gọi trong danh sách"
        override val contactsBullet3 = "Chỉ ĐỌC danh bạ — không sửa, không gửi đi"
        override fun stepIndicator(current: Int, total: Int, title: String) = "Bước $current/$total · $title"
        override val deniedMessage = "Bạn đã từ chối quyền này. Vui lòng bật thủ công trong Cài đặt để tiếp tục."
        override val consentStepTitle = "Điều khoản & quyền riêng tư"
        override val consentTitle = "Điều khoản & Quyền riêng tư"
        override val consentIntro =
            "Bước cuối cùng trước khi bắt đầu. Vui lòng đọc nhanh vài điểm quan trọng rồi đồng ý với " +
                "điều khoản để sử dụng CallHS."
        override val consentPoint1 = "Ứng dụng CHỈ ĐỌC dữ liệu — không sửa, xoá hay tạo cuộc gọi, tin nhắn, danh bạ trên máy bạn."
        override val consentPoint2 = "Nhật ký cuộc gọi và danh bạ chỉ nằm trên máy bạn, không tải lên máy chủ hay chia sẻ cho bên thứ ba."
        override val consentPoint3 = "Danh bạ cơ quan được tải từ Internet để tra cứu, chỉ mang tính tham khảo — không phải dịch vụ chính thức của cơ quan nhà nước."
        override val consentPoint4 = "Cước phí hiển thị trong ứng dụng chỉ là ước tính, có thể khác với bảng giá thực tế của nhà mạng."
        override val readTerms = "Đọc Điều khoản sử dụng"
        override val readPrivacy = "Đọc Chính sách quyền riêng tư"
        override val consentCheckLabel = "Tôi đã đọc và đồng ý với Điều khoản sử dụng và Chính sách quyền riêng tư của CallHS."
        override val consentAccept = "Đồng ý & vào ứng dụng"
        override val consentRequired = "Vui lòng tích vào ô đồng ý để tiếp tục"
        override val consentFooter = "Bạn bắt buộc đồng ý để dùng ứng dụng. Có thể xem lại các văn bản này bất cứ lúc nào trong phần Cài đặt."
        override val openInBrowser = "Mở trên trình duyệt"
    }

    private object Actions : ActionStrings {
        override val linkOpenFailed = "Không mở được liên kết"
        override val browserOpenFailed = "Không mở được trình duyệt"
        override val contactsAppOpenFailed = "Không mở được ứng dụng Danh bạ"
        override val contactOpenFailed = "Không mở được liên hệ"
        override val invalidPhone = "Số điện thoại không hợp lệ"
        override val invalidPhoneSearch = "Số điện thoại không hợp lệ để tìm kiếm"
        override val zaloAndBrowserUnavailable = "Chưa cài Zalo và không mở được trình duyệt"
        override val feedbackEmailSubject = "Góp ý ứng dụng CallHS"
        override val shareContactChooser = "Chia sẻ liên hệ"
        override val shareQrChooser = "Chia sẻ mã QR"
    }

    private object UpdateNotice : UpdateNoticeStrings {
        override val policyTitle = "Cập nhật chính sách"
        override val whatsNewTitle = "Có gì mới"
        override val gotIt = "Đã rõ"
        override val seeMore = "Xem thêm"
    }

    private object Language : LanguageStrings {
        override val cardTitle = "Ngôn ngữ"
        override val sectionChoose = "Chọn ngôn ngữ"
        override val optionSystem = "Theo hệ thống"
        override val vietnamese = "Tiếng Việt"
        override val english = "English"
        override fun currentlyUsing(name: String): String = "Đang dùng: $name"
        override val note =
            "Ngôn ngữ áp dụng ngay khi chọn, không cần khởi động lại. “Theo hệ thống” sẽ bám theo ngôn ngữ máy (chỉ hỗ trợ tiếng Việt và tiếng Anh; ngôn ngữ khác dùng tiếng Việt)."
    }

    private object Theme : ThemeStrings {
        override val cardTitle = "Giao diện"
        override val sectionChoose = "Chọn giao diện"
        override val optionSystem = "Theo hệ thống"
        override val optionLight = "Sáng"
        override val optionDark = "Tối"
        override fun currentlyUsing(name: String): String = "Đang dùng: $name"
        override val note =
            "Giao diện áp dụng ngay khi chọn, không cần khởi động lại. “Theo hệ thống” sẽ bám theo chế độ Sáng/Tối của máy."
    }

    private object Category : CategoryStrings {
        override val settingsTitle = "Phân loại nhóm"
        override val settingsSubtitle = "Gắn nhãn & lọc số theo nhóm bạn tự tạo"
        override val settingsSection = "Phân loại"
        override val open = "Mở"
        override val listTitle = "Phân loại nhóm"
        override fun memberCount(n: Int): String = "$n số"
        override val builtinWork = "Công việc"
        override val builtinFavorite = "Yêu thích"
        override val builtinLocked = "Nhóm mặc định — không đổi tên và không thể xoá."
        override val createTitle = "Tạo nhóm"
        override val editTitle = "Sửa nhóm"
        override val nameLabel = "Tên nhóm"
        override val nameHint = "VD: Khách hàng, Gia đình…"
        override val descLabel = "Mô tả"
        override val descHint = "Mô tả ngắn (tuỳ chọn)"
        override val iconLabel = "Biểu tượng & màu"
        override val pickIconTitle = "Chọn biểu tượng"
        override val iconGroupBasic = "Cơ bản"
        override val iconGroupDelivery = "Giao hàng"
        override val iconGroupWork = "Công việc"
        override val iconGroupIssue = "Sự cố giao"
        override val iconGroupSocial = "Xã giao"
        override val save = "Lưu"
        override val update = "Cập nhật"
        override val tabInfo = "Thông tin"
        override fun tabNumbers(n: Int): String = "Số điện thoại ($n)"
        override val emptyMembers = "Chưa có số nào trong nhóm này"
        override val removeMember = "Gỡ"
        override fun addedAt(time: String): String = "Đã thêm · $time"
        override val menuEdit = "Chỉnh sửa"
        override val menuDelete = "Xoá nhóm"
        override val deleteTitle = "Xoá nhóm?"
        override fun deleteWithMembers(name: String, n: Int): String =
            "Nhóm “$name” đang có $n số điện thoại. Xoá nhóm sẽ gỡ toàn bộ $n số khỏi nhóm. Bạn chắc chắn muốn xoá?"
        override fun deleteEmpty(name: String): String = "Xoá nhóm “$name”? Thao tác này không thể hoàn tác."
        override val deleteConfirm = "Xoá"
        override val cancel = "Huỷ"
        override val discardTitle = "Thoát mà không lưu?"
        override val discardMessage = "Thay đổi của bạn sẽ không được lưu."
        override val discardStay = "Ở lại"
        override val discardExit = "Thoát"
        override val addToCategoryTitle = "Thêm vào nhóm"
        override val createNew = "Tạo nhóm mới"
        override val noCategories = "Chưa có nhóm nào"
        override val newCategory = "Tạo phân loại"
        override val addToCategory = "Thêm vào nhóm"
        override val maxCategories = "Chỉ tạo được tối đa 5 nhóm"
        override val maxMembers = "Nhóm đã đạt tối đa 100 số"
        override val alreadyAdded = "Số đã có trong nhóm này"
        override fun addedTo(name: String): String = "Đã thêm vào “$name”"
        override fun removedFrom(name: String): String = "Đã gỡ khỏi “$name”"
    }

    private object Donate : DonateStrings {
        override val settingsSection = "Ủng hộ"
        override val cardTitle = "Ủng hộ nhà phát triển"
        override val cardSubtitle = "Quét QR chuyển khoản · Hoàn toàn tự nguyện · Cảm ơn bạn ❤️"
        override val open = "Mở trang ủng hộ"

        override val screenTitle = "Ủng hộ nhà phát triển"

        override val heroTitle = "Ủng hộ nhà phát triển"
        override val heroMessage =
            "CallHS được làm bằng tất cả tâm huyết và luôn miễn phí. Nếu ứng dụng có ích cho bạn, một ly cà phê nhỏ sẽ tiếp thêm động lực để mình duy trì và cải thiện. Hoàn toàn tự nguyện — không ủng hộ, mọi tính năng vẫn nguyên vẹn."

        override val amountSection = "Chọn số tiền"
        override val amountOpen = "Tuỳ tâm"
        override val amountOpenHint = "Bạn tự nhập số tiền khi chuyển khoản"
        override val amountCustom = "Số khác…"
        override val customDialogTitle = "Nhập số tiền ủng hộ"
        override val customFieldLabel = "Nhập số tiền (VND)"
        override val customInvalid = "Vui lòng nhập số tiền hợp lệ"
        override val customMax = "Tối đa 1.000.000 đ"
        override val confirm = "Xong"

        override val qrSection = "Quét mã để chuyển khoản"
        override val qrHint = "Mở app ngân hàng của bạn và quét mã QR này để chuyển khoản ủng hộ"
        override val qrOpenAmountNote = "Mã mở — bạn tự nhập số tiền tuỳ tâm khi chuyển"
        override val qrLoading = "Đang tạo mã QR…"
        override val qrError = "Không tải được mã QR. Kiểm tra kết nối mạng rồi thử lại."
        override val qrRetry = "Thử lại"
        override val qrOfflineFallback = "Mã dự phòng tạo tại máy (ngoại tuyến)"
        override val saveQr = "Lưu ảnh"
        override val shareQr = "Chia sẻ"
        override val qrSaved = "Đã lưu ảnh mã QR vào thư viện"
        override val qrSaveFailed = "Không lưu được ảnh mã QR"
        override val shareSubject = "Mã QR ủng hộ CallHS"

        override val accountSection = "Thông tin chuyển khoản"
        override val bankLabel = "Ngân hàng"
        override val accountNoLabel = "Số tài khoản"
        override val accountNameLabel = "Chủ tài khoản"
        override val amountRowLabel = "Số tiền"
        override val messageLabel = "Nội dung"
        override val amountOpenValue = "Tuỳ tâm"
        override val copied = "Đã sao chép"

        override val bankAppsSection = "Mở nhanh app ngân hàng"
        override val bankAppsHint = "Chọn ngân hàng để mở app — thông tin chuyển khoản sẽ được điền sẵn (nếu ngân hàng hỗ trợ). Nếu chưa, hãy quét mã QR ở trên."
        override val bankAppsShowAll = "Xem tất cả"
        override val bankAppsShowLess = "Thu gọn"
        override val bankAppOpenFailed = "Không mở được app ngân hàng"
        override val bankAppPrefill = "Điền sẵn"
        override val bankAppNeedNetwork = "Cần kết nối mạng để mở app ngân hàng"

        override val footerTitle = "Đóng góp hoàn toàn tự nguyện"
        override val footerMessage =
            "Đây là khoản đóng góp tự nguyện dành cho nhà phát triển, không phải phí bắt buộc và không đổi lấy bất kỳ tính năng nào. CallHS vẫn luôn miễn phí. Xin chân thành cảm ơn sự ủng hộ của bạn."
        override val thankYou = "Cảm ơn bạn rất nhiều ❤️"
    }
}
