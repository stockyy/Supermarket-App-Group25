# SuperMarket Web Application (G25)
A web application for supermarket shopping, stock management, warehouse operations and business analytics

## Overview
This application has been desinged to suppor tthe operations of a modern online supermarket. It couples together customer-facing shopping, warehouse and stock management along with business analytics into a single paltofrm allowing supermakets to manager orders, inventory and sales more effectively.

## What does it do
- **Customers** shop online browsing products, managing their basket, palcing orders, and tracking deliveries with a substituion system for out of stock items. 
- **Warehouse Staff** handles the operational side receiving deliveries, updatin stock, picking orders, recording wastage and offsales along with stock audits. 
- **Managers** oversee the business viewing sales dashboards, generating reports, managing staff acocoutns and accessing audit logs across the system. 

## Built With

| Layer    | Technology                          |
|----------|-------------------------------------|
| Server   | Kotlin 2.3 · Ktor 3.4 · JDK 21    |
| Database | PostgreSQL 17 · Exposed ORM        |
| Frontend | HTML/CSS/JS served from Ktor static |
| CI/CD    | GitHub Actions · Gradle 9.3        |

## Repository Layout

```
src/
├── main/
│   ├── kotlin/
│   │   ├── Application.kt              # Entry point
│   │   ├── Routing.kt                  # Central route config + admin dashboard
│   │   ├── Serialization.kt            # JSON content negotiation setup
│   │   ├── database/                   # Models, repositories, DB setup
│   │   └── routes/
│   │       ├── customerRoutes.kt       # Auth, profiles, account management
│   │       ├── productRoutes.kt        # Products, stock, offsales, wastage
│   │       ├── orderRoutes.kt          # Basket, orders, substitutions
│   │       ├── stockRoutes.kt          # Stock levels, movements, alerts
│   │       ├── userRoutes.kt           # Staff/worker queries
│   │       ├── warehouseRoutes.kt      # Deliveries, picking, audits
│   │       └── managementRoutes.kt     # Dashboard, reports, staff admin
│   └── resources/
│       ├── db-seed-data/               # Seed data for development
│       ├── static/
│       │   ├── views/                  # HTML pages
│       │   ├── stylesheets/            # CSS
│       │   └── js/                     # Client-side JavaScript
│       ├── application.yaml            # Ktor server config
│       └── logback.xml                 # Logging config
└── test/
    └── kotlin/                         # Unit and integration tests
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
Delivery receiving with automatic stock updates, picking list generation, inventory overview, aisle location management, and stock audits with variance detection.

**Management:**
Dashboard statistics, sales and order reports, staff account CRUD, system-wide audit log.

---

---

## Our Development Strategy

**Branching**: feature branches (`feature/...`) merge into `implementation` via pull requests, then into `main` once tested and reviewed. We do not accept direct commits to `main`.

**Testing**: TODO

**Meetings**: Team meeting scheduled in lieu of sprint goals time periods along with occasional check up meetings, plus retrospectives after each sprint. All recorded on the wiki.


---






