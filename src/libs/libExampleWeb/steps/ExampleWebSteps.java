package libs.libExampleWeb.steps;

import libs.libCore.modules.BaseSteps;
import libs.libCore.modules.Log;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

public class ExampleWebSteps extends BaseSteps {


    /**
     * Opens web page with url taken from environment configuration
     */
    @When("^I open seleniumframework website$")
    public void i_open_seleniumframework_website() {
        String url = Storage.get("Environment.Active.Web.url");
        Log.debug("Going to open " + url);
        PageCore.open(url);
    }


    /**
     * Verifies page title and url
     * Expected values are hardcode in the step
     */
    @Then("^I validate title and URL$")
    public void i_print_title_and_URL(){
        Log.debug("Going to validate an Url");
        Log.debug("Page title is " + PageCore.getTitle());
        Log.debug("Current url is " + PageCore.getCurrentUrl());
        if ( ! PageCore.getTitle().equals("Google") ) {
            Log.error("Wrong title provided. Expected Google" +
                " but was " + PageCore.getTitle() );
        }
        if ( ! PageCore.getCurrentUrl().contains("www.google.pl") ) {
            Log.error("Wrong url. It shall contain www.google.pl but was " +
                    PageCore.getCurrentUrl() );
        }
    }


    /**
     * Opens web page with url taken from environment configuration
     */
    @When("^I open google page$")
    public void i_open_google_website(){
        String url = Storage.get("Environment.Active.Web.url");
        Log.debug("Going to open " + url);
        PageCore.open(url);
    }


    /**
     * Check if input element exists
     * Expected element selector is hardcoded and points to google search box
     */
    @Then("^I check for input element$")
    public void i_check_for_input_element() {
        Log.debug("Going to locate input element");
        WebElement element = PageCore.findElement(By.id("lst-ib"));
    }


    /**
     * Enters text into the search box
     *
     */
    @Then("^I search for text$")
    public void i_search_for(){
        Log.debug("Going to search for");
        WebElement element = PageCore.findElement(By.id("lst-ib"));

        String sVal = Storage.get("TestData.search_sentence");

        Log.debug("Entering text " + sVal);
        element.sendKeys(sVal);
        element.submit();
    }

    /**
     * Enters text {} into the search box
     */
    @Then("^I search for text (.*)$")
    public void i_search_for2(String input){
        Log.debug("Going to search for");
        WebElement element = PageCore.findElement(By.id("lst-ib"));

        Integer iVal = StepCore.checkIfInputIsVariable(input);

        String sVal = iVal.toString();

        Log.debug("Show me sVal " + sVal);

        element.sendKeys(sVal);
        element.submit();
    }


    /**
     * Attaches sample file to the report
     */
    @Then("^attach sample file to report$")
    public void attache_file_to_report(){
        Log.debug("Going to attach file");
        StepCore.attachFileToReport("SimpleTextAttachment", "text/plain", "C:\\Users\\akowa\\Documents\\przykladowy_plik_tekstowt.txt");
        StepCore.attachFileToReport("PdfAttachment", "application/pdf", "C:\\Users\\akowa\\Documents\\API_design.pdf");
        StepCore.attachMessageToReport("Some name", "Some random message");
    }


}