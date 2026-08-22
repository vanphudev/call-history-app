package com.antimobile.callhs.ui.blocking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.antimobile.callhs.data.blocking.CallBlockCallHistorySelection
import com.antimobile.callhs.data.blocking.CallHistoryRuleCodec
import com.antimobile.callhs.data.model.CallEntry
import com.antimobile.callhs.i18n.LanguageSettings
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.calllist.CallListViewModel
import com.antimobile.callhs.ui.calllist.DateFilter
import com.antimobile.callhs.ui.calllist.ListCallItem
import com.antimobile.callhs.ui.calllist.Row2
import com.antimobile.callhs.ui.calllist.TypeFilter
import com.antimobile.callhs.ui.calllist.TypeFilterButton
import com.antimobile.callhs.ui.calllist.TypeFilterSheet
import com.antimobile.callhs.ui.calllist.ViewMode
import com.antimobile.callhs.ui.calllist.buildRows
import com.antimobile.callhs.ui.components.EmptyState
import com.antimobile.callhs.ui.components.Avatar
import com.antimobile.callhs.ui.components.LoadingState
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.PermissionState
import com.antimobile.callhs.ui.components.SectionHeader
import com.antimobile.callhs.ui.components.SimScopeStatusPill
import com.antimobile.callhs.ui.components.SimSegmentedToggle
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.FieldSurface
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.PhoneKey
import com.antimobile.callhs.util.SimScope
import com.antimobile.callhs.util.formatPhone
import com.antimobile.callhs.util.hasPermission

/**
 * Picker toàn màn hình chọn nhiều SỐ từ Call Log. Danh sách, tìm kiếm, lọc loại cuộc gọi,
 * lọc SIM và nhóm timeline dùng lại trực tiếp logic của [com.antimobile.callhs.ui.calllist.CallListScreen].
 * Mỗi [PhoneKey] chỉ hiện một dòng đại diện mới nhất sau bộ lọc; snapshot ngoài cửa sổ Call Log
 * hoặc bị phạm vi SIM ẩn vẫn được giữ nguyên khi người dùng hoàn tất.
 */
