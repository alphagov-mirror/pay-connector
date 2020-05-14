package uk.gov.pay.connector.events.eventdetails.payout;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.connector.events.MicrosecondPrecisionDateTimeSerializer;
import uk.gov.pay.connector.events.eventdetails.EventDetails;

import java.time.ZonedDateTime;

public class PayoutCreatedEventDetails extends EventDetails {

    private Long amount;
    @JsonSerialize(using = MicrosecondPrecisionDateTimeSerializer.class)
    private ZonedDateTime arrivalDate;
    private String status;
    private String type;
    private String statementDescriptor;

    public PayoutCreatedEventDetails(Long amount, ZonedDateTime arrivalDate, String status, String type, String statementDescriptor) {
        this.amount = amount;
        this.arrivalDate = arrivalDate;
        this.status = status;
        this.type = type;
        this.statementDescriptor = statementDescriptor;
    }

    public Long getAmount() {
        return amount;
    }

    public ZonedDateTime getArrivalDate() {
        return arrivalDate;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public String getStatementDescriptor() {
        return statementDescriptor;
    }
}
