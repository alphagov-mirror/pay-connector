package uk.gov.pay.connector.model;

import uk.gov.pay.connector.model.domain.ChargeStatus;

import java.util.Optional;

public interface ChargeStatusRequest {
    public String getTransactionId();

    Optional<ChargeStatus> getChargeStatus();

}
