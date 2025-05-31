package auth.backup

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GoogleDriveBackupManager {
    private val driveService by lazy { DriveServiceFactory().create() }

    private val httpTransport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val dataStoreFactory = FileDataStoreFactory(File("tokens"))

    fun isConfigured(): Boolean {
        return File("credentials.json").exists() &&
                File("tokens/StoredCredential").exists()
    }

    fun completeAuthFlow(authCode: String) {
        val clientSecrets = DriveServiceFactory.loadClientSecrets()

        val response = GoogleAuthorizationCodeTokenRequest(
            httpTransport,
            jsonFactory,
            clientSecrets.details.clientId,
            clientSecrets.details.clientSecret,
            authCode,
            "http://localhost:8888"
        ).execute()

        // Create StoredCredential from response
        val credential = StoredCredential()
        credential.accessToken = response.accessToken
        credential.refreshToken = response.refreshToken
        credential.expirationTimeMilliseconds = response.expiresInSeconds?.times(1000L)

        // Get and store credentials with proper typing
        val credentialDataStore = dataStoreFactory.getDataStore<StoredCredential>("StoredCredential")
        credentialDataStore.set("user", credential)
    }

    fun backupDatabase(dbPath: String) {
        val dbFile = File(dbPath)
        if (!dbFile.exists()) throw Exception("Database file not found")

        // Check for existing backup file
        val existingFileId = findExistingBackupFile()

        val mediaContent = FileContent("application/x-sqlite3", dbFile)

        if (existingFileId != null) {
            // Update existing file
            driveService.files().update(existingFileId, null, mediaContent)
                .setFields("id")
                .execute()
        } else {
            // Create new file
            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = "pizza_pos.db"
                parents = listOf("root")
            }
            driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()
        }
    }

    private fun findExistingBackupFile(): String? {
        val query = "name = 'pizza_pos.db' and 'root' in parents and trashed = false"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("drive")
            .execute()

        return result.files.firstOrNull()?.id
    }
}