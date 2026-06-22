package com.eatclub.runners;

import org.testng.annotations.DataProvider;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Entry point that bridges Cucumber and TestNG.
 *
 * Extending {@link AbstractTestNGCucumberTests} lets the BrowserStack SDK and
 * TestNG drive Cucumber scenarios. Overriding the scenarios DataProvider with
 * parallel=true enables scenario-level parallelism (paired with
 * data-provider-thread-count in testng.xml).
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.eatclub.stepdefs", "com.eatclub.hooks"},
        plugin = {
                "pretty",
                "html:target/cucumber-report/cucumber.html",
                "json:target/cucumber-report/cucumber.json",
                "junit:target/cucumber-report/cucumber.xml"
        },
        monochrome = true
)
public class TestNGRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
