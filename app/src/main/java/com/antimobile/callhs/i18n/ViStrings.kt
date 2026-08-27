package com.antimobile.callhs.i18n

import com.antimobile.callhs.data.blocking.ContactRuleCodec
import com.antimobile.callhs.data.blocking.CallHistoryRuleCodec
import com.antimobile.callhs.data.blocking.GeographicBlockOption
import com.antimobile.callhs.data.blocking.SpecialCallCondition
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
    override val outgoingCall: OutgoingCallStrings = OutgoingCall
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
    override val backup: BackupStrings = Backup
    override val blocker: CallBlockStrings = Blocker

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
        override val scrollToBottom = "Xuống cuối danh sách"
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
        override fun emptyNoCallsInScope(label: String) = "Không có cuộc gọi nào của $label với số này. Chọn “Tất cả SIM” ở Cài đặt để xem đầy đủ."
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
        override val loadError = "Không đọc được danh bạ. Kiểm tra quyền và thử lại."
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

    private object OutgoingCall : OutgoingCallStrings {
        override val settingsSection = "Cuộc gọi đi"
        override val settingsTitle = "Cảnh báo cuộc gọi đi"
        override val settingsSubtitle = "Ngoại mạng · Danh sách chặn · Danh sách cho phép"
        override val settingsOpen = "Mở cài đặt cuộc gọi đi"

        override val screenTitle = "Cài đặt cuộc gọi đi"
        override val activationSection = "Kích hoạt"
        override val enabledTitle = "Nhận biết cuộc gọi đi"
        override val enabledSubtitle = "Hiển thị cảnh báo ngay khi bạn gọi từ ứng dụng Điện thoại mặc định"
        override val roleGateTitle = "Bật cảnh báo cuộc gọi đi"
        override val roleGateBody =
            "Android yêu cầu bạn chọn CallHS làm ứng dụng chuyển tiếp cuộc gọi. Quyền này chỉ giúp CallHS nhận số và SIM trước khi gọi để hiển thị cảnh báo; CallHS luôn giữ nguyên số, không chuyển hướng và không chặn cuộc gọi đi."
        override val roleGateAction = "Chọn CallHS để nhận biết cuộc gọi"
        override val roleUnavailableTitle = "Thiết bị chưa hỗ trợ"
        override val roleUnavailableBody =
            "Thiết bị này không cung cấp vai trò chuyển tiếp cuộc gọi của Android nên CallHS chưa thể nhận biết cuộc gọi đi."
        override val roleActive = "Đã được Android cho phép nhận biết cuộc gọi"
        override val roleRequired = "Cần cấp quyền nhận dạng cuộc gọi của Android"
        override val roleUnavailable = "Thiết bị này không hỗ trợ vai trò nhận dạng cuộc gọi"
        override val roleExplanation =
            "Android cấp vai trò xử lý cuộc gọi đi để CallHS nhận số và SIM trước khi gọi. CallHS luôn giữ nguyên số, không chuyển hướng và không chặn cuộc gọi đi."
        override val conditionsSection = "Thông báo khi"
        override val offNetworkTitle = "Gọi đến số ngoại mạng"
        override val offNetworkSubtitle = "Chỉ cảnh báo khi xác định chắc nhà mạng của SIM gọi và số nhận"
        override val simPermissionTitle = "Chưa có quyền đọc thông tin SIM"
        override val simPermissionSubtitle =
            "Quyền Điện thoại chỉ dùng để xác định nhà mạng của SIM đang gọi. Không cần quyền Nhật ký cuộc gọi."
        override val grantSimPermission = "Cấp quyền SIM"
        override val blocklistTitle = "Số nằm trong danh sách chặn"
        override val blocklistSubtitle = "Dùng danh sách số chặn hiện có, kể cả khi màn Chặn cuộc gọi đang tắt"
        override val allowlistTitle = "Số nằm trong danh sách cho phép"
        override val allowlistSubtitle = "Xác nhận nhanh số gọi đi đã có trong danh sách cho phép"
        override val presentationSection = "Kiểu hiển thị"
        override val presentationTitle = "Cách cảnh báo"
        override val headsUpTitle = "Thông báo nổi (heads-up)"
        override val headsUpSubtitle = "Hiện ở đầu màn hình và lưu trong thanh thông báo"
        override val overlayTitle = "Popup trên ứng dụng khác"
        override val overlaySubtitle = "Hiện hộp thoại theo kiểu AppDialog trên màn hình đang gọi"
        override val overlayPermissionTitle = "Chưa có quyền hiển thị trên ứng dụng khác"
        override val overlayPermissionSubtitle =
            "Nếu chưa cấp quyền, CallHS sẽ tự dùng thông báo heads-up làm phương án dự phòng."
        override val grantOverlayPermission = "Cấp quyền hiển thị"
        override val notificationPermissionTitle = "Thông báo nổi chưa sẵn sàng"
        override val notificationPermissionSubtitle =
            "Cho phép thông báo và giữ kênh cảnh báo ở mức ưu tiên cao để heads-up có thể xuất hiện."
        override val grantNotificationPermission = "Cho phép thông báo"
        override val openNotificationSettings = "Mở cài đặt thông báo"
        override val privacyNote =
            "Số gọi và thông tin nhà mạng chỉ được xử lý trên thiết bị. CallHS không chuyển hướng, thay đổi hay tự thực hiện cuộc gọi."

        override val notificationChannelName = "Cảnh báo cuộc gọi đi"
        override val notificationChannelDescription =
            "Cảnh báo ngoại mạng và trạng thái danh sách khi người dùng bắt đầu cuộc gọi đi"
        override val alertBlocklistTitle = "Cảnh báo số trong danh sách chặn"
        override val alertOffNetworkTitle = "Cảnh báo cuộc gọi ngoại mạng"
        override val alertAllowlistTitle = "Số thuộc danh sách cho phép"
        override val reasonBlocklist = "Số này nằm trong danh sách chặn."
        override val reasonAllowlist = "Số này nằm trong danh sách cho phép."
        override fun reasonOffNetwork(simCarrier: String, targetCarrier: String) =
            "Cuộc gọi ngoại mạng: $simCarrier → $targetCarrier."
        override val close = "Đóng"
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
        override val contactsHeadline = "Sàng lọc cả số trong danh bạ"
        override val contactsDesc =
            "Android chỉ chuyển cuộc gọi từ số đã lưu tới CallHS khi quyền Danh bạ còn hiệu lực. " +
                "CallHS dùng quyền này để áp dụng đầy đủ quy tắc chặn và hiển thị tên, ảnh; ứng dụng " +
                "chỉ đọc, không sửa hoặc gửi danh bạ."
        override val contactsBullet1 = "Áp dụng quy tắc chặn cho cả số đã lưu trong danh bạ"
        override val contactsBullet2 = "Hiển thị tên và ảnh trong lịch sử cuộc gọi"
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
        override val invalidNumber = "Số không hợp lệ, không thể thêm vào nhóm"
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

    private object Blocker : CallBlockStrings {
        override val settingsSection = "Chặn cuộc gọi"
        override val settingsTitle = "Chặn cuộc gọi & spam"
        override val settingsSubtitle = "Quy tắc chặn, lịch sử và thông báo"
        override val settingsOpen = "Mở chặn cuộc gọi"

        override val screenTitle = "Chặn cuộc gọi"
        override val settingsScreenTitle = "Cài đặt chặn cuộc gọi"
        override val openSettings = "Mở cài đặt chặn cuộc gọi"
        override val featureDetailsAction = "Xem chi tiết tính năng"
        override val featureInfoSheetTitle = "Thông tin tính năng chặn"
        override val featureInfoAvailabilityNote =
            "Chỉ áp dụng cho cuộc gọi đến khi CallHS đang là ứng dụng sàng lọc, Bảo vệ cuộc gọi đang bật và không trong thời gian tạm dừng."
        override val roleTitle = "Bật chặn cuộc gọi"
        override val roleBody =
            "Android yêu cầu bạn chọn CallHS làm ứng dụng sàng lọc cuộc gọi. CallHS chỉ sàng lọc cuộc gọi đến, không thay ứng dụng gọi điện mặc định của máy."
        override val roleAction = "Chọn CallHS để chặn spam"
        override val roleUnavailableTitle = "Thiết bị chưa hỗ trợ"
        override val roleUnavailableBody = "Thiết bị này không cung cấp vai trò sàng lọc cuộc gọi của Android."
        override val roleActive = "CallHS đang là ứng dụng sàng lọc cuộc gọi"

        override val protectionTitle = "Bảo vệ cuộc gọi"
        override val protectionSubtitle =
            "Trong thời gian tạm dừng, mọi cuộc gọi đều được cho qua; quy tắc và các thiết lập bảo vệ vẫn được giữ nguyên rồi tự hoạt động lại khi hết giờ."
        override val protectionOn = "Đang sàng lọc cuộc gọi"
        override val protectionOff = "Đang tắt bảo vệ"
        override val enableProtectionAction = "Bật chặn cuộc gọi"
        override val disableProtectionAction = "Tắt chặn cuộc gọi"
        override val pauseTimerTitle = "Hẹn giờ tạm dừng"
        override val pauseTimerOff = "Tắt"
        override val pauseTimer10Minutes = "10p"
        override val pauseTimer30Minutes = "30p"
        override val pauseTimer1Hour = "1h"
        override val pauseTimerOffExplanation =
            "“Tắt” chỉ huỷ hẹn giờ; bảo vệ vẫn hoạt động khi công tắc chính đang bật."
        override val pauseActive = "Đang tạm dừng theo hẹn giờ"
        override fun pausePeriod(from: String, to: String) = "$from → $to"
        override fun pauseRemaining(countdown: String) = "Còn lại $countdown"
        override val pauseUnavailableWhileOff = "Bật bảo vệ để dùng hẹn giờ tạm dừng."
        override val dailyScheduleTitle = "Lịch chặn & ngưng"
        override fun dailyScheduleCount(count: Int, max: Int) = "$count/$max khung giờ"
        override val dailyScheduleDescription =
            "Lặp lại vào các thứ đã chọn. Trong khung Chặn, CallHS bật bảo vệ; trong khung Ngưng, mọi cuộc gọi được cho qua."
        override val dailyScheduleBaseState = "Ngoài các khung giờ, trạng thái theo công tắc Bảo vệ cuộc gọi."
        override val dailyScheduleEmpty = "Chưa có khung giờ. Chọn một mẫu hoặc tự đặt giờ."
        override val dailyScheduleAdd = "Thêm khung giờ"
        override val dailyScheduleLimitReached = "Đã đạt giới hạn 4 khung giờ."
        override val dailyScheduleBlock = "Chặn"
        override val dailySchedulePause = "Ngưng"
        override val dailyScheduleBlockActive = "Đang chặn theo lịch"
        override val dailySchedulePauseActive = "Đang ngưng theo lịch"
        override val dailyScheduleTimelineDescription = "Biểu đồ lịch chặn và ngưng trong 24 giờ"
        override val dailyScheduleEditorAddTitle = "Thêm lịch trong ngày"
        override val dailyScheduleEditorEditTitle = "Sửa lịch trong ngày"
        override val dailyScheduleActionTitle = "Trong khung giờ này"
        override val dailySchedulePresetTitle = "Chọn nhanh"
        override val dailyScheduleMorning = "Sáng"
        override val dailyScheduleAfternoon = "Chiều"
        override val dailyScheduleEvening = "Tối"
        override val dailyScheduleNight = "Đêm"
        override val dailyScheduleCustom = "Tự đặt giờ"
        override val dailyScheduleDaysTitle = "Ngày áp dụng"
        override val dailyScheduleEveryDay = "Hằng ngày"
        override fun dailyScheduleWeekdayShort(day: java.time.DayOfWeek) = when (day) {
            java.time.DayOfWeek.MONDAY -> "T2"
            java.time.DayOfWeek.TUESDAY -> "T3"
            java.time.DayOfWeek.WEDNESDAY -> "T4"
            java.time.DayOfWeek.THURSDAY -> "T5"
            java.time.DayOfWeek.FRIDAY -> "T6"
            java.time.DayOfWeek.SATURDAY -> "T7"
            java.time.DayOfWeek.SUNDAY -> "CN"
        }
        override fun dailyScheduleToday(day: String) = "Hôm nay · $day"
        override val dailyScheduleEnabled = "Đang bật"
        override val dailyScheduleDisabled = "Đang tắt"
        override val dailyScheduleStartTime = "Bắt đầu"
        override val dailyScheduleEndTime = "Kết thúc"
        override val dailyScheduleTimeConfirm = "Xong"
        override val dailyScheduleNextDay = "sang hôm sau"
        override val dailyScheduleSave = "Lưu khung giờ"
        override val dailyScheduleDelete = "Xoá khung giờ"
        override val dailyScheduleOverlapTitle = "Lịch bị chồng lấn"
        override val dailyScheduleOverlapConfirm = "Đã hiểu"
        override fun dailyScheduleOverlapError(from: String, to: String) =
            "Khung giờ này chồng lấn với lịch $from–$to. Hãy chọn giờ khác."
        override val dailyScheduleInvalidError = "Giờ bắt đầu và kết thúc phải khác nhau."
        override val dailyScheduleNoDayError = "Hãy chọn ít nhất một ngày áp dụng."
        override val dailyScheduleStorageError = "Không lưu được lịch. Vui lòng thử lại."
        override val protectionOffBannerBody =
            "Quy tắc và các thiết lập vẫn được lưu nhưng hiện không sàng lọc cuộc gọi. Nhấn nút đỏ bên dưới để bật lại bảo vệ."
        override val protectionPausedBannerBody =
            "Mọi cuộc gọi đang được cho qua; các tính năng bảo vệ sẽ tự hoạt động lại khi hết thời gian."
        override val repeatCallerExceptionTitle = "Cho phép số lạ gọi lặp"
        override fun repeatCallerExceptionSubtitle(threshold: Int, minutes: Int) =
            "Cơ chế này chỉ áp dụng khi CallHS xác nhận số nằm ngoài danh bạ và cuộc gọi không khớp bất kỳ quy tắc nào. Các lượt trước ngưỡng $threshold được xử lý theo Phương thức xử lý đã chọn: hai chế độ Chặn ghi lịch sử và thông báo theo cài đặt; Chỉ tắt tiếng chỉ tắt chuông, không ghi lịch sử chặn; Bỏ qua cho phép mọi cuộc gọi đi qua và không đếm lượt. Khi có đủ $threshold lượt trong cửa sổ $minutes phút gần nhất, cuộc hiện tại và các cuộc tiếp theo được cho qua miễn cửa sổ trượt vẫn đủ ngưỡng. Nếu số đã khớp quy tắc, cơ chế gọi lặp không được xét và không ghi đè kết quả. Ngoại lệ danh bạ vẫn áp dụng riêng cho quy tắc diện rộng. Riêng khi xét cơ chế gọi lặp này, nếu CallHS không thể xác minh danh bạ, cuộc gọi được cho qua để tránh chặn nhầm."
        override val repeatCallerExceptionOff = "Cơ chế số lạ gọi lặp đang tắt"
        override fun repeatCallerExceptionOn(threshold: Int, minutes: Int) =
            "Trước lượt $threshold: theo phương thức đã chọn · Từ lượt $threshold: cho qua trong $minutes phút"
        override val repeatCallerThresholdTitle = "Ngưỡng bắt đầu cho qua"
        override fun repeatCallerThresholdOption(threshold: Int) = "Cho qua từ lượt thứ $threshold"
        override val repeatCallerWindowTitle = "Cửa sổ đếm cuộc gọi"
        override fun repeatCallerWindowValue(minutes: Int) = "$minutes phút"
        override val repeatCallerWindowSheetTitle = "Cửa sổ đếm số lạ gọi lặp"
        override fun repeatCallerWindowHint(minMinutes: Int, maxMinutes: Int) =
            "Nhập cửa sổ từ $minMinutes đến $maxMinutes phút"
        override fun repeatCallerWindowInvalid(minMinutes: Int, maxMinutes: Int) =
            "Hãy nhập cửa sổ từ $minMinutes đến $maxMinutes phút."
        override val repeatCallerApply = "Áp dụng"
        override val blockMethodTitle = "Phương thức xử lý"
        override val blockMethodSubtitle =
            "Chọn cách CallHS xử lý khi cuộc gọi khớp quy tắc hoặc bị cổng bảo vệ số lạ giữ lại trước ngưỡng."
        override val chooseBlockMethod = "Chọn phương thức xử lý"
        override val methodBlockAndReject = "Chặn và ngắt cuộc gọi"
        override val methodBlockAndRejectDesc = "Không cho cuộc gọi đi qua và gửi tín hiệu từ chối tới người gọi."
        override val methodBlockWithoutReject = "Chặn mà không từ chối"
        override val methodBlockWithoutRejectDesc = "Không cho cuộc gọi đi qua nhưng không chủ động gửi tín hiệu từ chối."
        override val methodSilenceOnly = "Chỉ tắt tiếng"
        override val methodSilenceOnlyDesc = "Cuộc gọi vẫn xuất hiện nhưng thiết bị không đổ chuông."
        override val methodAllow = "Bỏ qua"
        override val methodAllowDesc =
            "Cho phép mọi cuộc gọi đi qua. Quy tắc vẫn được lưu nhưng không được áp dụng, và cơ chế số lạ gọi lặp không đếm lượt cho tới khi chọn phương thức khác."
        override val notificationTitleSetting = "Thông báo cuộc gọi bị chặn"
        override val notificationSubtitle = "Bật hoặc tắt cảnh báo âm thanh mỗi khi CallHS chặn cuộc gọi."
        override val notificationPermissionNeeded = "Cần cho phép thông báo trong Android để nhận cảnh báo."
        override val notificationPermissionAction = "Cho phép thông báo"
        override val notificationChannelNeedsAttention =
            "Kênh cảnh báo đang bị tắt, tắt âm thanh hoặc chưa ở mức Khẩn cấp để hiện heads-up."
        override val notificationChannelSettingsAction = "Mở cài đặt kênh"
        override val notificationOff = "Tắt thông báo"
        override val notificationEvery = "Thông báo mỗi lần chặn"

        override val alwaysAllowTitle = "Danh sách cho phép"
        override val alwaysAllowSubtitle = "Các số trong danh sách này luôn được cho qua trước tiên."
        override val alwaysAllowDetails =
            "Thêm số bằng cách nhập tay, chọn từ Danh bạ, Lịch sử cuộc gọi hoặc Phân loại nhóm. Khi một số trong danh sách này gọi đến, CallHS cho qua ngay.\n\n" +
                "Danh sách cho phép luôn được kiểm tra đầu tiên. Nguồn chọn chỉ giúp lấy số và không tự cập nhật khi liên hệ thay đổi."
        override val blockedNumbersTitle = "Danh sách chặn"
        override val blockedNumbersSubtitle = "Các số cụ thể sẽ bị chặn khi gọi đến."
        override val blockedNumbersDetails =
            "Thêm số bằng cách nhập tay, chọn từ Danh bạ, Lịch sử cuộc gọi hoặc Phân loại nhóm. Khi một số trong danh sách này gọi đến, CallHS áp dụng phương thức chặn bạn đã chọn.\n\n" +
                "Danh sách chặn được kiểm tra sau Danh sách cho phép và trước mọi lựa chọn khác."
        override val groupBlockingTitle = "Xử lý theo danh bạ"
        override val groupBlockingSubtitle = "Chọn cách xử lý số trong danh bạ và số ngoài danh bạ."
        override val groupBlockingDetails =
            "Chọn cách CallHS xử lý số đã lưu trong danh bạ và số ngoài danh bạ. Mỗi lựa chọn đều ghi rõ cuộc gọi sẽ được cho qua, bị chặn hay tiếp tục xét theo Quy tắc nâng cao.\n\n" +
                "Danh sách cho phép và Danh sách chặn luôn được kiểm tra trước. Chế độ gọi lặp chỉ chạy khi không có quy tắc nào khớp."
        override val advancedRulesTitle = "Quy tắc nâng cao"
        override val advancedRulesSubtitle = "Bộ lọc dấu hiệu spam, mẫu số, nhà mạng, khu vực và loại đặc biệt."
        override val advancedRulesDetails =
            "Dùng bộ lọc cuộc gọi có dấu hiệu spam hoặc tạo điều kiện theo đầu số, đuôi số, chuỗi số, độ dài, nhà mạng, khu vực hay loại cuộc gọi. Mỗi quy tắc có thể áp dụng cho số trong danh bạ, ngoài danh bạ hoặc tất cả.\n\n" +
                "CallHS xét các quy tắc đang bật từ trên xuống và áp dụng quy tắc đầu tiên khớp."
        override fun savedNumberCount(count: Int) = "$count số"
        override val manageSection = "Quản lý chặn cuộc gọi"
        override val allowlistScreenTitle = "Danh sách cho phép"
        override val blocklistScreenTitle = "Danh sách chặn"
        override val allowlistEmpty = "Danh sách cho phép đang trống."
        override val blocklistEmpty = "Danh sách chặn đang trống."
        override val addNumber = "Thêm số"
        override val addNumberSourceTitle = "Chọn nguồn số"
        override val sourceEnterManually = "Nhập số thủ công"
        override val sourceFromContacts = "Chọn từ danh bạ"
        override val sourceFromCallHistory = "Chọn từ lịch sử cuộc gọi"
        override val sourceFromCategories = "Chọn từ phân loại nhóm"
        override val enterNumberTitle = "Thêm số điện thoại"
        override val enterNumberHint = "Số điện thoại"
        override val enterNumberNameHint = "Tên gợi nhớ (không bắt buộc)"
        override val addToAllowlist = "Thêm vào Danh sách cho phép"
        override val addToBlocklist = "Thêm vào danh sách chặn"
        override val numberAlreadyExists = "Số này đã có trong danh sách."
        override val numberMovedToAllowlist = "Đã chuyển số sang Danh sách cho phép."
        override val numberMovedToBlocklist = "Đã chuyển số sang danh sách chặn."
        override fun numberAddedAt(time: String) = "Đã thêm lúc $time"
        override val menuDeleteNumber = "Xoá số khỏi danh sách"
        override val menuMoveToAllowlist = "Chuyển sang Danh sách cho phép"
        override val menuMoveToBlocklist = "Chuyển sang danh sách chặn"
        override val menuEnableNumber = "Bật số này"
        override val menuDisableNumber = "Tắt số này"
        override val advancedOrderNote = "Quy tắc đầu tiên khớp sẽ được áp dụng. Nhấn giữ một quy tắc để đổi thứ tự."
        override val menuMoveRuleUp = "Đưa quy tắc lên"
        override val menuMoveRuleDown = "Đưa quy tắc xuống"
        override val menuEnableRule = "Bật quy tắc"
        override val menuDisableRule = "Tắt quy tắc"
        override val enableAllAdvancedRules = "Bật tất cả quy tắc"
        override val disableAllAdvancedRules = "Tắt tất cả quy tắc"
        override val deleteAllAdvancedRules = "Xoá tất cả quy tắc"
        override fun enableAllAdvancedRulesMessage(count: Int) =
            "Bật toàn bộ $count quy tắc nâng cao. Các quy tắc sẽ được áp dụng lại theo thứ tự hiện tại khi Bảo vệ cuộc gọi hoạt động."
        override fun disableAllAdvancedRulesMessage(count: Int) =
            "Tắt toàn bộ $count quy tắc nâng cao. Quy tắc vẫn được lưu để bật lại; các danh sách số và xử lý theo danh bạ không bị ảnh hưởng."
        override fun deleteAllAdvancedRulesMessage(count: Int) =
            "Xoá vĩnh viễn toàn bộ $count quy tắc nâng cao. Thao tác này không thể hoàn tác; các danh sách số và lịch sử chặn không bị xoá."
        override val groupScreenTitle = "Xử lý theo danh bạ"
        override val blockSavedContactsGroup = "Chặn toàn bộ số trong danh bạ"
        override val blockSavedContactsGroupDesc = "Áp dụng với mọi số đã lưu, trừ số trong Danh sách cho phép."
        override val blockUnknownNumbersGroup = "Xử lý số ngoài danh bạ"
        override val blockUnknownNumbersGroupDesc = "Chọn cho qua, luôn chặn, hoặc chặn tới khi số gọi lặp đạt ngưỡng."
        override val unknownPolicyTitle = "Ngoài danh bạ"
        override val unknownPolicyPass = "Cho qua nếu không khớp quy tắc"
        override val unknownPolicyBlockAlways = "Chặn toàn bộ"
        override val unknownPolicyBlockUntilRepeat = "Chặn đến khi gọi lặp"
        override val unknownPolicyPassDesc = "CallHS vẫn xét Danh sách chặn và Quy tắc nâng cao. Nếu không có gì khớp, cuộc gọi được cho qua."
        override val unknownPolicyBlockAlwaysDesc = "Mọi số được xác nhận là ngoài danh bạ sẽ bị chặn, trừ số có trong Danh sách cho phép."
        override val unknownPolicyBlockUntilRepeatDesc = "Nếu không khớp quy tắc nào, CallHS chặn các lần gọi đầu và cho qua khi số đó gọi lặp đạt ngưỡng trong thời gian đã đặt."
        override val specialGroupsTitle = "Loại cuộc gọi đặc biệt"
        override val advancedRulesScreenTitle = "Quy tắc nâng cao"
        override val advancedRulesEmpty = "Chưa có quy tắc nâng cao."
        override val addAdvancedRule = "Thêm quy tắc nâng cao"
        override val ruleScopeLabel = "Kiểm tra số nào?"
        override val scopeUnknown = "Số chưa lưu"
        override val scopeContacts = "Số đã lưu"
        override val scopeAll = "Mọi số"
        override val scopeUnknownDesc = "Chỉ kiểm tra số chưa lưu trong danh bạ."
        override val scopeContactsDesc = "Chỉ kiểm tra số đã lưu trong danh bạ."
        override val scopeAllDesc = "Kiểm tra cả số đã lưu và chưa lưu."
        override fun ruleScopeSummary(scope: String) = when (scope) {
            "saved_contact" -> scopeContacts
            "not_saved" -> scopeUnknown
            else -> scopeAll
        }
        override fun rulePreview(summary: String, scope: String) = "Chặn · $scope · $summary"
        override val typeLength = "Số có bao nhiêu chữ số"
        override val lengthHint = "Ví dụ: 10"
        override val ruleActionLabel = "CallHS sẽ làm gì?"
        override val actionBlock = "Chặn"
        override val actionAllow = "Cho qua"
        override val actionBlockDesc = "Chặn theo cách đã chọn trong Cài đặt chặn cuộc gọi."
        override val actionAllowDesc = "Cho cuộc gọi đi qua."
        override val savedPolicyTitle = "Trong danh bạ"
        override val savedPolicyFollowRules = "Áp dụng theo quy tắc"
        override val savedPolicyAllow = "Luôn cho qua"
        override val savedPolicyBlock = "Chặn toàn bộ"
        override val savedPolicyFollowRulesDesc = "Lựa chọn này sẽ được tiếp tục xét theo Quy tắc nâng cao."
        override val savedPolicyAllowDesc = "Mọi số đã lưu trong danh bạ sẽ được cho qua, trừ số có trong Danh sách chặn."
        override val savedPolicyBlockDesc = "Mọi số đã lưu trong danh bạ sẽ bị chặn, trừ số có trong Danh sách cho phép."
        override val groupPriorityNote = "Danh sách cho phép và Danh sách chặn luôn được xét trước các lựa chọn tại đây."
        override val processingGuideItemTitle = "Tìm hiểu cách CallHS chặn cuộc gọi"
        override val processingGuideItemSubtitle = "Xem thứ tự CallHS kiểm tra và xử lý một cuộc gọi đến."
        override val processingGuideSheetTitle = "Cách CallHS xử lý cuộc gọi"
        override val processingGuideIntro = "CallHS kiểm tra lần lượt theo thứ tự dưới đây và dừng ngay khi đã có kết quả."
        override fun processingGuideStepTitle(step: Int) = when (step) {
            1 -> "Kiểm tra bảo vệ cuộc gọi"
            2 -> "Kiểm tra Danh sách cho phép"
            3 -> "Kiểm tra Danh sách chặn"
            4 -> "Xử lý theo danh bạ"
            5 -> "Kiểm tra Quy tắc nâng cao"
            else -> "Áp dụng kết quả mặc định"
        }
        override fun processingGuideStepDescription(step: Int) = when (step) {
            1 -> "Nếu bảo vệ đang tắt hoặc tạm dừng, mọi cuộc gọi đều được cho qua."
            2 -> "Số có trong Danh sách cho phép được cho qua ngay."
            3 -> "Nếu số có trong Danh sách chặn, cuộc gọi sẽ bị chặn."
            4 -> "CallHS áp dụng lựa chọn dành cho số trong hoặc ngoài danh bạ. Chọn Áp dụng theo quy tắc để tiếp tục bước kế tiếp."
            5 -> "CallHS xét từ trên xuống. Quy tắc đầu tiên khớp sẽ được áp dụng."
            else -> "Nếu không có danh sách hay quy tắc nào khớp, cuộc gọi được cho qua. Chế độ gọi lặp chỉ áp dụng ở bước cuối cho số ngoài danh bạ."
        }
        override val processingGuideConclusion = "Danh sách cho phép luôn có ưu tiên cao nhất."

        override val commonIssuesTitle = "Các vấn đề thường gặp"
        override val commonIssuesSubtitle =
            "Nguyên nhân và cách xử lý khi chặn cuộc gọi hoặc thông báo hoạt động chưa đúng."
        override val commonIssuesIntro =
            "Chọn vấn đề bạn đang gặp. Hãy kiểm tra các nguyên nhân theo thứ tự trước khi thay đổi quy tắc."
        override val commonIssuesPossibleCause = "Nguyên nhân có thể"
        override val commonIssuesHowToFix = "Cách khắc phục"
        override val commonIssuesOpenBlockSettings = "Mở cài đặt chặn cuộc gọi"
        override val commonIssuesOpenNotificationSettings = "Mở cài đặt thông báo Android"
        override val commonIssuesExpand = "Xem nguyên nhân và cách khắc phục"
        override val commonIssuesCollapse = "Thu gọn hướng dẫn"
        override fun commonIssueTitle(issue: Int) = when (issue) {
            1 -> "Chặn cuộc gọi không hoạt động"
            2 -> "Một số cuộc gọi vẫn lọt qua"
            3 -> "Số quan trọng hoặc số trong danh bạ bị chặn nhầm"
            4 -> "Không thấy thông báo sau khi chặn"
            5 -> "Thông báo không có âm thanh, rung hoặc cửa sổ nổi"
            6 -> "Không thấy cuộc gọi trong Lịch sử chặn"
            else -> "Không chặn được số ẩn hoặc VoIP"
        }
        override fun commonIssueCause(issue: Int) = when (issue) {
            1 -> "CallHS có thể không còn là ứng dụng sàng lọc cuộc gọi; Bảo vệ cuộc gọi đang tắt, tạm dừng hoặc nằm trong khung giờ tạm dừng. Sau khi buộc dừng ứng dụng, hoặc trước lần mở khóa đầu tiên sau khi khởi động lại, Android cũng có thể chưa chuyển cuộc gọi cho CallHS."
            2 -> "Số gọi đến có thể nằm trong Danh sách cho phép, được lựa chọn xử lý theo danh bạ hoặc quy tắc đầu tiên cho qua, hay đã đạt ngưỡng gọi lặp. Phương thức Chỉ tắt tiếng và Bỏ qua cũng không từ chối cuộc gọi."
            3 -> "Danh sách chặn được kiểm tra trước cách xử lý theo danh bạ. Một Quy tắc nâng cao áp dụng cho số đã lưu hoặc mọi số cũng có thể khớp và chặn liên hệ đó."
            4 -> "Thông báo có thể đang tắt trong CallHS, bị tắt ở khung giờ hiện tại, chưa được Android cấp quyền, hoặc kênh thông báo đã bị tắt. Chỉ cuộc gọi thực sự bị chặn mới tạo thông báo; Chỉ tắt tiếng và Bỏ qua không tạo cảnh báo chặn."
            5 -> "Âm thanh, rung hoặc kiểu hiển thị có thể đang tắt trong Cài đặt thông báo nâng cao. Chế độ Không làm phiền, mức ưu tiên của kênh và cài đặt thông báo riêng của hãng điện thoại cũng có thể hạn chế cảnh báo."
            6 -> "Lịch sử của CallHS chỉ ghi cuộc gọi thực sự bị chặn. Cuộc gọi được cho qua hoặc chỉ tắt tiếng không được ghi tại đây; Lịch sử chặn cũng không phải là bản sao Nhật ký cuộc gọi của Android."
            else -> "Android chuẩn thường chỉ chuyển cho ứng dụng sàng lọc những cuộc gọi có số điện thoại hiển thị hợp lệ. Số ẩn/không khả dụng và cuộc gọi VoIP/SIP phụ thuộc ứng dụng Điện thoại cùng phần mở rộng của từng hãng máy nên có thể không tới CallHS."
        }
        override fun commonIssueFix(issue: Int) = when (issue) {
            1 -> "Mở lại CallHS, cấp lại quyền chặn cuộc gọi và bật Bảo vệ cuộc gọi. Kiểm tra lịch bật/tạm dừng; sau khi khởi động lại hãy mở khóa thiết bị ít nhất một lần."
            2 -> "Kiểm tra Danh sách cho phép, xử lý theo danh bạ, quy tắc đầu tiên khớp và cấu hình gọi lặp. Trong Phương thức xử lý, chọn một trong hai chế độ Chặn nếu bạn muốn Android ngăn cuộc gọi."
            3 -> "Thêm số vào Danh sách cho phép — danh sách này có ưu tiên cao nhất. Sau đó kiểm tra các quy tắc đang bật và phạm vi Số đã lưu/Mọi số để tránh chặn rộng hơn dự định."
            4 -> "Chọn Thông báo mỗi lần chặn, kiểm tra lịch thông báo nâng cao, rồi cho phép thông báo và bật kênh cảnh báo của CallHS trong Android."
            5 -> "Kiểm tra âm thanh, rung và kiểu hiển thị trong Cài đặt thông báo nâng cao. Sau đó mở cài đặt kênh của Android, bật âm thanh/cửa sổ nổi và kiểm tra chế độ Không làm phiền."
            6 -> "Chọn một phương thức Chặn, mở tab Lịch sử và chọn đúng ngày hoặc khoảng thời gian. Đối chiếu Nhật ký cuộc gọi hệ thống nếu bạn cần xem cả cuộc gọi được cho qua."
            else -> "Không thể bảo đảm chặn các cuộc gọi mà Android không chuyển cho CallHS. Với số có hiển thị, hãy thêm số đó vào Danh sách chặn; với số ẩn hoặc VoIP, kiểm tra thêm tính năng chặn của ứng dụng Điện thoại và nhà mạng."
        }

        override val tabRules = "Quy tắc chặn"
        override fun tabHistory(count: Int) = "Lịch sử ($count)"
        override val addRule = "Thêm quy tắc chặn"
        override val emptyRules =
            "Chưa có quy tắc nâng cao. Danh sách cho phép, Danh sách chặn và Xử lý theo danh bạ được quản lý ở các mục riêng."
        override val emptyHistory = "Chưa có cuộc gọi nào bị chặn. Lịch sử sẽ xuất hiện tại đây sau khi CallHS chặn."
        override fun ruleCount(count: Int) = "$count quy tắc đang lưu"
        override val ruleEnabledStatus = "Đang áp dụng"
        override val ruleDisabledStatus = "Đang tắt"
        override fun blockedCount(count: Int) = "Đã chặn $count lần"
        override fun blockedAt(time: String) = "Chặn lúc $time"
        override fun matchedRule(rule: String) = "Theo quy tắc: $rule"
        override fun repeatCallerGuardReason(attempt: Int, threshold: Int, minutes: Int) =
            "Chặn số lạ trước ngưỡng · lượt $attempt/$threshold · cửa sổ $minutes phút"
        override fun consecutiveMissed(count: Int) = "$count cuộc liên tiếp chưa trả lời"
        override val menuDeleteRule = "Xoá quy tắc chặn"
        override val menuDeleteHistory = "Xoá lịch sử"

        override val historyPeriodDay = "Ngày"
        override val historyPeriodWeek = "Tuần"
        override val historyPeriodMonth = "Tháng"
        override val historyPickDate = "Chọn ngày cụ thể"
        override val historyDateRangeNote = "Chỉ có thể chọn một ngày trong 30 ngày gần nhất."
        override val historyOverviewTitle = "Tổng quan chặn"
        override val historyTotalBlocks = "Lượt chặn"
        override val historyUniqueNumbers = "Số riêng biệt"
        override val historyPeakHour = "Giờ cao điểm"
        override val historyPeakDay = "Ngày cao điểm"
        override val historyNoPeak = "Chưa có"
        override val historyActivityTitle = "Phân bố hoạt động"
        override val historyHourlySubtitle = "Từng khung một giờ · từ 0h đến 24h"
        override val historyDailySubtitle = "Từng ngày đầy đủ trong khoảng đang xem"
        override val historySwipeChartHint = "Vuốt ngang để xem đầy đủ biểu đồ"
        override fun historyHourBucket(fromHour: Int, toHour: Int) = "${fromHour}h–${toHour}h"
        override fun historyDayAxis(day: Int) = "Ngày %02d".format(day)
        override fun historyWeekdayAxis(day: java.time.DayOfWeek) = when (day) {
            java.time.DayOfWeek.MONDAY -> "Thứ Hai"
            java.time.DayOfWeek.TUESDAY -> "Thứ Ba"
            java.time.DayOfWeek.WEDNESDAY -> "Thứ Tư"
            java.time.DayOfWeek.THURSDAY -> "Thứ Năm"
            java.time.DayOfWeek.FRIDAY -> "Thứ Sáu"
            java.time.DayOfWeek.SATURDAY -> "Thứ Bảy"
            java.time.DayOfWeek.SUNDAY -> "Chủ Nhật"
        }
        override val historyDayPartsTitle = "Theo buổi trong ngày"
        override val historyDayPartsSubtitle = "Cùng quy ước với Phân tích cuộc gọi"
        override fun historyDayPartRange(fromHour: Int, toHour: Int) = "%02dh–%02dh".format(fromHour, toHour)
        override val historyReasonsTitle = "Lý do chặn nổi bật"
        override val historyTopNumbersTitle = "Số bị chặn nhiều nhất"
        override fun historyDetails(count: Int) = "Lịch sử chi tiết ($count)"
        override fun historyEvents(count: Int) = "$count lượt"
        override fun historyRange(from: String, to: String) = "$from – $to"
        override fun historyTrendUp(count: Int) = "Tăng $count lượt so với kỳ trước"
        override fun historyTrendDown(count: Int) = "Giảm $count lượt so với kỳ trước"
        override val historyTrendSame = "Không đổi so với kỳ trước"
        override val historyNoEventsInPeriod = "Không có cuộc gọi bị chặn trong khoảng này."

        override val createRuleTitle = "Thêm quy tắc chặn"
        override val editRuleTitle = "Sửa quy tắc chặn"
        override val save = "Lưu quy tắc"
        override val update = "Cập nhật"
        override val ruleTypeLabel = "Chọn cuộc gọi cần xử lý"
        override val ruleValueLabel = "Số hoặc dãy số"
        override val exactValueLabel = "Nhập số điện thoại"
        override val prefixValueLabel = "Nhập đầu số"
        override val suffixValueLabel = "Nhập đuôi số"
        override val containsValueLabel = "Nhập dãy số"
        override val lengthValueLabel = "Nhập số chữ số"
        override val numberHint = "Nhập số hoặc dãy số"
        override val carrierHint = "Chọn nhà mạng"
        override val chooseRuleType = "Chọn cuộc gọi cần xử lý"
        override val chooseCarrier = "Chọn nhà mạng"
        override val typeExact = "Một số điện thoại cụ thể"
        override val typePrefix = "Số bắt đầu bằng"
        override val typeSuffix = "Số kết thúc bằng"
        override val typeContains = "Số có chứa"
        override val typeCarrier = "Theo nhà mạng"
        override val typeSpamRisk = "Bộ lọc cuộc gọi có dấu hiệu spam"
        override val spamRiskPickerDescription =
            "Cuộc gọi có ít nhất một dấu hiệu rủi ro do CallHS nhận diện."
        override val spamRiskDetailsTitle =
            "CallHS chặn khi phát hiện ít nhất một dấu hiệu sau:"
        override val spamRiskPrefixDetail =
            "Số Việt Nam hoàn chỉnh bắt đầu bằng 022, 023, 024, 028, 059 hoặc 099."
        override val spamRiskUnknownPrefixDetail =
            "Số di động Việt Nam có 10 chữ số nhưng đầu số chưa được CallHS nhận diện."
        override val spamRiskVerificationDetail =
            "Trên Android 11 trở lên, mạng di động báo không xác minh được số gọi. Nếu thiết bị không cung cấp thông tin này, CallHS bỏ qua dấu hiệu đó."
        override val spamRiskWarning =
            "Các đầu số trên vẫn có thể là số hợp lệ. Bộ lọc có thể chặn nhầm cuộc gọi bình thường; Danh sách cho phép luôn được ưu tiên."
        override fun spamRiskReasonPrefix(prefix: String) = "Dấu hiệu spam · Khớp đầu số $prefix"
        override fun spamRiskReasonUnknownMobilePrefix(prefix: String) =
            "Dấu hiệu spam · Đầu số di động $prefix chưa được nhận diện"
        override val spamRiskReasonVerificationFailed =
            "Dấu hiệu spam · Mạng báo xác minh số gọi thất bại"
        override val typeSpecial = "Loại cuộc gọi đặc biệt"
        override val typeContacts = "Theo danh bạ"
        override val typeCallHistory = "Theo lịch sử cuộc gọi"
        override val typeCountryAndAreaCode = "Quốc gia & đầu số Việt Nam"
        override val specialTitle = "Chọn một loại cuộc gọi"
        override val specialPrivate = "Cuộc gọi ẩn số"
        override val specialPrivateDesc =
            "Cuộc gọi không cung cấp CLI hoặc số điện thoại của bên gọi."
        override val specialUnknownContact = "Số chưa lưu trong danh bạ"
        override val specialUnknownContactDesc = "Nhận diện số điện thoại không trùng với liên hệ nào trên thiết bị."
        override val specialVoip = "Cuộc gọi Internet (quy tắc cũ)"
        override val specialVoipDesc = "Chỉ dùng để đọc lịch sử hoặc dữ liệu cũ."
        override val specialSipPhone = "SIP URI có user là số điện thoại"
        override val specialSipPhoneDesc =
            "Phần user trước dấu @ là một số điện thoại, ví dụ sip:+84912345678@provider.vn."
        override val specialSipText = "SIP URI có user dạng chữ"
        override val specialSipTextDesc =
            "Phần user trước dấu @ là một chuỗi chữ hoặc ký tự, ví dụ sip:support@company.vn."
        override val identityTermsTitle = "Tìm hiểu thuật ngữ cuộc gọi"
        override val learnVoip = "VoIP là gì?"
        override val learnSip = "SIP là gì?"
        override val learnUri = "URI là gì?"
        override val learnCli = "CLI là gì?"
        override val voipExplanation =
            "VoIP (Voice over Internet Protocol) là công nghệ truyền giọng nói qua mạng IP như Wi-Fi, 4G/5G hoặc Internet thay vì chỉ dùng mạng thoại truyền thống.\n\n" +
                "Một cuộc gọi VoIP có thể hiển thị số điện thoại thông thường hoặc một định danh SIP. VoIP chỉ mô tả cách cuộc gọi được truyền, không có nghĩa cuộc gọi đó là spam hay lừa đảo.\n\n" +
                "Trong CallHS, mục “Cuộc gọi Internet (quy tắc cũ)” chỉ được giữ để đọc dữ liệu cũ. Khi tạo quy tắc mới, hãy chọn điều kiện SIP phù hợp nếu thiết bị cung cấp URI cuộc gọi."
        override val sipExplanation =
            "SIP (Session Initiation Protocol) là giao thức dùng để thiết lập, quản lý và kết thúc phiên gọi qua mạng IP. Một định danh SIP thường có dạng sip:user@domain; sips: là biến thể dùng kết nối bảo mật cho tín hiệu SIP.\n\n" +
                "Phần user trước dấu @ có thể là số điện thoại, ví dụ sip:+84912345678@provider.vn, hoặc chuỗi chữ như sip:support@company.vn. CallHS tách hai trường hợp này thành hai điều kiện cuộc gọi đặc biệt.\n\n" +
                "Android chuẩn thường chỉ chuyển địa chỉ tel: tới CallScreeningService. CallHS chỉ nhận diện được SIP/sips khi thiết bị hoặc nhà sản xuất chuyển URI đó cho ứng dụng."
        override val uriExplanation =
            "URI (Uniform Resource Identifier) là chuỗi dùng để định danh một tài nguyên. Trong cuộc gọi, URI cho biết kiểu địa chỉ và giá trị của bên gọi.\n\n" +
                "Phần đứng trước dấu hai chấm là scheme. Ví dụ: tel:+84912345678 dùng scheme tel:, sip:user@domain dùng sip:, và sips:user@domain dùng sips:. URI không nhất thiết là địa chỉ web và cũng không phải lúc nào chứa số điện thoại.\n\n" +
                "CallHS chỉ phân loại điều kiện SIP khi URI hợp lệ, có scheme sip: hoặc sips: và có phần user trước dấu @."
        override val cliExplanation =
            "CLI (Calling Line Identification) là thông tin số điện thoại của bên gọi mà mạng hoặc dịch vụ cuộc gọi cung cấp, ví dụ +84912345678. CallHS dùng CLI cho các quy tắc theo số, đầu số, đuôi số, độ dài, nhà mạng hoặc khu vực.\n\n" +
                "CLI là dữ liệu số điện thoại, không phải tên người dùng tự lưu trong Danh bạ. CLI có thể bị ẩn hoặc giả mạo, vì vậy số hiển thị không bảo đảm tuyệt đối danh tính thật của người gọi."
        override val specialAndroidLimit =
            "Android chuẩn chỉ chuyển handle tel: vào CallScreeningService. SIP/sips là khả năng mở rộng tùy thiết bị hoặc OEM (Original Equipment Manufacturer — nhà sản xuất thiết bị); nếu URI không được chuyển tới CallHS thì ứng dụng không có dữ liệu để nhận diện tiêu chí SIP."
        override val contactPickerTitle = "Chọn liên hệ"
        override val contactPickerOpen = "Chọn từ danh bạ"
        override val contactPickerSearchHint = "Tìm tên hoặc số điện thoại"
        override val contactPickerPermissionTitle = "Cần quyền truy cập danh bạ"
        override val contactPickerPermissionBody =
            "Cho phép CallHS đọc danh bạ để bạn chọn liên hệ và nhận diện số lạ. Ứng dụng không sửa hoặc gửi danh bạ đi."
        override val contactPickerPermissionAction = "Cho phép truy cập"
        override fun contactPickerSelectedCount(count: Int) = "Đã chọn $count liên hệ"
        override val contactPickerDone = "Hoàn tất"
        override val contactPickerEmpty = "Danh bạ chưa có liên hệ kèm số điện thoại."
        override val contactPickerNoResults = "Không tìm thấy liên hệ phù hợp."
        override val callHistoryPickerTitle = "Chọn số từ lịch sử cuộc gọi"
        override val callHistoryPickerOpen = "Chọn từ lịch sử cuộc gọi"
        override val callHistoryPickerSearchHint = "Tìm tên hoặc số điện thoại"
        override fun callHistoryPickerSelectedCount(count: Int) = "Đã chọn $count số"
        override val callHistoryPickerEmpty = "Chưa có số điện thoại hợp lệ trong lịch sử cuộc gọi."
        override val callHistoryPickerNoResults = "Không tìm thấy số phù hợp với tìm kiếm hoặc bộ lọc."
        override val callHistoryPickerPreviouslySelected = "Đã chọn trước đó"
        override val callHistoryPickerPreviouslySelectedNote =
            "Các số này không nằm trong lịch sử đang hiển thị. Bạn vẫn có thể bỏ chọn tại đây."
        override val validationSelectSpecial = "Hãy chọn ít nhất một loại cuộc gọi."
        override val validationSelectContact = "Hãy chọn ít nhất một liên hệ trong danh bạ."
        override val validationSelectCallHistory = "Hãy chọn ít nhất một số trong lịch sử cuộc gọi."
        override val regionPickerTitle = "Chọn quốc gia & đầu số Việt Nam"
        override val regionInternationalSection = "Mã quốc gia quốc tế"
        override val regionVietnamPrefixSection = "Đầu số Việt Nam"
        override val regionAllInternationalExceptVietnam = "Tất cả số quốc tế, trừ Việt Nam (+84)"
        override val regionAllInternationalExceptVietnamDesc =
            "Nhận diện số quốc tế có mã quốc gia khác Việt Nam (+84)."
        override val regionChina = "Trung Quốc (+86)"
        override val regionCambodia = "Campuchia (+855)"
        override val regionMyanmar = "Myanmar (+95)"
        override val regionNanpShared = "NANP (+1, gồm Hoa Kỳ)"
        override val regionGermany = "Đức (+49)"
        override val regionLaos = "Lào (+856)"
        override val regionThailand = "Thái Lan (+66)"
        override val regionMalaysia = "Malaysia (+60)"
        override val regionSingapore = "Singapore (+65)"
        override val regionIndonesia = "Indonesia (+62)"
        override val regionPhilippines = "Philippines (+63)"
        override val regionIndia = "Ấn Độ (+91)"
        override val regionPrefix024 = "024 · Hà Nội"
        override val regionPrefix022 = "022 · Nhóm đầu số cố định"
        override val regionPrefix028 = "028 · TP. Hồ Chí Minh"
        override val regionPrefix059 = "059 · Đầu số di động"
        override val regionPrefix099 = "099 · Đầu số di động Gmobile"
        override val regionCallerIdWarning =
            "Bộ lọc dựa trên số người gọi hiển thị. Thông tin này có thể bị giả mạo nên mã số không đảm bảo nguồn gốc thật của cuộc gọi."
        override val validationSelectRegion = "Hãy chọn ít nhất một mã quốc gia hoặc đầu số Việt Nam."
        override val invalidRule = "Giá trị chưa hợp lệ. Với dãy số, hãy nhập ít nhất 2 chữ số."
        override val duplicateRule = "Quy tắc này đã có trong danh sách."
        override val maxRules = "Đã đạt tối đa 200 quy tắc chặn."
        override val discardTitle = "Bỏ thay đổi?"
        override val discardMessage = "Các thay đổi chưa lưu của quy tắc chặn này sẽ bị mất."
        override val discardStay = "Ở lại"
        override val discardExit = "Bỏ thay đổi"

        override fun ruleSummary(type: String, value: String) = when (type) {
            "exact" -> "Số $value"
            "prefix" -> "Bắt đầu bằng $value"
            "suffix" -> "Kết thúc bằng $value"
            "contains" -> "Có chứa $value"
            "length" -> "Có $value chữ số"
            "any" -> "Mọi số trong phạm vi"
            "carrier" -> "Nhà mạng $value"
            "repeat_unanswered" -> "Quy tắc cũ · Gọi lặp chưa trả lời"
            "spam_risk" -> typeSpamRisk
            "special" -> specialSummary(value)
            "contacts" -> contactsSummary(value)
            "call_history" -> callHistorySummary(value)
            "geographic" -> regionSummary(value)
            else -> value
        }

        override fun specialSummary(value: String): String {
            val labels = SpecialCallCondition.decode(value).map { condition ->
                when (condition) {
                    SpecialCallCondition.PRIVATE_NUMBER -> specialPrivate
                    SpecialCallCondition.UNKNOWN_CONTACT -> specialUnknownContact
                    SpecialCallCondition.VOIP -> specialVoip
                    SpecialCallCondition.SIP_PHONE_NUMBER -> specialSipPhone
                    SpecialCallCondition.SIP_TEXT_ID -> specialSipText
                }
            }
            return labels.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: typeSpecial
        }

        override fun contactsSummary(value: String): String {
            val count = ContactRuleCodec.selectedCount(value)
            return when (count) {
                0 -> typeContacts
                1 -> "Một liên hệ trong danh bạ"
                else -> "$count liên hệ trong danh bạ"
            }
        }

        override fun callHistorySummary(value: String): String {
            val count = CallHistoryRuleCodec.selectedCount(value)
            return when (count) {
                0 -> typeCallHistory
                1 -> "Một số từ lịch sử cuộc gọi"
                else -> "$count số từ lịch sử cuộc gọi"
            }
        }

        override fun regionSummary(value: String): String {
            val labels = GeographicBlockOption.decode(value).map { option ->
                when (option) {
                    GeographicBlockOption.ALL_INTERNATIONAL_EXCEPT_VIETNAM -> regionAllInternationalExceptVietnam
                    GeographicBlockOption.CHINA -> regionChina
                    GeographicBlockOption.CAMBODIA -> regionCambodia
                    GeographicBlockOption.MYANMAR -> regionMyanmar
                    GeographicBlockOption.NANP_SHARED -> regionNanpShared
                    GeographicBlockOption.GERMANY -> regionGermany
                    GeographicBlockOption.LAOS -> regionLaos
                    GeographicBlockOption.THAILAND -> regionThailand
                    GeographicBlockOption.MALAYSIA -> regionMalaysia
                    GeographicBlockOption.SINGAPORE -> regionSingapore
                    GeographicBlockOption.INDONESIA -> regionIndonesia
                    GeographicBlockOption.PHILIPPINES -> regionPhilippines
                    GeographicBlockOption.INDIA -> regionIndia
                    GeographicBlockOption.VIETNAM_PREFIX_024 -> regionPrefix024
                    GeographicBlockOption.VIETNAM_PREFIX_022 -> regionPrefix022
                    GeographicBlockOption.VIETNAM_PREFIX_028 -> regionPrefix028
                    GeographicBlockOption.VIETNAM_PREFIX_059 -> regionPrefix059
                    GeographicBlockOption.VIETNAM_PREFIX_099 -> regionPrefix099
                }
            }
            return labels.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: typeCountryAndAreaCode
        }

        override val notificationChannelName = "Cảnh báo chặn cuộc gọi · Khẩn cấp"
        override val notificationChannelDescription =
            "Cảnh báo heads-up, âm thanh và rung khi CallHS vừa chặn một cuộc gọi"
        override fun notificationTitle(number: String) = "Đã chặn cuộc gọi từ $number"
        override fun notificationBody(total: Int, rule: String) = "Lần chặn thứ $total của số này · $rule"
    }

    private object Backup : BackupStrings {
        override val settingsSection = "Sao lưu & khôi phục"
        override val cardTitle = "Sao lưu & khôi phục dữ liệu"
        override val cardSubtitle = "Xuất dữ liệu ra file và nhập lại khi cần"
        override val open = "Mở sao lưu & khôi phục"

        override val screenTitle = "Sao lưu & khôi phục"
        override val callLogNote =
            "Nhật ký cuộc gọi hệ thống là chỉ đọc nên không nằm trong bản sao lưu; riêng lịch sử do CallHS đã chặn có thể sao lưu tại đây."

        override val backupTitle = "Sao lưu (xuất file)"
        override val backupDesc =
            "Chọn dữ liệu cần sao lưu rồi xuất ra một file. Bạn có thể lưu file này để phòng khi mất máy hoặc chuyển sang máy khác."
        override val chooseData = "Chọn dữ liệu"
        override val exportButton = "Xuất ra file"
        override val exporting = "Đang xuất…"

        override val restoreTitle = "Khôi phục (nhập file)"
        override val restoreDesc =
            "Chọn một file sao lưu đã lưu để khôi phục lại dữ liệu vào ứng dụng."
        override val pickFileButton = "Chọn file sao lưu"
        override val pickAnotherButton = "Chọn file khác"
        override val restoreButton = "Khôi phục"
        override val restoring = "Đang khôi phục…"
        override val fileLabel = "File sao lưu"
        override fun fileMeta(date: String, appVersion: String) =
            if (appVersion.isBlank()) "Tạo lúc $date" else "Tạo lúc $date · phiên bản $appVersion"
        override val chooseSections = "Chọn phần cần khôi phục"

        override val modeTitle = "Cách khôi phục"
        override val modeReplace = "Ghi đè toàn bộ"
        override val modeReplaceDesc = "Xoá dữ liệu hiện tại, thay bằng bản sao lưu."
        override val modeAdd = "Thêm, không ghi đè"
        override val modeAddDesc = "Chỉ thêm mục mới, giữ nguyên dữ liệu đang có."
        override val modeUpdate = "Cập nhật & thêm"
        override val modeUpdateDesc = "Thêm mục mới và cập nhật mục trùng theo bản sao lưu."

        override val secTemplates = "Mẫu tin nhắn"
        override val secTemplatesSub = "Các mẫu tin nhắn soạn sẵn"
        override val secQr = "Lịch sử quét QR"
        override val secQrSub = "Các mã QR đã quét gần đây"
        override val secCategories = "Phân loại nhóm"
        override val secCategoriesSub = "Nhóm & số điện thoại thành viên"
        override val secBlockRules = "Quy tắc chặn cuộc gọi"
        override val secBlockRulesSub = "Quy tắc, danh sách, lịch, bảo vệ và thông báo nâng cao"
        override val secBlockHistory = "Lịch sử cuộc gọi bị chặn"
        override val secBlockHistorySub = "Các sự kiện CallHS đã chặn"
        override val secMyNumber = "Số của tôi"
        override val secMyNumberSub = "Số điện thoại của bạn theo SIM"
        override val secOutgoingCall = "Cài đặt cuộc gọi đi"
        override val secOutgoingCallSub = "Trạng thái, điều kiện cảnh báo và kiểu hiển thị"
        override val secDisplay = "Cài đặt hiển thị"
        override val secDisplaySub = "Giao diện, ngôn ngữ, cỡ chữ"
        override fun itemsCount(n: Int) = "$n mục"

        override val confirmReplaceTitle = "Ghi đè dữ liệu?"
        override val confirmReplaceMessage =
            "Chế độ “Ghi đè toàn bộ” sẽ XOÁ dữ liệu hiện tại của các phần được chọn và thay bằng bản sao lưu. Thao tác này không thể hoàn tác."

        override val exportOkTitle = "Đã lưu bản sao lưu"
        override val exportOkMessage = "Đã xuất dữ liệu ra file thành công."
        override val resultTitle = "Đã khôi phục"
        override fun resultLine(section: String, added: Int, updated: Int, skipped: Int): String {
            val parts = buildList {
                if (added > 0) add("+$added thêm")
                if (updated > 0) add("$updated cập nhật")
                if (skipped > 0) add("$skipped bỏ qua")
            }
            return if (parts.isEmpty()) "$section: không có thay đổi" else "$section: ${parts.joinToString(" · ")}"
        }
        override val displayApplied = "Đã áp dụng"
        override val displayKept = "Giữ nguyên"
        override val truncatedNote = "Một số mục vượt giới hạn nên đã được bỏ bớt."
        override val done = "Xong"

        override val errInvalidFile = "File không hợp lệ hoặc không phải bản sao lưu của CallHS."
        override val errWriteFailed = "Không lưu được file. Vui lòng thử lại."
        override val errReadFailed = "Không đọc được file. Vui lòng thử lại."
        override val errNothingSelected = "Hãy chọn ít nhất một phần dữ liệu."
        override val errEmptyBackup = "File sao lưu không có dữ liệu nào để khôi phục."
    }
}
