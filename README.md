# OneSense

Android home-screen widgets that show your **AdSense estimated earnings** — today, yesterday,
last 7 days, this month. No account, no server, no tracking of the user — the app talks
straight to Google.

**No ads, no analytics, no tracking, no accounts, no background services — just a tiny app
that does what it says.**

- Sign-in: Google OAuth (PKCE, no client secret), scope `adsense.readonly`
- Data: AdSense Management API v2, `reports:generate` with metric `ESTIMATED_EARNINGS`;
  currency comes from the report header (EUR → "€")
- Main screen: tap the card to cycle Today / Yesterday / Last 7 days / This month
- 1×1 widget: pick which period it shows when you place it; label under the number is the
  period name
- 1×4 widget: all four periods side by side in one row
- Refresh: `JobScheduler` on your chosen interval + tap any widget to refresh now
- Multiple widgets, each with its own period and colour palette
- No third-party libraries, `minSdk 26`

Earnings are Google's estimates until roughly the 21st of the following month (AdSense rule).

## Build

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
gradle assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
```

The OAuth client ID lives in `gradle.properties` (`adsenseClientId=...`); the redirect scheme
is derived from it automatically.

### Signing

The keystore and its passwords are **not** in this repository. Put them in your local
`~/.gradle/gradle.properties`:

```properties
KEYSTORE_FILE=/path/to/onesense.keystore
KEYSTORE_PASSWORD=...
KEY_PASSWORD=...
```

Without those three properties the release build still works, it is just unsigned.

## Google Cloud setup (once)

1. console.cloud.google.com → new project (e.g. `onesense`)
2. APIs & Services → Library → enable **AdSense Management API**
3. OAuth consent screen → External → app name "OneSense", test user = your Gmail
4. Credentials → Create → **OAuth client ID** → type **iOS**
   - Bundle ID: `rs.racic.onesense`
   (iOS clients are used because Android clients no longer offer a custom URI scheme
   redirect in the new Google Auth Platform; the flow is still plain PKCE)
5. Put the client ID into `gradle.properties` → rebuild
6. No extra setup in AdSense — you sign in with your own Google account that owns the
   AdSense account.

## Notes

- `adsense.readonly` is a Google **restricted** scope: a public Play release requires OAuth
  verification of the app, internal/external testing tracks do not.
- Nothing leaves the phone except the requests to Google APIs; tokens are stored only in the
  app's private storage.
