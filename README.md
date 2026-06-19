# Billing App

Offline-first billing application for small shops with real-time cross-device sync, Google Sign-In, PDF invoice generation, and member management.

## Features

- **Authentication**: Email/password & Google Sign-In, QR-based shop joining
- **Dashboard**: Real-time stats (items, bills, sales, customers), quick bill creation
- **Billing**: Create itemized bills with credit/payment tracking, print/share PDF invoices
- **Customer Ledger**: Per-customer transaction history with pending/credit display
- **Inventory**: Item management with category filtering, search, and pricing
- **Bill History**: Searchable bill archive with date range filtering, batch delete
- **Real-time Sync**: Automatic two-way sync across all devices via Firebase Firestore
- **Background Sync**: Foreground service keeps data in sync even when app is backgrounded
- **Member Management**: Role-based access (Owner/Admin/Member), ownership transfer
- **Offline-first**: All data stored locally in Room DB; sync happens asynchronously
- **App Updates**: Built-in update checker and APK downloader from GitHub releases
- **Backup/Restore**: Export/import full database as ZIP

## Role System

| Capability | Owner | Admin | Member |
|---|---|---|---|
| Edit shop info | ✓ | ✓ | ✗ |
| Add/edit items | ✓ | ✓ | ✗ |
| Delete items | ✓ | ✓ | ✗ |
| Delete contacts | ✓ | ✓ | ✗ |
| Clear payment history | ✓ | ✓ | ✗ |
| Manage members | ✓ | ✓ | ✗ |
| Transfer ownership | ✓ | ✗ | ✗ |
| Backup & restore | ✓ | ✓ | ✗ |
| View real-time log | ✓ | ✓ | ✓ |
| Create bills | ✓ | ✓ | ✓ |

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material3
- **Architecture**: MVVM with Hilt DI
- **Local DB**: Room (SQLite)
- **Backend**: Firebase Firestore + Firebase Auth
- **Sync**: Firestore snapshot listeners + Foreground Service
- **Auth**: Firebase Auth (email/password, Google Sign-In)
- **PDF**: Custom PdfGenerator
- **QR**: ZXing barcode scanner
- **Build**: Gradle KTS, Kotlin 1.9.22, Java 17

## Setup

1. Clone the repo
2. Add your `google-services.json` to `app/`
3. Enable Firebase Auth (Email/Password + Google) and Firestore in your Firebase console
4. Build & run:
   ```
   ./gradlew assembleDebug
   ```

## Build Variants

| Variant | Use |
|---|---|
| `debug` | Development, debuggable |
| `release` | Production (signing config not included) |

## Versioning

Format: `DD.M.YY` (e.g., `19.6.26` = June 19, 2026)
