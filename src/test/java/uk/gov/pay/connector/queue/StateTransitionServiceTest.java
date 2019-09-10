package uk.gov.pay.connector.queue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.connector.charge.model.domain.ChargeStatus;
import uk.gov.pay.connector.chargeevent.model.domain.ChargeEventEntity;
import uk.gov.pay.connector.events.EventService;
import uk.gov.pay.connector.events.model.charge.PaymentStarted;
import uk.gov.pay.connector.events.model.refund.RefundCreatedByUser;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;

import static java.time.ZonedDateTime.now;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.AUTHORISATION_READY;
import static uk.gov.pay.connector.charge.model.domain.ChargeStatus.ENTERING_CARD_DETAILS;
import static uk.gov.pay.connector.events.model.ResourceType.PAYMENT;
import static uk.gov.pay.connector.events.model.ResourceType.REFUND;
import static uk.gov.pay.connector.model.domain.RefundEntityFixture.aValidRefundEntity;
import static uk.gov.pay.connector.refund.model.domain.RefundStatus.CREATED;

@RunWith(MockitoJUnitRunner.class)
public class StateTransitionServiceTest {

    StateTransitionService stateTransitionService;

    @Mock
    StateTransitionQueue mockStateTransitionQueue;
    @Mock
    EventService mockEventService;

    @Before
    public void setUp() {
        stateTransitionService = new StateTransitionService(mockStateTransitionQueue, mockEventService);
    }

    @Test
    public void shouldOfferPaymentStateTransitionMessageForAValidStateTransitionIntoNonLockingState() {
        ChargeEventEntity chargeEvent = mock(ChargeEventEntity.class);
        when(chargeEvent.getId()).thenReturn(100L);
        when(chargeEvent.getUpdated()).thenReturn(now());

        stateTransitionService.offerPaymentStateTransition("external-id", ChargeStatus.CREATED, ENTERING_CARD_DETAILS, chargeEvent);
        ArgumentCaptor<PaymentStateTransition> paymentStateTransitionArgumentCaptor = ArgumentCaptor.forClass(PaymentStateTransition.class);
        verify(mockStateTransitionQueue).offer(paymentStateTransitionArgumentCaptor.capture());

        assertThat(paymentStateTransitionArgumentCaptor.getValue().getChargeEventId(), is(100L));
        assertThat(paymentStateTransitionArgumentCaptor.getValue().getStateTransitionEventClass(), is(PaymentStarted.class));

        verify(mockEventService).recordOfferedEvent(PAYMENT, "external-id", "PAYMENT_STARTED", chargeEvent.getUpdated());
    }

    @Test
    public void shouldNotOfferStateTransitionMessageForAValidStateTransitionIntoLockingState() {
        ChargeEventEntity chargeEvent = mock(ChargeEventEntity.class);
        stateTransitionService.offerPaymentStateTransition("external-id", ChargeStatus.CREATED, AUTHORISATION_READY, chargeEvent);

        verifyNoMoreInteractions(mockStateTransitionQueue);
        verifyNoMoreInteractions(mockEventService);
    }

    @Test
    public void shouldOfferRefundStateTransitionMessageForAValidStateTransition() {
        RefundEntity refundEntity = aValidRefundEntity()
                .withExternalId("external-id")
                .withStatus(CREATED)
                .build();
        
        stateTransitionService.offerRefundStateTransition(refundEntity, CREATED);
        
        ArgumentCaptor<RefundStateTransition> refundStateTransitionArgumentCaptor = ArgumentCaptor.forClass(RefundStateTransition.class);
        verify(mockStateTransitionQueue).offer(refundStateTransitionArgumentCaptor.capture());

        assertThat(refundStateTransitionArgumentCaptor.getValue().getRefundExternalId(), is(refundEntity.getExternalId()));
        assertThat(refundStateTransitionArgumentCaptor.getValue().getStateTransitionEventClass(), is(RefundCreatedByUser.class));

        verify(mockEventService).recordOfferedEvent(REFUND, "external-id", "REFUND_CREATED_BY_USER", null);
    }
}
