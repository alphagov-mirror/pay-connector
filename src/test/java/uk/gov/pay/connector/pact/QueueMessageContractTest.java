package uk.gov.pay.connector.pact;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junit.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junit.PactRunner;
import au.com.dius.pact.provider.junit.Provider;
import au.com.dius.pact.provider.junit.loader.PactBroker;
import au.com.dius.pact.provider.junit.loader.PactBrokerAuth;
import au.com.dius.pact.provider.junit.target.AmqpTarget;
import au.com.dius.pact.provider.junit.target.Target;
import au.com.dius.pact.provider.junit.target.TestTarget;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.runner.RunWith;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;
import uk.gov.pay.commons.model.charge.ExternalMetadata;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.ChargeEntityFixture;
import uk.gov.pay.connector.charge.model.domain.ChargeStatus;
import uk.gov.pay.connector.chargeevent.model.domain.ChargeEventEntity;
import uk.gov.pay.connector.events.eventdetails.charge.CaptureConfirmedEventDetails;
import uk.gov.pay.connector.events.eventdetails.charge.CaptureSubmittedEventDetails;
import uk.gov.pay.connector.events.eventdetails.charge.PaymentCreatedEventDetails;
import uk.gov.pay.connector.events.eventdetails.charge.PaymentDetailsEnteredEventDetails;
import uk.gov.pay.connector.events.model.charge.CaptureConfirmed;
import uk.gov.pay.connector.events.model.charge.CaptureSubmitted;
import uk.gov.pay.connector.events.model.charge.PaymentCreated;
import uk.gov.pay.connector.events.model.charge.PaymentDetailsEntered;
import uk.gov.pay.connector.events.model.charge.PaymentNotificationCreated;
import uk.gov.pay.connector.events.model.payout.PayoutCreated;
import uk.gov.pay.connector.events.model.refund.RefundCreatedByUser;
import uk.gov.pay.connector.events.model.refund.RefundSubmitted;
import uk.gov.pay.connector.events.model.refund.RefundSucceeded;
import uk.gov.pay.connector.gateway.stripe.json.StripePayout;
import uk.gov.pay.connector.refund.model.domain.RefundHistory;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;

import java.time.ZonedDateTime;
import java.util.Map;

import static uk.gov.pay.commons.model.Source.CARD_API;
import static uk.gov.pay.commons.model.Source.CARD_EXTERNAL_TELEPHONE;
import static uk.gov.pay.connector.events.model.payout.PayoutCreated.from;
import static uk.gov.pay.connector.model.domain.AuthCardDetailsFixture.anAuthCardDetails;
import static uk.gov.pay.connector.pact.RefundHistoryEntityFixture.aValidRefundHistoryEntity;

@RunWith(PactRunner.class)
@Provider("connector")
@PactBroker(scheme = "https", host = "pact-broker-test.cloudapps.digital", tags = {"${PACT_CONSUMER_TAG}"},
        authentication = @PactBrokerAuth(username = "${PACT_BROKER_USERNAME}", password = "${PACT_BROKER_PASSWORD}"),
        consumers = {"ledger"})
@IgnoreNoPactsToVerify
public class QueueMessageContractTest {

    @TestTarget
    public final Target target = new AmqpTarget();

    private String resourceId = "anExternalResourceId";

    @PactVerifyProvider("a payment created message")
    public String verifyPaymentCreatedEvent() throws Exception {
        ChargeEntity charge = ChargeEntityFixture.aValidChargeEntity()
                .withExternalMetadata(new ExternalMetadata(ImmutableMap.of("key", "value")))
                .withCorporateSurcharge(55L)
                .withSource(CARD_API)
                .withCardDetails(anAuthCardDetails().getCardDetailsEntity())
                .build();

        PaymentCreated paymentCreatedEvent = new PaymentCreated(
                resourceId,
                PaymentCreatedEventDetails.from(charge),
                ZonedDateTime.now()
        );

        return paymentCreatedEvent.toJsonString();
    }

    @PactVerifyProvider("a capture confirmed message")
    public String verifyCaptureConfirmedEvent() throws JsonProcessingException {
        ChargeEventEntity chargeEventEntity = ChargeEventEntityFixture
                .aValidChargeEventEntity()
                .withGatewayEventDate(ZonedDateTime.now())
                .build();

        CaptureConfirmed captureConfirmedEvent = new CaptureConfirmed(
                resourceId,
                CaptureConfirmedEventDetails.from(chargeEventEntity),
                ZonedDateTime.now()
        );

        return captureConfirmedEvent.toJsonString();
    }

