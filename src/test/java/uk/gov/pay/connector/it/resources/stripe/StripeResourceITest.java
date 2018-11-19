package uk.gov.pay.connector.it.resources.stripe;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.pay.connector.app.ConnectorApp;
import uk.gov.pay.connector.charge.model.domain.ChargeStatus;
import uk.gov.pay.connector.gateway.PaymentGatewayName;
import uk.gov.pay.connector.junit.DropwizardConfig;
import uk.gov.pay.connector.junit.DropwizardJUnitRunner;
import uk.gov.pay.connector.junit.DropwizardTestContext;
import uk.gov.pay.connector.junit.TestContext;
import uk.gov.pay.connector.rules.StripeMockClient;
import uk.gov.pay.connector.util.DatabaseTestHelper;
import uk.gov.pay.connector.util.RestAssuredClient;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.AUTHORISATION_3DS_REQUIRED;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.AUTHORISATION_SUCCESS;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.CAPTURE_APPROVED;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.ENTERING_CARD_DETAILS;
import static uk.gov.pay.connector.it.JsonRequestHelper.buildJsonAuthorisationDetailsFor;
import static uk.gov.pay.connector.junit.DropwizardJUnitRunner.WIREMOCK_PORT;

@RunWith(DropwizardJUnitRunner.class)
@DropwizardConfig(app = ConnectorApp.class, config = "config/test-it-config.yaml")
public class StripeResourceITest {

    private static final String CVC = "123";
    private static final String EXP_MONTH = "11";
    private static final String EXP_YEAR = "99";
    private static final String CARD_NUMBER = "4444333322221111";
    private static final String AMOUNT = "6234";
    private static final String DESCRIPTION = "Test description";

    protected RestAssuredClient connectorRestApiClient;

    private String stripeAccountId;
    private String validAuthorisationDetails = buildJsonAuthorisationDetailsFor("4444333322221111", CVC, EXP_MONTH + "/" + EXP_YEAR, "visa");
    private String paymentProvider = PaymentGatewayName.STRIPE.getName();
    private String accountId;
    private StripeMockClient stripeMockClient = new StripeMockClient();
    private DatabaseTestHelper databaseTestHelper;

