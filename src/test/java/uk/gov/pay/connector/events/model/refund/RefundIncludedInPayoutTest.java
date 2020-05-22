package uk.gov.pay.connector.events.model.refund;

import org.junit.Test;

import java.time.ZonedDateTime;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class RefundIncludedInPayoutTest {

    @Test
    public void serializesEventDetailsGivenPaymentIncludedInPayoutTest() throws Exception {
        var paymentExternalId = "payment-id";
        var gatewayPayoutId = "payout-id";
        String eventDateStr = "2020-05-10T10:30:00.000000Z";
        var event = new RefundIncludedInPayout(paymentExternalId, gatewayPayoutId, ZonedDateTime.parse(eventDateStr));

        var json = event.toJsonString();

        assertThat(json, hasJsonPath("$.event_type", equalTo("REFUND_INCLUDED_IN_PAYOUT")));
        assertThat(json, hasJsonPath("$.resource_type", equalTo("refund")));
        assertThat(json, hasJsonPath("$.resource_external_id", equalTo(paymentExternalId)));
        assertThat(json, hasJsonPath("$.timestamp", equalTo(eventDateStr)));

        assertThat(json, hasJsonPath("$.event_details.gateway_payout_id", equalTo(gatewayPayoutId)));
    }
}