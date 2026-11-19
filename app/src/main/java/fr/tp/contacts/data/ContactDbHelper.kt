package fr.tp.contacts.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import fr.tp.contacts.Contact

class ContactDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE ${ContactContract.TABLE} (" +
                "${ContactContract.ID} INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "${ContactContract.NOM} TEXT NOT NULL, " +
                "${ContactContract.PRENOM} TEXT NOT NULL, " +
                "${ContactContract.TELEPHONE} TEXT NOT NULL, " +
                "${ContactContract.EMAIL} TEXT, " +
                "${ContactContract.PHOTO_URI} TEXT, " +
                "${ContactContract.LATITUDE} REAL, " +
                "${ContactContract.LONGITUDE} REAL)"
        )
        demoContacts.forEach { contact ->
            db.insert(ContactContract.TABLE, null, contact.toContentValues())
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // V1 est le premier schéma. Toute évolution future doit migrer les lignes sans les supprimer.
    }

    companion object {
        private const val DATABASE_NAME = "contacts.db"
        private const val DATABASE_VERSION = 1
        private val demoContacts = listOf(
            Contact(nom = "Lovelace", prenom = "Ada", telephone = "+33142220001", email = "ada@example.com", latitude = 48.8566, longitude = 2.3522),
            Contact(nom = "Turing", prenom = "Alan", telephone = "+442079460001", email = "alan@example.com", latitude = 51.5072, longitude = -0.1276),
            Contact(nom = "Hopper", prenom = "Grace", telephone = "+12125550101", email = "grace@example.com"),
            Contact(nom = "Johnson", prenom = "Katherine", telephone = "+12025550102", email = "katherine@example.com", latitude = 38.9072, longitude = -77.0369),
            Contact(nom = "O'Neil", prenom = "Mae", telephone = "+33142220005", email = "mae@example.com")
        )

        @Volatile private var instance: ContactDbHelper? = null

        fun getInstance(context: Context): ContactDbHelper =
            instance ?: synchronized(this) {
                instance ?: ContactDbHelper(context.applicationContext).also { instance = it }
            }

        private fun Contact.toContentValues() = ContentValues().apply {
            put(ContactContract.NOM, nom)
            put(ContactContract.PRENOM, prenom)
            put(ContactContract.TELEPHONE, telephone)
            put(ContactContract.EMAIL, email)
            put(ContactContract.PHOTO_URI, photoUri)
            if (latitude == null) putNull(ContactContract.LATITUDE) else put(ContactContract.LATITUDE, latitude)
            if (longitude == null) putNull(ContactContract.LONGITUDE) else put(ContactContract.LONGITUDE, longitude)
        }
    }
}
