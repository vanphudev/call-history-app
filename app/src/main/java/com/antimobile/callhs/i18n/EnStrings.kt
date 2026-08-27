package com.antimobile.callhs.i18n

import com.antimobile.callhs.data.blocking.ContactRuleCodec
import com.antimobile.callhs.data.blocking.CallHistoryRuleCodec
import com.antimobile.callhs.data.blocking.GeographicBlockOption
import com.antimobile.callhs.data.blocking.SpecialCallCondition
import java.time.DayOfWeek

/** Bảng chuỗi TIẾNG ANH — bản dịch song song với [ViStrings]. */
object EnStrings : AppStrings {

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
        override val back = "Back"
        override val settings = "Settings"
        override val contacts = "Contacts"
        override val search = "Search"
        override val openSettings = "Open Settings"
        override val grantPermission = "Grant"
        override val allowAccess = "Allow access"
        override val dismiss = "Dismiss"
        override val cancel = "Cancel"
        override val hiddenNumber = "Private number"

        override val dialerOpenFailed = "Couldn't open the dialer"
        override val featureComingSoon = "This feature is coming soon"
        override val messagingOpenFailed = "Couldn't open the messaging app"
        override val emailOpenFailed = "Couldn't open the email app"
        override val wifiSettingsOpenFailed = "Couldn't open Wi-Fi settings"
        override val mapsOpenFailed = "Couldn't open the maps app"
        override val appSettingsOpenFailed = "Couldn't open app settings"
        override val numberCopied = "Number copied"
        override val contentCopied = "Content copied"