    @DropwizardTestContext
    private TestContext testContext;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);

    @Before
    public void setup() {
        stripeAccountId = String.valueOf(RandomUtils.nextInt());
        databaseTestHelper = testContext.getDatabaseTestHelper();
        accountId = String.valueOf(RandomUtils.nextInt());

        stripeMockClient.mockCreateToken();
        stripeMockClient.mockCreateSource();
        stripeMockClient.mockCreateCharge();

        connectorRestApiClient = new RestAssuredClient(testContext.getPort(), accountId);
    }

    @Test
    public void authoriseCharge() {
        addGatewayAccount(ImmutableMap.of("stripe_account_id", stripeAccountId));

        String externalChargeId = addCharge();

        given().port(testContext.getPort())
                .contentType(JSON)
                .body(validAuthorisationDetails)
                .post(authoriseChargeUrlFor(externalChargeId))
                .then()
                .body("status", is(AUTHORISATION_SUCCESS.toString()))
                .statusCode(200);

        verify(postRequestedFor(urlEqualTo("/v1/tokens"))
                .withHeader("Content-Type", equalTo(APPLICATION_FORM_URLENCODED))
                .withRequestBody(matching(constructExpectedTokensRequestBody())));

        verify(postRequestedFor(urlEqualTo("/v1/sources"))
                .withHeader("Content-Type", equalTo(APPLICATION_FORM_URLENCODED))
                .withRequestBody(matching(constructExpectedSourcesRequestBody())));

        verify(postRequestedFor(urlEqualTo("/v1/charges"))
                .withHeader("Content-Type", equalTo(APPLICATION_FORM_URLENCODED)));

        List<LoggedRequest> requests = findAll(postRequestedFor(urlMatching("/v1/charges")));
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getBodyAsString()).isEqualTo(constructExpectedAuthoriseRequestBody());
    }

    @Test
    public void shouldRespondAs3dsRequired_whenAuthorisationRequires3ds() {
        addGatewayAccount(ImmutableMap.of("stripe_account_id", stripeAccountId));

        stripeMockClient.mockCreateSourceWithThreeDSecureRequired();
        stripeMockClient.mockCreate3dsSource();

        String externalChargeId = addCharge();
        given().port(testContext.getPort())
                .contentType(JSON)
                .body(validAuthorisationDetails)
                .post(authoriseChargeUrlFor(externalChargeId))
                .then()
                .body("status", is(AUTHORISATION_3DS_REQUIRED.toString()))
                .statusCode(200);

        assertFrontendChargeStatusIs(externalChargeId, AUTHORISATION_3DS_REQUIRED.toString());

        List<LoggedRequest> requests = findAll(postRequestedFor(urlMatching("/v1/tokens")));
        assertThat(requests).hasSize(1);
        requests = findAll(postRequestedFor(urlMatching("/v1/sources")));
        assertThat(requests).hasSize(2);
        requests = findAll(postRequestedFor(urlMatching("/v1/charges")));
        assertThat(requests).hasSize(0);
    }

    @Test
    public void invalidAuthCredentialsShouldReturnAnInternalServerError() {
        stripeMockClient.mockUnauthorizedResponse();

        addGatewayAccount(ImmutableMap.of("stripe_account_id", stripeAccountId));

        String externalChargeId = addCharge();

        given().port(testContext.getPort())
                .contentType(JSON)
                .body(validAuthorisationDetails)
                .post(authoriseChargeUrlFor(externalChargeId))
                .then()
                .statusCode(500)
                .body("message", containsString("There was an internal server error. ErrorId:"));
    }

    @Test
    public void shouldReturnInternalServerResponseWhenGatewayAccountHasNoStripeAccountId() {
        addGatewayAccount(emptyMap());

        String externalChargeId = addCharge();

        given().port(testContext.getPort())
                .contentType(JSON)
                .body(validAuthorisationDetails)
                .post(authoriseChargeUrlFor(externalChargeId))
                .then()
                .statusCode(500)
                .body("message", containsString("There is no stripe_account_id for gateway account with id"));
    }

    @Test
    public void shouldCaptureCardPayment_IfChargeWasPreviouslyAuthorised() {

        addGatewayAccount(ImmutableMap.of("stripe_account_id", stripeAccountId));

        String externalChargeId = addChargeWithStatus(AUTHORISATION_SUCCESS);

        given().port(testContext.getPort())
                .contentType(JSON)
                .body(StringUtils.EMPTY)
                .post(captureChargeUrlFor(externalChargeId))
                .then().statusCode(204);

        assertFrontendChargeStatusIs(externalChargeId, CAPTURE_APPROVED.getValue());
    }

    private String addChargeWithStatus(ChargeStatus chargeStatus) {
        long chargeId = RandomUtils.nextInt();
        String externalChargeId = "charge-" + chargeId;
        databaseTestHelper.addCharge(chargeId, externalChargeId, accountId, Long.valueOf(AMOUNT), chargeStatus, "RETURN_URL", null, DESCRIPTION);
        return externalChargeId;
    }

    protected void assertFrontendChargeStatusIs(String chargeId, String status) {
        connectorRestApiClient
                .withChargeId(chargeId)
                .getFrontendCharge()
                .body("status", is(status));
    }

    private String addCharge() {
        return addChargeWithStatus(ENTERING_CARD_DETAILS);
    }

    private void addGatewayAccount(Map credentials) {
        databaseTestHelper.addGatewayAccount(accountId, paymentProvider, credentials);
    }

    private String authoriseChargeUrlFor(String chargeId) {
        return "/v1/frontend/charges/{chargeId}/cards".replace("{chargeId}", chargeId);
    }

    private String constructExpectedSourcesRequestBody() {
        Map<String, String> params = new HashMap<>();
        params.put("type", "card");
        params.put("token", "tok_1DJfnpHj08j2jFuBPMcHN1F8"); //This comes from resources/stripe/create_token_response.json
        params.put("usage", "single_use");
        return encode(params);
    }

    private String captureChargeUrlFor(String chargeId) {
        return "/v1/frontend/charges/{chargeId}/capture".replace("{chargeId}", chargeId);
    }

    private String constructExpectedAuthoriseRequestBody() {
        Map<String, String> params = new HashMap<>();
        params.put("amount", AMOUNT);
        params.put("currency", "GBP");
        params.put("description", DESCRIPTION);
        params.put("source", "src_1DT9bn2eZvKYlo2Cg5okt8WC"); //This comes from resources/stripe/create_sources_response.json
        params.put("capture", "false");
        params.put("destination[account]", stripeAccountId);
        return encode(params);
    }

    private String constructExpectedTokensRequestBody() {
        Map<String, String> params = new HashMap<>();
        params.put("card[cvc]", CVC);
        params.put("card[exp_month]", EXP_MONTH);
        params.put("card[exp_year]", EXP_YEAR);
        params.put("card[number]", CARD_NUMBER);
        return encode(params);
    }

    private String encode(Map<String, String> params) {
        return params.keySet().stream()
                .map(key -> encode(key) + "=" + encode(params.get(key)))
                .collect(joining("&"));
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(format("Exception thrown when encoding %s", value));
        }
    }
}
