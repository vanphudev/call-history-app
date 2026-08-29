package com.antimobile.mcas.ui.blocking

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.antimobile.mcas.data.blocking.CallBlockContactSelection
import com.antimobile.mcas.data.contacts.Contact
import com.antimobile.mcas.data.contacts.ContactSearch
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.Avatar
import com.antimobile.mcas.ui.components.LoadingState
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.theme.AccentGreenBg
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.FieldSurface
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.PhoneKey
import com.antimobile.mcas.util.formatPhone

/**
 * Picker danh bạ toàn màn hình. Danh sách dài dùng LazyColumn; chỉ trả snapshot tên + số để quy tắc/backup
 * không phụ thuộc contact id hoặc lookup key của riêng thiết bị hiện tại.
 */
@Composable
internal fun CallBlockContactPickerScreen(
    vm: CallBlockRuleEditorViewModel,
    initialSelection: List<CallBlockContactSelection>,
    onBack: () -> Unit,
    onDone: (List<CallBlockContactSelection>) -> Unit,
) {
    val s = appStrings().blocker
    val focus = LocalFocusManager.current
    val initialKeys = remember(initialSelection) {
        initialSelection
            .asSequence()
            .flatMap { it.numbers.asSequence() }
            .map(PhoneKey::of)
            .filter(String::isNotEmpty)
            .toSet()
    }
    var query by rememberSaveable { mutableStateOf("") }
    var searchFocused by remember { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var selectionSeeded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { if (!vm.contactsLoaded) vm.loadContacts() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.refreshContacts() }
    LaunchedEffect(vm.contacts, vm.contactsLoaded, initialKeys) {
        if (vm.contactsLoaded && !selectionSeeded) {
            selectedIds = vm.contacts
                .filter { contact -> contact.phones.any { PhoneKey.of(it.number) in initialKeys } }
                .map { it.id }
            selectionSeeded = true
        }
    }

    BackHandler(enabled = searchFocused) { focus.clearFocus() }
    BackHandler(enabled = !searchFocused, onBack = onBack)

    val selectedSet = selectedIds.toSet()
    val filtered = remember(vm.contacts, query) { ContactSearch.filter(vm.contacts, query) }
    val currentPhoneKeys = remember(vm.contacts) {
        vm.contacts.asSequence()
            .flatMap { it.phones.asSequence() }
            .map { PhoneKey.of(it.number) }
            .filter(String::isNotEmpty)
            .toSet()
    }
    // Snapshot không còn trên thiết bị hiện tại (ví dụ restore từ máy khác) vẫn được giữ nguyên.
    val unmatchedInitial = remember(initialSelection, currentPhoneKeys, vm.contactsLoaded) {
        if (!vm.contactsLoaded) initialSelection else initialSelection.filter { selection ->
            selection.numbers.none { PhoneKey.of(it) in currentPhoneKeys }
        }
    }
    val selectedCount = selectedIds.size + unmatchedInitial.size

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            EditorTopBar(
                title = s.contactPickerTitle,
                onBack = onBack,
            )
        },
        bottomBar = {
            EditorSaveBar(
                label = s.contactPickerDone,
                enabled = selectedCount > 0,
                onSave = {
                    val currentSelections = vm.contacts
                        .asSequence()
                        .filter { it.id in selectedSet }
                        .mapNotNull(Contact::toBlockSelection)
                        .toList()
                    val selections = (unmatchedInitial + currentSelections).distinctBy { selection ->
                        selection.numbers.map(PhoneKey::of).filter(String::isNotEmpty).sorted().joinToString(",")
                    }
                    if (selections.isNotEmpty()) onDone(selections)
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (vm.hasContactsPermission) {
                ContactPickerSearchBar(
                    query = query,
                    onQueryChange = { query = it },
                    onFocusChanged = { searchFocused = it },
                    onImeSearch = { focus.clearFocus() },
                )
            }
            if (selectedCount > 0) {
                Text(
                    text = s.contactPickerSelectedCount(selectedCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 6.dp),
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } },
            ) {
                when {
                    !vm.hasContactsPermission -> ContactPickerPermissionGate(onGranted = vm::refreshContacts)
                    vm.contactsLoading && !vm.contactsLoaded -> LoadingState(text = appStrings().contacts.loading)
                    vm.contactsLoadFailed && vm.contacts.isEmpty() -> ContactPickerMessage(appStrings().contacts.loadError)
                    vm.contacts.isEmpty() -> ContactPickerMessage(s.contactPickerEmpty)
                    filtered.isEmpty() -> ContactPickerMessage(s.contactPickerNoResults)
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 18.dp),
                    ) {
                        items(filtered, key = { it.id }) { contact ->
                            ContactPickerRow(
                                contact = contact,
                                selected = contact.id in selectedSet,
                                onClick = {
                                    selectedIds = if (contact.id in selectedSet) {
                                        selectedIds.filterNot { it == contact.id }
                                    } else {
                                        selectedIds + contact.id
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactPickerSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onImeSearch: () -> Unit,
) {
    val s = appStrings().blocker
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
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
                    s.contactPickerSearchHint,
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
                Icon(Icons.Rounded.Close, contentDescription = appStrings().callList.delete, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ContactPickerRow(contact: Contact, selected: Boolean, onClick: () -> Unit) {
    PanelCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
        radius = 16.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                label = contact.displayNameOrNumber,
                photoUri = contact.photoUri,
                isNamed = contact.displayName.isNotBlank(),
                size = 46.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayNameOrNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                contact.primaryPhone?.let { phone ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = formatPhone(phone.number),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (contact.phones.size > 1) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        appStrings().contacts.morePhones(contact.phones.size - 1),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if (selected) Primary else CardFill),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Rounded.Check, contentDescription = appStrings().callList.selected, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ContactPickerPermissionGate(onGranted: () -> Unit) {
    val context = LocalContext.current
    val s = appStrings().blocker
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            permanentlyDenied = false
            onGranted()
        } else {
            val activity = context.findActivity()
            permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)
        }
    }
    val request: () -> Unit = {
        if (permanentlyDenied) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        } else {
            launcher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(AccentGreenBg), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = Primary, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(
            s.contactPickerPermissionTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(s.contactPickerPermissionBody, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier.clip(CircleShape).background(Primary).clickable(onClick = request).padding(horizontal = 28.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (permanentlyDenied) appStrings().common.openSettings else s.contactPickerPermissionAction,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ContactPickerMessage(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(Icons.Rounded.Contacts, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

private fun Contact.toBlockSelection(): CallBlockContactSelection? {
    val uniqueNumbers = phones
        .map { it.number.trim() }
        .filter(String::isNotEmpty)
        .distinctBy { PhoneKey.of(it).ifEmpty { it } }
    if (uniqueNumbers.isEmpty()) return null
    return CallBlockContactSelection(displayName = displayNameOrNumber, numbers = uniqueNumbers)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
