# SuperMarket Web Application (Group 25)
A comprehensive web application for supermarket shopping, stock management, warehouse operations, and business analytics.

## Overview
This platform is designed to support the entire lifecycle of a modern online supermarket. It integrates customer-facing shopping, warehouse logistics, and management analytics into a single cohesive system, allowing for efficient inventory management and order fulfillment.

## Core Features
- **Customers**: Browse products, manage baskets, place orders, and handle substitutions for out-of-stock items.
- **Warehouse Staff**: Receive deliveries, update stock, pick orders into physical crates, and record wastage/offsales.
- **Managers**: View real-time sales dashboards, generate reports, manage staff accounts, and access system audit logs.

## Tech Stack

| Layer          | Technology                                   |
|----------------|----------------------------------------------|
| **Server**     | Kotlin 2.3 · Ktor 3.4 · JDK 21               |
| **Database**   | SQLite · Exposed ORM                         |
| **Frontend**   | HTML / Vanilla CSS / JS (Served via Ktor)    |
| **CI/CD**      | GitHub Actions · Gradle 9.3                  |
| **Utilities**  | BCrypt (Security) · DataFaker (Seeding)      |

---

## Getting Started (Full Setup Guide)

You can run this project locally using **Visual Studio Code (VSCode)** or entirely in your browser using **GitHub Codespaces**. Follow the instructions below for your preferred environment.

### Option 1: Running via GitHub Codespaces (Recommended)

If you don't want to install anything locally, you can run the application entirely in the cloud.

#### 1. Launch the Codespace
1. Navigate to the repository page on GitHub.
2. Click the green **`<> Code`** button.
3. Switch to the **Codespaces** tab.
4. Click **Create codespace on main** (or click the `+` icon).
5. Wait a minute or two for the cloud environment to build and load in your browser.

#### 2. Running the Application
Once the VSCode interface loads in your browser, open a terminal (`Terminal > New Terminal`) and follow these steps:

1. **Grant execution permissions to Gradle.** *This step is critical in Codespaces before you can run the app:*
   ```sh
   chmod +x gradlew
   ```
