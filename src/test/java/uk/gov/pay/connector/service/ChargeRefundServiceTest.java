package uk.gov.pay.connector.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.connector.charge.dao.ChargeDao;
import uk.gov.pay.connector.charge.model.ChargeResponse;
import uk.gov.pay.connector.charge.model.domain.Charge;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.service.ChargeService;
import uk.gov.pay.connector.client.ledger.model.LedgerTransaction;
import uk.gov.pay.connector.gateway.PaymentGatewayName;
import uk.gov.pay.connector.gateway.PaymentProvider;
import uk.gov.pay.connector.gateway.PaymentProviders;
import uk.gov.pay.connector.gateway.model.ErrorType;
import uk.gov.pay.connector.gateway.model.request.RefundGatewayRequest;
import uk.gov.pay.connector.gateway.model.response.BaseRefundResponse;
import uk.gov.pay.connector.gateway.model.response.GatewayRefundResponse;
import uk.gov.pay.connector.gateway.smartpay.SmartpayRefundResponse;
import uk.gov.pay.connector.gateway.worldpay.WorldpayRefundResponse;
import uk.gov.pay.connector.gatewayaccount.dao.GatewayAccountDao;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.queue.statetransition.StateTransitionService;
import uk.gov.pay.connector.refund.dao.RefundDao;
import uk.gov.pay.connector.refund.exception.RefundException;
import uk.gov.pay.connector.refund.model.RefundRequest;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;
import uk.gov.pay.connector.refund.service.ChargeRefundResponse;
import uk.gov.pay.connector.refund.service.ChargeRefundService;
import uk.gov.pay.connector.usernotification.service.UserNotificationService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture.aValidChargeEntity;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.AUTHORISATION_SUCCESS;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.CAPTURED;
import static uk.gov.pay.connector.common.model.api.ExternalChargeRefundAvailability.EXTERNAL_AVAILABLE;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.SANDBOX;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.SMARTPAY;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.WORLDPAY;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity.Type.TEST;
import static uk.gov.pay.connector.model.domain.RefundEntityFixture.aValidRefundEntity;
import static uk.gov.pay.connector.model.domain.RefundEntityFixture.userExternalId;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.CREATED;

@RunWith(MockitoJUnitRunner.class)
public class ChargeRefundServiceTest {

    private ChargeRefundService chargeRefundService;
    private Long refundId;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private GatewayAccountDao mockGatewayAccountDao;
    @Mock
    private ChargeDao mockChargeDao;
    @Mock
    private ChargeService mockChargeService;
    @Mock
    private RefundDao mockRefundDao;
    @Mock
    private PaymentProviders mockProviders;
    @Mock
    private PaymentProvider mockProvider;
    @Mock
    private UserNotificationService mockUserNotificationService;
    @Mock
    private StateTransitionService mockStateTransitionService;

    @Before
    public void setUp() {
        refundId = ThreadLocalRandom.current().nextLong();
        when(mockProviders.byName(any(PaymentGatewayName.class))).thenReturn(mockProvider);
        when(mockProvider.getExternalChargeRefundAvailability(any(Charge.class), any(List.class))).thenReturn(EXTERNAL_AVAILABLE);
        chargeRefundService = new ChargeRefundService(
                mockRefundDao, mockGatewayAccountDao, mockProviders, mockUserNotificationService, mockStateTransitionService
        );
    }