@Composable
internal fun CallBlockCallHistoryPickerScreen(
    vm: CallListViewModel,
    initialSelection: List<CallBlockCallHistorySelection>,
    onBack: () -> Unit,
    onDone: (List<CallBlockCallHistorySelection>) -> Unit,
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val s = appStrings().blocker
    var hasCallLogPermission by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.READ_CALL_LOG))
    }
    var refreshBaseline by remember { mutableStateOf(vm.completedLoadGeneration) }
    var awaitingInitialRefresh by remember { mutableStateOf(hasCallLogPermission) }
    fun requestFreshHistory() {
        refreshBaseline = vm.completedLoadGeneration
        awaitingInitialRefresh = true
        vm.load()
    }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCallLogPermission = granted
        if (granted) {
            permanentlyDenied = false
            requestFreshHistory()
        } else {
            val activity = context.findPickerActivity()
            permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CALL_LOG)
        }
    }

    // Luôn nạp lại khi mở picker để CallLogRepository áp đúng SimScope hiện hành.
    LaunchedEffect(Unit) {
        if (hasCallLogPermission) requestFreshHistory()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        val nowGranted = hasPermission(context, Manifest.permission.READ_CALL_LOG)
        if (nowGranted && !hasCallLogPermission) requestFreshHistory()
        if (!nowGranted) awaitingInitialRefresh = false
        hasCallLogPermission = nowGranted
    }
    LaunchedEffect(vm.completedLoadGeneration, refreshBaseline, awaitingInitialRefresh) {
        if (awaitingInitialRefresh && vm.completedLoadGeneration > refreshBaseline) {
            awaitingInitialRefresh = false
        }
    }

    val initialPayload = remember(initialSelection) { CallHistoryRuleCodec.encode(initialSelection) }
    var selectedPayload by rememberSaveable(initialPayload) { mutableStateOf(initialPayload) }
    var query by rememberSaveable { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    var typeFilter by rememberSaveable { mutableStateOf(TypeFilter.ALL) }
    var simFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showTypeSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = searchFocused) { focus.clearFocus() }
    BackHandler(enabled = !searchFocused, onBack = onBack)

    val selectableEntries = remember(vm.entries) {
        vm.entries.filter { entry -> CallHistoryRuleCodec.isSelectableNumber(entry.number) }
    }
    val currentByKey = remember(selectableEntries) {
        buildMap {
            // CallLogRepository trả mới → cũ; putIfAbsent giữ đại diện mới nhất cho mỗi số.
            selectableEntries.forEach { entry -> putIfAbsent(PhoneKey.of(entry.number), entry.toHistorySelection()) }
        }
    }
    val globalSim = SimScope.effectiveLabel
    val simLabels = remember(selectableEntries) {
        selectableEntries.mapNotNull { it.simLabel }.distinct().sorted()
    }
    val showSimFilter = globalSim == null && simLabels.size >= 2
    val effectiveSim = if (globalSim != null) null else simFilter?.takeIf { it in simLabels && simLabels.size >= 2 }
    val selectedSnapshots = remember(selectedPayload) { CallHistoryRuleCodec.decode(selectedPayload) }
    val selectedByKey = remember(selectedSnapshots) {
        selectedSnapshots.associateBy { PhoneKey.of(it.rawNumber) }
    }
    val selectedSet = selectedByKey.keys
    val unmatchedSelections = remember(selectedSnapshots, currentByKey) {
        selectedSnapshots.filter { PhoneKey.of(it.rawNumber) !in currentByKey }
    }
    val rows = remember(
        selectableEntries,
        query,
        typeFilter,
        effectiveSim,
        LanguageSettings.lang,
    ) {
        buildRows(
            entries = selectableEntries,
            query = query,
            typeFilter = typeFilter,
            dateFilter = DateFilter.NONE,
            customEpochDay = 0L,
            simFilter = effectiveSim,
            viewMode = ViewMode.BY_PHONE,
            categoryNumbers = null,
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { EditorTopBar(title = s.callHistoryPickerTitle, onBack = onBack) },
        bottomBar = {
            EditorSaveBar(
                label = s.contactPickerDone,
                enabled = selectedSet.isNotEmpty() && !awaitingInitialRefresh,
                onSave = {
                    val result = selectedSet.mapNotNull { key -> currentByKey[key] ?: selectedByKey[key] }
                    if (result.isNotEmpty()) onDone(result)
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (hasCallLogPermission) {
                CallHistoryPickerSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onFocusChanged = { searchFocused = it },
                    onImeSearch = { focus.clearFocus() },
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TypeFilterButton(
                        current = typeFilter,
                        onClick = { focus.clearFocus(); showTypeSheet = true },
                    )
                    Spacer(Modifier.weight(1f))
                    if (selectedSet.isNotEmpty()) {
                        Text(
                            text = s.callHistoryPickerSelectedCount(selectedSet.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                when {
                    globalSim != null -> Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        SimScopeStatusPill(globalSim)
                    }
                    showSimFilter -> {
                        val simRowModifier = if (simLabels.size > 2) {
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        } else {
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        }
                        Row(
                            modifier = simRowModifier,
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SimSegmentedToggle(
                                allLabel = appStrings().callList.filterAll,
                                labels = simLabels,
                                selected = effectiveSim,
                                onSelect = { simFilter = it },
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } },
            ) {
                when {
                    !hasCallLogPermission -> PermissionState(
                        onRequest = {
                            if (permanentlyDenied) {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null),
                                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_CALL_LOG)
                            }
                        },
                        buttonLabel = if (permanentlyDenied) appStrings().common.openSettings else appStrings().common.allowAccess,
                    )
                    awaitingInitialRefresh -> LoadingState()
                    vm.loading && vm.entries.isEmpty() -> LoadingState()
                    selectableEntries.isEmpty() && unmatchedSelections.isEmpty() -> EmptyState(s.callHistoryPickerEmpty)
                    rows.isEmpty() && unmatchedSelections.isEmpty() -> EmptyState(s.callHistoryPickerNoResults)
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 18.dp),
                    ) {
                        if (unmatchedSelections.isNotEmpty()) {
                            item(key = "previously_selected_header") {
                                SectionHeader(s.callHistoryPickerPreviouslySelected)
                            }
                            item(key = "previously_selected_note") {
                                Text(
                                    text = s.callHistoryPickerPreviouslySelectedNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp),
                                )
                            }
                            items(
                                items = unmatchedSelections,
                                key = { selection -> "previously_selected_${PhoneKey.of(selection.rawNumber)}" },
                            ) { selection ->
                                PreviousHistorySelectionRow(
                                    selection = selection,
                                    onClick = {
                                        val key = PhoneKey.of(selection.rawNumber)
                                        selectedPayload = CallHistoryRuleCodec.encode(
                                            selectedSnapshots.filterNot { PhoneKey.of(it.rawNumber) == key },
                                        )
                                    },
                                )
                            }
                        }
                        items(
                            items = rows,
                            key = { row ->
                                when (row) {
                                    is Row2.Header -> "history_header_${row.label}"
                                    is Row2.Item -> "history_entry_${row.entry.id}"
                                }
                            },
                        ) { row ->
                            when (row) {
                                is Row2.Header -> SectionHeader(row.label)
                                is Row2.Item -> {
                                    val key = PhoneKey.of(row.entry.number)
                                    ListCallItem(
                                        entry = row.entry,
                                        onOpen = {
                                            focus.clearFocus()
                                            val next = if (key in selectedSet) {
                                                selectedSnapshots.filterNot { PhoneKey.of(it.rawNumber) == key }
                                            } else {
                                                selectedSnapshots + row.entry.toHistorySelection()
                                            }
                                            selectedPayload = CallHistoryRuleCodec.encode(next)
                                        },
                                        selectionState = key in selectedSet,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTypeSheet) {
        TypeFilterSheet(
            selected = typeFilter,
            onSelect = { typeFilter = it },
            onDismiss = { showTypeSheet = false },
        )
    }
}

@Composable
private fun PreviousHistorySelectionRow(
    selection: CallBlockCallHistorySelection,
    onClick: () -> Unit,
) {
    val displayName = selection.displayName.ifBlank { formatPhone(selection.rawNumber) }
    PanelCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp),
        radius = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                label = displayName,
                photoUri = null,
                isNamed = selection.displayName.isNotBlank(),
                size = 46.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selection.displayName.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = formatPhone(selection.rawNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = appStrings().callList.selected,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CallHistoryPickerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onImeSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp)
            .height(46.dp)
            .clip(CircleShape)
            .background(FieldSurface)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = appStrings().blocker.callHistoryPickerSearchHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(Primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onImeSearch() }),
                modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged(it.isFocused) },
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).clickable { onQueryChange("") },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = appStrings().callList.delete,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private fun CallEntry.toHistorySelection() = CallBlockCallHistorySelection(
    displayName = cachedName?.trim().orEmpty(),
    rawNumber = number,
)

private tailrec fun Context.findPickerActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findPickerActivity()
    else -> null
}