        override val phoneNumberLabel = "Phone number"
        override val contentLabel = "Content"
        override val shareNumberTitle = "Share number"
    }

    private object DateTime : DateTimeStrings {
        override val today = "Today"
        override val yesterday = "Yesterday"
        override fun weekdayShort(dow: DayOfWeek): String = when (dow) {
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
        }
        override fun duration(minutes: Long, seconds: Long): String = when {
            minutes > 0 && seconds > 0 -> "$minutes min $seconds sec"
            minutes > 0 -> "$minutes min"
            else -> "$seconds sec"
        }
        override val unitDay = "day"
        override val unitHour = "hr"
        override val unitMinute = "min"
        override val unitSecond = "sec"
        override val unitHourShort = "h"
        override val unitMinuteShort = "m"
        override val unitSecondShort = "s"
    }

    private object CallStatus : CallStatusStrings {
        override val connected = "Connected"
        override val noResponse = "No response"
        override val received = "Received"
        override val noAnswer = "No answer"
        override val rejected = "Declined"
        override val blocked = "Blocked"
        override val voicemail = "Voicemail"
        override val answeredElsewhere = "Answered elsewhere"
        override val unknown = "Unknown"
        override val videoLabel = "Video"

        override fun connectionSuccess(duration: String) = "Connected successfully · $duration"
        override val connectionFailed = "Call failed · no response"
        override val connectionNoAnswer = "No answer · no response"
        override val connectionRejected = "You declined · no conversation"
        override val connectionBlocked = "Blocked · no conversation"
        override val connectionVoicemail = "Voice message"
        override val connectionExternal = "Answered on another device"
        override val connectionNotConnected = "Not connected"
    }

    private object Emergency : EmergencyStrings {
        override val police = "Police"
        override val fire = "Fire & Rescue"
        override val medical = "Ambulance"
        override fun of(kind: EmergencyKind): String = when (kind) {
            EmergencyKind.POLICE -> police
            EmergencyKind.FIRE -> fire
            EmergencyKind.MEDICAL -> medical
        }
    }

    private object CallList : CallListStrings {
        override val title = "Call history"
        override val searchHint = "Search name or number"
        override val voice = "Voice"
        override val close = "Close"
        override val voicePrompt = "Speak to search"
        override val voiceUnsupported = "Voice search isn't supported on this device"
        override val dialpad = "Dialpad"
        override val scrollToTop = "Scroll to top"
        override val scrollToBottom = "Scroll to bottom"
        override val chooseFilter = "Choose filter"
        override val selected = "Selected"

        override val filterAll = "All"
        override val filterMissed = "Missed"
        override val filterOutgoing = "Outgoing"
        override val filterIncoming = "Incoming"
        override val typeSheetTitle = "Filter by call type"

        override val viewByTime = "Time"
        override val viewByPhone = "Number"

        override fun simViewing(label: String) = "Viewing $label"

        override val dateToday = "Today"
        override val dateYesterday = "Yesterday"
        override val dateWeek = "This week"
        override val dateMonth = "This month"
        override val pickDate = "Pick date"

        override val prevMonth = "Previous month"
        override val nextMonth = "Next month"
        override fun monthYear(month: Int, year: Int): String {
            val name = MONTHS.getOrElse(month - 1) { month.toString() }
            return "$name $year"
        }
        override val weekdayHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        override fun selectedDay(label: String): String = "Selected: $label"
        override val dateRangeNote = "You can only pick a date within the last 3 months."
        override val apply = "Apply"
        override val clearDateFilter = "Clear date filter"

        override val emptyNoResults = "No results found."
        override val emptyNoCalls = "No calls on this device yet."
        override val loadingCalls = "Loading call log…"

        override val permTitle = "View your call history"
        override val permBody =
            "Grant call log access to review your history along with SIM, carrier and duration details — all on your device."
        override val permBullet1 = "Read-only — never edits or deletes calls"
        override val permBullet2 = "Your data stays on your device, never sent anywhere"
        override val permBullet3 = "See call type, SIM, carrier and duration"
        override val permRevoke = "You can revoke this permission anytime in Settings."

        override val bannerTitle = "Show saved contact names"
        override val bannerBody = "Grant Contacts access to show caller names instead of numbers."

        override val searchRecentEmpty = "No recent searches"
        override val searchRecentTitle = "Recent searches"
        override val clearAll = "Clear all"
        override val delete = "Delete"

        override val menuMessage = "Send message"
        override val menuCall = "Call"
        override val menuZalo = "Find on Zalo"
        override val menuCopy = "Copy number"
    }

    private object CallDetail : CallDetailStrings {
        override val loading = "Loading details…"
        override val emptyNoCalls = "No calls with this number."
        override fun emptyNoCallsInScope(label: String) = "No calls on $label with this number. Choose “All SIMs” in Settings to see everything."
        override val costStats = "Call charges"
        override val copyNumber = "Copy number"

        override val numberTypeMobile = "Mobile"
        override val numberTypeHome = "Home"
        override val numberTypeWork = "Work"
        override val numberTypeMain = "Main"
        override val numberTypeOther = "Other"
        override val numberTypeGeneric = "Phone"
        override val call = "Call"
        override val message = "Message"
        override val sendTemplate = "Send a templated message"
        override val viewInContacts = "View in contacts"
        override val addToContacts = "Add to contacts"
        override val searchZalo = "Search on Zalo"
        override val searchGoogle = "Search on Google"

        override val historyTitle = "Call history"
        override val showMore = "Show more"
        override val toolsTitle = "Tools"
        override val shareContact = "Share contact"
        override val shareContactSubtitle = "As text or QR code"
        override val notSavedContact = "Not in contacts"

        override val dirOutgoing = "Outgoing call"
        override val dirIncoming = "Incoming call"
        override val dirMissed = "Missed call"
        override val dirVoicemail = "Voicemail"
        override val dirOther = "Other"
        override val callNormal = "Standard"

        override val statsTitle = "Statistics"
        override fun totalCalls(n: Int) = if (n == 1) "1 call total" else "$n calls total"
        override fun lastCallAt(time: String) = "Last call at $time"
        override fun inclSims(n: Int) = "(incl. $n SIMs)"
        override fun lastCallAtSim(sim: String, time: String) = "Last call ($sim) at $time"
        override val metricCalls = "Calls"
        override val metricDuration = "Duration"
        override val metricMissed = "Missed"
        override val statOutgoing = "Outgoing"
        override val statIncoming = "Incoming"
        override val statTotalTime = "Total time"
        override val statActiveHours = "Active hours"
        override val statLastCall = "Most recent call"
    }

    private object AllCalls : AllCallsStrings {
        override val loading = "Loading calls…"
        override val title = "All calls"
        override val expandAll = "Expand all"
        override val collapseAll = "Collapse all"
    }

    private object Timeline : TimelineStrings {
        override val title = "Timeline"
        override val openTimeline = "View as timeline"
        override val dirOutgoingSide = "Outgoing"
        override val dirIncomingSide = "Incoming"
        override fun apart(gap: String) = "$gap apart"
    }

    private object Contacts : ContactsStrings {
        override fun count(n: Int) = if (n == 1) "1 contact" else "$n contacts"
        override fun countFiltered(shown: Int, total: Int) = "$shown/$total contacts"
        override val loading = "Loading contacts…"
        override val emptyNoContacts = "No contacts on this device."
        override val emptyNoResults = "No matching contacts."
        override val loadError = "Couldn't read contacts. Check the permission and try again."
        override fun morePhones(n: Int) = "+$n more"
        override val sheetTitle = "Contact info"
        override val phonesSection = "Phone numbers"
        override val openInContactsApp = "Open in Contacts app"
        override val createContact = "New contact"
        override val editContact = "Edit contact"
        override val deleteContact = "Delete contact"
        override val copyContactInfo = "Copy contact info"
        override val searchHint = "Search name, number, email"
        override val permTitle = "View your contacts"
        override val permBody =
            "Grant Contacts access to see your full contact list with numbers, carriers and related info — all on your device."
        override val noName = "No name"

        override val phoneTypeMobile = "Mobile"
        override val phoneTypeHome = "Home"
        override val phoneTypeWork = "Work"
        override val phoneTypeMain = "Main"
        override val phoneTypeWorkMobile = "Work mobile"
        override val phoneTypeFax = "Fax"
        override val phoneTypePager = "Pager"
        override val phoneTypeOther = "Other"
    }

    private object Settings : SettingsStrings {
        override val brandTagline = "Shipper call history"
        override val sectionStats = "Statistics"
        override val sectionMessaging = "Messaging"
        override val sectionAgency = "Agency directory"
        override val sectionDisplay = "Display"
        override val sectionPolicy = "Policy & Support"
        override val sectionApp = "App"

        override val simScopeSection = "View by SIM"
        override val simScopeTitle = "App-wide SIM scope"
        override val simScopeDesc =
            "Pick a SIM to make the WHOLE app (list, statistics, details, cost) count and show only that SIM's calls. Choose \"All\" to see every SIM — you can still quick-filter by SIM right on the call list."
        override val simScopeAll = "All"

        override val statsTitle = "Statistics & detailed analytics"
        override val statsSubtitle = "By number · Active hours · Charges"
        override val statsOpen = "Open statistics"
        override val repeatTitle = "Repeat & duplicate numbers"
        override val repeatSubtitle = "Frequently called · Recall cycle · 30 days"
        override val repeatOpen = "Open repeat statistics"

        override val templatesTitle = "Message templates"
        override val templatesSubtitle = "Prewritten content · Quick send · Insert QR"
        override val templatesOpen = "Open message templates"
        override val myNumberTitle = "My phone number"
        override val myNumberSubtitle = "Enter numbers per SIM · Auto-fill {phonesim} · My QR"
        override val myNumberOpen = "Open my phone number"
        override val qrHistoryTitle = "QR scan history"
        override val qrHistorySubtitle = "Review scanned codes · Quick open · Pick a message template"
        override val qrHistoryOpen = "Open QR scan history"
        override val smsStripTitle = "Send SMS without accents"
        override val smsStripSubtitle =
            "Strip Vietnamese accents & special characters when handing off to the messaging app to save costs. Content inside the app stays unchanged."

        override val agencyTitle = "Government & police directory"
        override val agencySubtitle = "Public administration & Police · Hanoi, HCMC"
        override val agencyOpen = "Choose a directory"

        override val fontSizeCardNote = "Won't change with system font size"
        override val fontSizeOpen = "Open font size settings"

        override val privacyTitle = "Privacy policy"
        override val privacySubtitle = "How the app handles your data"
        override val termsTitle = "Terms of use"
        override val termsSubtitle = "Terms and disclaimers"
        override val websiteTitle = "Official website"
        override val contactTitle = "Contact the developer"

        override fun version(name: String) = "Version $name"
        override val appInfoDesc = "View app info in Settings"

        override val pickerTitle = "Choose an agency directory"
        override val pickerNote =
            "Data is downloaded from the official GitHub page, stored on your device and refreshed at most once every 7 days. More provinces/cities will be added later."
    }

    private object OutgoingCall : OutgoingCallStrings {
        override val settingsSection = "Outgoing calls"
        override val settingsTitle = "Outgoing-call alerts"
        override val settingsSubtitle = "Off-net · Blocklist · Allowlist"
        override val settingsOpen = "Open outgoing-call settings"

        override val screenTitle = "Outgoing-call settings"
        override val activationSection = "Activation"
        override val enabledTitle = "Detect outgoing calls"
        override val enabledSubtitle = "Show an alert when you call from the default Phone app"
        override val roleGateTitle = "Enable outgoing-call alerts"
        override val roleGateBody =
            "Android needs you to choose CallHS as the call-redirection app. This access only lets CallHS receive the number and SIM before dialing so it can show an alert; CallHS always keeps the number unchanged and never redirects or blocks outgoing calls."
        override val roleGateAction = "Choose CallHS for call detection"
        override val roleUnavailableTitle = "Not supported on this device"
        override val roleUnavailableBody =
            "This device does not provide Android's call-redirection role, so CallHS cannot detect outgoing calls."
        override val roleActive = "Android has allowed call identification"
        override val roleRequired = "Android call-identification access is required"
        override val roleUnavailable = "Call identification isn't supported on this device"
        override val roleExplanation =
            "Android grants an outgoing-call role so CallHS receives the number and selected SIM before dialing. CallHS always keeps the number unchanged and never blocks outgoing calls."
        override val conditionsSection = "Notify when"
        override val offNetworkTitle = "Calling an off-net number"
        override val offNetworkSubtitle = "Alerts only when both the calling SIM and destination carrier are known"
        override val simPermissionTitle = "SIM information access is missing"
        override val simPermissionSubtitle =
            "Phone access is used only to identify the carrier of the calling SIM. Call-log access is not required."
        override val grantSimPermission = "Grant SIM access"
        override val blocklistTitle = "Number is on the blocklist"
        override val blocklistSubtitle = "Uses the existing exact-number blocklist even when call blocking is off"
        override val allowlistTitle = "Number is on the allowlist"
        override val allowlistSubtitle = "Quickly confirms that the outgoing number is on your allowlist"
        override val presentationSection = "Presentation"
        override val presentationTitle = "Alert style"
        override val headsUpTitle = "Heads-up notification"
        override val headsUpSubtitle = "Appears at the top and remains in the notification shade"
        override val overlayTitle = "Popup over other apps"
        override val overlaySubtitle = "Shows an AppDialog-style popup over the active call screen"
        override val overlayPermissionTitle = "Display-over-other-apps access is missing"
        override val overlayPermissionSubtitle =
            "Until it is granted, CallHS falls back to a heads-up notification."
        override val grantOverlayPermission = "Grant display access"
        override val notificationPermissionTitle = "Heads-up alerts aren't ready"
        override val notificationPermissionSubtitle =
            "Allow notifications and keep the alert channel at high priority so heads-up can appear."
        override val grantNotificationPermission = "Allow notifications"
        override val openNotificationSettings = "Open notification settings"
        override val privacyNote =
            "The number and carrier information are processed only on your device. CallHS never redirects, changes, or places the call."

        override val notificationChannelName = "Outgoing-call alerts"
        override val notificationChannelDescription =
            "Off-net and list-status alerts when the user starts an outgoing call"
        override val alertBlocklistTitle = "Blocklisted-number warning"
        override val alertOffNetworkTitle = "Off-net call warning"
        override val alertAllowlistTitle = "Allowlisted number"
        override val reasonBlocklist = "This number is on your blocklist."
        override val reasonAllowlist = "This number is on your allowlist."
        override fun reasonOffNetwork(simCarrier: String, targetCarrier: String) =
            "Off-net call: $simCarrier → $targetCarrier."
        override val close = "Close"
    }

    private object FontSize : FontSizeStrings {
        override val small = "Small"
        override val default = "Default"
        override val large = "Large"
        override val xlarge = "Extra large"
        override val screenTitle = "Font size"
        override val previewSection = "Preview"
        override val chooseSection = "Choose size"
        override val note =
            "The app uses the font size you pick here and does NOT change with the system “Font size / Display size” settings, keeping the interface true to its design."
        override val sampleSubtitle = "Outgoing call · 5 min ago"
    }

    private object RepeatStats : RepeatStatsStrings {
        override val permNeeded = "Call log permission is required to view statistics."
        override val analyzing = "Analyzing your call history…"
        override val emptyNoCalls = "No calls in the last 30 days."
        override val distTitle = "Call-count distribution"
        override val cycleTitle = "Recall cycle"
        override fun listTitle(n: Int) = "Numbers ($n)"
        override fun intro(windowDays: Int, start: String, end: String) =
            "Numbers called repeatedly in the last $windowDays days — from $start to $end."

        override val metricTotalCalls = "Total calls"
        override val metricDistinctNumbers = "Numbers"
        override val metricRepeatNumbers = "Called ≥2×"
        override val metricRepeatCalls = "Repeat calls"
        override val metricMultiDay = "Multi-day returns"
        override val metricMaxCalls = "Most called"
        override fun timesCount(n: Int) = if (n == 1) "1 call" else "$n calls"

        override val barUnitNumbers = "no."
        override val cycleEmpty = "No number has been called back on different days in this period."
        override val cycleDesc = "Average gap between the days a number is called back."

        override val sortLabel = "Sort"
        override val legend = "Each cell = 1 day (old → today); darker = more calls"

        override val callsUnit = "calls"
        override val collapse = "Collapse"
        override val expand = "See details"
        override fun maxPerDay(n: Int) = "Up to $n/day"
        override fun detailDates(first: String, last: String) = "First $first · Last $last"
        override val daysCalledLabel = "Days called"
        override val noMatch = "No numbers match the filter."

        override val classReturning = "Returning caller"
        override val classSameDay = "Same-day repeat"

        override fun daysCount(n: Int) = if (n == 1) "1 day" else "$n days"
        override fun outgoing(n: Int) = "Outgoing $n"
        override fun incoming(n: Int) = "Incoming $n"
        override fun missed(n: Int) = "Missed $n"
        override fun talkTime(duration: String) = "Talk $duration"
        override val onlyOneDay = "Called on a single day"
        override fun avgCycle(gap: String, span: Int) = "Returns on average every ~$gap days · over $span days"

        override val bucketOnce = "1 call"
        override val bucketTwice = "2 calls"
        override val bucket3to5 = "3–5 calls"
        override val bucket6to10 = "6–10 calls"
        override val bucketOver10 = ">10 calls"
        override val cycleWithinWeek = "<1 week"
        override val cycle1to2w = "1–2 weeks"
        override val cycle2to3w = "2–3 weeks"
        override val cycleOver3w = ">3 weeks"
        override val filterRepeat = "≥2 calls"
        override val filterMultiDay = "Multiple days"
        override val filterAll = "All"
        override val sortTotal = "Total calls"
        override val sortDays = "Days"
        override val sortRecent = "Recent"
    }

    private object DetailedStats : DetailedStatsStrings {
        override val title = "Detailed statistics"
        override val tabByNumber = "Numbers"
        override val tabHourly = "Hours"
        override val tabCost = "Charges"

        override val rankTitle = "Rankings"
        override fun byNumberTitle(n: Int) = "By number ($n)"
        override fun emptyNoCallsInPeriod(period: String) = "No calls $period."

        override val metricTotalCalls = "Total calls"
        override val metricOutgoing = "Outgoing"
        override val metricIncoming = "Received"
        override val metricMissed = "Missed"
        override val metricDuration = "Duration"
        override val metricNumbers = "Numbers"

        override val rankTopOutgoing = "Most outgoing"
        override val rankTopIncoming = "Most received"
        override val rankTopMissed = "Most missed"

        override fun calls(n: Int) = if (n == 1) "1 call" else "$n calls"
        override fun callsFull(n: Int) = if (n == 1) "1 call" else "$n calls"
        override val callsUnit = "calls"

        override fun hourlyIntro(days: Int) =
            "Call distribution across 24 hours over the last ${if (days == 1) "day" else "$days days"} — with peak hours and the change vs the previous day."
        override val emptyNoActivity = "No activity data yet."
        override val legendOutgoing = "Outgoing"
        override val legendIncoming = "Incoming"
        override val legendMissed = "Missed"
        override fun deltaVsPrev(value: String) = "$value vs prev. day"
        override val deltaSame = "No change"

        override val costPhonePermNote =
            "Phone permission not granted — some outgoing calls have an unidentified SIM carrier, so they may not be charged."
        override val costByNetworkTitle = "By network type"
        override fun costTopTitle(n: Int) = if (n == 1) "Highest charges (1 number)" else "Highest charges ($n numbers)"
        override fun emptyNoBilledInPeriod(period: String) = "No billed outgoing calls $period."
        override val costDisclaimer =
            "Charges are ESTIMATES based on standard (out-of-plan) rates, counting only connected outgoing calls. Actual charges may be lower if you're on a plan."
        override val costTotalLabel = "Estimated total charges"
        override fun costChargeableLine(n: Int, duration: String) =
            if (n == 1) "1 billed call · $duration" else "$n billed calls · $duration"
        override val networkOnNet = "On-net"
        override val networkOffNet = "Off-net"
        override val networkOther = "Landline / Other"
        override fun billedOutgoing(n: Int) = if (n == 1) "1 outgoing call" else "$n outgoing calls"

        override val periodDay = "Today"
        override val periodWeek = "This week"
        override val periodMonth = "This month"
    }

    private object PhoneStats : PhoneStatsStrings {
        override val title = "Call analysis"
        override val openDesc = "Open detailed analysis"

        override val breakdownTitle = "Call breakdown"
        override val centerCallsLabel = "calls"
        override fun countPercent(count: Int, percent: Int) = "$count · $percent%"

        override val metricAnswerRate = "Answer rate"

        override val dailyTitle = "Calls per day"
        override fun dailyIntro(days: Int) = if (days == 1) "Last day with calls" else "Last $days days with calls"

        override val talkTitle = "Duration & connection"
        override val talkTotal = "Total talk time"
        override val talkAverage = "Average per call"
        override val talkLongest = "Longest call"
        override val connectRate = "Connect rate"
        override val missedRate = "Missed rate"
        override val rejectedBlocked = "Rejected / blocked"

        override val hourlyTitle = "Time of day"
        override val peakHourLabel = "Peak hour"
        override fun hourRange(from: Int, to: Int) = "%02d:00–%02d:00".format(from, to)
        override val partMorning = "Morning"
        override val partAfternoon = "Afternoon"
        override val partEvening = "Evening"
        override val partNight = "Night"

        override val weekdayTitle = "By weekday"
        override val peakWeekdayLabel = "Busiest day"

        override val featuresTitle = "Call features"
        override val featureVideo = "Video calls"
        override val featureVolte = "VoLTE (HD calls)"
        override val featuresNone = "No video or VoLTE calls with this number."

        override val spanTitle = "Over time"
        override val spanFirst = "First call"
        override val spanLast = "Latest call"
        override val spanDistinctDays = "Days with calls"
        override fun distinctDaysValue(days: Int) = if (days == 1) "1 day" else "$days days"
        override val spanAvgGap = "Average gap"

        override val simTitle = "By SIM"

        override val empty = "No calls with this number yet."
    }

    private object CostStats : CostStatsStrings {
        override val calculating = "Calculating charges…"
        override val infoDesc = "Rate info"
        override val detailTitle = "Per-call breakdown"
        override val estimateTag = "ESTIMATE"
        override fun chargeableCallsLine(n: Int) = if (n == 1) "1 billable outgoing call" else "$n billable outgoing calls"
        override fun billedSuffix(duration: String) = "$duration billed"

        override val freeCallsLabel = "Free calls"
        override fun freeCallsValue(n: Int) = if (n == 1) "1 call · free" else "$n calls · free"
        override val unknownSimLabel = "Unidentified SIM"
        override fun unknownSimValue(n: Int) = if (n == 1) "1 call · not counted" else "$n calls · not counted"

        override val simCardTitle = "SIM carriers on this device"
        override val simNoneNote =
            "No SIM detected and you haven't entered a number. Go to Settings › “My phone number” to enter one — used to infer the carrier & estimate charges."
        override val fromEnteredNumber = "from your entered number"
        override val unknownCarrier = "Unknown carrier"
        override val unknownSimCallsNote =
            "Some outgoing calls don't match any inserted SIM (the SIM may have been changed/removed), so their charges couldn't be estimated."

        override val calcSim = "Outgoing SIM"
        override val calcCalled = "Called"
        override val calledFallback = "landline / other"
        override val calcDuration = "Actual duration"
        override val calcBilling = "Billing"
        override val calcRate = "Unit price"
        override val rateUnitSlash = "/min"
        override val calcTotal = "Total"

        override val badgeOther = "Other"
        override val badgeUnknownSim = "SIM unknown"
        override val badgeNoConnect = "Not connected"
        override val badgeFree = "Free"

        override val hintUnknownSim =
            "Couldn't identify the SIM/carrier used for this call (the SIM may be removed). Can't estimate charges."
        override val hintNoConnect =
            "The outgoing call didn't connect (no answer / busy…), so no charge applies."
        override val hintFreeIncoming = "This call isn't charged to you."
        override val noOutgoing = "No outgoing calls with this number — you weren't charged."
        override val disclaimer =
            "This amount is only an ESTIMATE based on standard (out-of-plan) rates. If you're on a plan with free minutes or a postpaid subscription, the actual charge is usually lower — often zero within your allowance."

        override val permTitle = "Estimate call charges"
        override val permBody =
            "To tell whether each call is on-net or off-net, the app needs to read the carrier of the SIM you used to call. This is only used to estimate charges on your device, never sent anywhere."

        override val infoSheetTitle = "Reference rate table"
        override val infoSheetDesc =
            "Standard (out-of-plan) rates incl. VAT — looked up 07/2026 from carrier sites & telecom retailers."
        override val tableCarrier = "Carrier"
        override fun tariffRate(amount: Int) = "$amount đ/min"
        override val infoNote1 =
            "Actual charges depend on your plan, promotions and subscription type (prepaid/postpaid), so the amount is only a rough estimate."
        override val infoNote2 =
            "MVNO SIMs (iTel, Wintel, VNSKY, FPT…) may be misdetected as their host carrier (VinaPhone/MobiFone), so on-net calls on these networks are sometimes charged as off-net."

        override val block6 = "6s + 1 block"
        override val block60 = "60s + 1 block"
        override val blockPerMinute = "rounded per minute"
    }

    private object MyNumber : MyNumberStrings {
        override val errorInvalid = "Phone number isn't valid (9–11 digits)"
        override val saved = "Your phone number is saved"
        override val save = "Save"
        override val done = "Done"
        override val introTitle = "Your number for templates & QR"
        override val introBody =
            "Many SIMs in Vietnam don't store the subscriber number, so the app can't read it automatically. " +
                "Enter your number here (saved on your device) to auto-fill the {phonesim1}/{phonesim2} patterns " +
                "in message templates and to create “My QR”. If the device can read the number automatically, the field is locked and no input is needed."
        override val simPresent = "Inserted"
        override val simAbsent = "No SIM"
        override val autoRead = "Read automatically from the SIM — no input needed."
        override val inputHint = "e.g. 0987654321"
        override fun carrierHint(carrier: String) = "Carrier: $carrier — used to estimate call charges."
        override fun enterHint(slotLabel: String) = "Enter your number for $slotLabel (digits only)."
        override val checkingSim = "Checking SIM…"
    }

    private object Qr : QrStrings {
        override val typeWeb = "Web link"
        override val typePhone = "Phone number"
        override val typeSms = "SMS"
        override val typeEmail = "Email"
        override val typeWifi = "Wi‑Fi network"
        override val typeContact = "Contact card"
        override val typeGeo = "Map location"
        override val typeText = "Text"
        override val empty = "(empty)"
        override val scan = "Scan QR"
    }

    private object QrHistory : QrHistoryStrings {
        override val clearAllTitle = "Clear all history?"
        override val clearAllMessage = "All QR scan history will be deleted."
        override val hint = "Tap to open · Tap the delete button to remove an item."
        override val emptyTitle = "No QR scan history yet"
        override val emptyBody = "Tap the scan icon at the top to scan your first QR code."
        override val resultSheetTitle = "QR scan result"
        override val pickTemplateForText = "Pick a message template to insert this content"
        override val copyContent = "Copy content"
        override fun noQrTemplates(token: String) =
            "No message template uses a QR code ($token). Go to \"Message templates\" in Settings to create one."
        override val pickTemplate = "Pick a template to message"
    }

    private object Templates : TemplatesStrings {
        override fun noQrTemplatesToast(token: String) = "No template uses a QR code ($token)"
        override fun maxReached(max: Int) = "You can create up to $max message templates"
        override val menuEdit = "Edit"
        override val deleteTitle = "Delete this template?"
        override fun deleteMessage(title: String) = "Template \"$title\" will be deleted."
        override val hint = "Tap a template to send a message · Long-press to edit, delete or scan a QR code."
        override val createButton = "New template"
        override val emptyTitle = "No message templates yet"
        override val emptyBody = "Tap \"New template\" to write ready-to-send content."
        override val pickToSend = "Pick a template to send"
        override val qrScannedPickTemplate = "QR code scanned. Pick a template to insert the code and open messaging:"
    }

    private object TemplateEditor : TemplateEditorStrings {
        override val editTitle = "Edit template"
        override val createTitle = "New template"
        override val save = "Save"
        override val update = "Update"
        override val fieldTitle = "Title"
        override val titlePlaceholder = "Template title"
        override val fieldContent = "Content"
        override val contentPlaceholder = "Message content…"
        override val quickInsert = "Quick insert — auto-filled on send"
        override val patternInfoDesc = "Pattern help"
        override val discardTitle = "Exit without saving?"
        override val discardMessage = "The content you just entered won't be saved."
        override val stay = "Stay"
        override val exit = "Exit"
        override val qrPlaceholder = "[scanned QR content]"
        override val preview = "Preview"
        override val qrSampleText = "(scanned QR content)"
        override val patternHelpTitle = "Pattern reference"
        override val patternHelpIntro = "These patterns are replaced with real values on send. Example based on the current time:"
        override val patternExampleEmpty = "(not read / empty)"
        override val previewTitle = "Message preview"
        override val previewIntro = "The content will be prefilled in the messaging app:"
        override val previewNote = "Note: [scanned QR content] is replaced with the QR scan result when you send."
        override val hintDate = "Current date"
        override val hintDatetime = "Date and time"
        override val hintTimedate = "Time and date"
        override val hintWeekdate = "Weekday and date"
        override val hintPhonesim1 = "SIM 1 number"
        override val hintPhonesim2 = "SIM 2 number"
        override val hintContextqr = "QR scan result"
    }

    private object Legal : LegalStrings {
        override fun lastUpdated(date: String) = "Last updated: $date · Applies to the CallHS app"
        override fun contactLine(email: String, author: String) = "Contact: $email · Developer $author"
        override val offlineNote =
            "The content needs an internet connection to load the first time.\nYou can view the full version on the official website."
        override val openFullWeb = "Open the full version online"
    }

    private object Agency : AgencyStrings {
        override val loading = "Loading directory…"
        override val needNetwork = "A network connection is required to load the directory."
        override val noNetworkTitle = "No network connection"
        override val noNetworkMessage = "A network connection is required to load the directory the first time. Please turn on Wi-Fi or mobile data and try again."
        override val retry = "Retry"
        override val refresh = "Refresh data"
        override val infoTitle = "Important notes"
        override val disclaimerShort = "Information is for REFERENCE only — you contact these numbers at your own risk."
        override val disclaimerSub = "Numbers/addresses may be inaccurate or changed. Copying or forgery is prohibited. Tap for details."
        override val searchHint = "Search by name, ward/commune…"
        override val clearSearch = "Clear search"
        override fun categoryTotal(category: String, total: Int) =
            "$category · " + if (total == 1) "1 agency" else "$total agencies"
        override fun sourceUpdated(updated: String) = " · updated $updated"
        override fun sourceNetwork(suffix: String) = "Source: GitHub (just fetched)$suffix"
        override fun sourceCache(suffix: String) = "Source: local copy$suffix"
        override fun phonesMore(primary: String, more: Int) = "$primary  ·  +$more more"
        override fun noPrivatePhoneEmergency(fallback: String) = "No dedicated number · emergency $fallback"
        override val noPhone = "No phone number"
        override val unnamed = "(Unnamed)"
        override val noAddress = "No address"
        override fun emergencyCallNote(fallback: String) = "No dedicated number. In an emergency, call $fallback."
        override val mapChip = "Map"
        override val prevPage = "Previous page"
        override val nextPage = "Next page"
        override fun pageOf(current: Int, count: Int) = "Page $current / $count"
        override fun pageCompact(current: Int, count: Int) = "Page $current/$count"
        override val emptyNone = "No agencies yet."
        override fun emptySearch(query: String) = "No results for \"$query\"."
        override val loadFailed = "Couldn't load the directory.\nCheck your network connection and try again."
        override val note1Title = "For reference only"
        override val note1Body = "Phone numbers and addresses in the directory are for REFERENCE only. The data may be inaccurate or may have been updated/changed by government agencies (especially after the administrative-unit merger of 01/07/2025)."
        override val note2Title = "Copying or forgery is prohibited"
        override val note2Body = "Copying, forging or altering this information to impersonate authorities is strictly prohibited in any form."
        override val note3Title = "You assume all risk"
        override val note3Body = "You assume ALL risk when contacting the phone numbers in the directory. Verify the information before providing personal data or making any transaction."
        override fun emergencyPolice(number: String) = "$number (police)"
        override fun emergencyFire(number: String) = "$number (fire)"
        override fun emergencyMedical(number: String) = "$number (medical)"
        override val emergencyTitle = "Emergency numbers"
        override fun emergencyCallBody(numbers: String) = "In an emergency, call: $numbers."
        override fun metaUpdated(updated: String) = "\nUpdated: $updated"
    }

    private object ShareSheet : ShareSheetStrings {
        override val invalidNumber = "Phone number isn't valid to share"
        override val asTextTitle = "Share as text"
        override val asTextSubtitle = "Send name and phone number"
        override val asQrTitle = "Share as QR code"
        override val asQrSubtitle = "Show and download the QR code"
        override val myQrTitle = "Share My QR"
        override val myQrSubtitle = "A QR code of your number per SIM"
        override val contactQrTitle = "Contact QR code"
        override val qrUnavailable = "Couldn't create a QR code for this number."
        override fun contactQrHelper(number: String) = "Show this QR code for others to scan with a camera and call $number."
        override val myQr = "My QR"
        override val chooseSim = "Choose SIM"
        override val noSim = "No SIM found on this device to create your QR code."
        override val noNumberEnter = "No number — enter it in Settings"
        override val chooseSimHint = "Choose a SIM to create a QR code of your number"
        override fun mobileLine(number: String) = "Mobile: $number"
        override fun carrierLine(carrier: String) = "Carrier: $carrier"
        override val unknown = "Unknown"
        override fun noNumberForSlot(slotLabel: String) = "No number for $slotLabel yet. Go to Settings › “My phone number” to enter one."
        override val myQrHelper = "Others scan this QR code to save and call your number."
        override val qrImageDesc = "QR code"
        override val saving = "Saving…"
        override val saveToDevice = "Save to device"
        override val savedToGallery = "QR image saved to your gallery"
        override val saveFailed = "Couldn't save the QR image"
        override val opening = "Opening…"
        override val share = "Share"
        override val shareFailed = "Couldn't share the QR image"
    }

    private object QrScanner : QrScannerStrings {
        override val noQrInImage = "No QR code found in the image"
        override val torchOn = "Turn on flash"
        override val torchOff = "Turn off flash"
        override val instruction = "Point at a QR code · Tap to focus, pinch to zoom."
        override val decoding = "Reading image…"
        override val pickImage = "Pick an image from the gallery"
        override val noCameraPerm = "Camera permission not granted. You can still pick an image from the gallery to scan a QR code."
    }

    private object QrAction : QrActionStrings {
        override val scannedContent = "Scanned QR content"
        override val openLink = "Open link"
        override val sendEmail = "Send email"
        override val openWifiSettings = "Open Wi‑Fi settings"
        override val ssid = "Network name (SSID)"
        override val password = "Password"
        override val security = "Security"
        override val hiddenNetwork = "Hidden network"
        override val yes = "Yes"
        override val addToContacts = "Add to contacts"
        override val name = "Name"
        override val phone = "Phone"
        override val org = "Organization"
        override val openMap = "Open map"
        override val label = "Label"
        override val address = "Address"
        override val latitude = "Latitude"
        override val longitude = "Longitude"
        override val wifiNoPassword = "No password"
    }

    private object Permission : PermissionStrings {
        override val callLogStep = "Call log"
        override val callLogHeadline = "View your call history"
        override val callLogDesc =
            "CallHS reads the call log on your device to show it in full: call type (outgoing / incoming / missed), " +
                "time, duration, SIM and carrier for each call. This permission is required for the app to work."
        override val callLogBullet1 = "Read-only — never edits or deletes calls"
        override val callLogBullet2 = "Your data stays on your device, never sent anywhere"
        override val callLogBullet3 = "See call type, SIM, carrier and duration"
        override val simStep = "SIM info"
        override val simHeadline = "Read SIM info"
        override val simDesc =
            "CallHS reads the carrier of the inserted SIMs (SIM 1 / SIM 2) to tell whether each call is on-net " +
                "or off-net, for a more accurate charge estimate. It doesn't read contacts and never places calls itself."
        override val simBullet1 = "Detect the carrier of each SIM on the device"
        override val simBullet2 = "Determine on/off-net to estimate call charges"
        override val simBullet3 = "Used only for on-device calculation, never sent"
        override val contactsStep = "Contacts"
        override val contactsHeadline = "Screen calls from saved contacts"
        override val contactsDesc =
            "Android only sends calls from saved numbers to CallHS while Contacts permission remains granted. " +
                "CallHS uses it to apply every blocking rule and show names and photos; it only reads contacts " +
                "and never edits or uploads them."
        override val contactsBullet1 = "Apply blocking rules to saved contact numbers"
        override val contactsBullet2 = "Show names and photos in call history"
        override val contactsBullet3 = "Read-only contacts — never edits or sends anything"
        override fun stepIndicator(current: Int, total: Int, title: String) = "Step $current/$total · $title"
        override val deniedMessage = "You've denied this permission. Please enable it manually in Settings to continue."
        override val consentStepTitle = "Terms & privacy"
        override val consentTitle = "Terms & Privacy"
        override val consentIntro =
            "One last step before you start. Please quickly read a few key points, then accept the terms to use CallHS."
        override val consentPoint1 = "The app only READS data — it never edits, deletes or creates calls, messages or contacts on your device."
        override val consentPoint2 = "Your call log and contacts stay on your device — never uploaded to a server or shared with third parties."
        override val consentPoint3 = "The agency directory is downloaded from the internet for reference only — it is not an official government service."
        override val consentPoint4 = "Charges shown in the app are only estimates and may differ from your carrier's actual rates."
        override val readTerms = "Read the Terms of Use"
        override val readPrivacy = "Read the Privacy Policy"
        override val consentCheckLabel = "I have read and agree to CallHS's Terms of Use and Privacy Policy."
        override val consentAccept = "Agree & enter the app"
        override val consentRequired = "Please tick the agreement box to continue"
        override val consentFooter = "You must agree to use the app. You can review these documents anytime in Settings."
        override val openInBrowser = "Open in browser"
    }

    private object Actions : ActionStrings {
        override val linkOpenFailed = "Couldn't open the link"
        override val browserOpenFailed = "Couldn't open the browser"
        override val contactsAppOpenFailed = "Couldn't open the Contacts app"
        override val contactOpenFailed = "Couldn't open the contact"
        override val invalidPhone = "Invalid phone number"
        override val invalidPhoneSearch = "Invalid phone number for search"
        override val zaloAndBrowserUnavailable = "Zalo isn't installed and the browser couldn't open"
        override val feedbackEmailSubject = "CallHS app feedback"
        override val shareContactChooser = "Share contact"
        override val shareQrChooser = "Share QR code"
    }

    private object UpdateNotice : UpdateNoticeStrings {
        override val policyTitle = "Policy update"
        override val whatsNewTitle = "What's new"
        override val gotIt = "Got it"
        override val seeMore = "See more"
    }

    private object Language : LanguageStrings {
        override val cardTitle = "Language"
        override val sectionChoose = "Choose language"
        override val optionSystem = "System default"
        override val vietnamese = "Tiếng Việt"
        override val english = "English"
        override fun currentlyUsing(name: String): String = "Currently: $name"
        override val note =
            "The language applies instantly, no restart needed. “System default” follows your device language (only Vietnamese and English are supported; other languages fall back to Vietnamese)."
    }

    private object Theme : ThemeStrings {
        override val cardTitle = "Appearance"
        override val sectionChoose = "Choose theme"
        override val optionSystem = "System default"
        override val optionLight = "Light"
        override val optionDark = "Dark"
        override fun currentlyUsing(name: String): String = "Currently: $name"
        override val note =
            "The theme applies instantly, no restart needed. “System default” follows your device's light/dark mode."
    }

    private object Category : CategoryStrings {
        override val settingsTitle = "Categories"
        override val settingsSubtitle = "Tag & filter numbers by your own groups"
        override val settingsSection = "Categories"
        override val open = "Open"
        override val listTitle = "Categories"
        override fun memberCount(n: Int): String = if (n == 1) "1 number" else "$n numbers"
        override val builtinWork = "Work"
        override val builtinFavorite = "Favorites"
        override val builtinLocked = "Default group — can't be renamed or deleted."
        override val createTitle = "New category"
        override val editTitle = "Edit category"
        override val nameLabel = "Category name"
        override val nameHint = "e.g. Clients, Family…"
        override val descLabel = "Description"
        override val descHint = "Short description (optional)"
        override val iconLabel = "Icon & color"
        override val pickIconTitle = "Choose an icon"
        override val iconGroupBasic = "Basic"
        override val iconGroupDelivery = "Delivery"
        override val iconGroupWork = "Work"
        override val iconGroupIssue = "Delivery issues"
        override val iconGroupSocial = "Social"
        override val save = "Save"
        override val update = "Update"
        override val tabInfo = "Info"
        override fun tabNumbers(n: Int): String = "Numbers ($n)"
        override val emptyMembers = "No numbers in this category yet"
        override val removeMember = "Remove"
        override fun addedAt(time: String): String = "Added · $time"
        override val menuEdit = "Edit"
        override val menuDelete = "Delete category"
        override val deleteTitle = "Delete category?"
        override fun deleteWithMembers(name: String, n: Int): String =
            "“$name” has $n phone number${if (n == 1) "" else "s"}. Deleting it will remove all $n from the category. Are you sure?"
        override fun deleteEmpty(name: String): String = "Delete “$name”? This can't be undone."
        override val deleteConfirm = "Delete"
        override val cancel = "Cancel"
        override val discardTitle = "Discard changes?"
        override val discardMessage = "Your changes won't be saved."
        override val discardStay = "Stay"
        override val discardExit = "Discard"
        override val addToCategoryTitle = "Add to category"
        override val createNew = "Create new category"
        override val noCategories = "No categories yet"
        override val newCategory = "New category"
        override val addToCategory = "Add to category"
        override val maxCategories = "You can create at most 5 categories"
        override val maxMembers = "This category is full (100 numbers max)"
        override val alreadyAdded = "Number already in this category"
        override val invalidNumber = "Invalid number, can't add to a category"
        override fun addedTo(name: String): String = "Added to “$name”"
        override fun removedFrom(name: String): String = "Removed from “$name”"
    }

    private object Donate : DonateStrings {
        override val settingsSection = "Support"
        override val cardTitle = "Support the developer"
        override val cardSubtitle = "Scan a transfer QR · Entirely voluntary · Thank you ❤️"
        override val open = "Open the support page"

        override val screenTitle = "Support the developer"

        override val heroTitle = "Support the developer"
        override val heroMessage =
            "CallHS is made with care and stays free forever. If the app helps you, a small coffee keeps me motivated to maintain and improve it. Entirely voluntary — every feature stays the same whether you donate or not."

        override val amountSection = "Choose an amount"
        override val amountOpen = "Any amount"
        override val amountOpenHint = "You enter the amount in your banking app"
        override val amountCustom = "Other…"
        override val customDialogTitle = "Enter a support amount"
        override val customFieldLabel = "Enter an amount (VND)"
        override val customInvalid = "Please enter a valid amount"
        override val customMax = "Maximum 1,000,000 đ"
        override val confirm = "Done"

        override val qrSection = "Scan to transfer"
        override val qrHint = "Open your banking app and scan this QR code to send your support"
        override val qrOpenAmountNote = "Open code — you enter any amount when transferring"
        override val qrLoading = "Generating QR code…"
        override val qrError = "Couldn't load the QR code. Check your connection and try again."
        override val qrRetry = "Retry"
        override val qrOfflineFallback = "Backup code generated on-device (offline)"
        override val saveQr = "Save image"
        override val shareQr = "Share"
        override val qrSaved = "QR image saved to your gallery"
        override val qrSaveFailed = "Couldn't save the QR image"
        override val shareSubject = "CallHS support QR code"

        override val accountSection = "Transfer details"
        override val bankLabel = "Bank"
        override val accountNoLabel = "Account number"
        override val accountNameLabel = "Account holder"
        override val amountRowLabel = "Amount"
        override val messageLabel = "Message"
        override val amountOpenValue = "Any amount"
        override val copied = "Copied"

        override val bankAppsSection = "Open a banking app"
        override val bankAppsHint = "Pick your bank to open its app with the transfer pre-filled where supported. Otherwise, scan the QR above."
        override val bankAppsShowAll = "Show all"
        override val bankAppsShowLess = "Show less"
        override val bankAppOpenFailed = "Couldn't open the banking app"
        override val bankAppPrefill = "Pre-fill"
        override val bankAppNeedNetwork = "You need an internet connection to open the banking app"

        override val footerTitle = "A fully voluntary contribution"
        override val footerMessage =
            "This is a voluntary contribution to the developer — not a required fee, and it doesn't unlock any features. CallHS stays free for everyone. Thank you so much for your support."
        override val thankYou = "Thank you so much ❤️"
    }

    private object Blocker : CallBlockStrings {
        override val settingsSection = "Call blocking"
        override val settingsTitle = "Call & spam blocking"
        override val settingsSubtitle = "Blocking rules, history and notifications"
        override val settingsOpen = "Open call blocking"

        override val screenTitle = "Call blocking"
        override val settingsScreenTitle = "Call blocking settings"
        override val openSettings = "Open call blocking settings"
        override val featureDetailsAction = "View feature details"
        override val featureInfoSheetTitle = "Call-blocking feature information"
        override val featureInfoAvailabilityNote =
            "Applies only to incoming calls while CallHS is the active call-screening app and Call protection is on and not paused."
        override val roleTitle = "Enable call blocking"
        override val roleBody =
            "Android needs you to choose CallHS as the call-screening app. CallHS only screens incoming calls; it does not replace your phone's default dialer."
        override val roleAction = "Choose CallHS for spam blocking"
        override val roleUnavailableTitle = "Not supported on this device"
        override val roleUnavailableBody = "This device does not provide Android's call-screening role."
        override val roleActive = "CallHS is the current call-screening app"

        override val protectionTitle = "Call protection"
        override val protectionSubtitle =
            "While paused, every call is allowed through; your rules and protection settings stay saved and automatically resume when the timer ends."
        override val protectionOn = "Call screening is active"
        override val protectionOff = "Protection is off"
        override val enableProtectionAction = "Turn on call blocking"
        override val disableProtectionAction = "Turn off call blocking"
        override val pauseTimerTitle = "Pause timer"
        override val pauseTimerOff = "Off"
        override val pauseTimer10Minutes = "10m"
        override val pauseTimer30Minutes = "30m"
        override val pauseTimer1Hour = "1h"
        override val pauseTimerOffExplanation =
            "Off only cancels the timer; protection stays active while the main switch is on."
        override val pauseActive = "Paused by timer"
        override fun pausePeriod(from: String, to: String) = "$from → $to"
        override fun pauseRemaining(countdown: String) = "$countdown remaining"
        override val pauseUnavailableWhileOff = "Turn on protection to use the pause timer."
        override val dailyScheduleTitle = "Block & pause schedule"
        override fun dailyScheduleCount(count: Int, max: Int) = "$count/$max time windows"
        override val dailyScheduleDescription =
            "Repeats on selected days. Block windows turn protection on; Pause windows allow every call through."
        override val dailyScheduleBaseState = "Outside these windows, the Call protection switch applies."
        override val dailyScheduleEmpty = "No time windows yet. Choose a preset or set custom times."
        override val dailyScheduleAdd = "Add time window"
        override val dailyScheduleLimitReached = "The four-window limit has been reached."
        override val dailyScheduleBlock = "Block"
        override val dailySchedulePause = "Pause"
        override val dailyScheduleBlockActive = "Blocking by schedule"
        override val dailySchedulePauseActive = "Paused by schedule"
        override val dailyScheduleTimelineDescription = "24-hour block and pause schedule chart"
        override val dailyScheduleEditorAddTitle = "Add daily schedule"
        override val dailyScheduleEditorEditTitle = "Edit daily schedule"
        override val dailyScheduleActionTitle = "During this time"
        override val dailySchedulePresetTitle = "Quick presets"
        override val dailyScheduleMorning = "Morning"
        override val dailyScheduleAfternoon = "Afternoon"
        override val dailyScheduleEvening = "Evening"
        override val dailyScheduleNight = "Night"
        override val dailyScheduleCustom = "Custom time"
        override val dailyScheduleDaysTitle = "Active days"
        override val dailyScheduleEveryDay = "Every day"
        override fun dailyScheduleWeekdayShort(day: java.time.DayOfWeek) = when (day) {
            java.time.DayOfWeek.MONDAY -> "Mon"
            java.time.DayOfWeek.TUESDAY -> "Tue"
            java.time.DayOfWeek.WEDNESDAY -> "Wed"
            java.time.DayOfWeek.THURSDAY -> "Thu"
            java.time.DayOfWeek.FRIDAY -> "Fri"
            java.time.DayOfWeek.SATURDAY -> "Sat"
            java.time.DayOfWeek.SUNDAY -> "Sun"
        }
        override fun dailyScheduleToday(day: String) = "Today · $day"
        override val dailyScheduleEnabled = "Enabled"
        override val dailyScheduleDisabled = "Disabled"
        override val dailyScheduleStartTime = "Starts"
        override val dailyScheduleEndTime = "Ends"
        override val dailyScheduleTimeConfirm = "Done"
        override val dailyScheduleNextDay = "next day"
        override val dailyScheduleSave = "Save time window"
        override val dailyScheduleDelete = "Delete time window"
        override val dailyScheduleOverlapTitle = "Schedule overlap"
        override val dailyScheduleOverlapConfirm = "Got it"
        override fun dailyScheduleOverlapError(from: String, to: String) =
            "This overlaps the $from–$to schedule. Choose different times."
        override val dailyScheduleInvalidError = "Start and end times must be different."
        override val dailyScheduleNoDayError = "Select at least one active day."
        override val dailyScheduleStorageError = "The schedule could not be saved. Please try again."
        override val protectionOffBannerBody =
            "Your rules and settings are still saved, but calls are not being screened. Tap the red button below to turn protection back on."
        override val protectionPausedBannerBody =
            "Every call is currently allowed through; protection features will automatically resume when the timer ends."
        override val repeatCallerExceptionTitle = "Allow repeated unknown callers"
        override fun repeatCallerExceptionSubtitle(threshold: Int, minutes: Int) =
            "This feature only applies when CallHS confirms the number is outside Contacts and the call matches no rule. Attempts before threshold $threshold use the selected Handling method: the two Block modes record blocked-call history and notify according to settings; Silence only mutes the ringtone and creates no blocked-call record; Allow passes every call and does not count attempts. Once there are $threshold attempts in the most recent $minutes-minute window, the current and later calls are allowed while that rolling window remains at the threshold. If the number already matches a rule, the repeat feature is not evaluated and never overrides the result. The saved-contacts exception still applies separately to broad rules. Only while evaluating this repeated-caller feature, if CallHS cannot verify Contacts, it allows the call to avoid a false block."
        override val repeatCallerExceptionOff = "Repeated unknown-caller feature is off"
        override fun repeatCallerExceptionOn(threshold: Int, minutes: Int) =
            "Before attempt $threshold: use selected method · From attempt $threshold: allow within $minutes min"
        override val repeatCallerThresholdTitle = "Allow-from threshold"
        override fun repeatCallerThresholdOption(threshold: Int) = "Allow from attempt $threshold"
        override val repeatCallerWindowTitle = "Call-counting window"
        override fun repeatCallerWindowValue(minutes: Int) =
            if (minutes == 1) "1 minute" else "$minutes minutes"
        override val repeatCallerWindowSheetTitle = "Repeated unknown-call window"
        override fun repeatCallerWindowHint(minMinutes: Int, maxMinutes: Int) =
            "Enter a $minMinutes–$maxMinutes minute window"
        override fun repeatCallerWindowInvalid(minMinutes: Int, maxMinutes: Int) =
            "Enter a window from $minMinutes to $maxMinutes minutes."
        override val repeatCallerApply = "Apply"
        override val blockMethodTitle = "Handling method"
        override val blockMethodSubtitle =
            "Choose what CallHS does when a call matches a rule or is held below the unknown-caller gate threshold."
        override val chooseBlockMethod = "Choose handling method"
        override val methodBlockAndReject = "Block and reject"
        override val methodBlockAndRejectDesc = "Prevent the call from reaching you and send a rejection response to the caller."
        override val methodBlockWithoutReject = "Block without rejecting"
        override val methodBlockWithoutRejectDesc = "Prevent the call from reaching you without actively sending a rejection response."
        override val methodSilenceOnly = "Silence only"
        override val methodSilenceOnlyDesc = "Keep the incoming call visible but silence the device's ringtone."
        override val methodAllow = "Allow"
        override val methodAllowDesc =
            "Allow every call through. Rules stay saved but are not applied, and repeated unknown-caller attempts are not counted until you select another method."
        override val notificationTitleSetting = "Blocked-call notifications"
        override val notificationSubtitle = "Turn the distinct sound alert on or off for every blocked call."
        override val notificationPermissionNeeded = "Allow Android notifications to receive blocked-call alerts."
        override val notificationPermissionAction = "Allow notifications"
        override val notificationChannelNeedsAttention =
            "The alert channel is disabled, muted or not set to Urgent for heads-up alerts."
        override val notificationChannelSettingsAction = "Open channel settings"
        override val notificationOff = "Notifications off"
        override val notificationEvery = "Notify on every block"

        override val alwaysAllowTitle = "Allow list"
        override val alwaysAllowSubtitle = "Numbers in this list are always allowed through first."
        override val alwaysAllowDetails =
            "Add a number manually or pick it from Contacts, call history, or Categories. When a number in this list calls, CallHS allows it immediately.\n\n" +
                "The Allow list is always checked first. Picking a source only copies the number and does not follow later contact changes."
        override val blockedNumbersTitle = "Block list"
        override val blockedNumbersSubtitle = "Specific numbers that will be blocked when they call."
        override val blockedNumbersDetails =
            "Add a number manually or pick it from Contacts, call history, or Categories. When a number in this list calls, CallHS uses your selected blocking method.\n\n" +
                "The Block list is checked after the Allow list and before every other choice."
        override val groupBlockingTitle = "Handle by Contacts"
        override val groupBlockingSubtitle = "Choose how to handle saved and unsaved numbers."
        override val groupBlockingDetails =
            "Choose how CallHS handles numbers saved in Contacts and numbers outside Contacts. Each choice clearly says whether the call is allowed, blocked, or continues to Advanced rules.\n\n" +
                "The Allow list and Block list are always checked first. Repeat handling only runs when no rule matches."
        override val advancedRulesTitle = "Advanced rules"
        override val advancedRulesSubtitle = "Spam-risk filter, number patterns, carriers, regions and special types."
        override val advancedRulesDetails =
            "Use the spam-risk signals filter or create conditions for prefixes, suffixes, contained digits, length, carrier, region, or call type. Each rule can apply to saved numbers, unsaved numbers, or everyone.\n\n" +
                "CallHS checks enabled rules from top to bottom and applies the first matching rule."
        override fun savedNumberCount(count: Int) = if (count == 1) "1 number" else "$count numbers"
        override val manageSection = "Manage call blocking"
        override val allowlistScreenTitle = "Allow list"
        override val blocklistScreenTitle = "Block list"
        override val allowlistEmpty = "The Allow list is empty."
        override val blocklistEmpty = "The Block list is empty."
        override val addNumber = "Add number"
        override val addNumberSourceTitle = "Choose number source"
        override val sourceEnterManually = "Enter manually"
        override val sourceFromContacts = "Choose from Contacts"
        override val sourceFromCallHistory = "Choose from call history"
        override val sourceFromCategories = "Choose from Categories"
        override val enterNumberTitle = "Add phone number"
        override val enterNumberHint = "Phone number"
        override val enterNumberNameHint = "Display name (optional)"
        override val addToAllowlist = "Add to Allow list"
        override val addToBlocklist = "Add to Block list"
        override val numberAlreadyExists = "This number is already in the list."
        override val numberMovedToAllowlist = "Number moved to Allow list."
        override val numberMovedToBlocklist = "Number moved to Block list."
        override fun numberAddedAt(time: String) = "Added at $time"
        override val menuDeleteNumber = "Remove number from list"
        override val menuMoveToAllowlist = "Move to Allow list"
        override val menuMoveToBlocklist = "Move to Block list"
        override val menuEnableNumber = "Enable this number"
        override val menuDisableNumber = "Disable this number"
        override val advancedOrderNote = "The first matching rule is applied. Long-press a rule to change its order."
        override val menuMoveRuleUp = "Move rule up"
        override val menuMoveRuleDown = "Move rule down"
        override val menuEnableRule = "Enable rule"
        override val menuDisableRule = "Disable rule"
        override val enableAllAdvancedRules = "Enable all rules"
        override val disableAllAdvancedRules = "Disable all rules"
        override val deleteAllAdvancedRules = "Delete all rules"
        override fun enableAllAdvancedRulesMessage(count: Int) =
            "Enable all $count advanced rules. They will apply again in their current order whenever Call protection is active."
        override fun disableAllAdvancedRulesMessage(count: Int) =
            "Disable all $count advanced rules. The rules stay saved for later; number lists and Contacts handling are not affected."
        override fun deleteAllAdvancedRulesMessage(count: Int) =
            "Permanently delete all $count advanced rules. This cannot be undone; number lists and blocking history are not deleted."
        override val groupScreenTitle = "Handle by Contacts"
        override val blockSavedContactsGroup = "Block all saved contacts"
        override val blockSavedContactsGroupDesc = "Applies to every saved number except entries in the Allow list."
        override val blockUnknownNumbersGroup = "Handle numbers outside Contacts"
        override val blockUnknownNumbersGroupDesc = "Allow, always block, or block until a repeated caller reaches the threshold."
        override val unknownPolicyTitle = "Outside Contacts"
        override val unknownPolicyPass = "Allow if no rule matches"
        override val unknownPolicyBlockAlways = "Block all"
        override val unknownPolicyBlockUntilRepeat = "Block until repeated"
        override val unknownPolicyPassDesc = "CallHS still checks the Block list and Advanced rules. If nothing matches, the call is allowed."
        override val unknownPolicyBlockAlwaysDesc = "Every number confirmed outside Contacts is blocked unless it is in the Allow list."
        override val unknownPolicyBlockUntilRepeatDesc = "If no rule matches, CallHS blocks the first calls and allows the number after it reaches the repeat threshold within the selected time."
        override val specialGroupsTitle = "Special call types"
        override val advancedRulesScreenTitle = "Advanced rules"
        override val advancedRulesEmpty = "There are no advanced rules yet."
        override val addAdvancedRule = "Add advanced rule"
        override val ruleScopeLabel = "Which numbers?"
        override val scopeUnknown = "Not saved"
        override val scopeContacts = "Saved"
        override val scopeAll = "All numbers"
        override val scopeUnknownDesc = "Only check numbers not saved in Contacts."
        override val scopeContactsDesc = "Only check numbers saved in Contacts."
        override val scopeAllDesc = "Check both saved and unsaved numbers."
        override fun ruleScopeSummary(scope: String) = when (scope) {
            "saved_contact" -> scopeContacts
            "not_saved" -> scopeUnknown
            else -> scopeAll
        }
        override fun rulePreview(summary: String, scope: String) = "Block · $scope · $summary"
        override val typeLength = "Number of digits"
        override val lengthHint = "Example: 10"
        override val ruleActionLabel = "What should CallHS do?"
        override val actionBlock = "Block"
        override val actionAllow = "Allow through"
        override val actionBlockDesc = "Use the method selected in Call blocking settings."
        override val actionAllowDesc = "Let the call through."
        override val savedPolicyTitle = "Saved in Contacts"
        override val savedPolicyFollowRules = "Apply rules"
        override val savedPolicyAllow = "Always allow"
        override val savedPolicyBlock = "Block all"
        override val savedPolicyFollowRulesDesc = "This choice continues to Advanced rules."
        override val savedPolicyAllowDesc = "Every saved number is allowed unless it is in the Block list."
        override val savedPolicyBlockDesc = "Every saved number is blocked unless it is in the Allow list."
        override val groupPriorityNote = "The Allow list and Block list are always checked before the choices on this screen."
        override val processingGuideItemTitle = "Learn how CallHS handles calls"
        override val processingGuideItemSubtitle = "See the order CallHS uses to check and handle an incoming call."
        override val processingGuideSheetTitle = "How CallHS handles a call"
        override val processingGuideIntro = "CallHS checks the following steps in order and stops as soon as it has a result."
        override fun processingGuideStepTitle(step: Int) = when (step) {
            1 -> "Check call protection"
            2 -> "Check the Allow list"
            3 -> "Check the Block list"
            4 -> "Handle by Contacts"
            5 -> "Check Advanced rules"
            else -> "Use the default result"
        }
        override fun processingGuideStepDescription(step: Int) = when (step) {
            1 -> "If protection is off or paused, every call is allowed."
            2 -> "A number in the Allow list is allowed immediately."
            3 -> "A number in the Block list is blocked."
            4 -> "CallHS applies the selected choice for saved or unsaved numbers. Choose Apply rules to continue."
            5 -> "CallHS checks from top to bottom. The first matching rule is applied."
            else -> "If no list or rule matches, the call is allowed. Repeat handling only runs at the final step for unsaved numbers."
        }
        override val processingGuideConclusion = "The Allow list always has the highest priority."

        override val commonIssuesTitle = "Common issues"
        override val commonIssuesSubtitle =
            "Causes and fixes when call blocking or notifications do not work as expected."
        override val commonIssuesIntro =
            "Choose the issue you are seeing. Check the possible causes in order before changing your rules."
        override val commonIssuesPossibleCause = "Possible cause"
        override val commonIssuesHowToFix = "How to fix it"
        override val commonIssuesOpenBlockSettings = "Open call blocking settings"
        override val commonIssuesOpenNotificationSettings = "Open Android notification settings"
        override val commonIssuesExpand = "See causes and fixes"
        override val commonIssuesCollapse = "Collapse guidance"
        override fun commonIssueTitle(issue: Int) = when (issue) {
            1 -> "Call blocking is not working"
            2 -> "Some calls still get through"
            3 -> "An important or saved number was blocked by mistake"
            4 -> "No notification appears after a block"
            5 -> "A notification has no sound, vibration, or heads-up alert"
            6 -> "A call is missing from blocked-call history"
            else -> "Private or VoIP calls are not blocked"
        }
        override fun commonIssueCause(issue: Int) = when (issue) {
            1 -> "CallHS may no longer be the call-screening app, or Call protection may be off, paused, or inside a scheduled pause. Android may also stop sending calls to CallHS after the app is force-stopped or before the first unlock following a restart."
            2 -> "The number may be on the Allow list, allowed by Contacts handling or the first matching rule, or may have reached the repeat-caller threshold. Silence only and Allow through also do not reject the call."
            3 -> "The Block list is checked before Contacts handling. An Advanced rule scoped to saved numbers or all numbers can also match and block that contact."
            4 -> "Notifications may be off in CallHS, disabled for the current scheduled period, denied by Android, or disabled for the notification channel. Only a call that is actually blocked creates an alert; Silence only and Allow through do not create blocked-call alerts."
            5 -> "Sound, vibration, or the display style may be off in Advanced notification settings. Do Not Disturb, channel importance, and device-maker notification controls can also limit the alert."
            6 -> "CallHS history records only calls that were actually blocked. Allowed and silence-only calls are not recorded there, and blocked-call history is not a copy of Android's system call log."
            else -> "Standard Android generally sends a screening app only calls with a valid, visible telephone number. Private or unavailable callers and VoIP/SIP calls depend on the Phone app and device-maker extensions, so they may never reach CallHS."
        }
        override fun commonIssueFix(issue: Int) = when (issue) {
            1 -> "Reopen CallHS, grant the call-blocking role again, and turn on Call protection. Check enabled/pause schedules; after a restart, unlock the device at least once."
            2 -> "Review the Allow list, Contacts handling, the first matching rule, and repeat-caller settings. Under Handling method, select either Block option if you want Android to stop the call."
            3 -> "Add the number to the Allow list, which has the highest priority. Then review enabled rules and their Saved/All numbers scope to avoid broader blocking than intended."
            4 -> "Select Notify on every block, check the advanced notification schedule, then allow notifications and enable the CallHS alert channel in Android."
            5 -> "Check sound, vibration, and display style in Advanced notification settings. Then open the Android channel settings, enable sound and heads-up alerts, and check Do Not Disturb."
            6 -> "Select a Block handling method, open the History tab, and choose the correct date or range. Check the system call log if you need to see calls that were allowed."
            else -> "CallHS cannot guarantee blocking when Android does not deliver the call for screening. Add a visible number to the Block list; for private or VoIP calls, also check blocking features from the Phone app and your carrier."
        }

        override val tabRules = "Blocking rules"
        override fun tabHistory(count: Int) = "History ($count)"
        override val addRule = "Add blocking rule"
        override val emptyRules =
            "There are no advanced rules yet. Allow list, Block list and Handle by Contacts are managed separately."
        override val emptyHistory = "No calls have been blocked yet. CallHS will show them here after blocking."
        override fun ruleCount(count: Int) = if (count == 1) "1 saved rule" else "$count saved rules"
        override val ruleEnabledStatus = "Active"
        override val ruleDisabledStatus = "Off"
        override fun blockedCount(count: Int) = if (count == 1) "Blocked once" else "Blocked $count times"
        override fun blockedAt(time: String) = "Blocked at $time"
        override fun matchedRule(rule: String) = "By rule: $rule"
        override fun repeatCallerGuardReason(attempt: Int, threshold: Int, minutes: Int) =
            "Unknown caller blocked below threshold · attempt $attempt/$threshold · $minutes min window"
        override fun consecutiveMissed(count: Int) =
            if (count == 1) "1 unanswered call" else "$count consecutive unanswered calls"
        override val menuDeleteRule = "Delete blocking rule"
        override val menuDeleteHistory = "Delete history"

        override val historyPeriodDay = "Day"
        override val historyPeriodWeek = "Week"
        override val historyPeriodMonth = "Month"
        override val historyPickDate = "Choose a specific date"
        override val historyDateRangeNote = "You can only choose a date within the last 30 days."
        override val historyOverviewTitle = "Blocking overview"
        override val historyTotalBlocks = "Blocks"
        override val historyUniqueNumbers = "Unique numbers"
        override val historyPeakHour = "Peak hour"
        override val historyPeakDay = "Peak day"
        override val historyNoPeak = "No data"
        override val historyActivityTitle = "Activity distribution"
        override val historyHourlySubtitle = "One-hour intervals · from 0h through 24h"
        override val historyDailySubtitle = "Every calendar day in the selected period"
        override val historySwipeChartHint = "Swipe horizontally to view the full chart"
        override fun historyHourBucket(fromHour: Int, toHour: Int) = "${fromHour}h–${toHour}h"
        override fun historyDayAxis(day: Int) = "Day %02d".format(day)
        override fun historyWeekdayAxis(day: java.time.DayOfWeek) = when (day) {
            java.time.DayOfWeek.MONDAY -> "Monday"
            java.time.DayOfWeek.TUESDAY -> "Tuesday"
            java.time.DayOfWeek.WEDNESDAY -> "Wednesday"
            java.time.DayOfWeek.THURSDAY -> "Thursday"
            java.time.DayOfWeek.FRIDAY -> "Friday"
            java.time.DayOfWeek.SATURDAY -> "Saturday"
            java.time.DayOfWeek.SUNDAY -> "Sunday"
        }
        override val historyDayPartsTitle = "By time of day"
        override val historyDayPartsSubtitle = "Uses the same ranges as Call analysis"
        override fun historyDayPartRange(fromHour: Int, toHour: Int) = "%02dh–%02dh".format(fromHour, toHour)
        override val historyReasonsTitle = "Top blocking reasons"
        override val historyTopNumbersTitle = "Most-blocked numbers"
        override fun historyDetails(count: Int) = "Detailed history ($count)"
        override fun historyEvents(count: Int) = if (count == 1) "1 event" else "$count events"
        override fun historyRange(from: String, to: String) = "$from – $to"
        override fun historyTrendUp(count: Int) = if (count == 1) {
            "Up 1 event from the previous period"
        } else {
            "Up $count events from the previous period"
        }
        override fun historyTrendDown(count: Int) = if (count == 1) {
            "Down 1 event from the previous period"
        } else {
            "Down $count events from the previous period"
        }
        override val historyTrendSame = "No change from the previous period"
        override val historyNoEventsInPeriod = "No calls were blocked in this period."

        override val createRuleTitle = "Add blocking rule"
        override val editRuleTitle = "Edit blocking rule"
        override val save = "Save rule"
        override val update = "Update"
        override val ruleTypeLabel = "Choose calls to handle"
        override val ruleValueLabel = "Number or digit sequence"
        override val exactValueLabel = "Enter a phone number"
        override val prefixValueLabel = "Enter a starting sequence"
        override val suffixValueLabel = "Enter an ending sequence"
        override val containsValueLabel = "Enter a digit sequence"
        override val lengthValueLabel = "Enter the number of digits"
        override val numberHint = "Enter a number or number sequence"
        override val carrierHint = "Choose a carrier"
        override val chooseRuleType = "Choose calls to handle"
        override val chooseCarrier = "Choose carrier"
        override val typeExact = "One specific phone number"
        override val typePrefix = "Number starts with"
        override val typeSuffix = "Number ends with"
        override val typeContains = "Number contains"
        override val typeCarrier = "By mobile carrier"
        override val typeSpamRisk = "Calls with spam-risk signals"
        override val spamRiskPickerDescription =
            "A call with at least one risk signal recognized by CallHS."
        override val spamRiskDetailsTitle =
            "CallHS blocks when it detects at least one of these signals:"
        override val spamRiskPrefixDetail =
            "A complete Vietnamese number uses the 022, 023, 024, 028, 059 or 099 prefix."
        override val spamRiskUnknownPrefixDetail =
            "A 10-digit Vietnamese mobile number has a prefix CallHS does not recognize."
        override val spamRiskVerificationDetail =
            "On Android 11 or later, the mobile network cannot verify the caller's number. CallHS skips this signal if the device provides no verification information."
        override val spamRiskWarning =
            "These prefixes can still belong to valid numbers. This filter may block legitimate calls; the Allow list always takes priority."
        override fun spamRiskReasonPrefix(prefix: String) = "Spam-risk signal · Matches prefix $prefix"
        override fun spamRiskReasonUnknownMobilePrefix(prefix: String) =
            "Spam-risk signal · Unrecognized mobile prefix $prefix"
        override val spamRiskReasonVerificationFailed =
            "Spam-risk signal · Network verification failed"
        override val typeSpecial = "Special call types"
        override val typeContacts = "From contacts"
        override val typeCallHistory = "From call history"
        override val typeCountryAndAreaCode = "Countries & Vietnam prefixes"
        override val specialTitle = "Choose one call type"
        override val specialPrivate = "Hidden-number calls"
        override val specialPrivateDesc =
            "A call that does not provide the caller's CLI or phone number."
        override val specialUnknownContact = "Numbers not saved in Contacts"
        override val specialUnknownContactDesc = "Detect phone numbers that do not match any contact on this device."
        override val specialVoip = "Internet calls (legacy rule)"
        override val specialVoipDesc = "Used only to read old history or data."
        override val specialSipPhone = "SIP URI with a phone user"
        override val specialSipPhoneDesc =
            "The user part before @ is a phone number, for example sip:+84912345678@provider.vn."
        override val specialSipText = "SIP URI with a text user"
        override val specialSipTextDesc =
            "The user part before @ is a text or character string, for example sip:support@company.vn."
        override val identityTermsTitle = "Learn call terminology"
        override val learnVoip = "What is VoIP?"
        override val learnSip = "What is SIP?"
        override val learnUri = "What is a URI?"
        override val learnCli = "What is CLI?"
        override val voipExplanation =
            "VoIP (Voice over Internet Protocol) carries voice over an IP network such as Wi-Fi, 4G/5G, or the Internet instead of relying only on a traditional voice network.\n\n" +
                "A VoIP call may display a regular phone number or a SIP identity. VoIP describes how a call is transported; it does not by itself mean that the call is spam or a scam.\n\n" +
                "In CallHS, “Internet call (legacy rule)” is retained only for reading older data. For a new rule, choose the appropriate SIP condition when the device supplies the call URI."
        override val sipExplanation =
            "SIP (Session Initiation Protocol) is used to establish, manage, and end calling sessions over an IP network. A SIP identity commonly looks like sip:user@domain; sips: uses a secured connection for SIP signaling.\n\n" +
                "The user part before @ may be a phone number, such as sip:+84912345678@provider.vn, or text such as sip:support@company.vn. CallHS exposes these as two separate special-call conditions.\n\n" +
                "Standard Android normally delivers only tel: addresses to CallScreeningService. CallHS can identify SIP/sips only when the device or manufacturer passes that URI to the app."
        override val uriExplanation =
            "URI (Uniform Resource Identifier) is a string that identifies a resource. For a call, the URI describes the kind of caller address and its value.\n\n" +
                "The part before the colon is the scheme. For example, tel:+84912345678 uses tel:, sip:user@domain uses sip:, and sips:user@domain uses sips:. A URI is not necessarily a web address and does not always contain a phone number.\n\n" +
                "CallHS classifies a SIP condition only when the URI is valid, uses the sip: or sips: scheme, and contains a user before @."
        override val cliExplanation =
            "CLI (Calling Line Identification) is the caller's phone-number information supplied by the network or calling service, for example +84912345678. CallHS uses CLI for rules based on a number, prefix, suffix, length, carrier, or region.\n\n" +
                "CLI is phone-number data, not a name saved by the user in Contacts. CLI can be hidden or spoofed, so the displayed number does not guarantee the caller's true identity."
        override val specialAndroidLimit =
            "Standard Android sends only tel: handles to CallScreeningService. SIP/sips support is an OEM (Original Equipment Manufacturer) or device extension; if the URI is not delivered to CallHS, the app has no data with which to identify the SIP criterion."
        override val contactPickerTitle = "Choose contacts"
        override val contactPickerOpen = "Choose from contacts"
        override val contactPickerSearchHint = "Search names or phone numbers"
        override val contactPickerPermissionTitle = "Contacts permission needed"
        override val contactPickerPermissionBody =
            "Allow CallHS to read contacts so you can choose people and identify unknown numbers. The app never edits or uploads your contacts."
        override val contactPickerPermissionAction = "Allow access"
        override fun contactPickerSelectedCount(count: Int) =
            if (count == 1) "1 contact selected" else "$count contacts selected"
        override val contactPickerDone = "Done"
        override val contactPickerEmpty = "There are no contacts with phone numbers yet."
        override val contactPickerNoResults = "No matching contacts found."
        override val callHistoryPickerTitle = "Choose numbers from call history"
        override val callHistoryPickerOpen = "Choose from call history"
        override val callHistoryPickerSearchHint = "Search names or phone numbers"
        override fun callHistoryPickerSelectedCount(count: Int) =
            if (count == 1) "1 number selected" else "$count numbers selected"
        override val callHistoryPickerEmpty = "There are no valid phone numbers in call history yet."
        override val callHistoryPickerNoResults = "No numbers match your search or filters."
        override val callHistoryPickerPreviouslySelected = "Previously selected"
        override val callHistoryPickerPreviouslySelectedNote =
            "These numbers are not in the currently displayed history. You can still deselect them here."
        override val validationSelectSpecial = "Choose at least one call type."
        override val validationSelectContact = "Choose at least one contact."
        override val validationSelectCallHistory = "Choose at least one number from call history."
        override val regionPickerTitle = "Choose countries & Vietnam prefixes"
        override val regionInternationalSection = "International country codes"
        override val regionVietnamPrefixSection = "Vietnam prefixes"
        override val regionAllInternationalExceptVietnam = "All international numbers except Vietnam (+84)"
        override val regionAllInternationalExceptVietnamDesc =
            "Detect international numbers whose country code is not Vietnam (+84)."
        override val regionChina = "China (+86)"
        override val regionCambodia = "Cambodia (+855)"
        override val regionMyanmar = "Myanmar (+95)"
        override val regionNanpShared = "NANP (+1, including the United States)"
        override val regionGermany = "Germany (+49)"
        override val regionLaos = "Laos (+856)"
        override val regionThailand = "Thailand (+66)"
        override val regionMalaysia = "Malaysia (+60)"
        override val regionSingapore = "Singapore (+65)"
        override val regionIndonesia = "Indonesia (+62)"
        override val regionPhilippines = "Philippines (+63)"
        override val regionIndia = "India (+91)"
        override val regionPrefix024 = "024 · Hanoi"
        override val regionPrefix022 = "022 · Fixed-line prefix family"
        override val regionPrefix028 = "028 · Ho Chi Minh City"
        override val regionPrefix059 = "059 · Mobile prefix"
        override val regionPrefix099 = "099 · Gmobile mobile prefix"
        override val regionCallerIdWarning =
            "Filtering uses the displayed caller ID. Caller ID can be spoofed, so a code does not guarantee the call's true origin."
        override val validationSelectRegion = "Choose at least one country code or Vietnam prefix."
        override val invalidRule = "That value is not valid. For a number sequence, enter at least 2 digits."
        override val duplicateRule = "This blocking rule already exists."
        override val maxRules = "You've reached the limit of 200 blocking rules."
        override val discardTitle = "Discard changes?"
        override val discardMessage = "Your unsaved changes to this blocking rule will be lost."
        override val discardStay = "Keep editing"
        override val discardExit = "Discard"

        override fun ruleSummary(type: String, value: String) = when (type) {
            "exact" -> "Number $value"
            "prefix" -> "Starts with $value"
            "suffix" -> "Ends with $value"
            "contains" -> "Contains $value"
            "length" -> "$value digits long"
            "any" -> "Any number in scope"
            "carrier" -> "$value carrier"
            "repeat_unanswered" -> "Legacy rule · Repeated unanswered calls"
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
                1 -> "One selected contact"
                else -> "$count selected contacts"
            }
        }

        override fun callHistorySummary(value: String): String {
            val count = CallHistoryRuleCodec.selectedCount(value)
            return when (count) {
                0 -> typeCallHistory
                1 -> "One number from call history"
                else -> "$count numbers from call history"
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

        override val notificationChannelName = "Urgent blocked-call alerts"
        override val notificationChannelDescription =
            "Heads-up, sound and vibration when CallHS blocks a call"
        override fun notificationTitle(number: String) = "Blocked a call from $number"
        override fun notificationBody(total: Int, rule: String) = "Block #$total for this number · $rule"
    }

    private object Backup : BackupStrings {
        override val settingsSection = "Backup & restore"
        override val cardTitle = "Back up & restore data"
        override val cardSubtitle = "Export your data to a file and import it back when needed"
        override val open = "Open backup & restore"

        override val screenTitle = "Backup & restore"
        override val callLogNote =
            "The system call log is read-only and is not backed up; CallHS's own blocked-call history can be backed up here."

        override val backupTitle = "Backup (export file)"
        override val backupDesc =
            "Pick the data to back up, then export it to a file. Keep this file in case you lose your phone or switch to a new one."
        override val chooseData = "Choose data"
        override val exportButton = "Export to file"
        override val exporting = "Exporting…"

        override val restoreTitle = "Restore (import file)"
        override val restoreDesc = "Pick a saved backup file to restore its data into the app."
        override val pickFileButton = "Choose backup file"
        override val pickAnotherButton = "Choose another file"
        override val restoreButton = "Restore"
        override val restoring = "Restoring…"
        override val fileLabel = "Backup file"
        override fun fileMeta(date: String, appVersion: String) =
            if (appVersion.isBlank()) "Created $date" else "Created $date · version $appVersion"
        override val chooseSections = "Choose what to restore"

        override val modeTitle = "How to restore"
        override val modeReplace = "Replace all"
        override val modeReplaceDesc = "Delete current data and replace it with the backup."
        override val modeAdd = "Add, don't overwrite"
        override val modeAddDesc = "Only add new items, keep existing data untouched."
        override val modeUpdate = "Update & add"
        override val modeUpdateDesc = "Add new items and update matching ones from the backup."

        override val secTemplates = "Message templates"
        override val secTemplatesSub = "Your saved message templates"
        override val secQr = "QR scan history"
        override val secQrSub = "Recently scanned QR codes"
        override val secCategories = "Categories"
        override val secCategoriesSub = "Groups & their member numbers"
        override val secBlockRules = "Call-blocking rules"
        override val secBlockRulesSub = "Rules, lists, schedules, protection and advanced notifications"
        override val secBlockHistory = "Blocked-call history"
        override val secBlockHistorySub = "Events blocked by CallHS"
        override val secMyNumber = "My number"
        override val secMyNumberSub = "Your phone numbers per SIM"
        override val secOutgoingCall = "Outgoing-call settings"
        override val secOutgoingCallSub = "Status, alert conditions and presentation style"
        override val secDisplay = "Display settings"
        override val secDisplaySub = "Theme, language, text size"
        override fun itemsCount(n: Int) = if (n == 1) "1 item" else "$n items"

        override val confirmReplaceTitle = "Overwrite data?"
        override val confirmReplaceMessage =
            "“Replace all” will DELETE the current data of the selected sections and replace it with the backup. This cannot be undone."

        override val exportOkTitle = "Backup saved"
        override val exportOkMessage = "Your data was exported to the file successfully."
        override val resultTitle = "Restored"
        override fun resultLine(section: String, added: Int, updated: Int, skipped: Int): String {
            val parts = buildList {
                if (added > 0) add("+$added added")
                if (updated > 0) add("$updated updated")
                if (skipped > 0) add("$skipped skipped")
            }
            return if (parts.isEmpty()) "$section: no changes" else "$section: ${parts.joinToString(" · ")}"
        }
        override val displayApplied = "Applied"
        override val displayKept = "Kept"
        override val truncatedNote = "Some items exceeded the limit and were left out."
        override val done = "Done"

        override val errInvalidFile = "Invalid file, or not a CallHS backup."
        override val errWriteFailed = "Couldn't save the file. Please try again."
        override val errReadFailed = "Couldn't read the file. Please try again."
        override val errNothingSelected = "Please select at least one section."
        override val errEmptyBackup = "The backup file has no data to restore."
    }

    /** Tên tháng tiếng Anh cho [CallList.monthYear]. */
    private val MONTHS = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
}
