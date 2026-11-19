package fr.tp.contacts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.io.File

class ListeContactsFragment : Fragment() {
    private val model: ContactsViewModel by activityViewModels()
    private lateinit var adapter: ContactAdapter
    private var searchText = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?) =
        inflater.inflate(R.layout.fragment_list, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        val recycler = view.findViewById<RecyclerView>(R.id.recycler)
        val empty = view.findViewById<TextView>(R.id.empty)
        val search = view.findViewById<EditText>(R.id.search)
        adapter = ContactAdapter(
            onOpen = { id -> parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DetailContactFragment.newInstance(id))
                .addToBackStack("detail").commit() },
            onCall = ::dial
        )
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter
        model.contacts.observe(viewLifecycleOwner) { contacts ->
            adapter.submit(contacts)
            empty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
        }
        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchText = s?.toString().orEmpty()
                model.loadContacts(searchText)
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.add_contact)
            .setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, DetailContactFragment.newInstance())
                    .addToBackStack("create").commit()
            }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.mutations.collect { mutation ->
                    if (mutation.type == MutationType.ERROR) {
                        Toast.makeText(requireContext(), mutation.message ?: "Erreur", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        model.loadContacts(searchText)
        adapter.setPreferences(PreferencesManager.read(requireContext()))
    }

    private fun dial(phone: String) {
        if (phone.isBlank()) {
            Toast.makeText(requireContext(), "Ce contact n'a pas de numéro de téléphone.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))))
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Aucune application téléphone disponible.", Toast.LENGTH_LONG).show()
        }
    }

    private class ContactAdapter(
        private val onOpen: (Long) -> Unit,
        private val onCall: (String) -> Unit
    ) : RecyclerView.Adapter<ContactAdapter.Holder>() {
        private var items = emptyList<Contact>()
        private var prefs = ContactDisplayPreferences(16f, android.graphics.Color.BLACK, false)

        fun submit(value: List<Contact>) { items = value; notifyDataSetChanged() }
        fun setPreferences(value: ContactDisplayPreferences) { prefs = value; notifyDataSetChanged() }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.row_contact, parent, false), onOpen, onCall)
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(items[position], prefs)

        class Holder(view: View, private val onOpen: (Long) -> Unit, private val onCall: (String) -> Unit) :
            RecyclerView.ViewHolder(view) {
            fun bind(contact: Contact, prefs: ContactDisplayPreferences) {
                itemView.findViewById<TextView>(R.id.first_name).apply {
                    text = contact.prenom; textSize = prefs.textSizeSp; setTextColor(prefs.textColor)
                }
                itemView.findViewById<TextView>(R.id.last_name).apply {
                    text = contact.nom; textSize = prefs.textSizeSp; setTextColor(prefs.textColor)
                }
                itemView.findViewById<TextView>(R.id.phone).apply {
                    text = contact.telephone; textSize = prefs.textSizeSp
                }
                val photo = itemView.findViewById<ImageView>(R.id.photo)
                photo.setImageResource(android.R.drawable.ic_menu_myplaces)
                contact.photoUri?.let { path -> runCatching { photo.setImageURI(Uri.fromFile(File(path))) } }
                itemView.findViewById<Button>(R.id.row_call).apply {
                    visibility = if (prefs.callFromList) View.VISIBLE else View.GONE
                    setOnClickListener { onCall(contact.telephone) }
                }
                itemView.setOnClickListener { onOpen(contact.id) }
            }
        }
    }
}
