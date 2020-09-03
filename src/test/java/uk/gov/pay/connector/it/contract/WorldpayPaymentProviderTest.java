package uk.gov.pay.connector.it.contract;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import io.dropwizard.setup.Environment;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.app.WorldpayConfig;
import uk.gov.pay.connector.charge.model.domain.Charge;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.common.model.domain.Address;
import uk.gov.pay.connector.gateway.CaptureResponse;
import uk.gov.pay.connector.gateway.GatewayClient;
import uk.gov.pay.connector.gateway.GatewayClientFactory;
import uk.gov.pay.connector.gateway.GatewayOperation;
import uk.gov.pay.connector.gateway.PaymentGatewayName;
import uk.gov.pay.connector.gateway.model.AuthCardDetails;
import uk.gov.pay.connector.gateway.model.request.CancelGatewayRequest;
import uk.gov.pay.connector.gateway.model.request.CaptureGatewayRequest;
import uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequest;
import uk.gov.pay.connector.gateway.model.request.RefundGatewayRequest;
import uk.gov.pay.connector.gateway.model.response.BaseAuthoriseResponse;
import uk.gov.pay.connector.gateway.model.response.GatewayRefundResponse;
import uk.gov.pay.connector.gateway.model.response.GatewayResponse;
import uk.gov.pay.connector.gateway.util.DefaultExternalRefundAvailabilityCalculator;
import uk.gov.pay.connector.gateway.util.ExternalRefundAvailabilityCalculator;
import uk.gov.pay.connector.gateway.worldpay.WorldpayPaymentProvider;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;

import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity.Type.TEST;
import static uk.gov.pay.connector.model.domain.AuthCardDetailsFixture.anAuthCardDetails;
import static uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture.aValidChargeEntity;
import static uk.gov.pay.connector.model.domain.RefundEntityFixture.userEmail;
import static uk.gov.pay.connector.model.domain.RefundEntityFixture.userExternalId;
import static uk.gov.pay.connector.util.SystemUtils.envOrThrow;

@Ignore("Ignoring as this test is failing in Jenkins because it's failing to locate the certificates - PP-1707")
public class WorldpayPaymentProviderTest {

    private static final String MAGIC_CARDHOLDER_NAME_THAT_MAKES_WORLDPAY_TEST_REQUIRE_3DS = "3D";
    private static final String MAGIC_CARDHOLDER_NAME_FOR_3DS_FLEX_CHALLENGE_REQUIRED_RESPONSE = "3DS_V2_CHALLENGE_IDENTIFIED";

    private GatewayAccountEntity validGatewayAccount;
    private GatewayAccountEntity validGatewayAccountFor3ds;
    private Map<String, String> validCredentials;
    private Map<String, String> validCredentials3ds;
    private ChargeEntity chargeEntity;
    private MetricRegistry mockMetricRegistry;
    private Histogram mockHistogram;
    private Counter mockCounter;
    private Environment mockEnvironment;
    private ExternalRefundAvailabilityCalculator defaultExternalRefundAvailabilityCalculator = new DefaultExternalRefundAvailabilityCalculator();

    @Before
    public void checkThatWorldpayIsUp() throws IOException {
        try {
            validCredentials = Map.of(
                    "merchant_id", envOrThrow("GDS_CONNECTOR_WORLDPAY_MERCHANT_ID"),
                    "username", envOrThrow("GDS_CONNECTOR_WORLDPAY_USER"),
                    "password", envOrThrow("GDS_CONNECTOR_WORLDPAY_PASSWORD"));

            validCredentials3ds = Map.of(
                    "merchant_id", envOrThrow("GDS_CONNECTOR_WORLDPAY_MERCHANT_ID_3DS"),
                    "username", envOrThrow("GDS_CONNECTOR_WORLDPAY_USER_3DS"),
                    "password", envOrThrow("GDS_CONNECTOR_WORLDPAY_PASSWORD_3DS"));
        } catch (IllegalStateException ex) {
            Assume.assumeTrue("Ignoring test since credentials not configured", false);
        }

        new URL(getWorldpayConfig().getUrls().get(TEST.toString())).openConnection().connect();

        validGatewayAccount = new GatewayAccountEntity();
        validGatewayAccount.setId(1234L);
        validGatewayAccount.setGatewayName("worldpay");
        validGatewayAccount.setCredentials(validCredentials);
        validGatewayAccount.setType(TEST);

        validGatewayAccountFor3ds = new GatewayAccountEntity();
        validGatewayAccountFor3ds.setId(1234L);
        validGatewayAccountFor3ds.setGatewayName("worldpay");
        validGatewayAccountFor3ds.setCredentials(validCredentials3ds);
        validGatewayAccountFor3ds.setType(TEST);

        mockMetricRegistry = mock(MetricRegistry.class);
        mockHistogram = mock(Histogram.class);
        mockCounter = mock(Counter.class);
        when(mockMetricRegistry.histogram(anyString())).thenReturn(mockHistogram);
        when(mockMetricRegistry.counter(anyString())).thenReturn(mockCounter);
        mockEnvironment = mock(Environment.class);
        when(mockEnvironment.metrics()).thenReturn(mockMetricRegistry);

        chargeEntity = aValidChargeEntity()
                .withTransactionId(randomUUID().toString())
                .withGatewayAccountEntity(validGatewayAccount)
                .build();
    }

