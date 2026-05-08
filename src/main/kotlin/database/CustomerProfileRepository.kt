package com.supermarket.database

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDate
import java.time.YearMonth
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

@Serializable
data class CustomerPaymentResponse(
    val id: Int,
    val cardholderName: String,
    val cardLastFour: String,
    val expiry: String,
)

@Serializable
data class CustomerPaymentUpdateRequest(
    val cardName: String,
    val cardNumber: String,
    val cardExpiry: String,
    val cardCvv: String,
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
            getAddressRowsForUser(userId).firstOrNull()
        }

    fun getAddresses(userId: Int): List<CustomerAddressResponse> =
        transaction {
            getAddressRowsForUser(userId)
        }

    fun addAddress(
        userId: Int,
        request: CustomerAddressUpdateRequest,
    ): CustomerAddressResponse? =
        transaction {
            val line1 = request.line1.trim()
            val line2 = request.line2?.trim()?.takeIf { it.isNotEmpty() }
            val city = request.city.trim()
            val postcode = request.postcode.trim()

            if (line1.isBlank() || city.isBlank() || postcode.isBlank()) {
                return@transaction null
            }

            val insertedAddress =
                Address.insert {
                    it[Address.userId] = userId
                    it[Address.line1] = line1
                    it[Address.line2] = line2
                    it[Address.city] = city
                    it[Address.postcode] = postcode
                }

            CustomerAddressResponse(
                id = insertedAddress[Address.id],
                line1 = line1,
                line2 = line2,
                city = city,
                postcode = postcode,
            )
        }

    fun updateAddress(
        userId: Int,
        addressId: Int,
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

            val updatedRows =
                Address.update({ (Address.id eq addressId) and (Address.userId eq userId) }) {
                    it[Address.line1] = line1
                    it[Address.line2] = line2
                    it[Address.city] = city
                    it[Address.postcode] = postcode
                }

            if (updatedRows > 0) "SUCCESS" else "not_found"
        }

    fun deleteAddress(
        userId: Int,
        addressId: Int,
    ): String =
        transaction {
            val address =
                Address
                    .selectAll()
                    .where { (Address.id eq addressId) and (Address.userId eq userId) }
                    .singleOrNull()
                    ?: return@transaction "not_found"

            val usedByOrder =
                Order
                    .selectAll()
                    .where { Order.deliveryAddressId eq address[Address.id] }
                    .firstOrNull() != null

            if (usedByOrder) {
                return@transaction "address_in_use"
            }

            Address.deleteWhere { (Address.id eq addressId) and (Address.userId eq userId) }
            "SUCCESS"
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
                    .orderBy(Address.id)
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

    private fun getAddressRowsForUser(userId: Int): List<CustomerAddressResponse> =
        Address
            .selectAll()
            .where { Address.userId eq userId }
            .orderBy(Address.id)
            .map { row ->
                CustomerAddressResponse(
                    id = row[Address.id],
                    line1 = row[Address.line1],
                    line2 = row[Address.line2],
                    city = row[Address.city],
                    postcode = row[Address.postcode],
                )
            }
}

object PaymentRepository {
    private val cardholderNameRegex = "^[A-Za-z][A-Za-z .'\\-]{1,}$".toRegex()

    fun getPayment(userId: Int): CustomerPaymentResponse? =
        transaction {
            getPaymentRowsForUser(userId).firstOrNull()
        }

    fun getPayments(userId: Int): List<CustomerPaymentResponse> =
        transaction {
            getPaymentRowsForUser(userId)
        }

    fun addPayment(
        userId: Int,
        request: CustomerPaymentUpdateRequest,
    ): CustomerPaymentResponse? =
        transaction {
            val savedPaymentId =
                when (val result = savePaymentForUser(userId, request)) {
                    is PaymentSaveResult.Success -> result.paymentId
                    is PaymentSaveResult.Error -> return@transaction null
                }
            getPaymentByIdForUser(userId, savedPaymentId)
        }

    fun upsertPayment(
        userId: Int,
        request: CustomerPaymentUpdateRequest,
    ): String =
        transaction {
            upsertPaymentForUser(userId, request)
        }

    fun upsertPaymentForUser(
        userId: Int,
        request: CustomerPaymentUpdateRequest,
    ): String =
        when (val result = savePaymentForUser(userId, request)) {
            is PaymentSaveResult.Success -> "SUCCESS"
            is PaymentSaveResult.Error -> result.reason
        }

    fun deletePayment(
        userId: Int,
        paymentId: Int,
    ): String =
        transaction {
            val deletedRows =
                PaymentDetails.deleteWhere {
                    (PaymentDetails.id eq paymentId) and (PaymentDetails.userId eq userId)
                }

            if (deletedRows > 0) "SUCCESS" else "not_found"
        }

    fun paymentBelongsToUser(
        userId: Int,
        paymentId: Int,
    ): Boolean =
        PaymentDetails
            .selectAll()
            .where { (PaymentDetails.id eq paymentId) and (PaymentDetails.userId eq userId) }
            .firstOrNull() != null

