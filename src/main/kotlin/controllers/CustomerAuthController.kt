package com.supermarket.controllers

import com.supermarket.database.Users
import io.ktor.http.Parameters
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object CustomerAuthController {
    fun registerNewUser(
        firstName: String,
        lastName: String,
        dobString: String,
        email: String,
        rawPassword: String
    ): String {

        return transaction {
            // Check if email exists in database
            val existingEmail = Users.selectAll().where { Users.email eq email }.firstOrNull()

            "SUCCESS"
        }

    }
}