    @Test
    public void submit3DS2FlexAuthRequest() throws Exception {
        WorldpayPaymentProvider paymentProvider = getValidWorldpayPaymentProvider();
        var authCardDetails = anAuthCardDetails().withWorldpay3dsFlexDdcResult(UUID.randomUUID().toString()).build();
        CardAuthorisationGatewayRequest request = getCardAuthorisationRequest(authCardDetails);
        GatewayResponse<BaseAuthoriseResponse> response = paymentProvider.authorise(request);
        assertTrue(response.getBaseResponse().isPresent());
    }

    @Test
    public void submit3DS2FlexAuthRequest_requiresChallenge() throws Exception {
        WorldpayPaymentProvider paymentProvider = getValidWorldpayPaymentProvider();
        AuthCardDetails authCardDetails = anAuthCardDetails()
                .withCardHolder(MAGIC_CARDHOLDER_NAME_FOR_3DS_FLEX_CHALLENGE_REQUIRED_RESPONSE)
                .withWorldpay3dsFlexDdcResult(UUID.randomUUID().toString())
                .build();
        CardAuthorisationGatewayRequest request = getCardAuthorisationRequest(authCardDetails);
        GatewayResponse<BaseAuthoriseResponse> response = paymentProvider.authorise(request);
        assertTrue(response.getBaseResponse().isPresent());
        response.getBaseResponse().ifPresent(res -> {
            assertThat(res.getGatewayParamsFor3ds().isPresent(), is(true));
            assertThat(res.getGatewayParamsFor3ds().get().toAuth3dsRequiredEntity().getWorldpayChallengeAcsUrl(), is(notNullValue()));
            assertThat(res.getGatewayParamsFor3ds().get().toAuth3dsRequiredEntity().getWorldpayChallengeTransactionId(), is(notNullValue()));
            assertThat(res.getGatewayParamsFor3ds().get().toAuth3dsRequiredEntity().getWorldpayChallengePayload(), is(notNullValue()));
            assertThat(res.getGatewayParamsFor3ds().get().toAuth3dsRequiredEntity().getThreeDsVersion(), is(notNullValue()));
        });
    }

    @Test
    public void shouldBeAbleToSendAuthorisationRequestForMerchant() throws Exception {
        WorldpayPaymentProvider paymentProvider = getValidWorldpayPaymentProvider();
        AuthCardDetails authCardDetails = anAuthCardDetails().build();
        CardAuthorisationGatewayRequest request = getCardAuthorisationRequest(authCardDetails);
        GatewayResponse<BaseAuthoriseResponse> response = paymentProvider.authorise(request);
        assertTrue(response.getBaseResponse().isPresent());
    }

    @Test
    public void shouldBeAbleToSendAuthorisationRequestForMerchantWithoutAddress() throws Exception {
        WorldpayPaymentProvider paymentProvider = getValidWorldpayPaymentProvider();
        AuthCardDetails authCardDetails = anAuthCardDetails().withAddress(null).build();
        CardAuthorisationGatewayRequest request = getCardAuthorisationRequest(authCardDetails);
        GatewayResponse<BaseAuthoriseResponse> response = paymentProvider.authorise(request);
        assertTrue(response.getBaseResponse().isPresent());
    }

    @Test
    public void shouldBeAbleToSendAuthorisationRequestForMerchantWithUsAddress() throws Exception {
        WorldpayPaymentProvider paymentProvider = getValidWorldpayPaymentProvider();
        Address usAddress = new Address();
        usAddress.setLine1("125 Kingsway");
        usAddress.setLine2("Aviation House");
        usAddress.setPostcode("90210");
        usAddress.setCity("Washington D.C.");
        usAddress.setCountry("US");
        AuthCardDetails authCardDetails = anAuthCardDetails().withAddress(usAddress).build();
        CardAuthorisationGatewayRequest request = getCardAuthorisationRequest(authCardDetails);
        GatewayResponse<BaseAuthoriseResponse> response = paymentProvider.authorise(request);
        assertTrue(response.getBaseResponse().isPresent());
    }

