package auth.backup

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.File
import java.io.FileReader

class DriveServiceFactory {
    fun create(): Drive {
        val httpTransport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        val clientSecrets = GoogleClientSecrets.load(
            jsonFactory,
            FileReader(File("credentials.json")))

            val flow = GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
        jsonFactory,
        clientSecrets,
        listOf(DriveScopes.DRIVE_FILE)
        ).setDataStoreFactory(FileDataStoreFactory(File("tokens"))).setAccessType("offline")
            .build()

        val receiver = LocalServerReceiver.Builder()
            .setPort(8888)
            .build()

        val credential = AuthorizationCodeInstalledApp(flow, receiver).authorize("user")

        return Drive.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName("PizzaPOS")
            .build()
    }

    companion object {
        fun loadClientSecrets(): GoogleClientSecrets {
            return GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                FileReader(File("credentials.json")))
        }
    }

}