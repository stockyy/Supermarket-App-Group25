package com.supermarket.controllers

import com.supermarket.database.UserRole
import com.supermarket.database.Users
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

object ManagementAuthController {

    // Returns a Pair<Int, UserRole>? so the route knows if they are a Manager or Worker
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
}