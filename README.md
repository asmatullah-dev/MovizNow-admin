# MovizNow Admin (Android Kotlin + Jetpack Compose)

Native Android admin application located separately under `/android/admin`, leaving the web application completely untouched.

## Firebase & Backend Binding
- **Firebase Project**: `app-moviznow`
- **Firestore Database ID**: `moviznow-app`
- **Application ID**: `com.moviznow.admin`
- **Backend Server URL**: `https://moviznow.com` (adjustable inside **Settings & Sync** in the app)

`MovizNowFirebase.kt` automatically initializes Firebase with `app-moviznow` and binds Firestore explicitly to `moviznow-app` so the project compiles and connects even before adding `google-services.json`. If you download an Android `google-services.json` for `com.moviznow.admin` from Firebase Console and place it at `/android/admin/app/google-services.json`, Gradle applies the `com.google.gms.google-services` plugin automatically.

## Included Admin Features
- **Authentication & Role-Based Access**: Supports `owner`, `admin`, `manager`, `user_manager`, and `content_manager` roles matching `firestore.rules`.
- **Dashboard**: Live KPIs (users, VIP count, catalog size, pending orders, revenue, movie requests, reported links) + pending sync alert bar.
- **Users & Membership**: Search, status/tier/Whitelisted/ConShare filters, Web/Android/FCM/Whitelisted/ConShare badges, edit role/status/expiry (`+1M`, `+3M`, `+6M`, `+1Y`, `Lifetime`), ConShare toggle, phone whitelisting (`settings/whitelisted_phones`), Selected Content assignment, WhatsApp/Email actions, and queued batch **Sync Pending Changes** (`chunk_meta/versions`).
- **Orders**: Pending/Approved/Declined/All filter, payment receipt preview, Approve (auto-extends PKT membership expiry or unlocks assigned content + notifies user), Decline, and Gemini AI Order Verification (`/api/orders/admin-verify-order`).
- **Content, Collections & Taxonomy**: Full `content_chunks` codec (`minifyContent`/`expandContent`), Add/Edit Movies & Series, TMDB autofill (`/api/tmdb/search/multi`), download links & seasons management, Collections, Genres, Languages, and Qualities.
- **Requests & Reported Links**: Aggregated user movie requests (Mark Added / Reject), broken link reports, and live link health checker (`/api/check-link`).
- **Notifications**: Broadcast/targeted push & email composer (`/api/notifications/send`), templates CRUD, history, and automated expiry check trigger (`/api/notifications/check-expiry`).
- **Analytics & Income**: Most clicked content, top active users by sessions & watch time, and `income/data` ledger CRUD.
- **App Settings & DB Sync**: Edit `settings/app_settings` toggles/pricing and run multi-database sync (`/api/sync/status`, `/api/sync/push`).

## Building Separately for Android
Open `/android/admin` in **Android Studio**, let Gradle sync, and click **Run** or **Build -> Build APK(s)**, or run from `/android/admin`:
```bash
gradle assembleDebug
```
