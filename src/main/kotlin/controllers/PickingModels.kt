package com.supermarket.controllers

import com.supermarket.database.WasteBags
import kotlinx.serialization.Serializable

@Serializable
data class NextPickItem(
    val pickItemId: Int,
    val picklistId: Int,
    val productName: String,
    val orderId: Int,
    val crateId: Int,
    val quantityRequired: Int,
    val categoryName: String,
    val imageDir: String,
    val wasteBag: WasteBags
)