    @PactVerifyProvider("a payment details entered message")
    public String verifyPaymentDetailsEnteredEvent() throws JsonProcessingException {
        ChargeEntity charge = ChargeEntityFixture.aValidChargeEntity()
                .withTransactionId("gateway_transaction_id")
                .withCorporateSurcharge(55L)
                .withCardDetails(anAuthCardDetails().getCardDetailsEntity())
                .build();

        PaymentDetailsEntered captureConfirmedEvent = new PaymentDetailsEntered(
                resourceId,
                PaymentDetailsEnteredEventDetails.from(charge),
                ZonedDateTime.now()
        );

        return captureConfirmedEvent.toJsonString();
    }

    @PactVerifyProvider("a capture submitted message")
    public String verifyCaptureSubmittedEvent() throws JsonProcessingException {
        ChargeEventEntity chargeEventEntity = ChargeEventEntityFixture
                .aValidChargeEventEntity()
                .withGatewayEventDate(ZonedDateTime.now())
                .build();

        CaptureSubmitted captureSubmittedEvent = new CaptureSubmitted(
                resourceId,
                CaptureSubmittedEventDetails.from(chargeEventEntity),
                ZonedDateTime.now()
        );

        return captureSubmittedEvent.toJsonString();
    }

    @PactVerifyProvider("a refund created by user message")
    public String verifyRefundCreatedByUserEvent() throws JsonProcessingException {
        RefundHistory refundHistory = aValidRefundHistoryEntity()
                .withUserExternalId(RandomStringUtils.randomAlphanumeric(10))
                .withUserEmail("test@example.com")
                .build();
        RefundCreatedByUser refundCreatedByUser = RefundCreatedByUser.from(refundHistory, 1L);

        return refundCreatedByUser.toJsonString();
    }

    @PactVerifyProvider("a refund submitted message")
    public String verifyRefundSubmittedEvent() throws JsonProcessingException {
        RefundHistory refundHistory = aValidRefundHistoryEntity()
                .build();
        RefundSubmitted refundSubmitted = RefundSubmitted.from(refundHistory);

        return refundSubmitted.toJsonString();
    }

    @PactVerifyProvider("a refund succeeded message")
    public String verifyRefundedEvent() throws JsonProcessingException {
        RefundHistory refundHistory = aValidRefundHistoryEntity()
                .withStatus(RefundStatus.REFUNDED.getValue())
                .withReference(RandomStringUtils.randomAlphanumeric(14))
                .build();
        RefundSucceeded refundSucceeded = RefundSucceeded.from(refundHistory);

        return refundSucceeded.toJsonString();
    }

    @PactVerifyProvider("a payment notification created message")
    public String verifyPaymentNotificationCreatedEvent() throws JsonProcessingException {
        ExternalMetadata externalMetadata = new ExternalMetadata(Map.of(
                "processor_id", "processorId",
                "auth_code", "012345",
                "telephone_number", "+447700900796",
                "status", "success",
                "authorised_date", "2018-02-21T16:05:33Z",
                "created_date", "2018-02-21T15:05:13Z"));
        ChargeEntity charge = ChargeEntityFixture.aValidChargeEntity()
                .withStatus(ChargeStatus.PAYMENT_NOTIFICATION_CREATED)
                .withGatewayTransactionId("providerId")
                .withEmail("j.doe@example.org")
                .withSource(CARD_EXTERNAL_TELEPHONE)
                .withCardDetails(anAuthCardDetails().withAddress(null).getCardDetailsEntity())
                .withExternalMetadata(externalMetadata)
                .build();
        ChargeEventEntity chargeEventEntity = ChargeEventEntityFixture
                .aValidChargeEventEntity()
                .withCharge(charge)
                .build();

        PaymentNotificationCreated paymentNotificationCreated = PaymentNotificationCreated.from(chargeEventEntity);

        return paymentNotificationCreated.toJsonString();
    }

    @PactVerifyProvider("a payout created message")
    public String verifyPayoutCreatedEvent() throws JsonProcessingException {
        StripePayout payout = new StripePayout("po_1234567890", 1000L, 1589395533L,
                1589395500L, "pending", "bank_account", "SERVICE NAME");
        PayoutCreated payoutCreated = from(123456789L, payout);

        return payoutCreated.toJsonString();
    }
}