    @Test
    public void shouldBeAbleToSendAuthorisationRequestForMerchantWithCanadaAddress() throws Exception {
        WorldpayPaymentProvider paymentProvider = getValidWorldpayPaymentProvider();
        Address canadaAddress = new Address();
        canadaAddress.setLine1("125 Kingsway");
        canadaAddress.setLine2("Aviation House");
        canadaAddress.setPostcode("X0A0A0");
        canadaAddress.setCity("Arctic Bay");
        canadaAddress.setCountry("CA");
        AuthCardDetails authCardDetails = anAuthCardDetails().withAddress(canadaAddress).build();
        CardAuthorisationGatewayRequest request = getCardAuthorisationRequest(authCardDetails);
        GatewayResponse<BaseAuthoriseResponse> response = paymentProvider.authorise(request);
        assertTrue(response.getBaseResponse().isPresent());
    }

    @Test
    public void shouldBeAbleToSendAuthorisationRequestForMerchantUsing3ds() throws Exception {
        WorldpayPaymentProvider paymentProvider = getValidWorldpayPaymentProvider();
        AuthCardDetails authCardDetails = anAuthCardDetails()
                .withCardHolder(MAGIC_CARDHOLDER_NAME_THAT_MAKES_WORLDPAY_TEST_REQUIRE_3DS)
                .build();

        CardAuthorisationGatewayRequest request = getCardAuthorisationRequestWithRequired3ds(authCardDetails);
        GatewayResponse<BaseAuthoriseResponse> response = paymentProvider.authorise(request);

        assertTrue(response.getBaseResponse().isPresent());
        assertTrue(response.getSessionIdentifier().isPresent());
        response.getBaseResponse().ifPresent(res -> {
            assertThat(res.getGatewayParamsFor3ds().isPresent(), is(true));
            assertThat(res.getGatewayParamsFor3ds().get().toAuth3dsRequiredEntity().getPaRequest(), is(notNullValue()));
            assertThat(res.getGatewayParamsFor3ds().get().toAuth3dsRequiredEntity().getIssuerUrl(), is(notNullValue()));
        });
    }

    @Test
    public void shouldBeAbleToSendAuthorisationRequestForMerchantUsing3dsWithoutAddress() throws Exception {
        WorldpayPaymentProvider paymentProvider = getValidWorldpayPaymentProvider();
        AuthCardDetails authCardDetails = anAuthCardDetails()
                .withCardHolder(MAGIC_CARDHOLDER_NAME_THAT_MAKES_WORLDPAY_TEST_REQUIRE_3DS)
                .withAddress(null)
                .build();

        CardAuthorisationGatewayRequest request = getCardAuthorisationRequestWithRequired3ds(authCardDetails);

        GatewayResponse<BaseAuthoriseResponse> response = paymentProvider.authorise(request);
        assertTrue(response.getBaseResponse().isPresent());
        assertTrue(response.getSessionIdentifier().isPresent());
        response.getBaseResponse().ifPresent(res -> {
            assertThat(res.getGatewayParamsFor3ds().isPresent(), is(true));
            assertThat(res.getGatewayParamsFor3ds().get().toAuth3dsRequiredEntity().getPaRequest(), is(notNullValue()));
            assertThat(res.getGatewayParamsFor3ds().get().toAuth3dsRequiredEntity().getIssuerUrl(), is(notNullValue()));
        });
    }

    /**
     * Worldpay does not care about a successful authorization reference to make a capture request.
     * It simply accepts anything as long as the request is well formed. (And ignores it silently)
     */
    @Test
    public void shouldBeAbleToSendCaptureRequestForMerchant() {
        WorldpayPaymentProvider paymentProvider = getValidWorldpayPaymentProvider();
        CaptureResponse response = paymentProvider.capture(CaptureGatewayRequest.valueOf(chargeEntity));
        assertTrue(response.isSuccessful());
    }

    @Test
    public void shouldBeAbleToSubmitAPartialRefundAfterACaptureHasBeenSubmitted() throws Exception {
        WorldpayPaymentProvider paymentProvider = getValidWorldpayPaymentProvider();
        AuthCardDetails authCardDetails = anAuthCardDetails().build();
        CardAuthorisationGatewayRequest request = getCardAuthorisationRequest(authCardDetails);
        GatewayResponse<BaseAuthoriseResponse> response = paymentProvider.authorise(request);
        String transactionId = response.getBaseResponse().get().getTransactionId();

        assertThat(response.getBaseResponse().isPresent(), is(true));
        assertThat(response.getBaseResponse().isPresent(), is(true));
        assertThat(transactionId, is(not(nullValue())));

        chargeEntity.setGatewayTransactionId(transactionId);
        CaptureResponse captureResponse = paymentProvider.capture(CaptureGatewayRequest.valueOf(chargeEntity));

        assertThat(captureResponse.isSuccessful(), is(true));

        RefundEntity refundEntity = new RefundEntity(1L, userExternalId, userEmail, chargeEntity.getExternalId());

        GatewayRefundResponse refundResponse = paymentProvider.refund(RefundGatewayRequest.valueOf(Charge.from(chargeEntity), refundEntity, validGatewayAccount));

        assertTrue(refundResponse.isSuccessful());
    }