    private fun savePaymentForUser(
        userId: Int,
        request: CustomerPaymentUpdateRequest,
    ): PaymentSaveResult {
        val cardName = request.cardName.trim()
        val cardNumber = request.cardNumber.filter { it.isDigit() }
        val cvv = request.cardCvv.filter { it.isDigit() }

        if (cardName.isBlank() || cardNumber.isBlank() || request.cardExpiry.isBlank() || cvv.isBlank()) {
            return PaymentSaveResult.Error("missing_fields")
        }

        val expiry =
            parseExpiry(request.cardExpiry)
                ?: run {
                    return PaymentSaveResult.Error("invalid_card_expiry")
                }

        if (!cardholderNameRegex.matches(cardName)) {
            return PaymentSaveResult.Error("invalid_card_name")
        }

        if (cardNumber.length != 16 || !passesLuhnCheck(cardNumber)) {
            return PaymentSaveResult.Error("invalid_card_number")
        }

        if (cvv.length !in 3..4) {
            return PaymentSaveResult.Error("invalid_card_cvv")
        }

        val existingPayment =
            PaymentDetails
                .selectAll()
                .where { PaymentDetails.userId eq userId }
                .firstOrNull { row -> BCrypt.checkpw(cardNumber, row[PaymentDetails.cardNumberHash]) }

        val cardNumberHash = BCrypt.hashpw(cardNumber, BCrypt.gensalt())
        val cvvHash = BCrypt.hashpw(cvv, BCrypt.gensalt())
        val lastFour = cardNumber.takeLast(4)

        val paymentId =
            if (existingPayment == null) {
                PaymentDetails.insert {
                    it[PaymentDetails.userId] = userId
                    it[PaymentDetails.cardholderName] = cardName
                    it[PaymentDetails.cardNumberHash] = cardNumberHash
                    it[PaymentDetails.cvvHash] = cvvHash
                    it[PaymentDetails.cardLastFour] = lastFour
                    it[PaymentDetails.expiryMonth] = expiry.monthValue
                    it[PaymentDetails.expiryYear] = expiry.year
                }[PaymentDetails.id]
            } else {
                PaymentDetails.update({ PaymentDetails.id eq existingPayment[PaymentDetails.id] }) {
                    it[PaymentDetails.cardholderName] = cardName
                    it[PaymentDetails.cardNumberHash] = cardNumberHash
                    it[PaymentDetails.cvvHash] = cvvHash
                    it[PaymentDetails.cardLastFour] = lastFour
                    it[PaymentDetails.expiryMonth] = expiry.monthValue
                    it[PaymentDetails.expiryYear] = expiry.year
                }
                existingPayment[PaymentDetails.id]
            }

        return PaymentSaveResult.Success(paymentId)
    }

    private sealed class PaymentSaveResult {
        data class Success(
            val paymentId: Int,
        ) : PaymentSaveResult()

        data class Error(
            val reason: String,
        ) : PaymentSaveResult()
    }

    private fun getPaymentRowsForUser(userId: Int): List<CustomerPaymentResponse> =
        PaymentDetails
            .selectAll()
            .where { PaymentDetails.userId eq userId }
            .orderBy(PaymentDetails.id)
            .map { row -> mapPaymentRow(row) }

    private fun getPaymentByIdForUser(
        userId: Int,
        paymentId: Int,
    ): CustomerPaymentResponse? =
        PaymentDetails
            .selectAll()
            .where { (PaymentDetails.id eq paymentId) and (PaymentDetails.userId eq userId) }
            .singleOrNull()
            ?.let { row -> mapPaymentRow(row) }

    private fun mapPaymentRow(row: ResultRow): CustomerPaymentResponse =
        CustomerPaymentResponse(
            id = row[PaymentDetails.id],
            cardholderName = row[PaymentDetails.cardholderName],
            cardLastFour = row[PaymentDetails.cardLastFour],
            expiry = formatExpiry(row[PaymentDetails.expiryMonth], row[PaymentDetails.expiryYear]),
        )

    private fun parseExpiry(value: String): YearMonth? {
        val match = Regex("^(\\d{2})\\s*/\\s*(\\d{2})$").matchEntire(value.trim()) ?: return null
        val month = match.groupValues[1].toIntOrNull() ?: return null
        val year = match.groupValues[2].toIntOrNull()?.plus(2000) ?: return null

        if (month !in 1..12) {
            return null
        }

        val expiry = YearMonth.of(year, month)
        return if (expiry.atEndOfMonth().isBefore(LocalDate.now())) null else expiry
    }

    private fun formatExpiry(
        month: Int,
        year: Int,
    ): String = "%02d / %02d".format(month, year % 100)

    private fun passesLuhnCheck(cardNumber: String): Boolean {
        var sum = 0
        var shouldDouble = false

        for (index in cardNumber.length - 1 downTo 0) {
            var digit = cardNumber[index].digitToIntOrNull() ?: return false

            if (shouldDouble) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }

            sum += digit
            shouldDouble = !shouldDouble
        }

        return sum > 0 && sum % 10 == 0
    }
}
