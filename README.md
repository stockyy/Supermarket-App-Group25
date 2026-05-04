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

## 🛠 Getting Started (Full Setup Guide)

Follow these steps to get the system running on your local machine from scratch.

### 1. Prerequisites
Before you begin, ensure you have the following installed:
- **Git**: [Download Git](https://git-scm.com/downloads)
- **IntelliJ IDEA**: (Recommended) Download the [Community or Ultimate Edition](https://www.jetbrains.com/idea/download/).
- **JDK 21**: The project uses Java 21. If you don't have it, IntelliJ can install it for you in Step 3.

### 2. Clone the Repository
Open your terminal and run:
```sh
git clone https://github.com/stockyy/Supermarket-App-Group25
cd Supermarket-App-Group25
```

### 3. Open in IntelliJ & Install Plugins
1. Launch **IntelliJ IDEA** and select **Open**.
2. Navigate to the project folder and click **OK**.
3. **Plugins**: When prompted (or via `Settings > Plugins`), ensure you have the following installed:
   - **Kotlin** (Built-in)
   - **Ktor** (Search in Marketplace)
4. **Gradle Sync**: IntelliJ will detect the `build.gradle.kts` file and start syncing. This may take a few minutes as it downloads dependencies.
5. **JDK Setup**: If prompted that the SDK is missing, select **Download JDK** and choose version **21**.

### 4. Running the Application
There are two ways to start the server:

**Option A: Using the Play Button (Easiest)**
- Navigate to `src/main/kotlin/Application.kt`.
- Click the green **Play icon** next to the `fun main()` and select **Run 'ApplicationKt'**.

**Option B: Using the Terminal**
- Run the following command in the project root:
  ```sh
  ./gradlew run
  ```
- *Note: If the progress bar seems to hang at 83% (or "Execution 83%"), the server is actually running!*

Once started, visit: **[http://localhost:8080/](http://localhost:8080/)**

---

##  How to Use the System

> **Note:** Every time the application restarts, the `identifier.sqlite` database is fully refreshed and re-seeded with fresh data.

### Manager Access
*   **Login Route**: `/management/login`
*   **Staff ID**: `12345678`
*   **Password**: `Testing123!`
*   **Capabilities**: Access the `/db-admin` route (temporary) to view raw data or trigger manual picklist generation. This functionality is being migrated to the main management panel.

### Warehouse Picker
The whole system for the warehouse picker has been designed to try and reduce the chance of a worker making an error, allowing them to pick the items on the list with maximum efficiency.

*   **Workflow**: Once logged in as a worker, you get taken to a dashboard where you can start a pick, offsale an item, waste an item, or view the stock level for an item.
*   **Wastage, Offsale, Stock Level**: 
    - The user will be prompted to input a product ID.
    - You'll then be taken to a page for confirmation (Offsales), a reason/quantity (Wastage), or to view and change the current stock level (Stock Level).
*   **Starting a Pick List**:
    - Choose what type of pick list you would like to pick (Ambient, Chilled, FRV & Bread, Frozen).
    - You will be asked to input the crate IDs for the certain number of crates required.
      - *Barcode Scanning*: Ordinarily this would be done through scanning barcodes, however due to not being on a mobile device, we will have to manually input the IDs for now. 
    - **Crate ID Format**: `CRATE-XXX` (e.g., `CRATE-001`, `CRATE-123`).
*   **The Picking Flow**:
    - Once you click **Confirm Crates**, you can no longer go back. This is so the worker has minimal distractions and can focus directly on picking.
    - For each item, you can either pick the quantity or click **Not on Shelf**.
    - **Not on Shelf**: Takes you to a menu to choose a substitution. Substitutions are recommended by the system automatically (note: some products may not have subs due to limited database variety).
    - **Pick Validation**: Once you click to pick (simulating a barcode scan), the system asks if you have actually picked the correct quantity to reduce errors. After confirming, the system tells you exactly which crate to put the item in.
*   **Data Integrity**: Even if there is 0 in the database, a worker is able to pick/waste/offsale an item. This is because in a real situation, the worker would be scanning the barcode of a product—if they are scanning it but the system says we don't have it, then the database must be wrong, so we let the worker perform the action anyway.
*   **Putaway**: Once the list is empty, you'll see a putaway page. This tells the worker exactly where to put orders for drivers: **Freezer/Chiller** for temperature-controlled items, or the **Staging Area** for ambient items.
*   **Worker Settings**: There is a settings page where you can view your personal info, including your current pick rate and the total number of picklists you've ever completed.

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
    - *Tip*: You can auto-format your code locally by running `./gradlew ktlintFormat`.

### Meetings & Documentation
- Regular team meetings and retrospectives are held to track progress against sprint goals.
- All meeting minutes and architectural decisions are documented on the project wiki.

---
