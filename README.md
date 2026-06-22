# EatClub — Appium + TestNG + Cucumber + BrowserStack App Automate SDK

A cross-platform (Android **and** iOS) mobile UI automation framework built with:

| Layer | Choice |
|-------|--------|
| Driver | Appium Java Client 9.x (W3C, `UiAutomator2` / `XCUITest`) |
| Test runner | TestNG |
| BDD | Cucumber (Gherkin) |
| Cloud grid | BrowserStack **App Automate SDK** (zero-touch instrumentation via `-javaagent`) |
| Sample app | Wikipedia |
| Build | Maven |

The same step definitions and the same page objects run on both platforms — only
the locators (dual `@AndroidFindBy` / `@iOSXCUITFindBy` annotations) and the
`browserstack-*.yml` differ.

---

## How the BrowserStack SDK fits in

You **do not** write any BrowserStack-specific driver code. The SDK is attached
as a `-javaagent` (see `pom.xml` surefire config) and:

1. Reads the `browserstack.yml` pointed to by the `BROWSERSTACK_CONFIG_FILE` env var.
2. Intercepts `new AndroidDriver(...)` / `new IOSDriver(...)`, rewrites the hub URL,
   and merges capabilities (device, OS, app, build name, debugging artifacts).
3. Uploads the app, manages sessions, and reports results to the dashboard.

`DriverFactory` therefore passes **minimal** capabilities on the cloud path. The
exact same test code runs locally by flipping `EXECUTION_TARGET=local`.

```
browserstack.yml ──reads──► BrowserStack SDK (javaagent) ──intercepts──► AppiumDriver
       ▲                                                                      │
       └── BROWSERSTACK_CONFIG_FILE (set per Maven profile)                   ▼
                                                              session on BrowserStack cloud
```

---

## Project layout

```
.
├── pom.xml                      # deps, javaagent, android/ios profiles
├── testng.xml                   # suite + parallelism
├── browserstack-android.yml     # cloud caps for Android
├── browserstack-ios.yml         # cloud caps for iOS
├── .github/workflows/           # CI matrix (android + ios)
└── src
    ├── main/java/com/eatclub
    │   ├── config/ConfigManager.java     # sysprop > env > properties
    │   ├── driver/DriverFactory.java     # builds AndroidDriver/IOSDriver
    │   ├── driver/DriverManager.java     # ThreadLocal<AppiumDriver>
    │   ├── enums/Platform.java
    │   └── pages/
    │       ├── BasePage.java             # PageFactory + explicit waits
    │       └── SearchPage.java           # dual-locator page object
    └── test
        ├── java/com/eatclub
        │   ├── runners/TestNGRunner.java # Cucumber ↔ TestNG bridge (parallel)
        │   ├── hooks/Hooks.java          # driver lifecycle + BS status + screenshots
        │   └── stepdefs/SearchSteps.java
        └── resources
            ├── features/search.feature
            ├── config/local-*.properties # only for local runs
            └── cucumber.properties
```

---

## Prerequisites

- JDK 11+ and Maven 3.9+
- A BrowserStack account (App Automate)
- For **local** runs only: Appium 2.x server + Android SDK / Xcode

---

## Setup

Set credentials as environment variables (never commit them):

```bash
# macOS / Linux
export BROWSERSTACK_USERNAME="your_user"
export BROWSERSTACK_ACCESS_KEY="your_key"
```

```powershell
# Windows PowerShell
$env:BROWSERSTACK_USERNAME="your_user"
$env:BROWSERSTACK_ACCESS_KEY="your_key"
```

### App under test
- **Android**: `browserstack-android.yml` already points at BrowserStack's public
  Wikipedia sample APK — works out of the box.
- **iOS**: there is no public iOS Wikipedia `.ipa`. Upload your own signed build
  and put the returned `bs://<app-id>` into `browserstack-ios.yml`.

---

## Running

```bash
# Android on BrowserStack (default profile)
mvn clean test -Pandroid

# iOS on BrowserStack
mvn clean test -Pios

# Run a subset by Cucumber tag
mvn clean test -Pandroid -Dcucumber.filter.tags="@smoke"

# Run locally instead of the cloud (needs a running Appium server + device)
mvn clean test -Pandroid -Dexecution.target=local
```

Watch live sessions / video / logs on the **BrowserStack App Automate dashboard**.
Reports are also written to `target/cucumber-report/`.

---

## Best practices baked in

- **One yml per platform** — BrowserStack's recommended pattern for different
  app binaries (`.apk` vs `.ipa`).
- **SDK over manual capabilities** — less boilerplate, auto upload/report,
  Test Observability enabled.
- **ThreadLocal driver** — safe parallel scenarios (`@DataProvider(parallel=true)`).
- **Dual-locator page objects** — write each screen once for both platforms.
- **Explicit waits only** — no `Thread.sleep`; waits centralised in `BasePage`.
- **Config precedence** — system property → env var → properties file.
- **Fresh driver per scenario** — clean state, no test bleed-through.
- **Secrets via env vars** — `.gitignore` keeps `.env` and binaries out of git.
- **CI matrix** — Android and iOS run in parallel GitHub Actions jobs.