    @Test
    public void shouldRefundSuccessfully_forWorldpay() {
        String externalChargeId = "chargeId";
        Long refundAmount = 100L;
        Long accountId = 2L;
        String providerName = "worldpay";

        GatewayAccountEntity account = new GatewayAccountEntity(providerName, newHashMap(), TEST);
        account.setId(accountId);
        ChargeEntity chargeEntity = aValidChargeEntity()
                .withGatewayAccountEntity(account)
                .withTransactionId("transaction-id")
                .withExternalId(externalChargeId)
                .withStatus(CAPTURED)
                .build();

        RefundEntity refundEntity = aValidRefundEntity()
                .withAmount(refundAmount)
                .withChargeExternalId(chargeEntity.getExternalId())
                .build();
        RefundEntity spiedRefundEntity = spy(refundEntity);

        when(mockGatewayAccountDao.findById(accountId)).thenReturn(Optional.of(account));
        setupWorldpayMock(spiedRefundEntity.getExternalId(), null);

        doAnswer(invocation -> {
            ((RefundEntity) invocation.getArgument(0)).setId(refundId);
            return null;
        }).when(mockRefundDao).persist(any(RefundEntity.class));

        when(mockRefundDao.findById(refundId)).thenReturn(Optional.of(spiedRefundEntity));
        when(mockRefundDao.findRefundsByChargeExternalId(chargeEntity.getExternalId())).thenReturn(List.of());

        ChargeRefundResponse gatewayResponse = chargeRefundService.doRefund(accountId, Charge.from(chargeEntity), new RefundRequest(refundAmount, chargeEntity.getAmount(), userExternalId));

        assertThat(gatewayResponse.getGatewayRefundResponse().isSuccessful(), is(true));
        assertThat(gatewayResponse.getGatewayRefundResponse().getError().isPresent(), is(false));

        assertThat(gatewayResponse.getRefundEntity(), is(spiedRefundEntity));

        verify(mockRefundDao).persist(argThat(aRefundEntity(refundAmount, chargeEntity)));
        verify(mockProvider).refund(argThat(aRefundRequestWith(chargeEntity, refundAmount)));
        verify(mockRefundDao, times(1)).findById(refundId);
        verify(spiedRefundEntity).setStatus(RefundStatus.REFUND_SUBMITTED);
        verify(spiedRefundEntity).setGatewayTransactionId(refundEntity.getExternalId());

        verifyNoMoreInteractions(mockChargeDao);
    }

    @Test
    public void shouldRefundSuccessfully_forHistoricPayment() {
        String externalChargeId = "chargeId";
        Long refundAmount = 100L;
        Long accountId = 2L;
        String providerName = "worldpay";

        GatewayAccountEntity account = new GatewayAccountEntity(providerName, newHashMap(), TEST);
        account.setId(accountId);

        ChargeResponse.RefundSummary refundSummary = new ChargeResponse.RefundSummary();
        refundSummary.setStatus("available");

        LedgerTransaction transaction = new LedgerTransaction();
        transaction.setTransactionId(externalChargeId);
        transaction.setAmount(500L);
        transaction.setRefundSummary(refundSummary);
        transaction.setReference("reference");
        transaction.setDescription("description");
        transaction.setGatewayAccountId(String.valueOf(accountId));
        transaction.setCreatedDate(ZonedDateTime.now(ZoneId.of("UTC")).toString());

        RefundEntity refundEntity = aValidRefundEntity().withChargeExternalId(externalChargeId).withAmount(refundAmount).build();
        RefundEntity spiedRefundEntity = spy(refundEntity);

        when(mockGatewayAccountDao.findById(accountId)).thenReturn(Optional.of(account));
        when(mockProviders.byName(WORLDPAY)).thenReturn(mockProvider);

        setupWorldpayMock(spiedRefundEntity.getExternalId(), null);

        doAnswer(invocation -> {
            ((RefundEntity) invocation.getArgument(0)).setId(refundId);
            return null;
        }).when(mockRefundDao).persist(any(RefundEntity.class));

        when(mockRefundDao.findById(refundId)).thenReturn(Optional.of(spiedRefundEntity));

        ChargeRefundResponse gatewayResponse = chargeRefundService.doRefund(accountId, Charge.from(transaction), new RefundRequest(refundAmount, 500L, userExternalId));

        assertThat(gatewayResponse.getGatewayRefundResponse().isSuccessful(), is(true));
        assertThat(gatewayResponse.getGatewayRefundResponse().getError().isPresent(), is(false));

        assertThat(gatewayResponse.getRefundEntity(), is(spiedRefundEntity));
        
        verify(mockRefundDao).persist(any(RefundEntity.class));
        verify(mockProvider).refund(any(RefundGatewayRequest.class));
        verify(mockRefundDao, times(1)).findById(refundId);
        verify(spiedRefundEntity).setStatus(RefundStatus.REFUND_SUBMITTED);
        verify(spiedRefundEntity).setGatewayTransactionId(refundEntity.getExternalId());

        verifyNoMoreInteractions(mockChargeService);
    }

