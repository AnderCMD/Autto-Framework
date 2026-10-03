package io.github.andercmd.autto.features.showcase;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.datatable.DataTable;
import io.cucumber.docstring.DocString;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.andercmd.autto.core.report.Report;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Steps of the {@code @showcase} feature: they exercise every report capability without a browser. */
public class ReportShowcaseSteps {

    private Scenario scenario;
    private List<Map<String, String>> browsers;
    private String payload;

    @Before("@showcase")
    public void keepScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    @Given("a data table with the supported browsers:")
    public void aDataTableWithTheSupportedBrowsers(DataTable table) {
        browsers = table.asMaps();
    }

    @Given("the following JSON payload:")
    public void theFollowingJsonPayload(DocString json) {
        payload = json.getContent();
    }

    @When("the step writes logs, tables and attachments")
    public void theStepWritesLogsTablesAndAttachments() {
        Report.info("Plain information message");
        Report.pass("Highlighted success message");
        Report.warning("Highlighted warning message");
        Report.table("Browsers", browsers.stream().map(row -> List.of(row.get("browser"), row.get("engine"))).toList());
        Report.json("Payload", payload);
        Report.code("Multiline text", "line 1\nline 2\nline 3");
        scenario.log("Message written with scenario.log()");
        scenario.attach(payload.getBytes(StandardCharsets.UTF_8), "application/json", "payload.json");
        scenario.attach("id,name\n1,Autto\n".getBytes(StandardCharsets.UTF_8), "text/csv", "data.csv");
    }

    @Then("the report contains them")
    public void theReportContainsThem() {
        assertThat(Report.isActive()).as("a scenario is being reported").isTrue();
        assertThat(browsers).hasSize(4);
    }
}
