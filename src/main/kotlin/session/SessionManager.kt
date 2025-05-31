package session

import java.io.File
import java.util.Properties

// SessionManager.kt
object SessionManager {
    private const val SESSION_FILE = "session.properties"

    private fun sessionFile() = File("config", SESSION_FILE)

    fun isLoggedIn(): Boolean {
        val file = sessionFile()
        if (!file.exists()) return false

        val props = Properties().apply { load(file.inputStream()) }
        val loginTime = props.getProperty("loginTime")?.toLongOrNull() ?: return false

        val currentTime = System.currentTimeMillis()

        // Get today's 2 AM in millis
        val now = java.time.LocalDateTime.now()
        val twoAM = now.toLocalDate().atTime(2, 0)
        val twoAMMillis = twoAM.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Invalidate session if it's past 2 AM today and login time was before that
        return currentTime < twoAMMillis || loginTime >= twoAMMillis
    }


    fun login(username: String, password: String): Boolean {
        val storedUser = CredentialsManager.getUsername()
        val storedPass = CredentialsManager.getPassword()

        return if (username == storedUser && password == storedPass) {
            val file = sessionFile()
            file.parentFile.mkdirs()
            Properties().apply {
                setProperty("loginTime", System.currentTimeMillis().toString())
                store(file.outputStream(), null)
            }
            true
        } else {
            false
        }
    }

    fun logout() {
        sessionFile().delete()
    }
}
