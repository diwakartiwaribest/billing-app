# Anchored Summary — Billing App

## Goal
Offline-first billing app with Room local database, Supabase backend, real-time cross-device sync, WorkManager sync engine, PDF invoice generation, in-app APK update/install, and persistent config that survives uninstall/reinstall.

## Constraints & Preferences
- Room database as local cache and offline write store; UI reads only from Room via Repository Flows.
- Supabase PostgreSQL as authoritative source of truth; sync happens asynchronously via SyncEngine.
- Real-time updates via broadcast channels (not postgres_changes) to avoid self-triggering.
- Transaction list: newest-first with payments before bills for equal timestamps.
- Payment push to Supabase before local state update to prevent race conditions.
- Payment deletion by Supabase `uuid` column.
- PDF naming: `DUES_INVOICE_MobileNo_FirstName_LastName.pdf`; merged table with invoice number once per credit bill; blue sub-header row per bill; reduced row height; Balance Due in blue; Amount Paid in light green.
- Pending invoice PDF: FIFO bill-payment allocation; exclude fully-paid bills.
- Settled PDF: "NO DUES" in rounded light-blue box; overpaid shows "Amount Credited" with `+` prefix.
- Customer ledger: show pending, credit (`+`), or ₹0 Settled; never `totalSpent`.
- API and WebSocket status indicators reflect actual connection state.
- App update from GitHub releases checked on startup and every 24h; white download icon only when update available.
- Non-scrollable home screen; log window fills remaining space with no auto-scroll text.
- In-app APK downloader with progress overlay; cached APK launches installer directly without re-download.
- Config stored in Supabase `shops` table; survives uninstall/reinstall via login-triggered restore.
- All phone fields: digits only, exactly 10 characters.
- Owners can edit shop code/secret in Settings.
- Customer creation from Customer Ledger via PersonAdd icon.
- Cross-device sync uses WebSocket broadcast (not postgres_changes, not polling).
- Home screen stat cards use solid blue icon backgrounds (no gradient).
- All CRUD operations write to Room first; synchronization is asynchronous.
- Soft deletes only; records marked `deleted = true`, never hard-deleted.
- UUID v4 primary keys generated on client; no auto-increment IDs.
- SyncWorker runs with `NetworkType.CONNECTED` constraint, exponential backoff, unique work policy.

## Progress

### Done

**PDF & UI:**
- `generatePendingInvoicePdf` with merged table, blue sub-headers, FIFO allocation, settled "NO DUES" / overpaid row.
- Non-scrollable home screen; log window fills remaining space with no auto-scroll text.
- Home screen icon styling — solid blue (`Blue227ed4`) backgrounds for stat card icons and New Bill card; larger 56dp icons, 20sp/12sp fonts.
- "Generate Pending Invoice" button — light orange filled button (`#FFF0E6` bg, `#E65100` text).
- Database Manager redesigned — scrollable chip tab bar with color-coded SummaryBar styling (solid blue theme), all 6 tabs with improved card designs, status badges, circular icons.
- Removed auto-scroll & pulse animation from HomeScreen.

**Room Database & Repositories:**
- `AppDatabase` with entities for Product, Customer, Invoice, InvoiceItem, CustomerPayment; all with UUID PK, `SyncStatus` enum, soft-delete, version, owner_id; DAOs with `observeAll()` returning `Flow`, delta-since queries; TypeConverters for Instant and SyncStatus.
- Repository layer — ProductRepository, CustomerRepository, InvoiceRepository, CustomerPaymentRepository; each writes to Room immediately and enqueues sync.
- `DatabaseModule` provides all DAOs via Hilt `@Singleton`.

**SyncEngine:**
- Singleton with push→pull→merge phases; pushes local pending changes first; pulls remote changes with conflict resolution (local pending changes never overwritten).
- `lastSyncTimestamp` initializes to `0L` for full first pull; `since` captured before push phase to avoid re-fetching own records.
- Delta sync uses `created_at` filter (since Supabase tables lack `updated_at`).
- Push methods propagate HTTP errors (no silent try-catch) — `SyncEngine` marks records as `FAILED` for retry.

**SyncWorker & WorkManager:**
- `CoroutineWorker` with `NetworkType.CONNECTED` constraint, exponential backoff (30s initial, max 3 retries).
- `enqueueOneTime()` for on-demand sync, `enqueuePeriodic()` for 15-minute interval.
- `ExistingWorkPolicy.KEEP` for one-time work — prevents `JobCancellationException` from duplicate enqueue.
- `BillingWorkerFactory` creates `SyncWorker` with injected `SyncEngine` directly (no Hilt annotation processing needed).
- `BillingApp.onCreate()` calls `WorkManager.initialize()` with `BillingWorkerFactory`; catches `IllegalStateException` if already initialized.
- Manifest `WorkManagerInitializer` removed via `tools:node="remove"`.

**Cross-Device Sync:**
- `SupabaseRealtimeClient` — OkHttp WebSocket with Phoenix Channels; subscribes to all 5 tables + `realtime:sync` broadcast channel.
- Broadcast-based sync: `notifyChange()` sends broadcast on `realtime:sync` channel; all connected clients receive and trigger sync.
- `notifyChange()` only calls `sendBroadcast()` — no local `_events.tryEmit()` (prevents circular loop).
- `subscribeToTable()` uses empty `JSONArray()` for postgres_changes (prevents self-triggering).

