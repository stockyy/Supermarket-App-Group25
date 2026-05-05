package com.supermarket.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

@Serializable
data class CustomerProfileResponse(
    val id: Int,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val dateOfBirth: String,
)

@Serializable
data class CustomerProfileUpdateRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String? = null,
    val dateOfBirth: String,
)

@Serializable
data class CustomerPasswordUpdateRequest(
    val currentPassword: String,
    val newPassword: String,
)

@Serializable
data class CustomerAddressResponse(
    val id: Int,
    val line1: String,
    val line2: String?,
    val city: String,
    val postcode: String,
)

@Serializable
data class CustomerAddressUpdateRequest(
    val line1: String,
    val line2: String? = null,
    val city: String,
    val postcode: String,
)

object CustomerProfileRepository {
    fun getProfile(userId: Int): CustomerProfileResponse? =
        transaction {
            Users
                .selectAll()
                .where { (Users.id eq userId) and (Users.role eq UserRole.CUSTOMER) }
                .singleOrNull()
                ?.let { row ->
                    CustomerProfileResponse(
                        id = row[Users.id],
                        firstName = row[Users.firstName],
                        lastName = row[Users.lastName],
                        email = row[Users.email],
                        phone = row[Users.phoneNumber],
                        dateOfBirth = row[Users.dob].toString(),
                    )
                }
        }

    fun updateProfile(
        userId: Int,
        request: CustomerProfileUpdateRequest,
    ): String =
        transaction {
            val firstName = request.firstName.trim()
            val lastName = request.lastName.trim()
            val email = request.email.trim()
            val phone = request.phone?.trim()?.takeIf { it.isNotEmpty() }

            if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || request.dateOfBirth.isBlank()) {
                return@transaction "missing_fields"
            }

            if (!email.contains("@") || !email.contains(".")) {
                return@transaction "invalid_email"
            }

            val parsedDob =
                try {
                    LocalDate.parse(request.dateOfBirth)
                } catch (e: DateTimeParseException) {
                    return@transaction "invalid_date"
                }

            if (ChronoUnit.YEARS.between(parsedDob, LocalDate.now()) < 18) {
                return@transaction "underage"
            }

            if (phone != null) {
                val phoneRegex = "^[\\d\\s\\+\\-\\(\\)]{10,20}$".toRegex()
                if (!phoneRegex.matches(phone)) {
                    return@transaction "invalid_phone"
                }
            }

            val duplicateEmail =
                Users
                    .selectAll()
                    .where { (Users.email eq email) and (Users.id neq userId) }
                    .firstOrNull()

            if (duplicateEmail != null) {
                return@transaction "email_exists"
            }

            val updatedRows =
                Users.update({ (Users.id eq userId) and (Users.role eq UserRole.CUSTOMER) }) {
                    it[Users.firstName] = firstName
                    it[Users.lastName] = lastName
                    it[Users.email] = email
                    it[Users.phoneNumber] = phone
                    it[Users.dob] = parsedDob
                }

            if (updatedRows > 0) "SUCCESS" else "not_found"
        }

    fun updatePassword(
        userId: Int,
        request: CustomerPasswordUpdateRequest,
    ): String =
        transaction {
            if (request.currentPassword.isBlank() || request.newPassword.isBlank()) {
                return@transaction "missing_fields"
            }

            val userRow =
                Users
                    .selectAll()
                    .where { (Users.id eq userId) and (Users.role eq UserRole.CUSTOMER) }
                    .singleOrNull()
                    ?: return@transaction "not_found"

            if (!BCrypt.checkpw(request.currentPassword, userRow[Users.password])) {
                return@transaction "invalid_current_password"
            }

            val passRegex = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#\$%^&*]).{8,}\$".toRegex()
            if (!passRegex.matches(request.newPassword)) {
                return@transaction "weak_password"
            }

            val hashedPassword = BCrypt.hashpw(request.newPassword, BCrypt.gensalt())

            Users.update({ Users.id eq userId }) {
                it[Users.password] = hashedPassword
            }

            "SUCCESS"
        }
}

object AddressRepository {
    fun getAddress(userId: Int): CustomerAddressResponse? =
        transaction {
            Address
                .selectAll()
                .where { Address.userId eq userId }
                .firstOrNull()
                ?.let { row ->
                    CustomerAddressResponse(
                        id = row[Address.id],
                        line1 = row[Address.line1],
                        line2 = row[Address.line2],
                        city = row[Address.city],
                        postcode = row[Address.postcode],
                    )
                }
        }

    fun upsertAddress(
        userId: Int,
        request: CustomerAddressUpdateRequest,
    ): String =
        transaction {
            val line1 = request.line1.trim()
            val line2 = request.line2?.trim()?.takeIf { it.isNotEmpty() }
            val city = request.city.trim()
            val postcode = request.postcode.trim()

            if (line1.isBlank() || city.isBlank() || postcode.isBlank()) {
                return@transaction "missing_fields"
            }

            val existingAddress =
                Address
                    .selectAll()
                    .where { Address.userId eq userId }
                    .firstOrNull()

            if (existingAddress == null) {
                Address.insert {
                    it[Address.userId] = userId
                    it[Address.line1] = line1
                    it[Address.line2] = line2
                    it[Address.city] = city
                    it[Address.postcode] = postcode
                }
            } else {
                Address.update({ Address.id eq existingAddress[Address.id] }) {
                    it[Address.line1] = line1
                    it[Address.line2] = line2
                    it[Address.city] = city
                    it[Address.postcode] = postcode
                }
            }

            "SUCCESS"
        }
}
