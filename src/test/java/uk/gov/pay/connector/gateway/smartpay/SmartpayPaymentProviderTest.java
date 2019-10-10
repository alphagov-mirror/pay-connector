package uk.gov.pay.connector.gateway.smartpay;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.gateway.model.Auth3dsDetails;
import uk.gov.pay.connector.gateway.model.AuthCardDetails;
import uk.gov.pay.connector.gateway.model.request.Auth3dsResponseGatewayRequest;
import uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequest;
import uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse;
import uk.gov.pay.connector.gateway.model.response.Gateway3DSAuthorisationResponse;
import uk.gov.pay.connector.gateway.model.response.GatewayResponse;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.model.domain.AuthCardDetailsFixture;
import uk.gov.pay.connector.util.AuthUtils;
import uk.gov.pay.connector.util.TestTemplateResourceLoader;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse.AuthoriseStatus.REQUIRES_3DS;
import static uk.gov.pay.connector.model.domain.ChargeEntityFixture.aValidChargeEntity;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.SMARTPAY_3DS_AUTHORISATION_SUCCESS_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.SMARTPAY_AUTHORISATION_3DS_REQUIRED_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.SMARTPAY_AUTHORISATION_SUCCESS_RESPONSE;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.SMARTPAY_CAPTURE_SUCCESS_RESPONSE;

@RunWith(MockitoJUnitRunner.class)
public class SmartpayPaymentProviderTest extends BaseSmartpayPaymentProviderTest {

    @Test
    public void shouldGetPaymentProviderName() {
        assertThat(provider.getPaymentGatewayName().getName(), is("smartpay"));
    }

    @Test
    public void shouldGenerateTransactionId() {
        assertThat(provider.generateTransactionId().isPresent(), is(false));
    }

    @Test
    public void shouldSendSuccessfullyAOrderForMerchant() throws Exception {
        mockSmartpaySuccessfulOrderSubmitResponse();
        
        AuthCardDetails authCardDetails = AuthCardDetailsFixture.anAuthCardDetails().build();

        ChargeEntity chargeEntity = aValidChargeEntity()
                .withGatewayAccountEntity(aServiceAccount())
                .build();

        GatewayResponse<BaseAuthoriseResponse> response = provider.authorise(new CardAuthorisationGatewayRequest(chargeEntity, authCardDetails));

        assertTrue(response.isSuccessful());
        assertThat(response.getBaseResponse().isPresent(), is(true));
        String transactionId = response.getBaseResponse().get().getTransactionId();
        assertThat(transactionId, is(not(nullValue())));
    }

    @Test
    public void shouldRequire3dsFor3dsRequiredMerchant() throws Exception {
        AuthCardDetails authCardDetails = AuthCardDetailsFixture.anAuthCardDetails().build();
        GatewayAccountEntity gatewayAccountEntity = aServiceAccount();
        gatewayAccountEntity.setRequires3ds(true);
        ChargeEntity chargeEntity = aValidChargeEntity()
                .withGatewayAccountEntity(gatewayAccountEntity)
                .build();
        mockSmartpay3dsRequiredOrderSubmitResponse();

        GatewayResponse<BaseAuthoriseResponse> response = provider.authorise(new CardAuthorisationGatewayRequest(chargeEntity, authCardDetails));

        assertTrue(response.isSuccessful());
        assertThat(response.getBaseResponse().isPresent(), is(true));
        SmartpayAuthorisationResponse smartpayAuthorisationResponse = (SmartpayAuthorisationResponse) response.getBaseResponse().get();
        assertThat(smartpayAuthorisationResponse.authoriseStatus(), is(REQUIRES_3DS));
        assertThat(smartpayAuthorisationResponse.getMd(), is(not(nullValue())));
        assertThat(smartpayAuthorisationResponse.getIssuerUrl(), is(not(nullValue())));
        assertThat(smartpayAuthorisationResponse.getPaRequest(), is(not(nullValue())));

    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldThrow_IfTryingToAuthoriseAnApplePayPayment() {
        provider.authoriseWallet(null);
    }

    @Test
    public void shouldSuccess3DSAuthorisation() {
        mockSmartpay3dsAuthorisationSuccessfulOrderSubmitResponse();
        
        GatewayAccountEntity gatewayAccountEntity = aServiceAccount();
        gatewayAccountEntity.setRequires3ds(true);
        ChargeEntity chargeEntity = aValidChargeEntity()
                .withGatewayAccountEntity(gatewayAccountEntity)
                .build();
        Auth3dsDetails auth3dsDetails = AuthUtils.buildAuth3dsDetails();
        auth3dsDetails.setMd("Some smart text here");

        Gateway3DSAuthorisationResponse response = provider.authorise3dsResponse(new Auth3dsResponseGatewayRequest(chargeEntity, auth3dsDetails));

        assertTrue(response.isSuccessful());
    }

    private void mockSmartpaySuccessfulOrderSubmitResponse() {
        mockSmartpayResponse(200, successAuthoriseResponse());
    }

    private void mockSmartpay3dsRequiredOrderSubmitResponse() {
        mockSmartpayResponse(200, successAuthorise3dsrequiredResponse());
    }

    private void mockSmartpay3dsAuthorisationSuccessfulOrderSubmitResponse() {
        mockSmartpayResponse(200, success3dsAuthoriseResponse());
    }

    private String successAuthorise3dsrequiredResponse() {
        return TestTemplateResourceLoader.load(SMARTPAY_AUTHORISATION_3DS_REQUIRED_RESPONSE).replace("{{pspReference}}", "12345678");
    }

    private void mockSmartpaySuccessfulCaptureResponse() {
        mockSmartpayResponse(200, successCaptureResponse());
    }

    private String successAuthoriseResponse() {
        return TestTemplateResourceLoader.load(SMARTPAY_AUTHORISATION_SUCCESS_RESPONSE).replace("{{pspReference}}", "12345678");
    }

    private String success3dsAuthoriseResponse() {
        return TestTemplateResourceLoader.load(SMARTPAY_3DS_AUTHORISATION_SUCCESS_RESPONSE).replace("{{pspReference}}", "12345678");
    }

    private String successCaptureResponse() {
        return TestTemplateResourceLoader.load(SMARTPAY_CAPTURE_SUCCESS_RESPONSE).replace("{{pspReference}}", "8614440510830227");
    }
}
