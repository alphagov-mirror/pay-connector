package uk.gov.pay.connector.it;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.ValidatableResponse;
import org.junit.Rule;
import org.junit.Test;
import uk.gov.pay.connector.util.DropwizardAppWithPostgresRule;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.connector.util.JsonEncoder.toJson;

public class GatewayAccountResourceITest {

    @Rule
    public DropwizardAppWithPostgresRule app = new DropwizardAppWithPostgresRule();

    @Test
    public void createGatewayAccount() throws Exception {
        String testName = "test account";
        ValidatableResponse response = given().port(app.getLocalPort())
                .contentType(JSON)
                .body(toJson(ImmutableMap.of("name", testName)))
                .post("/v1/api/accounts")
                .then()
                .statusCode(201)
                .contentType(JSON);

        String accountId = response.extract().path("gateway_account_id");
        String urlSlug = "api/gateway/" + accountId;

        response.header("Location", containsString(urlSlug))
                .body("links[0].href", containsString(urlSlug))
                .body("links[0].rel", is("self"))
                .body("links[0].method", is("GET"));
    }

    @Test
    public void createGatewayAccountWithMissingNameFails() throws Exception {
        given().port(app.getLocalPort())
                .contentType(JSON)
                .body("{}")
                .post("/v1/api/accounts")
                .then()
                .statusCode(400)
                .contentType(JSON)
                .body("message", is("Missing fields: name"));
    }
}
