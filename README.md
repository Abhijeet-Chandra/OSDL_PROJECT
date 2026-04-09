# OSDL_PROJECT

### README (copy/paste)

## Hotel Management System (JavaFX + SQLite)

A desktop **Hotel Management** application built with **JavaFX (FXML + CSS)**, **Maven**, and **SQLite (JDBC)**.

It supports:
- **Admin**: manage Rooms and Extras (services), view bookings and bill history, dashboard analytics
- **Customer/Guest**: **Sign up**, log in, **book rooms**, view own bookings and bills

---

## Features

### Admin
- **Dashboard**: KPIs + charts
  - PieChart: room occupancy (Available / Occupied / Maintenance)
  - BarChart: last 7 days revenue
- **Rooms**: add/update/delete rooms, status management
- **Extras (Services)**: add/update/delete extra services
- **Bookings**: view bookings; check-out/cancel from bookings screen
- **Bill History**: view bill list and details

### Customer/Guest
- **Sign up**: create account with name, phone, username, password
- **Book room**: select room + dates, add extra services, generate bill
- **My bookings / My bills**: customer-only views

---

## Tech Stack
- **Java** + **JavaFX**
- **FXML** (Scene Builder compatible) + **CSS** styling
- **Maven**
- **SQLite** database using **JDBC**
- JavaFX charts: **PieChart**, **BarChart**

---

## Project Structure (high level)
- `src/main/java/com/hotel/controller/` – JavaFX controllers (UI logic)
- `src/main/java/com/hotel/model/` – model classes + enums
- `src/main/java/com/hotel/dao/` – DAO classes (JDBC/SQLite persistence)
- `src/main/java/com/hotel/service/` – business logic (billing + booking workflow, synchronization)
- `src/main/resources/com/hotel/` – FXML screens (UI)
- `src/main/resources/css/theme.css` – global UI styling
- `data/hotel.db` – SQLite database file (created automatically)

---

## Roles & Login
- **Admin account (fixed)**:
  - Username: `admin`
  - Password: `admin123`

- **Customer accounts**:
  - Use **Sign up** on the login screen to create new customers.

Passwords are stored as **SHA-256 hashes** in SQLite.

---

## Threading (Multithreading)
To keep the UI responsive, database operations run in background threads using **`javafx.concurrent.Task`** in:
- `LoginController` (authenticate)
- `SignupController` (create account)
- `BillingController` (book + save bill)
- `BookingsController` (check-out/cancel actions)

---

## Thread Synchronization (`synchronized`)
To prevent **double booking** under concurrent requests:
- Per-room locking is implemented using `synchronized` in:
  - `com.hotel.service.RoomLockRegistry`
  - `com.hotel.service.BookingWorkflowService` (`synchronized (roomLock)`)

The booking workflow is executed inside a **single DB transaction**.

---

## SQLite Reliability
To reduce `[SQLITE_BUSY] database is locked` errors:
- SQLite is configured with:
  - `PRAGMA journal_mode = WAL`
  - `PRAGMA busy_timeout = 5000`
  - `PRAGMA synchronous = NORMAL`
in `com.hotel.dao.Database`.

---

## How to Run

### Prerequisites
- Java installed
- Maven installed

### Run (from `hotel-management` folder)
```powershell
mvn clean javafx:run
```

---

## How to Test Synchronization (Real-time)
1. Start the app **twice** (two terminals):
```powershell
mvn javafx:run
```
(run again in another terminal)

2. Create **two different customer accounts** (Sign up).
3. In both app windows, try booking the **same room** at the same time.

Expected: one succeeds, the other shows **“Room is not available (current status: OCCUPIED)”**.

---

## Notes / Limitations
- Customer bill filtering uses the stored guest label; can be improved by directly linking bills to guest IDs.
- No password reset feature (demo scope).

---

## Future Enhancements
- Guest profile edit page
- Stronger billing-to-guest linkage (guest_id in bill table)
- Invoice export (PDF) and email
- More analytics (monthly occupancy, top services)
