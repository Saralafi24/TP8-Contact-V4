package fr.tp.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import fr.tp.contacts.data.ContactDao
import fr.tp.contacts.data.ContactDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MutationType { SAVED, DELETED, ERROR }
data class ContactMutation(val type: MutationType, val id: Long = 0L, val message: String? = null)

class ContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = ContactDao(ContactDbHelper.getInstance(application))
    private val mutableContacts = MutableLiveData<List<Contact>>(emptyList())
    val contacts: LiveData<List<Contact>> = mutableContacts
    private val events = Channel<ContactMutation>(Channel.BUFFERED)
    val mutations = events.receiveAsFlow()
    private var loadJob: kotlinx.coroutines.Job? = null

    fun loadContacts(filter: String = "") {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { if (filter.isBlank()) dao.getAll() else dao.search(filter) } }
                .onSuccess { mutableContacts.value = it }
                .onFailure { events.send(ContactMutation(MutationType.ERROR, message = "Impossible de lire la base de contacts.")) }
        }
    }

    suspend fun getContact(id: Long): Contact? = withContext(Dispatchers.IO) { dao.getById(id) }

    fun save(contact: Contact) = viewModelScope.launch {
        try {
            val savedId = withContext(Dispatchers.IO) {
                if (contact.id == 0L) dao.insert(contact).takeIf { it > 0L }
                    ?: error("L'insertion n'a pas abouti.")
                else contact.id.takeIf { dao.update(contact) > 0 }
                    ?: error("Le contact n'existe plus dans la base.")
            }
            mutableContacts.value = withContext(Dispatchers.IO) { dao.getAll() }
            events.send(ContactMutation(MutationType.SAVED, savedId))
        } catch (exception: Exception) {
            events.send(ContactMutation(MutationType.ERROR, message = exception.message ?: "Enregistrement impossible."))
        }
    }

    fun delete(id: Long) = viewModelScope.launch {
        try {
            val count = withContext(Dispatchers.IO) { dao.delete(id) }
            if (count == 0) error("Le contact n'existe plus dans la base.")
            mutableContacts.value = withContext(Dispatchers.IO) { dao.getAll() }
            events.send(ContactMutation(MutationType.DELETED, id))
        } catch (exception: Exception) {
            events.send(ContactMutation(MutationType.ERROR, message = exception.message ?: "Suppression impossible."))
        }
    }
}
