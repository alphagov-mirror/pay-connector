package uk.gov.pay.connector.gateway.stripe.request;

import org.apache.commons.lang3.StringUtils;
import uk.gov.pay.connector.app.StripeGatewayConfig;
import uk.gov.pay.connector.charge.model.AddressEntity;
import uk.gov.pay.connector.common.model.domain.Address;
import uk.gov.pay.connector.gateway.model.AuthCardDetails;
import uk.gov.pay.connector.gateway.model.OrderRequestType;
import uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequest;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.northamericaregion.CanadaPostalcodeToProvinceOrTerritoryMapper;
import uk.gov.pay.connector.northamericaregion.NorthAmericaRegion;
import uk.gov.pay.connector.northamericaregion.NorthAmericanRegionMapper;
import uk.gov.pay.connector.northamericaregion.UsZipCodeToStateMapper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StripePaymentMethodRequest extends StripeRequest {
    private final AuthCardDetails authCardDetails;
    private final NorthAmericanRegionMapper northAmericanRegionMapper;
    
    public StripePaymentMethodRequest(
            GatewayAccountEntity gatewayAccount,
            String idempotencyKey,
            StripeGatewayConfig stripeGatewayConfig,
            AuthCardDetails authCardDetails)
    {
        super(gatewayAccount, idempotencyKey, stripeGatewayConfig);
        this.authCardDetails = authCardDetails;
        this.northAmericanRegionMapper = new NorthAmericanRegionMapper();
    }
    
    public static StripePaymentMethodRequest of(CardAuthorisationGatewayRequest request, StripeGatewayConfig config) {
        return new StripePaymentMethodRequest(
                request.getGatewayAccount(),
                request.getChargeExternalId(),
                config,
                request.getAuthCardDetails()
        );
    }

    @Override
    protected Map<String, String> params() {
        Map<String, String> localParams = new HashMap<>();
        localParams.put("card[exp_month]", authCardDetails.expiryMonth());
        localParams.put("card[exp_year]", authCardDetails.expiryYear());
        localParams.put("card[number]", authCardDetails.getCardNo());
        localParams.put("card[cvc]", authCardDetails.getCvc());
        localParams.put("billing_details[name]", authCardDetails.getCardHolder());
        localParams.put("type", "card");

        authCardDetails.getAddress().ifPresent(address -> {
            localParams.put("billing_details[address[line1]]", address.getLine1());
            if (StringUtils.isNotBlank(address.getLine2())) {
                localParams.put("billing_details[address[line2]]", address.getLine2());
            }
            northAmericanRegionMapper.getNorthAmericanRegionForCountry(address)
                    .map(NorthAmericaRegion::getFullName)
                    .ifPresent(stateOrProvince -> localParams.put("billing_details[address[state]]", stateOrProvince));
            localParams.put("billing_details[address[city]]", address.getCity());
            localParams.put("billing_details[address[country]]", address.getCountry());
            localParams.put("billing_details[address[postal_code]]", address.getPostcode());
        });
        
        return Map.copyOf(localParams);
    }

    @Override
    protected String urlPath() {
        return "/v1/payment_methods";
    }

    @Override
    protected OrderRequestType orderRequestType() {
        return OrderRequestType.AUTHORISE;
    }

    @Override
    protected String idempotencyKeyType() {
        return "payment_method";
    }
}