    @Test
    public void shouldBeAbleToSendCancelRequestForMerchant() throws Exception {
        WorldpayPaymentProvider paymentProvider = getValidWorldpayPaymentProvider();
        AuthCardDetails authCardDetails = anAuthCardDetails().build();
        CardAuthorisationGatewayRequest request = getCardAuthorisationRequest(authCardDetails);
        GatewayResponse<BaseAuthoriseResponse> response = paymentProvider.authorise(request);

        assertThat(response.getBaseResponse().isPresent(), is(true));
        String transactionId = response.getBaseResponse().get().getTransactionId();
        assertThat(transactionId, is(not(nullValue())));

        chargeEntity.setGatewayTransactionId(transactionId);

        CancelGatewayRequest cancelGatewayRequest = CancelGatewayRequest.valueOf(chargeEntity);
        GatewayResponse cancelResponse = paymentProvider.cancel(cancelGatewayRequest);

        assertThat(cancelResponse.getBaseResponse().isPresent(), is(true));
    }

    @Test
    public void shouldFailRequestAuthorisationIfCredentialsAreNotCorrect() throws Exception {

        WorldpayPaymentProvider paymentProvider = getValidWorldpayPaymentProvider();

        Long gatewayAccountId = 112233L;
        String providerName = "worldpay";
        var credentials = Map.of(
                "merchant_id", "non-existent-id",
                "username", "non-existent-username",
                "password", "non-existent-password"
        );

        GatewayAccountEntity gatewayAccountEntity = new GatewayAccountEntity(providerName, credentials, TEST);
        gatewayAccountEntity.setId(gatewayAccountId);

        ChargeEntity charge = aValidChargeEntity().withGatewayAccountEntity(gatewayAccountEntity).build();
        AuthCardDetails authCardDetails = anAuthCardDetails().build();
        CardAuthorisationGatewayRequest request = new CardAuthorisationGatewayRequest(charge, authCardDetails);
        assertFalse(paymentProvider.authorise(request).getBaseResponse().isPresent());
    }

    private CardAuthorisationGatewayRequest getCardAuthorisationRequest(AuthCardDetails authCardDetails) {
        ChargeEntity charge = aValidChargeEntity()
                .withTransactionId(randomUUID().toString())
                .withGatewayAccountEntity(validGatewayAccount)
                .build();
        return new CardAuthorisationGatewayRequest(charge, authCardDetails);
    }

    private CardAuthorisationGatewayRequest getCardAuthorisationRequestWithRequired3ds(AuthCardDetails authCardDetails) {
        ChargeEntity charge = aValidChargeEntity()
                .withTransactionId(randomUUID().toString())
                .withGatewayAccountEntity(validGatewayAccountFor3ds)
                .build();
        charge.getGatewayAccount().setRequires3ds(true);
        return new CardAuthorisationGatewayRequest(charge, authCardDetails);
    }

    private WorldpayPaymentProvider getValidWorldpayPaymentProvider() {
        GatewayClient gatewayClient = new GatewayClient(ClientBuilder.newClient(), mockMetricRegistry);

        ConnectorConfiguration configuration = mock(ConnectorConfiguration.class);
        when(configuration.getGatewayConfigFor(PaymentGatewayName.WORLDPAY)).thenReturn(getWorldpayConfig());
        when(configuration.getWorldpayConfig()).thenReturn(getWorldpayConfig());

        GatewayClientFactory gatewayClientFactory = mock(GatewayClientFactory.class);
        when(gatewayClientFactory.createGatewayClient(
                any(PaymentGatewayName.class),
                any(GatewayOperation.class),
                any(MetricRegistry.class))).thenReturn(gatewayClient);

        return new WorldpayPaymentProvider(configuration, gatewayClientFactory, mockEnvironment);
    }

    private WorldpayConfig getWorldpayConfig() {
        return WORLDPAY_CREDENTIALS;
    }

    private static final WorldpayConfig WORLDPAY_CREDENTIALS = new WorldpayConfig() {
        @Override
        public Map<String, String> getUrls() {
            return Map.of(TEST.toString(), "https://secure-test.worldpay.com/jsp/merchant/xml/paymentService.jsp");
        }
    };
}
