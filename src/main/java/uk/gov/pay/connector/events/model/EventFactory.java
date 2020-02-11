package uk.gov.pay.connector.events.model;

import uk.gov.pay.connector.charge.model.domain.Charge;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.service.ChargeService;
import uk.gov.pay.connector.chargeevent.dao.ChargeEventDao;
import uk.gov.pay.connector.chargeevent.model.domain.ChargeEventEntity;
import uk.gov.pay.connector.common.model.domain.PaymentGatewayStateTransitions;
import uk.gov.pay.connector.events.eventdetails.charge.RefundAvailabilityUpdatedEventDetails;
import uk.gov.pay.connector.events.eventdetails.refund.RefundEventWithReferenceDetails;
import uk.gov.pay.connector.events.exception.EventCreationException;
import uk.gov.pay.connector.events.model.charge.CaptureConfirmed;
import uk.gov.pay.connector.events.model.charge.CaptureSubmitted;
import uk.gov.pay.connector.events.model.charge.PaymentCreated;
import uk.gov.pay.connector.events.model.charge.PaymentDetailsEntered;
import uk.gov.pay.connector.events.model.charge.PaymentEvent;
import uk.gov.pay.connector.events.model.charge.PaymentNotificationCreated;
import uk.gov.pay.connector.events.model.charge.RefundAvailabilityUpdated;
import uk.gov.pay.connector.events.model.refund.RefundCreatedByService;
import uk.gov.pay.connector.events.model.refund.RefundCreatedByUser;
import uk.gov.pay.connector.events.model.refund.RefundError;
import uk.gov.pay.connector.events.model.refund.RefundEvent;
import uk.gov.pay.connector.gateway.PaymentProviders;
import uk.gov.pay.connector.queue.PaymentStateTransition;
import uk.gov.pay.connector.queue.RefundStateTransition;
import uk.gov.pay.connector.queue.StateTransition;
import uk.gov.pay.connector.refund.dao.RefundDao;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;
import uk.gov.pay.connector.refund.model.domain.RefundHistory;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EventFactory {
    private final ChargeService chargeService;
    private final RefundDao refundDao;
    private final PaymentProviders paymentProviders;
    private final ChargeEventDao chargeEventDao;
    private static final List<Class<? extends Event>> EVENTS_AFFECTING_REFUNDABILITY = List.of(
            RefundCreatedByUser.class,
            RefundCreatedByService.class,
            RefundError.class,
            PaymentCreated.class,
            CaptureSubmitted.class
    );
    
    private static final List<Class> EVENTS_LEADING_TO_TERMINAL_STATE = 
            PaymentGatewayStateTransitions.getAllEventsResultingInTerminalState();

    @Inject
    public EventFactory(ChargeService chargeService, RefundDao refundDao, ChargeEventDao chargeEventDao, PaymentProviders paymentProviders) {
        this.chargeService = chargeService;
        this.refundDao = refundDao;
        this.chargeEventDao = chargeEventDao;
        this.paymentProviders = paymentProviders;
    }

    public List<Event> createEvents(StateTransition stateTransition) throws EventCreationException {
        if (stateTransition instanceof PaymentStateTransition) {
            PaymentStateTransition paymentStateTransition = (PaymentStateTransition) stateTransition;
            return createPaymentEvents(paymentStateTransition);
        } else if (stateTransition instanceof RefundStateTransition) {
            RefundStateTransition refundStateTransition = (RefundStateTransition) stateTransition;
            return createRefundEvents(refundStateTransition);
        } else {
            throw new EventCreationException(stateTransition.getIdentifier());
        }
    }

    private List<Event> createPaymentEvents(PaymentStateTransition paymentStateTransition) throws EventCreationException {
        ChargeEventEntity chargeEvent = chargeEventDao.findById(ChargeEventEntity.class, paymentStateTransition.getChargeEventId())
                .orElseThrow(() -> new EventCreationException(String.valueOf(paymentStateTransition.getChargeEventId())));

         PaymentEvent paymentEvent = createPaymentEvent(chargeEvent, paymentStateTransition.getStateTransitionEventClass());

        Optional<Event> refundAvailabilityEvent = createRefundAvailabilityUpdatedEvent(
                chargeEvent.getChargeEntity().getExternalId(),
                chargeEvent.getUpdated(),
                paymentStateTransition.getStateTransitionEventClass()
        );

        return Stream.of(Optional.of(paymentEvent), refundAvailabilityEvent)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    private List<Event> createRefundEvents(RefundStateTransition refundStateTransition) throws EventCreationException {
        RefundHistory refundHistory = refundDao.getRefundHistoryByRefundExternalIdAndRefundStatus(
                refundStateTransition.getRefundExternalId(),
                refundStateTransition.getRefundStatus())
                .orElseThrow(() -> new EventCreationException(refundStateTransition.getIdentifier()));
        ChargeEntity chargeEntity = chargeService.findChargeByExternalId(refundHistory.getChargeExternalId());

        Event refundEvent = createRefundEvent(refundHistory, refundStateTransition.getStateTransitionEventClass(),
                chargeEntity.getGatewayAccount().getId());
        Optional<Event> refundAvailabilityEvent = createRefundAvailabilityUpdatedEvent(
                refundHistory.getChargeExternalId(),
                refundHistory.getHistoryStartDate(),
                refundStateTransition.getStateTransitionEventClass()
        );

        return Stream.of(Optional.of(refundEvent), refundAvailabilityEvent)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    public static PaymentEvent createPaymentEvent(ChargeEventEntity chargeEvent, Class<? extends PaymentEvent> eventClass) {
        try {
            if (eventClass == PaymentCreated.class) {
                return PaymentCreated.from(chargeEvent);
            } else if (eventClass == PaymentDetailsEntered.class) {
                return PaymentDetailsEntered.from(chargeEvent);
            } else if (eventClass == CaptureSubmitted.class) {
                return CaptureSubmitted.from(chargeEvent);
            } else if (eventClass == CaptureConfirmed.class) {
                return CaptureConfirmed.from(chargeEvent);
            } else if (eventClass == PaymentNotificationCreated.class) {
                return PaymentNotificationCreated.from(chargeEvent);
            } else {
                return eventClass.getConstructor(String.class, ZonedDateTime.class).newInstance(
                        chargeEvent.getChargeEntity().getExternalId(),
                        chargeEvent.getUpdated()
                );
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("Could not construct payment event: %s", eventClass));
        }
    }

    public static Event createRefundEvent(RefundHistory refundHistory, Class<? extends RefundEvent> eventClass, Long gatewayAccountId) {
        try {
            if (eventClass == RefundCreatedByService.class) {
                return RefundCreatedByService.from(refundHistory, gatewayAccountId);
            } else if (eventClass == RefundCreatedByUser.class) {
                return RefundCreatedByUser.from(refundHistory, gatewayAccountId);
            } else {
                return eventClass.getConstructor(String.class, String.class, RefundEventWithReferenceDetails.class, ZonedDateTime.class).newInstance(
                        refundHistory.getExternalId(),
                        refundHistory.getChargeExternalId(),
                        new RefundEventWithReferenceDetails(refundHistory.getReference()),
                        refundHistory.getHistoryStartDate()
                );
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalArgumentException(String.format("Could not construct refund event: %s", eventClass));
        }
    }

    private Optional<Event> createRefundAvailabilityUpdatedEvent(
            String chargeExternalId, ZonedDateTime eventTimestamp, Class eventClass) throws EventCreationException {
        if (EVENTS_AFFECTING_REFUNDABILITY.contains(eventClass) || EVENTS_LEADING_TO_TERMINAL_STATE.contains(eventClass)) {
            RefundAvailabilityUpdated refundAvailabilityUpdatedEvent =
                    Optional.ofNullable(chargeService.findChargeByExternalId(chargeExternalId))
                    .map(charge -> {
                        List<RefundEntity> refundEntityList = refundDao.findRefundsByChargeExternalId(chargeExternalId);
                        return new RefundAvailabilityUpdated(
                                        chargeExternalId,
                                        RefundAvailabilityUpdatedEventDetails.from(
                                                charge,
                                                refundEntityList,
                                                paymentProviders
                                                        .byName(charge.getPaymentGatewayName())
                                                        .getExternalChargeRefundAvailability(Charge.from(charge), refundEntityList)
                                        ),
                                        eventTimestamp
                                );
                            }
                    )
                    .orElseThrow(() -> new EventCreationException(chargeExternalId));

            return Optional.of(refundAvailabilityUpdatedEvent);
        }

        return Optional.empty();
    }
}
