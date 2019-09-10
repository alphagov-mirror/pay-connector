package uk.gov.pay.connector.queue;

import com.google.inject.persist.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.charge.model.domain.ChargeStatus;
import uk.gov.pay.connector.chargeevent.model.domain.ChargeEventEntity;
import uk.gov.pay.connector.common.model.domain.PaymentGatewayStateTransitions;
import uk.gov.pay.connector.events.EventService;
import uk.gov.pay.connector.events.model.Event;
import uk.gov.pay.connector.events.model.ResourceType;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;
import uk.gov.pay.connector.refund.service.RefundStateEventMap;

import javax.inject.Inject;

public class StateTransitionService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private StateTransitionQueue stateTransitionQueue;
    private EventService eventService;

    @Inject
    public StateTransitionService(StateTransitionQueue stateTransitionQueue,
                                  EventService eventService) {
        this.stateTransitionQueue = stateTransitionQueue;
        this.eventService = eventService;
    }

    @Transactional
    public void offerRefundStateTransition(RefundEntity refundEntity, RefundStatus refundStatus) {
        Class refundEventClass = RefundStateEventMap.calculateRefundEventClass(refundEntity.getUserExternalId(), refundStatus);
        RefundStateTransition refundStateTransition = new RefundStateTransition(refundEntity.getExternalId(), refundStatus, refundEventClass);
        stateTransitionQueue.offer(refundStateTransition);

        eventService.recordOfferedEvent(ResourceType.REFUND,
                refundEntity.getExternalId(),
                Event.eventTypeForClass(refundEventClass),
                null);
    }

    @Transactional
    public void offerPaymentStateTransition(String externalId, ChargeStatus fromChargeState, ChargeStatus targetChargeState, ChargeEventEntity chargeEventEntity) {
        PaymentGatewayStateTransitions.getInstance()
                .getEventForTransition(fromChargeState, targetChargeState)
                .ifPresent(eventType -> {
                    PaymentStateTransition transition = new PaymentStateTransition(chargeEventEntity.getId(), eventType);
                    stateTransitionQueue.offer(transition);
                    logger.info("Offered payment state transition to emitter queue [from={}] [to={}] [chargeEventId={}] [chargeId={}]", fromChargeState, targetChargeState, chargeEventEntity.getId(), externalId);

                    eventService.recordOfferedEvent(ResourceType.PAYMENT,
                            externalId,
                            Event.eventTypeForClass(eventType),
                            chargeEventEntity.getUpdated()
                    );
                });
    }
}
