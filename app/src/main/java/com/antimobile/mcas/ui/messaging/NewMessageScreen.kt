package com.antimobile.mcas.ui.messaging

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.antimobile.mcas.data.contacts.Contact
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.Avatar
import com.antimobile.mcas.ui.components.ContactsPermissionBanner
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.DividerColor
import com.antimobile.mcas.ui.theme.FieldSurface
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary

@Composable
fun NewMessageScreen(
    vm: NewMessageViewModel,
    onBack: () -> Unit,
    onOpenRecipient: (address: String, body: String) -> Unit,
) {
    val context = LocalContext.current
    val s = appStrings().messaging
    val requester = remember { FocusRequester() }
    var contactsBannerDismissed by remember { mutableStateOf(false) }
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) vm.loadContacts()
    }
    val continueTyped = {
        val recipient = vm.typedRecipientOrNull()
        if (recipient == null) vm.showInvalidRecipient() else onOpenRecipient(recipient, vm.initialBody)
    }
    LaunchedEffect(Unit) { requester.requestFocus() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.loadContacts() }

    Column(
        modifier = Modifier.fillMaxSize().background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding().imePadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(46.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, appStrings().common.back, tint = TextSecondary)
            }
            Text(s.newMessage, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${s.recipientTitle}:", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(FieldSurface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (vm.query.isEmpty()) Text(s.recipientHint, color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                BasicTextField(
                    value = vm.query,
                    onValueChange = vm::updateQuery,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Primary),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { continueTyped() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(requester),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(Primary).clickable { continueTyped() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, s.recipientContinue, tint = Color.White) }
        }
        if (vm.showInvalid) {
            Text(s.invalidRecipient, color = AccentRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 24.dp))
        }
        val hasContacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        if (!hasContacts && !contactsBannerDismissed) {
            ContactsPermissionBanner(
                onAllow = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) },
                onDismiss = { contactsBannerDismissed = true },
            )
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(vm.filteredContacts, key = { it.id }) { contact ->
                ContactRecipientRows(contact) { number -> onOpenRecipient(number, vm.initialBody) }
            }
        }
    }
}

@Composable
private fun ContactRecipientRows(contact: Contact, onSelect: (String) -> Unit) {
    contact.phones.forEachIndexed { index, phone ->
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onSelect(phone.number) }
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (index == 0) Avatar(contact.displayNameOrNumber, contact.photoUri, contact.displayName.isNotBlank(), size = 46.dp)
            else Spacer(Modifier.width(46.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.displayNameOrNumber, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(phone.number, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Icon(Icons.Rounded.Person, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
        Box(Modifier.fillMaxWidth().padding(start = 76.dp).height(1.dp).background(DividerColor))
    }
}
