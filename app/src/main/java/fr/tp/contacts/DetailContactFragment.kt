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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DetailContactFragment : Fragment() {
    private val model: ContactsViewModel by activityViewModels()
    private var contact: Contact? = null
    private var photoPath: String? = null
    private var cameraUri: Uri? = null
    private var editing = false
    private var isNew = false

    private val gallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) persistPhoto(uri)
    }
    private val camera = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let(::persistPhoto)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?) =
        inflater.inflate(R.layout.fragment_detail, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        val id = requireArguments().getLong(ARG_ID, 0L)
        isNew = id == 0L
        editing = state?.getBoolean(STATE_EDITING) ?: isNew
        photoPath = state?.getString(STATE_PHOTO)
        cameraUri = state?.getString(STATE_CAMERA_URI)?.let(Uri::parse)
        configureButtons(view)
        showPhoto(view, photoPath)

        if (isNew) {
            contact = Contact(nom = "", prenom = "", telephone = "")
            view.findViewById<TextView>(R.id.title).text = "Nouveau contact"
            if (state == null) setEditable(view, true)
        } else {
            viewLifecycleOwner.lifecycleScope.launch {
                val loaded = model.getContact(id)
                if (!isAdded) return@launch
                if (loaded == null) {
                    Toast.makeText(requireContext(), "Contact introuvable.", Toast.LENGTH_LONG).show()
                    parentFragmentManager.popBackStack()
                    return@launch
                }
                contact = loaded
                if (state == null) {
                    populate(view, loaded)
                    photoPath = loaded.photoUri
                    showPhoto(view, photoPath)
                    setEditable(view, editing)
                }
            }
        }

        styleFields(view)
        view.findViewById<Button>(R.id.edit).setOnClickListener { setEditable(view, true); editing = true }
        view.findViewById<Button>(R.id.cancel).setOnClickListener {
            if (isNew) {
                parentFragmentManager.popBackStack()
            } else {
                contact?.let { populate(view, it); photoPath = it.photoUri; showPhoto(view, photoPath) }
                editing = false
                setEditable(view, false)
            }
        }
        view.findViewById<Button>(R.id.save).setOnClickListener { saveFromForm(view) }
        view.findViewById<Button>(R.id.delete).setOnClickListener { confirmDelete() }
        view.findViewById<Button>(R.id.gallery).setOnClickListener { gallery.launch("image/*") }
        view.findViewById<Button>(R.id.camera).setOnClickListener { openCamera() }
        view.findViewById<Button>(R.id.call).setOnClickListener {
            contact?.let { dial(it.telephone) }
        }
        view.findViewById<Button>(R.id.mail).setOnClickListener {
            val email = contact?.email.orEmpty()
            if (email.isBlank()) Toast.makeText(requireContext(), "Ce contact n'a pas d'adresse e-mail.", Toast.LENGTH_SHORT).show()
            else launchExternal(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(email))))
        }
        view.findViewById<Button>(R.id.map).setOnClickListener {
            contact?.let {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, CarteContactFragment.newInstance(it.id))
                    .addToBackStack("map").commit()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.mutations.collect { mutation ->
                    when (mutation.type) {
                        MutationType.SAVED -> {
                            if (isNew || mutation.id == contact?.id) parentFragmentManager.popBackStack()
                        }
                        MutationType.DELETED -> if (mutation.id == contact?.id) parentFragmentManager.popBackStack()
                        MutationType.ERROR -> Toast.makeText(requireContext(), mutation.message ?: "Erreur", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun configureButtons(view: View) {
        view.findViewById<Button>(R.id.delete).visibility = if (isNew) View.GONE else View.VISIBLE
        view.findViewById<Button>(R.id.call).visibility = if (isNew) View.GONE else View.VISIBLE
        view.findViewById<Button>(R.id.mail).visibility = if (isNew) View.GONE else View.VISIBLE
        view.findViewById<Button>(R.id.map).visibility = if (isNew) View.GONE else View.VISIBLE
        setEditable(view, editing)
    }

    private fun setEditable(view: View, enabled: Boolean) {
        listOf(R.id.first, R.id.last, R.id.phone, R.id.email, R.id.latitude, R.id.longitude)
            .forEach { view.findViewById<EditText>(it).isEnabled = enabled }
        view.findViewById<Button>(R.id.edit).visibility = if (enabled || isNew) View.GONE else View.VISIBLE
        view.findViewById<Button>(R.id.save).visibility = if (enabled) View.VISIBLE else View.GONE
        view.findViewById<Button>(R.id.cancel).visibility = if (enabled) View.VISIBLE else View.GONE
        view.findViewById<Button>(R.id.gallery).visibility = if (enabled) View.VISIBLE else View.GONE
        view.findViewById<Button>(R.id.camera).visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun populate(view: View, value: Contact) {
        view.findViewById<EditText>(R.id.first).setText(value.prenom)
        view.findViewById<EditText>(R.id.last).setText(value.nom)
        view.findViewById<EditText>(R.id.phone).setText(value.telephone)
        view.findViewById<EditText>(R.id.email).setText(value.email.orEmpty())
        view.findViewById<EditText>(R.id.latitude).setText(value.latitude?.toString().orEmpty())
        view.findViewById<EditText>(R.id.longitude).setText(value.longitude?.toString().orEmpty())
    }

    private fun styleFields(view: View) {
        val prefs = PreferencesManager.read(requireContext())
        listOf(R.id.first, R.id.last, R.id.phone).forEach { view.findViewById<EditText>(it).textSize = prefs.textSizeSp }
        view.findViewById<EditText>(R.id.first).setTextColor(prefs.textColor)
        view.findViewById<EditText>(R.id.last).setTextColor(prefs.textColor)
    }

    private fun saveFromForm(view: View) {
        val first = view.findViewById<EditText>(R.id.first).text.toString().trim()
        val last = view.findViewById<EditText>(R.id.last).text.toString().trim()
        val phone = view.findViewById<EditText>(R.id.phone).text.toString().trim()
        val email = view.findViewById<EditText>(R.id.email).text.toString().trim()
        val latText = view.findViewById<EditText>(R.id.latitude).text.toString().trim()
        val lonText = view.findViewById<EditText>(R.id.longitude).text.toString().trim()
        val latitude = latText.toDoubleOrNull()
        val longitude = lonText.toDoubleOrNull()
        when {
            first.isBlank() || last.isBlank() || phone.isBlank() -> showError("Prénom, nom et téléphone sont obligatoires.")
            email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> showError("Adresse e-mail invalide.")
            latText.isBlank() != lonText.isBlank() -> showError("Renseignez les deux coordonnées GPS ou aucune.")
            latText.isNotBlank() && (latitude == null || longitude == null || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) ->
                showError("Coordonnées GPS invalides. Latitude : -90 à 90, longitude : -180 à 180.")
            else -> {
                val old = contact ?: Contact(nom = "", prenom = "", telephone = "")
                contact = old.copy(
                    prenom = first, nom = last, telephone = phone,
                    email = email.ifBlank { null }, photoUri = photoPath,
                    latitude = latitude, longitude = longitude
                )
                model.save(contact!!)
            }
        }
    }

    private fun confirmDelete() {
        val current = contact ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Supprimer le contact ?")
            .setMessage("Cette suppression est définitive.")
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Supprimer") { _, _ -> model.delete(current.id) }
            .show()
    }

    private fun openCamera() {
        try {
            val dir = File(requireContext().cacheDir, "camera").apply { mkdirs() }
            val file = File.createTempFile("contact_", ".jpg", dir)
            cameraUri = FileProvider.getUriForFile(requireContext(), requireContext().packageName + ".fileprovider", file)
            camera.launch(cameraUri!!)
        } catch (_: Exception) {
            showError("Caméra indisponible.")
        }
    }

    private fun persistPhoto(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val folder = File(requireContext().filesDir, "contact_photos").apply { mkdirs() }
                    val destination = File(folder, UUID.randomUUID().toString() + ".jpg")
                    requireContext().contentResolver.openInputStream(uri).use { input ->
                        requireNotNull(input) { "Image illisible" }.use { source ->
                            destination.outputStream().use { output -> source.copyTo(output) }
                        }
                    }
                    destination
                }
                photoPath = file.absolutePath
                view?.let { showPhoto(it, photoPath) }
            } catch (_: Exception) {
                showError("Impossible de conserver cette photo.")
            }
        }
    }

    private fun showPhoto(view: View, path: String?) {
        val image = view.findViewById<ImageView>(R.id.photo)
        image.setImageResource(android.R.drawable.ic_menu_myplaces)
        path?.let { runCatching { image.setImageURI(Uri.fromFile(File(it))) } }
    }

    private fun dial(phone: String) {
        if (phone.isBlank()) { showError("Ce contact n'a pas de numéro de téléphone."); return }
        launchExternal(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))))
    }

    private fun launchExternal(intent: Intent) {
        try { startActivity(intent) } catch (_: Exception) { showError("Aucune application disponible pour cette action.") }
    }

    private fun showError(message: String) {
        if (isAdded) Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_EDITING, editing)
        outState.putString(STATE_PHOTO, photoPath)
        outState.putString(STATE_CAMERA_URI, cameraUri?.toString())
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val ARG_ID = "contact_id"
        private const val STATE_EDITING = "editing"
        private const val STATE_PHOTO = "photo_path"
        private const val STATE_CAMERA_URI = "camera_uri"
        fun newInstance(id: Long = 0L) = DetailContactFragment().apply {
            arguments = Bundle().apply { putLong(ARG_ID, id) }
        }
    }
}
