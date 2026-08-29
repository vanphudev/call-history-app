package com.antimobile.mcas.ui.messaging

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.mcas.data.contacts.Contact
import com.antimobile.mcas.data.contacts.ContactSearch
import com.antimobile.mcas.data.contacts.ContactsRepository
import com.antimobile.mcas.data.messaging.SmsRecipientParser
import com.antimobile.mcas.util.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewMessageViewModel(app: Application) : AndroidViewModel(app) {
    @SuppressLint("StaticFieldLeak")
    private val context = app.applicationContext
    private val repository = ContactsRepository(app.contentResolver)

    var query by mutableStateOf("")
        private set
    var initialBody by mutableStateOf("")
        private set
    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var showInvalid by mutableStateOf(false)
        private set

    val filteredContacts: List<Contact>
        get() = ContactSearch.filter(contacts, query).take(40)

    fun prepare(recipient: String? = null, body: String = "") {
        query = recipient.orEmpty()
        initialBody = body
        showInvalid = false
        loadContacts()
    }

    fun updateQuery(value: String) {
        query = value
        showInvalid = false
    }

    fun loadContacts() {
        if (!hasPermission(context, Manifest.permission.READ_CONTACTS)) {
            contacts = emptyList()
            return
        }
        viewModelScope.launch {
            loading = true
            contacts = withContext(Dispatchers.IO) { runCatching { repository.loadAll() }.getOrDefault(emptyList()) }
            loading = false
        }
    }

    fun typedRecipientOrNull(): String? = query.trim().takeIf(SmsRecipientParser::isValidAddress)

    fun showInvalidRecipient() {
        showInvalid = true
    }
}
