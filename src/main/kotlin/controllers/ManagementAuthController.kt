package com.supermarket.controllers

import com.supermarket.database.UserRole
import com.supermarket.database.Users
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object ManagementAuthController {

    fun verifyStaffLogin(staffIdInput: String, rawPasswordInput: String): Pair<Int, UserRole>? {
        return transaction {
            // find user by staffId
            val userRow = Users.selectAll().where { Users.staffId eq staffIdInput }.singleOrNull()

            if (userRow == null) return@transaction null

            // check password
            val isMatch = BCrypt.checkpw(rawPasswordInput, userRow[Users.password])

            if (isMatch) {
                return@transaction Pair(userRow[Users.id], userRow[Users.role])
            } else {
                return@transaction null
            }
        }
    }

    fun createStaffAccount(
        firstName: String,
        lastName: String,
        dobString: String,
        email: String,
        phone: String?,
        rawPassword: String,
        roleString: String
    ): String {
        return transaction {
            // Check if email exists
            val existing = Users.selectAll().where { Users.email eq email }.firstOrNull()
            if (existing != null) return@transaction "email_exists"

            // Password Strength Check
            val passRegex = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#\$%^&*]).{8,}\$".toRegex()
            if (!passRegex.matches(rawPassword)) return@transaction "weak_password"

            // Parse Data (DOB and Role)
            val parsedDob = java.time.LocalDate.parse(dobString)
            val roleEnum = try { UserRole.valueOf(roleString) } catch (e: Exception) { UserRole.WORKER }

            // Generate a unique 8-digit Staff ID
            var generatedStaffId: String
            var isUnique = false
            do {
                generatedStaffId = (10000000..99999999).random().toString()
                val idCheck = Users.selectAll().where { Users.staffId eq generatedStaffId }.firstOrNull()
                if (idCheck == null) isUnique = true
            } while (!isUnique)

            // Hash Password & Insert User Info
            val hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt())

            Users.insert {
                it[Users.staffId] = generatedStaffId
                it[Users.firstName] = firstName
                it[Users.lastName] = lastName
                it[Users.email] = email
                it[Users.phoneNumber] = phone
                it[Users.password] = hashedPassword
                it[Users.role] = roleEnum
                it[Users.dob] = parsedDob
            }

            // Return the generated ID so the route can show it to the Manager
            return@transaction generatedStaffId
        }
    }
}