    @Test
    public void shouldCreateARefundEntitySuccessfully() {
        String externalChargeId = "chargeId";
        Long refundAmount = 100L;
        Long accountId = 2L;
        String providerName = "worldpay";

        GatewayAccountEntity account = new GatewayAccountEntity(providerName, newHashMap(), TEST);
        account.setId(accountId);
        ChargeEntity chargeEntity = aValidChargeEntity()
                .withGatewayAccountEntity(account)
                .withTransactionId("transaction-id")
                .withExternalId(externalChargeId)
                .withStatus(CAPTURED)
                .build();

        doAnswer(invocation -> {
            ((RefundEntity) invocation.getArgument(0)).setId(refundId);
            return null;
        }).when(mockRefundDao).persist(any(RefundEntity.class));

        RefundEntity refundEntity = chargeRefundService.createRefundEntity(new RefundRequest(refundAmount, chargeEntity.getAmount(), userExternalId), Charge.from(chargeEntity));

        assertThat(refundEntity.getAmount(), is(refundAmount));
        assertThat(refundEntity.getStatus(), is(CREATED));
        assertThat(refundEntity.getChargeExternalId(), is(externalChargeId));
    }

    @Test
    public void shouldRefundSuccessfully_forSmartpay() {
        String externalChargeId = "chargeId";
        Long amount = 100L;
        Long accountId = 2L;
        String providerName = "smartpay";

        GatewayAccountEntity account = new GatewayAccountEntity(providerName, newHashMap(), TEST);
        account.setId(accountId);
        ChargeEntity chargeEntity = aValidChargeEntity()
                .withGatewayAccountEntity(account)
                .withTransactionId("transaction-id")
                .withExternalId(externalChargeId)
                .withStatus(CAPTURED)
                .build();

        String refundExternalId = "refundExternalId";
        RefundEntity refundEntity = aValidRefundEntity()
                .withExternalId(refundExternalId).withAmount(amount).build();
        RefundEntity spiedRefundEntity = spy(refundEntity);

        when(mockGatewayAccountDao.findById(accountId)).thenReturn(Optional.of(account));

        when(mockProviders.byName(SMARTPAY)).thenReturn(mockProvider);
        String reference = "refund-pspReference";
        setupSmartpayMock(reference, null);

        doAnswer(invocation -> {
            ((RefundEntity) invocation.getArgument(0)).setId(refundId);
            return null;
        }).when(mockRefundDao).persist(any(RefundEntity.class));

        when(mockRefundDao.findById(refundId)).thenReturn(Optional.of(spiedRefundEntity));

        ChargeRefundResponse gatewayResponse = chargeRefundService.doRefund(accountId, Charge.from(chargeEntity), new RefundRequest(amount, chargeEntity.getAmount(), userExternalId));

        assertThat(gatewayResponse.getGatewayRefundResponse().isSuccessful(), is(true));
        assertThat(gatewayResponse.getGatewayRefundResponse().getError().isPresent(), is(false));
        assertThat(gatewayResponse.getRefundEntity(), is(spiedRefundEntity));

        verify(mockRefundDao).persist(argThat(aRefundEntity(amount, chargeEntity)));
        verify(mockProvider).refund(argThat(aRefundRequestWith(chargeEntity, amount)));
        verify(mockRefundDao, times(1)).findById(refundId);
        verify(spiedRefundEntity).setStatus(RefundStatus.REFUND_SUBMITTED);
        verify(spiedRefundEntity, never()).setStatus(RefundStatus.REFUNDED);
        verify(spiedRefundEntity).setGatewayTransactionId(reference);

        verifyNoMoreInteractions(mockChargeDao);
    }

