package session

import java.io.File
import java.util.Properties

// CredentialsManager.kt
object CredentialsManager {
    private const val CRED_FILE = "config/credentials.properties"
    private val file = File(CRED_FILE)

    private var storedPassword = "admin123"

    fun getUsername(): String = getProps().getProperty("username", "admin")
    fun getPassword(): String = getProps().getProperty("password", "admin123")

    fun changeCredentials(newUsername: String, newPassword: String) {
        file.parentFile.mkdirs()
        Properties().apply {
            setProperty("username", newUsername)
            setProperty("password", newPassword)
            store(file.outputStream(), null)
        }

        storedPassword = newPassword
        println("Credentials updated: $newUsername / $newPassword")
    }

    private fun getProps(): Properties {
        return Properties().apply {
            if (file.exists()) load(file.inputStream())
        }
    }

    fun verifyCurrentPassword(input: String): Boolean {
        return input == storedPassword
    }


}
