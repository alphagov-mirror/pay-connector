package uk.gov.pay.connector.rules;

import com.github.tomakehurst.wiremock.WireMockServer;
import uk.gov.pay.connector.util.TestTemplateResourceLoader;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_XML;
import static uk.gov.pay.connector.gateway.epdq.EpdqPaymentProvider.ROUTE_FOR_MAINTENANCE_ORDER;
import static uk.gov.pay.connector.gateway.epdq.EpdqPaymentProvider.ROUTE_FOR_NEW_ORDER;
import static uk.gov.pay.connector.gateway.epdq.EpdqPaymentProvider.ROUTE_FOR_QUERY_ORDER;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_AUTHORISATION_ERROR_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_AUTHORISATION_FAILED_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_AUTHORISATION_OTHER_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_AUTHORISATION_SUCCESS_3D_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_AUTHORISATION_SUCCESS_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_AUTHORISATION_WAITING_EXTERNAL_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_AUTHORISATION_WAITING_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_CANCEL_SUCCESS_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_CAPTURE_ERROR_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_CAPTURE_SUCCESS_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_REFUND_ERROR_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_REFUND_SUCCESS_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.EPDQ_UNKNOWN_RESPONSE;

public class EpdqMockClient {

    private WireMockServer wireMockServer;

    public EpdqMockClient(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    public void mockAuthorisationSuccess() {
        paymentServiceResponse(ROUTE_FOR_NEW_ORDER, TestTemplateResourceLoader.load(EPDQ_AUTHORISATION_SUCCESS_RESPONSE));
    }

    public void mockUnknown() {
        paymentServiceResponse(ROUTE_FOR_QUERY_ORDER, TestTemplateResourceLoader.load(EPDQ_UNKNOWN_RESPONSE));
    }

    public void mockAuthorisationQuerySuccess() {
        paymentServiceResponse(ROUTE_FOR_QUERY_ORDER, TestTemplateResourceLoader.load(EPDQ_AUTHORISATION_SUCCESS_RESPONSE));
    }
    
    public void mockAuthorisationQuerySuccessAuthFailed() {
        paymentServiceResponse(ROUTE_FOR_QUERY_ORDER, TestTemplateResourceLoader.load(EPDQ_AUTHORISATION_FAILED_RESPONSE));
    }
    
    public void mockAuthorisationQuerySuccessCaptured() {
        paymentServiceResponse(ROUTE_FOR_QUERY_ORDER, TestTemplateResourceLoader.load(EPDQ_CAPTURE_SUCCESS_RESPONSE));
    }

    public void mockCancelQuerySuccess() {
        paymentServiceResponse(ROUTE_FOR_QUERY_ORDER, TestTemplateResourceLoader.load(EPDQ_CANCEL_SUCCESS_RESPONSE));
    }    
    
    public void mockCaptureQuerySuccess() {
        paymentServiceResponse(ROUTE_FOR_QUERY_ORDER, TestTemplateResourceLoader.load(EPDQ_CAPTURE_SUCCESS_RESPONSE));
    }

    public void mockAuthorisation3dsSuccess() {
        paymentServiceResponse(ROUTE_FOR_NEW_ORDER, TestTemplateResourceLoader.load(EPDQ_AUTHORISATION_SUCCESS_3D_RESPONSE));
    }

    public void mockAuthorisationFailure() {
        paymentServiceResponse(ROUTE_FOR_NEW_ORDER, TestTemplateResourceLoader.load(EPDQ_AUTHORISATION_FAILED_RESPONSE));
    }

    public void mockAuthorisationError() {
        paymentServiceResponse(ROUTE_FOR_NEW_ORDER, TestTemplateResourceLoader.load(EPDQ_AUTHORISATION_ERROR_RESPONSE));
    }

    public void mockAuthorisationWaitingExternal() {
        paymentServiceResponse(ROUTE_FOR_NEW_ORDER, TestTemplateResourceLoader.load(EPDQ_AUTHORISATION_WAITING_EXTERNAL_RESPONSE));
    }

    public void mockAuthorisationWaiting() {
        paymentServiceResponse(ROUTE_FOR_NEW_ORDER, TestTemplateResourceLoader.load(EPDQ_AUTHORISATION_WAITING_RESPONSE));
    }

    public void mockAuthorisationOther() {
        paymentServiceResponse(ROUTE_FOR_NEW_ORDER, TestTemplateResourceLoader.load(EPDQ_AUTHORISATION_OTHER_RESPONSE));
    }

    public void mockCaptureSuccess() {
        paymentServiceResponse(ROUTE_FOR_MAINTENANCE_ORDER, TestTemplateResourceLoader.load(EPDQ_CAPTURE_SUCCESS_RESPONSE));
    }

    public void mockCaptureError() {
        paymentServiceResponse(ROUTE_FOR_MAINTENANCE_ORDER, TestTemplateResourceLoader.load(EPDQ_CAPTURE_ERROR_RESPONSE));
    }

    public void mockCancelSuccess() {
        paymentServiceResponse(ROUTE_FOR_MAINTENANCE_ORDER, TestTemplateResourceLoader.load(EPDQ_CANCEL_SUCCESS_RESPONSE));
    }

    public void mockRefundSuccess(String... payIdSub) {
        String payIdSubToUse = (payIdSub != null && payIdSub.length == 1) ? payIdSub[0] : "1";
        paymentServiceResponse(ROUTE_FOR_MAINTENANCE_ORDER,
                TestTemplateResourceLoader.load(EPDQ_REFUND_SUCCESS_RESPONSE)
                        .replace("{{payIdSub}}", payIdSubToUse)
        );
    }

    public void mockRefundError() {
        paymentServiceResponse(ROUTE_FOR_MAINTENANCE_ORDER, TestTemplateResourceLoader.load(EPDQ_REFUND_ERROR_RESPONSE));
    }

    private void paymentServiceResponse(String route, String responseBody) {
        //FIXME - This mocking approach is very poor. Needs to be revisited. Story PP-900 created.
        wireMockServer.stubFor(
                post(urlPathEqualTo(String.format("/epdq/%s", route)))
                        .willReturn(
                                aResponse()
                                        .withHeader(CONTENT_TYPE, TEXT_XML)
                                        .withStatus(200)
                                        .withBody(responseBody)
                        )
        );
    }
}
