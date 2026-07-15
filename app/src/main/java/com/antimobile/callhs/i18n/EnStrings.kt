package com.antimobile.callhs.i18n

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
        override val contactsHeadline = "Show contact names"
        override val contactsDesc =
            "CallHS reads your contacts to replace phone numbers with saved NAMES and photos, so you instantly " +
                "recognize who called. The app only reads — it never adds / edits / deletes contacts."
        override val contactsBullet1 = "Show contact name & photo instead of unknown numbers"
        override val contactsBullet2 = "Easily recognize who called in the list"
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

    /** Tên tháng tiếng Anh cho [CallList.monthYear]. */
    private val MONTHS = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
}
