package fr.tp.contacts.data

import android.content.ContentValues
import android.database.Cursor
import fr.tp.contacts.Contact

class ContactDao(private val helper: ContactDbHelper) {
    fun insert(contact: Contact): Long = helper.writableDatabase.insert(ContactContract.TABLE, null, contact.toValues())

    fun update(contact: Contact): Int = helper.writableDatabase.update(
        ContactContract.TABLE, contact.toValues(), "${ContactContract.ID} = ?", arrayOf(contact.id.toString())
    )

    fun delete(id: Long): Int = helper.writableDatabase.delete(
        ContactContract.TABLE, "${ContactContract.ID} = ?", arrayOf(id.toString())
    )

    fun getAll(): List<Contact> = query(null, null)

    fun getById(id: Long): Contact? = query("${ContactContract.ID} = ?", arrayOf(id.toString())).firstOrNull()

    fun search(texte: String): List<Contact> {
        val pattern = "%${texte.trim()}%"
        val selection = "${ContactContract.NOM} LIKE ? OR ${ContactContract.PRENOM} LIKE ? OR ${ContactContract.TELEPHONE} LIKE ?"
        return query(selection, arrayOf(pattern, pattern, pattern))
    }

    private fun query(selection: String?, args: Array<String>?): List<Contact> {
        val db = helper.readableDatabase
        val order = "${ContactContract.NOM} COLLATE NOCASE ASC, ${ContactContract.PRENOM} COLLATE NOCASE ASC"
        return db.query(ContactContract.TABLE, null, selection, args, null, null, order).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toContact()) }
        }
    }

    private fun Cursor.toContact(): Contact {
        fun text(column: String) = getColumnIndexOrThrow(column)
        val latitudeIndex = text(ContactContract.LATITUDE)
        val longitudeIndex = text(ContactContract.LONGITUDE)
        return Contact(
            id = getLong(text(ContactContract.ID)),
            nom = getString(text(ContactContract.NOM)),
            prenom = getString(text(ContactContract.PRENOM)),
            telephone = getString(text(ContactContract.TELEPHONE)),
            email = getString(text(ContactContract.EMAIL)),
            photoUri = getString(text(ContactContract.PHOTO_URI)),
            latitude = if (isNull(latitudeIndex)) null else getDouble(latitudeIndex),
            longitude = if (isNull(longitudeIndex)) null else getDouble(longitudeIndex)
        )
    }

    private fun Contact.toValues() = ContentValues().apply {
        put(ContactContract.NOM, nom)
        put(ContactContract.PRENOM, prenom)
        put(ContactContract.TELEPHONE, telephone)
        put(ContactContract.EMAIL, email)
        put(ContactContract.PHOTO_URI, photoUri)
        if (latitude == null) putNull(ContactContract.LATITUDE) else put(ContactContract.LATITUDE, latitude)
        if (longitude == null) putNull(ContactContract.LONGITUDE) else put(ContactContract.LONGITUDE, longitude)
    }
}
