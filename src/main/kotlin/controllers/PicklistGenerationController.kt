package com.supermarket.controllers

import com.supermarket.database.Order
import com.supermarket.database.OrderStatus
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.core.*
import java.time.LocalDate

object PicklistGenerationController {

    // The constraints of the warehouse
    private const val MAX_ITEMS_PER_CRATE = 20
    private const val MAX_CRATES_PER_TROLLEY = 6
    private const val MAX_ITEMS_PER_LIST = 80

    fun generatePicklists(targetDate: LocalDate): Int {
        val totalListsCreated = 0

        // Convert the target LocalDate to the very start and very end of that day
        val startOfDay = targetDate.atStartOfDay()
        val endOfDay = targetDate.plusDays(1).atStartOfDay()

        transaction {
            val waitingOrders =
                Order.selectAll().where(
                    (Order.status eq OrderStatus.WAITING) and
                            (Order.deliveryWindowStart greaterEq startOfDay) and
                            (Order.deliveryWindowEnd less endOfDay)
                )

        }

        return totalListsCreated
    }
}