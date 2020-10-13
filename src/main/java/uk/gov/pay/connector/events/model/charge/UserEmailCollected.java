package uk.gov.pay.connector.events.model.charge;

import uk.gov.pay.connector.charge.exception.ChargeEventNotFoundRuntimeException;
import uk.gov.pay.connector.charge.model.domain.ChargeEntity;
import uk.gov.pay.connector.charge.model.domain.ChargeStatus;
import uk.gov.pay.connector.chargeevent.model.domain.ChargeEventEntity;
import uk.gov.pay.connector.events.eventdetails.charge.UserEmailCollectedEventDetails;

import java.time.ZonedDateTime;

public class UserEmailCollected extends PaymentEvent {

    public UserEmailCollected(String resourceExternalId,
                              UserEmailCollectedEventDetails eventDetails,
                              ZonedDateTime timestamp) {
        super(resourceExternalId, eventDetails, timestamp);
    }

    public static UserEmailCollected from(ChargeEntity charge) {
        ZonedDateTime lastEventDate = charge.getEvents().stream()
                .filter(e -> e.getStatus() == ChargeStatus.ENTERING_CARD_DETAILS)
                .map(ChargeEventEntity::getUpdated)
                .max(ZonedDateTime::compareTo)
                .orElseThrow(() -> new ChargeEventNotFoundRuntimeException(charge.getExternalId()));

        return new UserEmailCollected(
                charge.getExternalId(),
                UserEmailCollectedEventDetails.from(charge),
                lastEventDate);
    }
}