    @Test
    public void shouldOverrideGeneratedReferenceIfProviderReturnAReference() {
        String externalChargeId = "chargeId";
        Long amount = 100L;
        Long accountId = 2L;
        String providerName = "worldpay";

        GatewayAccountEntity account = new GatewayAccountEntity(providerName, newHashMap(), TEST);
        account.setId(accountId);
        String generatedReference = "generated-reference";

        String providerReference = "worldpay-reference";
        ChargeEntity chargeEntity = aValidChargeEntity()
                .withGatewayAccountEntity(account)
                .withTransactionId("transaction-id")
                .withExternalId(externalChargeId)
                .withStatus(CAPTURED)
                .build();

        String refundExternalId = "someExternalId";
        RefundEntity spiedRefundEntity = spy(aValidRefundEntity().withExternalId(refundExternalId).withReference(generatedReference).build());

        when(mockGatewayAccountDao.findById(accountId)).thenReturn(Optional.of(account));

        when(mockProviders.byName(WORLDPAY)).thenReturn(mockProvider);
        setupWorldpayMock(providerReference, null);

        doAnswer(invocation -> {
            ((RefundEntity) invocation.getArgument(0)).setId(refundId);
            return null;
        }).when(mockRefundDao).persist(any(RefundEntity.class));

        when(mockRefundDao.findById(refundId)).thenReturn(Optional.of(spiedRefundEntity));

        ChargeRefundResponse gatewayResponse = chargeRefundService.doRefund(accountId, Charge.from(chargeEntity), new RefundRequest(amount, chargeEntity.getAmount(), userExternalId));

        assertThat(gatewayResponse.getGatewayRefundResponse().isSuccessful(), is(true));
        assertThat(gatewayResponse.getRefundEntity(), is(spiedRefundEntity));
        verify(spiedRefundEntity).setGatewayTransactionId(providerReference);

    }

    @Test
    public void shouldStoreEmptyGatewayReferenceIfGatewayReturnsAnError() {
        String externalChargeId = "chargeId";
        Long amount = 100L;
        Long accountId = 2L;
        String providerName = "worldpay";

        GatewayAccountEntity account = new GatewayAccountEntity(providerName, newHashMap(), TEST);
        account.setId(accountId);
        String generatedReference = "generated-reference";
        ChargeEntity chargeEntity = aValidChargeEntity()
                .withGatewayAccountEntity(account)
                .withTransactionId("transaction-id")
                .withExternalId(externalChargeId)
                .withStatus(CAPTURED)
                .build();

        String refundExternalId = "someExternalId";
        RefundEntity spiedRefundEntity = spy(aValidRefundEntity().withExternalId(refundExternalId).withReference(generatedReference).build());

        when(mockGatewayAccountDao.findById(accountId)).thenReturn(Optional.of(account));

        when(mockProviders.byName(WORLDPAY)).thenReturn(mockProvider);
        setupWorldpayMock(null, "error-code");

        doAnswer(invocation -> {
            ((RefundEntity) invocation.getArgument(0)).setId(refundId);
            return null;
        }).when(mockRefundDao).persist(any(RefundEntity.class));

        when(mockRefundDao.findById(refundId)).thenReturn(Optional.of(spiedRefundEntity));

        ChargeRefundResponse gatewayResponse = chargeRefundService.doRefund(accountId, Charge.from(chargeEntity), new RefundRequest(amount, chargeEntity.getAmount(), userExternalId));
        assertThat(gatewayResponse.getGatewayRefundResponse().isSuccessful(), is(false));
        assertThat(gatewayResponse.getRefundEntity(), is(spiedRefundEntity));
        verify(spiedRefundEntity, never()).setGatewayTransactionId(anyString());
    }

    @Test
    public void shouldRefundAndSendEmail_whenGatewayRefundStateIsComplete_forChargeWithNoCorporateSurcharge() {
        String externalChargeId = "chargeId";
        Long accountId = 2L;
        String providerName = "sandbox";

        GatewayAccountEntity account = new GatewayAccountEntity(providerName, newHashMap(), TEST);
        account.setId(accountId);
        String reference = "reference";

        ChargeEntity chargeEntity = aValidChargeEntity()
                .withGatewayAccountEntity(account)
                .withTransactionId(reference)
                .withExternalId(externalChargeId)
                .withStatus(CAPTURED)
                .build();
        Optional<Charge> optionalCharge = Optional.of(Charge.from(chargeEntity));

        testSuccessfulRefund(chargeEntity, 100L, chargeEntity.getAmount());
    }

