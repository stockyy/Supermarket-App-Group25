package com.supermarket.database

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime
import java.util.UUID

object PasswordResetRepository {
    // tokens last 1 hour
    private const val TOKEN_TTL = 60L

    // creates a new reset token for a given email, returns token string if it exists
    fun createToken(email: String): String? {
        return transaction {
            // find user by email
            val userRow = Users.selectAll().where { Users.email eq email }.firstOrNull()

            if (userRow == null) {
                println("Reset requested for unknown email: $email")
                return@transaction null
            }

            val userId = userRow[Users.id]

            // generate a random token using UUID
            val token = UUID.randomUUID().toString()

            // work out expiry
            val now = LocalDateTime.now()
            val expires = now.plusMinutes(TOKEN_TTL)

            // save token to db
            PasswordResetToken.insert {
                it[PasswordResetToken.userId] = userId
                it[PasswordResetToken.token] = token
                it[PasswordResetToken.expiresAt] = expires
                it[PasswordResetToken.used] = false
                it[PasswordResetToken.createdAt] = now
            }

            return@transaction token
        }
    }

    // checks if token is valid and returns the user id
    fun getUserIdForValidToken(token: String): Int? {
        return transaction {
            val tokenRow =
                PasswordResetToken
                    .selectAll()
                    .where { PasswordResetToken.token eq token }
                    .firstOrNull()

            // token doesn't exist
            if (tokenRow == null) {
                return@transaction null
            }

            // token already used
            if (tokenRow[PasswordResetToken.used] == true) {
                return@transaction null
            }

            // token expired
            val now = LocalDateTime.now()
            if (tokenRow[PasswordResetToken.expiresAt].isBefore(now)) {
                return@transaction null
            }

            // all good
            return@transaction tokenRow[PasswordResetToken.userId]
        }
    }

    // does the actual password reset
    fun resetPassword(
        token: String,
        newPassword: String,
    ): String {
        return transaction {
            // first check token is valid
            val userId = getUserIdForValidToken(token)
            if (userId == null) {
                return@transaction "invalid_token"
            }

            // check password is strong enough (same as registration)
            val passRegex = Regex("^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#\$%^&*]).{8,}\$")
            if (passRegex.matches(newPassword) == false) {
                return@transaction "weak_password"
            }

            // hash the new password
            val salt = BCrypt.gensalt()
            val hashedPassword = BCrypt.hashpw(newPassword, salt)

            // update the user
            Users.update({ Users.id eq userId }) {
                it[Users.password] = hashedPassword
            }

            // mark token as used so it can't be used again
            PasswordResetToken.update({ PasswordResetToken.token eq token }) {
                it[PasswordResetToken.used] = true
            }

            return@transaction "SUCCESS"
        }
    }
}
