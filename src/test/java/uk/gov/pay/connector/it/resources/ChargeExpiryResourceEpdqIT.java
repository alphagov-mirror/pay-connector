package uk.gov.pay.connector.it.resources;


import com.github.tomakehurst.wiremock.matching.UrlPattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.pay.connector.app.ConnectorApp;
import uk.gov.pay.connector.charge.model.domain.ChargeStatus;
import uk.gov.pay.connector.it.base.ChargingITestBase;
import uk.gov.pay.connector.junit.DropwizardConfig;
import uk.gov.pay.connector.junit.DropwizardJUnitRunner;
import uk.gov.pay.connector.util.RandomIdGenerator;

import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.restassured.http.ContentType.JSON;
import static java.time.temporal.ChronoUnit.MINUTES;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.is;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.AUTHORISATION_3DS_READY;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.AUTHORISATION_REJECTED;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.CAPTURED;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.EXPIRED;
import static uk.gov.pay.connector.gateway.epdq.EpdqPaymentProvider.ROUTE_FOR_MAINTENANCE_ORDER;
import static uk.gov.pay.connector.gateway.epdq.EpdqPaymentProvider.ROUTE_FOR_QUERY_ORDER;

@RunWith(DropwizardJUnitRunner.class)
@DropwizardConfig(app = ConnectorApp.class, config = "config/test-it-config.yaml")
public class ChargeExpiryResourceEpdqIT extends ChargingITestBase {

    public ChargeExpiryResourceEpdqIT() {
        super("epdq");
    }
    
    @Test
    public void shouldExpireWithGatewayIfExistsInCancellableStateWithGatewayEvenIfChargeIsPreAuthorisation() {
        String chargeId = addCharge(ChargeStatus.AUTHORISATION_3DS_REQUIRED, "ref", Instant.now().minus(90, MINUTES), RandomIdGenerator.newId());

        epdqMockClient.mockAuthorisationQuerySuccess();
        epdqMockClient.mockCancelSuccess();

        connectorRestApiClient
                .postChargeExpiryTask()
                .statusCode(OK.getStatusCode())
                .contentType(JSON)
                .body("expiry-success", is(1))
                .body("expiry-failed", is(0));

        connectorRestApiClient
                .withAccountId(accountId)
                .withChargeId(chargeId)
                .getCharge()
                .statusCode(OK.getStatusCode())
                .contentType(JSON)
                .body("charge_id", is(chargeId))
                .body("state.status", is(EXPIRED.toExternal().getStatus()));

        verifyPostToPath(String.format("/epdq/%s", ROUTE_FOR_MAINTENANCE_ORDER));
        verifyPostToPath(String.format("/epdq/%s", ROUTE_FOR_QUERY_ORDER));
    }

    @Test
    public void shouldHandleCaseWhereEpdqRespondsWithUnknownStatus() {
        String chargeId = addCharge(ChargeStatus.AUTHORISATION_3DS_REQUIRED, "ref", Instant.now().minus(90, MINUTES), RandomIdGenerator.newId());
        epdqMockClient.mockUnknown();

        connectorRestApiClient
                .postChargeExpiryTask()
                .statusCode(OK.getStatusCode())
                .contentType(JSON)
                .body("expiry-success", is(1))
                .body("expiry-failed", is(0));

        connectorRestApiClient
                .withAccountId(accountId)
                .withChargeId(chargeId)
                .getCharge()
                .statusCode(OK.getStatusCode())
                .contentType(JSON)
                .body("charge_id", is(chargeId))
                .body("state.status", is(EXPIRED.toExternal().getStatus()));

        verifyPostToPath(String.format("/epdq/%s", ROUTE_FOR_QUERY_ORDER));
    }

    @Test
    public void shouldUpdateChargeStatusToMatchTerminalStateOnGateway() {
        String chargeId = addCharge(AUTHORISATION_3DS_READY, "ref", Instant.now().minus(90, MINUTES), RandomIdGenerator.newId());
        
        epdqMockClient.mockAuthorisationQuerySuccessCaptured();

        connectorRestApiClient
                .postChargeExpiryTask()
                .statusCode(OK.getStatusCode())
                .contentType(JSON)
                .body("expiry-success", is(1))
                .body("expiry-failed", is(0));

        connectorRestApiClient
                .withAccountId(accountId)
                .withChargeId(chargeId)
                .getCharge()
                .statusCode(OK.getStatusCode())
                .contentType(JSON)
                .body("charge_id", is(chargeId))
                .body("state.status", is(CAPTURED.toExternal().getStatus()));

        verifyPostToPath(String.format("/epdq/%s", ROUTE_FOR_QUERY_ORDER));
    }

    @Test
    public void shouldUpdateChargeStatusToMatchTerminalStateOnGatewayWhenNotAForceTransition() {
        String chargeId = addCharge(AUTHORISATION_3DS_READY, "ref", Instant.now().minus(90, MINUTES), RandomIdGenerator.newId());

        epdqMockClient.mockAuthorisationQuerySuccessAuthFailed();

        connectorRestApiClient
                .postChargeExpiryTask()
                .statusCode(OK.getStatusCode())
                .contentType(JSON)
                .body("expiry-success", is(1))
                .body("expiry-failed", is(0));

        connectorRestApiClient
                .withAccountId(accountId)
                .withChargeId(chargeId)
                .getCharge()
                .statusCode(OK.getStatusCode())
                .contentType(JSON)
                .body("charge_id", is(chargeId))
                .body("state.status", is(AUTHORISATION_REJECTED.toExternal().getStatus()));

        verifyPostToPath(String.format("/epdq/%s", ROUTE_FOR_QUERY_ORDER));
    }

    private void verifyPostToPath(String path) {
        verify(
            postRequestedFor(
                UrlPattern.fromOneOf(
                    null,
                    null,
                    path,
                    null
                )
            )
        );
    }
}
