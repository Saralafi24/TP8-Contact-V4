package fr.tp.contacts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class CarteContactFragment : Fragment() {
    private val model: ContactsViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?) =
        inflater.inflate(R.layout.fragment_map, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        val id = requireArguments().getLong(ARG_ID)
        viewLifecycleOwner.lifecycleScope.launch {
            val contact = model.getContact(id)
            if (!isAdded) return@launch
            if (contact == null || contact.latitude == null || contact.longitude == null ||
                contact.latitude !in -90.0..90.0 || contact.longitude !in -180.0..180.0
            ) {
                view.findViewById<TextView>(R.id.map_message).apply {
                    text = "Aucune coordonnée GPS valide pour ce contact."
                    visibility = View.VISIBLE
                }
                return@launch
            }
            (childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment)?.getMapAsync { map ->
                val point = LatLng(contact.latitude, contact.longitude)
                map.addMarker(MarkerOptions().position(point).title(contact.displayName))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 14f))
            }
        }
    }

    companion object {
        private const val ARG_ID = "contact_id"
        fun newInstance(id: Long) = CarteContactFragment().apply {
            arguments = Bundle().apply { putLong(ARG_ID, id) }
        }
    }
}
