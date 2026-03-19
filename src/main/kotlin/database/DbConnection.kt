package com.supermarket.database

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.io.File

object DatabaseCreation {
    fun init() {
        // Check if the 'data' directory exists, and create it if it doesn't
        val dataFolder = File("data")
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }

        // Creates a file in data directory named supermarket.db
        Database.connect("jdbc:sqlite:data/supermarket.db", "org.sqlite.JDBC")

        // Open a transaction to execute database commands
        transaction {
            // CREATE TABLE IF NOT EXIST:
            SchemaUtils.create(
                Users, Product, Order, OrderItem, Category, Section, Route,
                Crate, WastageLog, OffsaleLog, ProductSubstituteMap, SubstituteItem
            )
        }

        println("Connected to database & Tables Created")
    }
}

fun refreshDatabase() {
    transaction {
        // 1. destroy tables (Children first, Parents last)
        SchemaUtils.drop(
            OrderItem, SubstituteItem, ProductSubstituteMap, WastageLog, OffsaleLog, Crate,
            Order, Route, Product, Category, Section, Users
        )

        // Create tables (Parents first, Children last)
        SchemaUtils.create(
            Users, Section, Category, Product, Route, Order, Crate, OffsaleLog,
            WastageLog, ProductSubstituteMap, SubstituteItem, OrderItem
        )

        println("Database schema successfully wiped and rebuilt.")

        seedDatabaseIfNeeded(true)

        println("Fresh dummy data successfully seeded.")
    }
}