2. Start the server:
   ```sh
   ./gradlew run

3. Once the server starts, VSCode will prompt you that an application is running on port `8080`. Click **Open in Browser** on the pop-up notification, or go to the "Ports" tab next to the terminal and click the globe icon next to port 8080.

### Option 2: Running Locally on VSCode

#### 1. Prerequisites
Before you begin, ensure you have the following installed on your machine:
*   **Git**: [Download Git](https://git-scm.com/downloads)
*   **Visual Studio Code**: [Download VSCode](https://code.visualstudio.com/download)
*   **JDK 21**: [Download Esclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21) (Ensure you set your `JAVA_HOME` environment variable during installation).
*   **VSCode Extensions**: For the best experience, search for and install the **Extension Pack for Java** and the **Kotlin** extension inside VSCode.

#### 2. Clone the Repository
Open your terminal (or Git Bash) and run:
```sh
git clone [https://github.com/stockyy/Supermarket-App-Group25](https://github.com/stockyy/Supermarket-App-Group25)
cd Supermarket-App-Group25
```

#### 3. Running the Application
1. Open the cloned folder in VSCode (`File > Open Folder...`).
2. Open a new terminal within VSCode (`Terminal > New Terminal`).
3. If you are on Mac/Linux, ensure the Gradle wrapper has execution permissions:
   ```sh
   chmod +x gradlew


4. Start the server by running:
   ```sh
   ./gradlew run

*(Note: If the progress bar seems to hang at 83% or "Execution 83%", the server is actually running!)*

Once started, visit: **[http://localhost:8080/](http://localhost:8080/)**

---

##  How to Use the System

> **Note:** Every time the application restarts, the `identifier.sqlite` database is fully refreshed and re-seeded with fresh data.

When You start up the system you have two options:
* Browse the website as a customer, add items to your cart, and create an account in order to checkout.
* Navigate to the employee login portal (management/login), and login as a manager using the following details:
### Manager Access
*   **Login Route**: `/management/login`
*   **Staff ID**: `12345678`
*   **Password**: `Testing123!` (**All pre seeded accounts have this password**)
From here you can either browse teh dashboard as a manager (view manager explained for this), or navigate to the "staff" page via the header, find the staffId for a "worker" ("drivers were never fully implemented ,and analysts have equivalent permissions to managers), and then logout and relogin using that staffId and the password "Testing123!" (as is the same for all seeded accounts). Refer to the warehouse picker explained section to understand how the interface works.

### Warehouse Manager Explained (To be completed by Yixuan)
generate pick list generates pick lists for all orders that are due on the selected date.

### Warehouse Picker Explained
The warehouse picker system has been meticulously designed to reduce user error and enforce cold-chain compliance.

* **The Dashboard**: Once logged in as a worker, you are taken to a central dashboard where you can start a new picklist, log an offsale, report wastage, or manually check stock levels.
* **Zone-Based Selection**: To start a pick, you must select the "Select Picks" buttons and then choose a specific warehouse zone (Ambient, Chilled, FRV & Bread, or Frozen). This ensures temperature-sensitive items are picked together.
    * (To claim a pick, picklists must have been generated and must exist in the database, this can be done by the manager on the manager dashboard)
* **Binding Crates**: Before picking begins, you will be asked to input the required crate IDs (formatted as `CRATE-XXX`, e.g., `CRATE-001`). This permanently binds specific customer orders to the up to 6 crates in your physical trolley.
  * In the final build, this would be done via a scanner, removing the need for manual input. However due to this being a desktop project at the moment, this has not been possible to implement.
* **Active Picking**: For each item on the list, you must click "pick item" (this would yet again normally be done via scanning), you must confirm the exact quantity picked to ensure that you pick the right quantitiy for the customer. To prevent mixed orders, the system will explicitly dictate which of your scanned crates the item must be placed into (you must then enter the crate id, once again this would normally be done via scanning).
* **Handling Exceptions**:
    * **Not on Shelf**: If an item is missing, click "Not on Shelf". The system will query the database to recommend pre-approved substitutions.
    * **Offsales**: Alternatively, you can log the item as an "Offsale," which immediately zeroes the stock in the database and routes you to the next item without halting your workflow.
    * *Note:* Workers can process items even if the database claims there is 0 stock, allowing them to correct "phantom stock" discrepancies in real-time (phantom stock is slightly wrong here, its more about the fact that the database may say that stock does not exist, but if a worker finsds some valid stock on the database, you can't argue with it bc the product is realk even if the database says that it isn't).
* When picking there is a button to automatically pick the entire list. This exists solely for testing and demonstration purposes so we do not need to manually pick the entire list to show that the system works.
* **Putaway**: Once the picklist is completely clear, the system generates a putaway summary, directing the worker to place their crates in the correct storage areas (e.g., the Freezer/Chiller or Staging Area).
* **Performance Tracking**: Workers can view their personal metrics via the Settings page, which calculates their live average pick rate (items per hour) and tracks total completed lists.

## Repository Layout
The project follows a modular Kotlin/Ktor structure. Below is a map of the key directories:

```text
.
├── .github/workflows/      # CI/CD: Automated builds and ktlint checks
├── gradle/                 # Gradle Wrapper for consistent builds
├── src/main/kotlin/        # Backend Logic
│   ├── Application.kt      # Main entry point and Ktor module setup
│   ├── controllers/        # Business logic for auth and picking workflows
│   ├── database/           # Exposed ORM tables, Seeder, and Query functions
│   └── routes/             # Ktor route definitions (Customer, Warehouse, etc.)
├── src/main/resources/     # Static Assets & Configuration
│   ├── application.yaml    # Server settings (Port, Modules)
│   ├── productData.json    # Source data for database seeding
│   └── static/             # Frontend Assets
│       ├── js/             # Client-side logic (Basket management, Nav)
│       ├── stylesheets/    # Structured CSS (Tokens, Base, Component-specific)
│       └── views/          # HTML templates (Partials, Dashboard views)
├── src/test/kotlin/        # Unit and Integration test suite
├── build.gradle.kts        # Dependency management and build scripts
└── identifier.sqlite       # Local database (generated automatically)
```

---

## Development Strategy

To maintain code quality and system stability, we adhere to the following workflow:

### Branching & Merging
- **Feature Branches**: All work must be done in `feature/...` branches.
- **Integration**: Feature branches must first merge into the `implementation` branch via a Pull Request.
- **Production**: Only the `implementation` branch is permitted to merge into `main`. Direct commits to `main` are blocked.

### Automated Checks (CI/CD)
Before any Pull Request can be merged into `implementation` or `main`, it **must** pass our GitHub Actions pipeline:
1.  **Gradle Build**: Ensures the project compiles correctly and all tests pass.
2.  **ktlint Check**: Enforces our coding standards. If your code is not formatted correctly, the check will fail.

### Meetings & Documentation
- Regular team meetings and retrospectives are held to track progress against sprint goals.
- All meeting minutes and architectural decisions are documented on the project wiki.

---
