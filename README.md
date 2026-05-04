# SuperMarket Web Application (G25)
A web application for supermarket shopping, stock management, warehouse operations and business analytics

## Overview
This application has been designed to support the operations of a modern online supermarket. It couples together customer-facing shopping, warehouse and stock management along with business analytics into a single platform allowing supermarkets to manage orders, inventory, and sales more effectively.

## What does it do
- **Customers** shop online browsing products, managing their basket, placing orders, and tracking deliveries with a substitution system for out-of-stock items. 
- **Warehouse Staff** handles the operational side receiving deliveries, updating stock, picking orders, recording wastage and offsales, along with stock audits. 
- **Managers** oversee the business viewing sales dashboards, generating reports, managing staff accounts, and accessing audit logs across the system. 

## Built With

| Layer    | Technology                          |
|----------|-------------------------------------|
| Server   | Kotlin 2.3 · Ktor 3.4 · JDK 21    |
| Database | SQLite · Exposed ORM        |
| Frontend | HTML/CSS/JS served from Ktor static |
| CI/CD    | GitHub Actions · Gradle 9.3        |

## Getting Started

This guide will walk you through setting up the project for development.

### Prerequisites

- **IntelliJ IDEA**: The recommended IDE for this project. You can download the free Community Edition or the paid Ultimate Edition.
  - [Download IntelliJ IDEA](https://www.jetbrains.com/idea/download/)
- **Git**: For cloning the repository.

### Setup Instructions

1.  **Clone the Repository**
    Open a terminal or command prompt and clone the project to your local machine:
    ```sh
    git clone "https://github.com/stockyy/Supermarket-App-Group25"
    ```

2.  **Open in IntelliJ IDEA**
    -   Launch IntelliJ IDEA.
    -   Select **Open** from the welcome screen, or **File > Open** from the menu.
    -   Navigate to the directory where you cloned the project and select it.

3.  **Automatic Setup (Gradle & JDK)**
    -   IntelliJ IDEA will automatically detect the `build.gradle.kts` file.
    -   It will use Gradle to download all the required dependencies.
    -   The project is configured to use **JDK 21**. If you don't have it installed, IntelliJ will prompt you to download and install it.

4.  **Run the Application**
    -   Once Gradle has finished syncing, locate the main entry point of the application:
        `src/main/kotlin/Application.kt`
    -   Open the `Application.kt` file.
    -   You will see a green "play" icon next to the `main` function. Click it and select **Run 'ApplicationKt'**.
    -   The first time you run the application, it will create and seed an `identifier.sqlite` database file in the project's root directory.

The Ktor server will start, and you can access the application in your web browser (typically at `http://localhost:8080`).

## Repository Layout
The project is structured as a standard Gradle project. Key directories include:
```
.
├── .github/              # CI/CD workflows for GitHub Actions
├── build/                # Compiled output from Gradle
├── gradle/               # Gradle wrapper files
├── src/
│   ├── main/
│   │   ├── kotlin/       # Main application source code
│   │   │   ├── Application.kt
│   │   │   ├── Routing.kt
│   │   │   ├── Serialization.kt
│   │   │   ├── controllers/
│   │   │   ├── database/
│   │   │   └── routes/
│   │   └── resources/    # Configuration and static assets
│   │       ├── static/
│   │       ├── application.yaml
│   │       └── logback.xml
│   └── test/
│       └── kotlin/       # Test source code
├── build.gradle.kts      # Gradle build script
├── identifier.sqlite     # Local SQLite database file
└── README.md             # This file
```

---

## API Endpoints at a Glance

**Customers:**
Register, login/logout, session validation, profile updates, password changes.

**Products:** 
Browse all, search by name, filter by category/section, barcode lookup, promotions. Admin CRUD for product management.

**Orders:**
Full basket lifecycle (add, update, remove, clear), place orders, delivery windows, order history, status tracking, cancellations, and product substitutions.

**Stock:**
Individual and bulk stock queries, low-stock alerts, increment/decrement operations, movement logging with full audit trail.

**Warehouse:**
Order picking (claim, view, and confirm picks), wastage/offsale reporting, picking list generation, inventory overview, aisle location management, and stock audits.

**Management:**
Dashboard statistics, sales and order reports, staff account CRUD, system-wide audit log.

---

---

## Our Development Strategy

**Branching**: feature branches (`feature/...`) merge into `implementation` via pull requests, then into `main` once tested and reviewed. We do not accept direct commits to `main`.

**Testing**: The project contains a test suite in `src/test/kotlin`. (Note: This section is a work in progress).

**Meetings**: Team meeting scheduled in lieu of sprint goals time periods along with occasional check up meetings, plus retrospectives after each sprint. All recorded on the wiki.

---
