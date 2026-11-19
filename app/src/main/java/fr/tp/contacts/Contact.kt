package fr.tp.contacts

data class Contact(
    val id: Long = 0L,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String? = null,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val displayName: String get() = "$prenom $nom".trim()
}