    @Test
    public void shouldRefundAndSendEmail_whenGatewayRefundStatusIsComplete_forChargeWithCorporateSurcharge() {
        String externalChargeId = "chargeId";
        Long accountId = 2L;
        String providerName = "sandbox";

        GatewayAccountEntity account = new GatewayAccountEntity(providerName, newHashMap(), TEST);
        account.setId(accountId);
        String reference = "reference";

        long corporateSurcharge = 50L;
        ChargeEntity chargeEntity = aValidChargeEntity()
                .withCorporateSurcharge(corporateSurcharge)
                .withGatewayAccountEntity(account)
                .withTransactionId(reference)
                .withExternalId(externalChargeId)
                .withStatus(CAPTURED)
                .build();

        // when there is a corporate surcharge we expect the amount available for refund to include this
        long amountAvailableForRefund = chargeEntity.getAmount() + corporateSurcharge;

        testSuccessfulRefund(chargeEntity, amountAvailableForRefund, amountAvailableForRefund);
    }

    private void testSuccessfulRefund(ChargeEntity chargeEntity, Long refundAmount, Long amountAvailableForRefund) {
        RefundEntity spiedRefundEntity = spy(aValidRefundEntity().withAmount(refundAmount).build());

        Long accountId = chargeEntity.getGatewayAccount().getId();

        when(mockProviders.byName(SANDBOX)).thenReturn(mockProvider);

        setupSandboxMock(chargeEntity.getGatewayTransactionId(), null);

        doAnswer(invocation -> {
            ((RefundEntity) invocation.getArgument(0)).setId(refundId);
            spiedRefundEntity.setId(refundId);
            return null;
        }).when(mockRefundDao).persist(any(RefundEntity.class));

        when(mockRefundDao.findById(refundId)).thenReturn(Optional.of(spiedRefundEntity));

        when(mockGatewayAccountDao.findById(accountId)).thenReturn(Optional.of(chargeEntity.getGatewayAccount()));

        Charge charge = Charge.from(chargeEntity);
        ChargeRefundResponse gatewayResponse = chargeRefundService.doRefund(accountId, charge, new RefundRequest(refundAmount, amountAvailableForRefund, userExternalId));

        assertThat(gatewayResponse.getGatewayRefundResponse().isSuccessful(), is(true));
        assertThat(gatewayResponse.getGatewayRefundResponse().getError().isPresent(), is(false));
        assertThat(gatewayResponse.getRefundEntity(), is(spiedRefundEntity));

        verify(mockRefundDao, times(2)).findById(refundId);
        verify(mockUserNotificationService).sendRefundIssuedEmail(spiedRefundEntity, charge, chargeEntity.getGatewayAccount());

        // should set refund status to both REFUND_SUBMITTED and REFUNDED in order - as gateway refund state is COMPLETE
        verify(spiedRefundEntity).setStatus(RefundStatus.REFUND_SUBMITTED);
        verify(spiedRefundEntity).setStatus(RefundStatus.REFUNDED);
    }

    @Test
    public void shouldFailWhenChargeRefundIsNotAvailable() {
        String externalChargeId = "chargeId";
        Long accountId = 2L;
        GatewayAccountEntity account = new GatewayAccountEntity("sandbox", newHashMap(), TEST);
        account.setId(accountId);
        ChargeEntity chargeEntity = aValidChargeEntity()
                .withGatewayAccountEntity(account)
                .withExternalId(externalChargeId)
                .withTransactionId("transactionId")
                .withStatus(AUTHORISATION_SUCCESS)
                .build();

        when(mockGatewayAccountDao.findById(accountId)).thenReturn(Optional.of(account));

        expectedException.expect(RefundException.class);
        expectedException.expectMessage("HTTP 412 Precondition Failed");

        chargeRefundService.doRefund(accountId, Charge.from(chargeEntity), new RefundRequest(100L, 0, userExternalId));

        verify(mockChargeDao).findByExternalIdAndGatewayAccount(externalChargeId, accountId);
        verifyNoMoreInteractions(mockChargeDao, mockRefundDao, mockChargeService);
    }