**ViewModel Migration:**
- All ViewModels (ItemsViewModel, HistoryViewModel, CustomerLedgerViewModel, CustomerDetailViewModel, NewBillViewModel, HomeViewModel, BillDetailViewModel) now read from Room via Repository `observeAll()`/`observeById()` Flows.
- `HomeViewModel` observes Room counts for `itemCount`, `billCount`, `customerCount`, `totalSales`; triggers `SyncWorker` on startup and on realtime events; periodic sync every 15 minutes.
- `BillDetailViewModel` injects `InvoiceRepository` instead of `SupabaseClient`.

**API & Status Monitoring:**
- API status check: `shop_items?shop_code=eq.TEST&limit=0&select=id` with `Accept: application/json`.
- WebSocket status: `SupabaseRealtimeClient.connected` StateFlow driven by `onOpen`/`onClosed`/`onFailure`.

**Config Persistence:**
- Config stored in `shops` table; `AuthViewModel.lookupAndRestoreConfig()` runs before auth state; `HomeViewModel.restoreConfigIfNeeded()` restores config on clean DataStore.
- Non-blocking management API — `enableRealtimePublication()` is fire-and-forget.

**Cleanup:**
- `AppDataCache` deleted; `DatabaseManagerViewModel` injects 5 Room DAOs instead.
- `SettingsViewModel` no longer injects `AppDataCache`; `loadDbStats()` removed cache read/write.

**Schema Migration:**
- Code updated: `BillItem` model has `createdAt`; push/pull methods include `created_at` for bill_items; `syncEngine` passes `createdAt` when merging.

**Bug Fixes:**
- "Bill not found" error fixed — `BillDetailViewModel` reads from Room, not Supabase.
- Circular sync loop fixed — removed `postgres_changes` subscriptions and local `_events.tryEmit()`.
- Push error swallowing fixed — all `SupabaseClient` push/delete methods propagate HTTP errors.
- `JobCancellationException` fixed — `ExistingWorkPolicy.REPLACE` → `KEEP`.

## Key Decisions

| Decision | Rationale |
|----------|-----------|
| Room over volatile cache | ACID transactions, reactive Flows, automatic UI updates |
| Broadcast over postgres_changes | postgres_changes causes self-triggering; broadcast only receives from other devices |
| `notifyChange` no local emit | Room Flows already update UI; prevents circular loop |
| Push methods throw on failure | Silent catch caused records marked SYNCED even when HTTP failed |
| Delta sync uses `created_at` | Tables lack `updated_at`; schema migration pending for proper column |
| `ExistingWorkPolicy.KEEP` | Prevents new enqueue from canceling running worker |
| Push before pull | Prevents lost writes |
| `since` captured before push | Pulled changes don't include records just pushed locally |
| Conflict resolution (local wins) | Local pending changes never overwritten by remote |
| Manual WorkerFactory over `@HiltWorker` | Hilt annotation processing didn't generate bindings reliably |
| Soft deletes with tombstones | Prevents reappearance during sync |
| UUID v4 client-generated PKs | Prevents ID collisions across devices |
| Solid blue over gradient icons | Matches screenshot design |

## Next Steps
- Add `updated_at` column to all 5 Supabase tables for proper delta sync
- Generate signed release APK and upload to GitHub release
- Test complete update flow (install v1.0.0, detect update, download, install)
- Test config restore on completely fresh device after login

## Critical Context
- Actual Supabase table names: `shop_items`, `bills`, `bill_items`, `customers`, `customer_payments`
- Cross-device sync via `realtime:sync` broadcast channel (no postgres_changes)
- `SyncEngine.fullSync()`: push→pull→merge; delta sync via `created_at` filter
- Room file: `billing_room.db`; `fallbackToDestructiveMigration()` enabled (schema version 1)
- Current build: `versionCode = 20260615`, `versionName = "15.6.26"`
- GitHub: `diwakartiwaribest/billing-app`
- `supabase_schema.sql` is intended/ideal schema; actual Supabase tables differ
- API health check hits `shop_items` endpoint — always returns 200
- WebSocket status: `SupabaseRealtimeClient.connected` StateFlow

## Relevant Files
- `app/src/main/java/com/shop/billing/data/local/` — Room entities, DAOs, AppDatabase, TypeConverters
- `app/src/main/java/com/shop/billing/data/sync/` — SyncEngine, SyncWorker, BillingWorkerFactory
- `app/src/main/java/com/shop/billing/data/repository/` — ProductRepository, CustomerRepository, InvoiceRepository, CustomerPaymentRepository
- `app/src/main/java/com/shop/billing/data/di/DatabaseModule.kt` — Hilt module
- `app/src/main/java/com/shop/billing/data/remote/SupabaseClient.kt` — REST API calls with delta sync
- `app/src/main/java/com/shop/billing/data/remote/SupabaseRealtimeClient.kt` — WebSocket realtime + broadcast
- `app/src/main/java/com/shop/billing/BillingApp.kt` — Hilt app + WorkManager init
- `app/src/main/AndroidManifest.xml` — WorkManagerInitializer removed
- `app/src/main/java/com/shop/billing/ui/screens/home/HomeViewModel.kt` — Room counts, sync trigger, config restore
- `app/src/main/java/com/shop/billing/ui/screens/billdetail/BillDetailViewModel.kt` — Room-based bill loading
- `app/src/main/java/com/shop/billing/data/local/entity/InvoiceItemEntity.kt` — `toBillItem()` with `createdAt`
- `app/src/main/java/com/shop/billing/data/model/BillItem.kt` — DTO with `createdAt` field
- `supabase_schema.sql` — Full ideal schema reference
