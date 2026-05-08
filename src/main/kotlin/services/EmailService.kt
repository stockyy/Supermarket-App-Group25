package com.supermarket.services

import io.ktor.server.application.*
import jakarta.mail.*
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

object EmailService {
    // will be filled once initialized from application.kt
    private var smtpHost: String = ""
    private var smtpPort: Int = 587
    private var fromAddress: String = ""
    private var fromName: String = ""
    private var appPassword: String = ""
    private var enabled: Boolean = false

    private var initialised: Boolean = false

    // reads email config and puts it in the variables above
    fun init(application: Application) {
        val config = application.environment.config

        // make sure everythings in place
        try {
            // Manual loading from config
            smtpHost = config.propertyOrNull("email.smtpHost")?.getString() ?: ""
            smtpPort = config.propertyOrNull("email.smtpPort")?.getString()?.toInt() ?: 587
            fromAddress = config.propertyOrNull("email.fromAddress")?.getString() ?: ""
            fromName = config.propertyOrNull("email.fromName")?.getString() ?: "Supermarket"
            appPassword = config.propertyOrNull("email.appPassword")?.getString() ?: ""
            enabled = config.propertyOrNull("email.enabled")?.getString()?.toBoolean() ?: false

            initialised = true

            if (enabled) {
                println("Email will be sent via $smtpHost:$smtpPort as $fromAddress")
            } else {
                println("EmailService initialised but disabled")
            }
        } catch (e: Exception) {
            println("Could not load email config from application.yaml")
            println("Error: " + e.message)
            enabled = false
            initialised = true
        }
    }

    // Send email
    fun sendEmail(
        toAddress: String,
        subject: String,
        bodyHtml: String,
    ): Boolean {
        if (!initialised) {
            println("ERROR: EmailService.sendEmail called before init()")
            logEmailToConsole(toAddress, subject, bodyHtml)
            return false
        }

        // kill switch
        if (!enabled) {
            logEmailToConsole(toAddress, subject, bodyHtml)
            return false
        }

        // send via the smtp
        try {
            val props = Properties()
            props.put("mail.smtp.host", smtpHost)
            props.put("mail.smtp.port", smtpPort.toString())
            props.put("mail.smtp.auth", "true")
            props.put("mail.smtp.starttls.enable", "true")

            val session =
                Session.getInstance(
                    props,
                    object : Authenticator() {
                        override fun getPasswordAuthentication(): PasswordAuthentication = PasswordAuthentication(fromAddress, appPassword)
                    },
                )

            val message = MimeMessage(session)
            message.setFrom(InternetAddress(fromAddress, fromName))
            message.setRecipient(Message.RecipientType.TO, InternetAddress(toAddress))
            message.setSubject(subject)
            message.setContent(bodyHtml, "text/html")

            Transport.send(message)
            println("Email sent to $toAddress: $subject")
            return true
        } catch (e: Exception) {
            // if email fails just log it and fall back to console, don't cause a crash
            println("ERROR: Failed to send email to $toAddress")
            println("Error: " + e.message)
            e.printStackTrace()
            logEmailToConsole(toAddress, subject, bodyHtml)
            return false
        }
    }

    // when email fails send here so I can debug
    private fun logEmailToConsole(
        toAddress: String,
        subject: String,
        bodyHtml: String,
    ) {
        println("Email not sent, fall back to console")
        println("To:      $toAddress")
        println("Subject: $subject")
        println("Body:")
        println(bodyHtml)
    }
}