---

## SMS OTP login on a web page (reusable sample)

A configurable sample any project can adapt: launch the app session, switch to the
device's **Chrome** browser, drive a **web login page**, and complete an OTP login by
reading the **real SMS** delivered to a BrowserStack **SIM** device.

BrowserStack allocates a real, working SIM so the device receives a real SMS OTP
([SIM docs](https://www.browserstack.com/docs/app-automate/appium/sim-devices)).
The OTP is read straight from the SIM inbox via the `mobile: listSms` command.

**Flow:** read the device's SIM number → open the login page in Chrome → enter the
number → request OTP → read the OTP from the SIM's SMS → submit → assert accepted.

| Piece | File |
|-------|------|
| Read SIM phone number (`deviceInfo` executor) | `sim/BrowserStackSim.java` |
| Read OTP from the SIM inbox (`mobile: listSms`) | `sim/SmsReader.java` |
| Launch Chrome + webview context switching | `web/ChromeSession.java` |
| Generic web login page object | `pages/WebOtpLoginPage.java` |
| Per-site configuration (URL, locators, OTP length, sender) | `config/web-otp-login.properties` |
| Config loader | `config/WebLoginConfig.java` |
| Steps | `stepdefs/WebOtpLoginSteps.java` |
| Scenario (`@sim @otp`) | `features/web-otp-login.feature` |

**Adapt it to your app (no code changes):**
1. Edit `src/test/resources/config/web-otp-login.properties` — set `weblogin.url`
   and the element locators (`loginButton`, `mobileInput`, `requestOtpButton`,
   `otpBox`, `verifyButton`), the `otpLength`, and the `smsSender`. Inspect your
   page's elements with **BrowserStack App Live** or **Appium Inspector**. Any key
   can also be overridden on the CLI with `-Dweblogin.<key>=...`.
2. Run on a SIM device (Device Cloud Pro plan + SIM-capable device such as
   Galaxy S23/S24, Pixel 7/8/9):

```bash
mvn clean test -Pandroid -Dbstack.sim=true -Dcucumber.filter.tags="@otp"
```

`-Dbstack.sim=true` adds `enableSim` + `simOptions{region}` (default India; set
`-Dbstack.sim.region=...`). The `@sim` scenarios are **excluded by default**
(`cucumber.filter.tags=not @sim`) so the smoke build stays green.

> **Note:** public sites apply anti-abuse/rate limiting to OTP requests. If the site
> returns a block/error message (configurable via `weblogin.blockedText`), the test
> fails fast with that reason rather than a vague timeout.

---

## Apple Pay testing (iOS sample)

A template for automating an **Apple Pay** checkout on iOS. BrowserStack pre-loads a
sandbox test card + device passcode and handles the AssistiveTouch / side-button /
passcode confirmation; the test only drives the in-app button and two executor calls.
([Apple Pay docs](https://www.browserstack.com/docs/app-automate/appium/apple-pay))

**Flow:** open checkout → tap the in-app Apple Pay button (presents the sheet) →
set details via `applePayDetails` executor → confirm via `applePay` executor → assert
the app shows an order confirmation.

| Piece | File |
|-------|------|
| Apple Pay executor calls (set details, confirm) | `payments/ApplePay.java` |
| Shipping/billing/contact data (editable US sample) | `payments/ApplePayDetails.java` |
| Checkout page object (placeholder iOS locators) | `pages/ApplePayCheckoutPage.java` |
| Steps | `stepdefs/ApplePaySteps.java` |
| Scenario (`@applepay @ios`) | `features/apple-pay.feature` |
| Capabilities (`enableApplePay`, `resignApp:false`, `nativeWebTap`) | `driver/DriverFactory.java` (gated by `-Dbstack.applePay=true`) |

**Wire it to your app:**
1. Provide an iOS app signed via the **Apple Developer Enterprise Program** with the
   **Apple Pay entitlement** — `-Dbstack.app=bs://<your-app-id>` (`resignApp` stays
   `false` to preserve the entitlement).
2. Replace the placeholder locators in `ApplePayCheckoutPage.java`, and edit the test
   data in `ApplePayDetails.sample()`.
3. Run on an Apple Pay-capable iOS device (iPhone 13 Pro→16; iOS 15→18). Apple Pay is
   a **private beta** on Device Cloud Pro+ — request access first.

```bash
mvn clean test -Pios -Dbstack.applePay=true -Dbstack.app=bs://<your-app> \
    -Dcucumber.filter.tags="@applepay"
```

`-Dbstack.applePay=true` adds `enableApplePay`, `resignApp:false`,
`applePayPreferredNetworks`, and `appium:nativeWebTap`. The `@applepay` scenarios are
**excluded by default** (`cucumber.filter.tags=not @sim and not @applepay`). Note the
session start is ~100–180s slower with Apple Pay enabled.

---

## Extending

1. Add a `.feature` file under `src/test/resources/features`.
2. Add a page object under `pages/` with dual `@AndroidFindBy`/`@iOSXCUITFindBy` locators.
3. Add step definitions under `stepdefs/`.
4. Add more devices to the `platforms:` list in the yml for wider coverage.
