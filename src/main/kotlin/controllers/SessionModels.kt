package com.supermarket.controllers

import kotlinx.serialization.Serializable

// The cookie assigned to the user
@Serializable
data class UserSession(val userId: Int)
@Serializable
data class StaffSession(val userId: Int, val role: String)