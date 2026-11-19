# TP Android Gestion des contacts V4

Projet Android autonome en Kotlin et XML. Il reprend les fonctions de contacts, les fragments et Google Maps de la V3, puis ajoute SQLite, les opérations CRUD et les préférences utilisateur. Il s’agit d’un nouveau projet indépendant : son identifiant d’application est fr.tp.contacts.v4.

## Ouvrir et configurer

1. Ouvrir le dossier ContactsV4 dans Android Studio et attendre la synchronisation Gradle.
2. Utiliser JDK 17 et installer Android SDK 35 dans le SDK Manager.
3. Copier local.properties.example sous le nom local.properties, puis renseigner sdk.dir avec le chemin du SDK Android local.
4. Ajouter MAPS_API_KEY=... à local.properties pour activer les cartes. Activer Maps SDK for Android dans Google Cloud et restreindre la clé au package fr.tp.contacts.v4 et à l’empreinte SHA-1 de l’application.
5. Lancer l’application sur un appareil ou un émulateur. Le projet se compile aussi sans clé, mais la carte ne pourra pas charger les tuiles.
6. Ce dossier constitue un projet et un dépôt Git local indépendants de ContactsV3. Pour publier cette version, crée un dépôt GitHub vide puis définis son URL comme remote origin avant le push.

Ne jamais publier local.properties ni une clé Maps. .gitignore exclut ces éléments et les fichiers de compilation.

## Architecture

- MainActivity est l’unique activité, avec FragmentContainerView, MaterialToolbar et navigation de retour.
- ListeContactsFragment affiche les contacts avec RecyclerView, recherche instantanée et bouton flottant de création. Le bouton Appeler par ligne respecte la préférence.
- DetailContactFragment gère création, consultation, modification, validation, suppression confirmée, téléphone, e-mail, galerie, caméra et carte.
- CarteContactFragment lit les coordonnées persistées et place un marqueur Google Maps.
- ContactsViewModel partage les données aux fragments et exécute les opérations de base hors du thread principal.
- data/ContactContract centralise le schéma. ContactDbHelper crée contacts.db et insère cinq exemples au premier lancement. ContactDao regroupe le CRUD, la recherche et la lecture du Cursor.
- PreferencesManager centralise la lecture des préférences. ParametresFragment utilise PreferenceFragmentCompat; la liste relit les réglages en revenant à l’écran.
- Les photos choisies depuis la galerie ou la caméra sont copiées sous filesDir/contact_photos; la base conserve ce chemin privé durable.

Le filtrage est effectué en SQLite avec LIKE ? et trois arguments de sélection. Les jokers sont donc des paramètres, et le texte saisi n’est jamais concaténé dans la requête. Le tri applique COLLATE NOCASE sur le nom puis le prénom.

## Vérifications manuelles

- Au premier lancement, vérifier les cinq contacts; fermer et relancer : aucun doublon ne doit apparaître.
- Ajouter un contact; vérifier qu’il est trié dans la liste et toujours présent après relance.
- Modifier téléphone, photo et coordonnées, enregistrer, relancer puis vérifier les valeurs.
- Supprimer un contact, annuler une première confirmation, puis confirmer une seconde suppression.
- Essayer O'Neil comme nom et un filtre avec apostrophe; vérifier qu’il n’y a pas d’erreur SQL.
- Enregistrer sans prénom, nom ou téléphone, avec un e-mail invalide, une seule coordonnée, une valeur non numérique ou une coordonnée hors limites : l’enregistrement doit être refusé.
- Annuler les sélecteurs Galerie et Caméra; vérifier que le contact n’est pas modifié. Vérifier aussi les actions Appeler et E-mail.
- Paramètres : tester 14, 16 et 20 sp, chacune des couleurs, puis l’appel depuis la liste. Vérifier que le réglage est appliqué immédiatement au retour et conservé après relance.
- Appeler depuis une ligne : le composeur s’ouvre avec le bon numéro et la fiche ne s’ouvre pas.
- Tester une carte avec et sans coordonnées GPS.
- Faire pivoter l’écran sur la liste, dans une fiche en cours de saisie et dans Paramètres.
- Dans Android Studio, ouvrir App Inspection > Database Inspector, consulter contacts.db et vérifier les colonnes de contacts.

## Questions de compréhension

1. onCreate est appelée à la création initiale du fichier de base; onUpgrade est appelée quand la version demandée dépasse celle stockée. Modifier seulement onCreate ne change pas une base déjà créée : il faut désinstaller l’application ou augmenter la version et migrer le schéma.
2. Les ? séparent la commande des valeurs, évitent l’injection SQL et traitent correctement apostrophes et caractères spéciaux. La saisie O'Neil casse une requête construite naïvement par concaténation.
3. insert renvoie l’identifiant de la nouvelle ligne ou -1 en cas d’échec; update et delete renvoient le nombre de lignes affectées. L’interface confirme l’opération si le résultat indique une ligne créée ou modifiée, et affiche une erreur si aucune ligne n’a été touchée.
4. Une requête disque sur le thread principal peut bloquer l’interface et déclencher une ANR. Les appels du ViewModel passent par withContext(Dispatchers.IO) et sont orchestrés par viewModelScope.
5. Un helper partagé évite plusieurs instances concurrentes de SQLiteOpenHelper et des connexions incohérentes. applicationContext survit aux activités et évite de garder une activité en mémoire.
6. SQLite convient aux contacts structurés et aux opérations de recherche, tri et mise à jour. SharedPreferences convient aux petits réglages indépendants, représentés par quelques paires clé-valeur.
7. Le fragment de liste relit PreferencesManager dans onResume, puis actualise l’adapter; aucun redémarrage n’est nécessaire.
8. ACTION_DIAL ouvre le composeur avec le numéro préparé et ne demande pas la permission d’appel. ACTION_CALL lance directement l’appel et exige une permission dangereuse.
9. La désinstallation et l’effacement des données suppriment les fichiers privés de l’application, donc la base SQLite, les photos internes et les SharedPreferences. Une nouvelle installation recrée la base et réinsère les contacts de démonstration.

## Limites

Les contacts sont stockés localement et ne sont pas synchronisés. La clé Maps est requise pour afficher les tuiles Google; sans clé correctement configurée, le reste de l’application fonctionne. Les captures de l’énoncé doivent être prises dans l’émulateur réel après exécution.

Sur Windows, une compilation en ligne de commande se lance depuis la racine du projet avec gradlew.bat assembleDebug. Sur macOS ou Linux, utiliser ./gradlew assembleDebug.
