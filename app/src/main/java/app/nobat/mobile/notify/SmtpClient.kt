package app.nobat.mobile.notify

import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Small SMTP sender using Android JavaMail (`com.sun.mail:android-mail`).
 * Runs on [Dispatchers.IO]. Never logs credentials.
 */
object SmtpClient {

    data class MailRequest(
        val host: String,
        val port: Int,
        val useTls: Boolean,
        val username: String,
        val password: String,
        val from: String,
        val to: String,
        val subject: String,
        val body: String,
    )

    suspend fun send(request: MailRequest): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(request.host.isNotBlank()) { "host" }
            require(request.to.isNotBlank()) { "to" }
            val fromAddr = request.from.ifBlank { request.username }
            require(fromAddr.isNotBlank()) { "from" }

            val props = Properties().apply {
                put("mail.smtp.host", request.host.trim())
                put("mail.smtp.port", request.port.toString())
                put("mail.smtp.auth", "true")
                put("mail.smtp.connectiontimeout", "15000")
                put("mail.smtp.timeout", "15000")
                put("mail.smtp.writetimeout", "15000")
                if (request.useTls) {
                    if (request.port == 465) {
                        put("mail.smtp.ssl.enable", "true")
                        put("mail.smtp.socketFactory.port", "465")
                        put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    } else {
                        put("mail.smtp.starttls.enable", "true")
                        put("mail.smtp.starttls.required", "true")
                    }
                }
            }

            val user = request.username
            val pass = request.password
            val session = Session.getInstance(
                props,
                object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication =
                        PasswordAuthentication(user, pass)
                },
            )
            session.debug = false

            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(fromAddr.trim()))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(request.to.trim(), false))
                subject = request.subject
                setText(request.body, "UTF-8")
            }
            Transport.send(message)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