    @Test
    public void shouldFailWhenAmountAvailableForRefundMismatchesWithoutCorporateSurcharge() {
        Long amount = 1000L;
        String externalChargeId = "chargeId";
        Long accountId = 2L;
        GatewayAccountEntity account = new GatewayAccountEntity("sandbox", newHashMap(), TEST);
        account.setId(accountId);
        ChargeEntity chargeEntity = aValidChargeEntity()
                .withAmount(amount)
                .withExternalId(externalChargeId)
                .withGatewayAccountEntity(account)
                .withTransactionId("transactionId")
                .withStatus(AUTHORISATION_SUCCESS)
                .build();

        when(mockGatewayAccountDao.findById(accountId)).thenReturn(Optional.of(account));
        when(mockRefundDao.findRefundsByChargeExternalId(chargeEntity.getExternalId())).thenReturn(List.of());

        expectedException.expect(RefundException.class);
        expectedException.expectMessage("HTTP 412 Precondition Failed");

        chargeRefundService.doRefund(accountId, Charge.from(chargeEntity), new RefundRequest(amount, amount + 1, userExternalId));

        verify(mockChargeDao).findByExternalIdAndGatewayAccount(externalChargeId, accountId);
        verifyNoMoreInteractions(mockChargeDao, mockRefundDao);
    }

    @Test
    public void shouldFailWhenAmountAvailableForRefundWithCorporateSurchargeMismatches() {
        Long amount = 1000L;
        Long corporateSurcharge = 250L;
        String externalChargeId = "chargeId";
        Long accountId = 2L;
        GatewayAccountEntity account = new GatewayAccountEntity("sandbox", newHashMap(), TEST);
        account.setId(accountId);
        ChargeEntity chargeEntity = aValidChargeEntity()
                .withAmount(amount)
                .withExternalId(externalChargeId)
                .withCorporateSurcharge(corporateSurcharge)
                .withGatewayAccountEntity(account)
                .withTransactionId("transactionId")
                .withStatus(AUTHORISATION_SUCCESS)
                .build();

        when(mockGatewayAccountDao.findById(accountId)).thenReturn(Optional.of(account));

        when(mockRefundDao.findRefundsByChargeExternalId(chargeEntity.getExternalId())).thenReturn(List.of());

        expectedException.expect(RefundException.class);
        expectedException.expectMessage("HTTP 412 Precondition Failed");

        // this should fail because amountAvailableForRefund is not including corporate surcharge
        chargeRefundService.doRefund(accountId, Charge.from(chargeEntity), new RefundRequest(amount, amount, userExternalId));

        verify(mockChargeDao).findByExternalIdAndGatewayAccount(externalChargeId, accountId);
        verifyNoMoreInteractions(mockChargeDao, mockRefundDao);
    }

    @Test
    public void shouldUpdateRefundRecordToFailWhenRefundFails() {
        String externalChargeId = "chargeId";
        Long amount = 100L;
        Long accountId = 2L;
        String providerName = "worldpay";

        GatewayAccountEntity account = new GatewayAccountEntity(providerName, newHashMap(), TEST);
        account.setId(accountId);
        ChargeEntity capturedCharge = aValidChargeEntity()
                .withGatewayAccountEntity(account)
                .withTransactionId("transaction-id")
                .withExternalId(externalChargeId)
                .withStatus(CAPTURED)
                .build();

        String refundReference = "someReference";
        String refundExternalId = "refundExternalId";
        RefundEntity spiedRefundEntity = spy(aValidRefundEntity()
                .withReference(refundReference)
                .withExternalId(refundExternalId)
                .withAmount(amount)
                .build()
        );

        when(mockGatewayAccountDao.findById(accountId)).thenReturn(Optional.of(account));

        when(mockProviders.byName(WORLDPAY)).thenReturn(mockProvider);

        setupWorldpayMock(null, "error-code");

        doAnswer(invocation -> {
            ((RefundEntity) invocation.getArgument(0)).setId(refundId);
            return null;
        }).when(mockRefundDao).persist(any(RefundEntity.class));

        when(mockRefundDao.findById(refundId)).thenReturn(Optional.of(spiedRefundEntity));

        ChargeRefundResponse gatewayResponse = chargeRefundService.doRefund(accountId, Charge.from(capturedCharge), new RefundRequest(amount, capturedCharge.getAmount(), userExternalId));

        assertThat(gatewayResponse.getGatewayRefundResponse().getError().isPresent(), is(true));
        assertThat(gatewayResponse.getGatewayRefundResponse().toString(),
                is("Randompay refund response (errorCode: error-code)"));
        assertThat(gatewayResponse.getGatewayRefundResponse().getError().get().getErrorType(), is(ErrorType.GENERIC_GATEWAY_ERROR));

        verify(mockRefundDao).persist(argThat(aRefundEntity(amount, capturedCharge)));
        verify(mockProvider).refund(argThat(aRefundRequestWith(capturedCharge, amount)));
        verify(mockRefundDao, times(1)).findById(refundId);
        verify(spiedRefundEntity).setStatus(RefundStatus.REFUND_ERROR);
    }

