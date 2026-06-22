@applepay @ios
Feature: Apple Pay checkout (iOS sample)
  As a tester of an Apple Pay-enabled iOS app
  I want to complete an Apple Pay transaction on a BrowserStack device
  So that I can verify the in-app payment flow end to end

  # SAMPLE / TEMPLATE - wire it to your app:
  #  - iOS app signed via the Apple Developer Enterprise Program with the Apple Pay
  #    entitlement. Provide it with -Dbstack.app=bs://<your-app-id> (resignApp is
  #    left false so the entitlement is preserved).
  #  - Enable Apple Pay on the session with -Dbstack.applePay=true (Device Cloud Pro
  #    plan; Apple Pay is a private beta - request access from BrowserStack).
  #  - Replace the placeholder iOS locators in ApplePayCheckoutPage.java.
  # BrowserStack pre-loads a sandbox test card + device passcode and handles the
  # AssistiveTouch / side-button / passcode confirmation automatically.
  # Excluded from default runs (tagged @applepay). Run with:
  #   mvn test -Pios -Dbstack.applePay=true -Dbstack.app=bs://<your-app> \
  #       -Dcucumber.filter.tags="@applepay"

  Scenario: Complete an Apple Pay payment
    Given the mobile app is launched
    When I start checkout and tap the Apple Pay button
    And I provide Apple Pay shipping, billing and contact details
    And I confirm the Apple Pay payment
    Then the payment should be successful
