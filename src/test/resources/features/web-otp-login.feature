@sim @otp
Feature: SMS OTP login on a web page via Chrome
  As a tester whose login happens on a web page (no native OTP screen)
  I want to launch the app, switch to Chrome, and complete a web OTP login
  So that I can verify real SMS-OTP delivery on a BrowserStack SIM device

  # Reusable sample/template. Point it at YOUR login page by editing
  #   src/test/resources/config/web-otp-login.properties
  # (URL + element locators + OTP length + SMS sender), or override any key with
  # -Dweblogin.<key>=...  Requires a BrowserStack SIM-enabled device on a Device
  # Cloud Pro plan. Excluded from the default suite (tagged @sim). Run with:
  #   mvn test -Pandroid -Dbstack.sim=true -Dcucumber.filter.tags="@otp"

  Scenario: Request and submit a web SMS OTP
    Given the mobile app is launched
    When I open the login page in Chrome
    And I request an OTP for the device's SIM number
    And I read the SMS OTP and submit it
    Then the OTP should be accepted