    @Test
    public void shouldOfferRefundStateTransition() {
        ChargeEntity charge = aValidChargeEntity()
                .build();
        RefundEntity refundEntity = aValidRefundEntity().withAmount(100L).build();

        chargeRefundService.transitionRefundState(refundEntity, CREATED);
        verify(mockStateTransitionService).offerRefundStateTransition(refundEntity, CREATED);
    }

    private ArgumentMatcher<RefundEntity> aRefundEntity(long amount, ChargeEntity chargeEntity) {
        return object -> {
            RefundEntity refundEntity = ((RefundEntity) object);
            return refundEntity.getAmount() == amount &&
                    refundEntity.getChargeExternalId().equals(chargeEntity.getExternalId());
        };
    }

    private ArgumentMatcher<RefundGatewayRequest> aRefundRequestWith(ChargeEntity capturedCharge, long amountInPence) {
        return object -> {
            RefundGatewayRequest refundGatewayRequest = ((RefundGatewayRequest) object);
            return refundGatewayRequest.getGatewayAccount().equals(capturedCharge.getGatewayAccount()) &&
                    refundGatewayRequest.getTransactionId().equals(capturedCharge.getGatewayTransactionId()) &&
                    refundGatewayRequest.getAmount().equals(String.valueOf(amountInPence));
        };
    }

    private void setupWorldpayMock(String reference, String errorCode) {
        WorldpayRefundResponse worldpayResponse = mock(WorldpayRefundResponse.class);
        when(worldpayResponse.getReference()).thenReturn(Optional.ofNullable(reference));
        when(worldpayResponse.getErrorCode()).thenReturn(errorCode);
        when(worldpayResponse.stringify()).thenReturn("Randompay refund response (errorCode: " + errorCode + ")");

        GatewayRefundResponse.RefundState refundState;
        if (isNotBlank(errorCode)) {
            refundState = GatewayRefundResponse.RefundState.ERROR;
        } else {
            refundState = GatewayRefundResponse.RefundState.PENDING;
        }

        GatewayRefundResponse gatewayRefundResponse =
                GatewayRefundResponse.fromBaseRefundResponse(worldpayResponse, refundState);

        when(mockProvider.refund(any())).thenReturn(gatewayRefundResponse);
    }

    private void setupSandboxMock(String reference, String errorCode) {
        BaseRefundResponse baseRefundResponse = BaseRefundResponse.fromReference(reference, SANDBOX);

        GatewayRefundResponse gatewayRefundResponse = GatewayRefundResponse.fromBaseRefundResponse(baseRefundResponse,
                GatewayRefundResponse.RefundState.COMPLETE);

        when(mockProvider.refund(any())).thenReturn(gatewayRefundResponse);
    }

    private void setupSmartpayMock(String reference, String errorCode) {
        SmartpayRefundResponse smartpayRefundResponse = mock(SmartpayRefundResponse.class);
        when(smartpayRefundResponse.getReference()).thenReturn(Optional.ofNullable(reference));
        when(smartpayRefundResponse.getErrorCode()).thenReturn(errorCode);

        GatewayRefundResponse.RefundState refundState;
        if (isNotBlank(errorCode)) {
            refundState = GatewayRefundResponse.RefundState.ERROR;
        } else {
            refundState = GatewayRefundResponse.RefundState.PENDING;
        }

        GatewayRefundResponse gatewayRefundResponse =
                GatewayRefundResponse.fromBaseRefundResponse(smartpayRefundResponse, refundState);

        when(mockProvider.refund(any())).thenReturn(gatewayRefundResponse);
    }
}
