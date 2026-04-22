package com.supermarket.controllers

import com.supermarket.database.UserRole
import com.supermarket.database.Users
import io.ktor.http.Parameters
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.format.DateTimeParseException
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
            if (existingEmail != null) {
                return@transaction "email_exists"
            }

            // Convert dobString to date
            val parsedDob = try {
                LocalDate.parse(dobString)
                // Safety check in case the user bypassed the HTML calendar
            } catch (e: DateTimeParseException) {
                return@transaction "invalid_date"
            }

            // Check if user is over 18
            val ageInYears = ChronoUnit.YEARS.between(parsedDob, LocalDate.now())
            if (ageInYears < 18) {
                return@transaction "underage"
            }

            // Hash & Salt password
            val hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt())

            // insert into database
            Users.insert {
                it[Users.firstName] = firstName
                it[Users.lastName] = lastName
                it[Users.email] = email
                it[Users.password] = hashedPassword
                it[Users.dob] = parsedDob
                it[Users.role] = UserRole.CUSTOMER
            }

            return@transaction "SUCCESS"
        }
    }

    fun verifyCustomerLogin(email: String, rawPassword: String): Int? {
        return transaction {
            val userRow = Users.selectAll().where { Users.email eq email }.singleOrNull()

            // If email doesn't exist then return null
            if (userRow == null) {
                return@transaction null
            }

            // Get user's hashed password
            val hashedPassword = userRow[Users.password]
            val isMatch = BCrypt.checkpw(rawPassword, hashedPassword)

            // Check if it was a match
            if (!isMatch) {
                return@transaction null
            }
            else {
                return@transaction userRow[Users.id]
            }
        }
    }
}