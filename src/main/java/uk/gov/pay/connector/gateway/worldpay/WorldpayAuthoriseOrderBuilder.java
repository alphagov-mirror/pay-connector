package uk.gov.pay.connector.gateway.worldpay;

import uk.gov.pay.connector.gateway.GatewayOrder;
import uk.gov.pay.connector.gateway.model.request.CardAuthorisationGatewayRequest;
import uk.gov.pay.connector.gatewayaccount.model.Worldpay3dsFlexCredentials;

import java.util.List;
import java.util.function.BiFunction;

import static uk.gov.pay.connector.gateway.worldpay.WorldpayOrderRequestBuilder.aWorldpayAuthoriseOrderRequestBuilder;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccount.CREDENTIALS_MERCHANT_ID;

public interface WorldpayAuthoriseOrderBuilder {
    
    BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder> ADD_3DS_REQUIRED =
            (worldpayOrderRequestBuilder, request) -> {
                boolean is3dsRequired = request.getAuthCardDetails().getWorldpay3dsFlexDdcResult().isPresent() ||
                        request.getGatewayAccount().isRequires3ds();
                return worldpayOrderRequestBuilder.with3dsRequired(is3dsRequired);
            };
    
    BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder> ADD_SESSION_ID =
            (worldpayOrderRequestBuilder, request) -> worldpayOrderRequestBuilder.withSessionId(WorldpayAuthoriseOrderSessionId.of(request.getChargeExternalId()));
    
    BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder> ADD_EXEMPTION_ENGINE =
            (worldpayOrderRequestBuilder, request) -> {
                boolean exemptionEngineEnabled = request.getGatewayAccount().getWorldpay3dsFlexCredentials()
                        .map(Worldpay3dsFlexCredentials::isExemptionEngineEnabled)
                        .orElse(false);
                return worldpayOrderRequestBuilder.withExemptionEngine(exemptionEngineEnabled);
            };

    BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder> ADD_TRANSACTION_ID =
            (worldpayOrderRequestBuilder, request) -> (WorldpayOrderRequestBuilder) worldpayOrderRequestBuilder.withTransactionId(request.getTransactionId().orElse(""));

    BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder> ADD_MERCHANT_CODE =
            (worldpayOrderRequestBuilder, request) -> (WorldpayOrderRequestBuilder) worldpayOrderRequestBuilder.withMerchantCode(request.getGatewayAccount().getCredentials().get(CREDENTIALS_MERCHANT_ID));

    BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder> ADD_DESCRIPTION =
            (worldpayOrderRequestBuilder, request) -> (WorldpayOrderRequestBuilder) worldpayOrderRequestBuilder.withDescription(request.getDescription());

    BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder> ADD_AMOUNT =
            (worldpayOrderRequestBuilder, request) -> (WorldpayOrderRequestBuilder) worldpayOrderRequestBuilder.withAmount(request.getAmount());

    BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder> ADD_AUTHORISATION_DETAILS =
            (worldpayOrderRequestBuilder, request) -> (WorldpayOrderRequestBuilder) worldpayOrderRequestBuilder.withAuthorisationDetails(request.getAuthCardDetails());

    BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder> ADD_PAYER_IP_ADDRESS =
            (worldpayOrderRequestBuilder, request) -> {
                if (request.getGatewayAccount().isSendPayerIpAddressToGateway()) {
                    request.getAuthCardDetails().getIpAddress().ifPresent(worldpayOrderRequestBuilder::withPayerIpAddress);
                }
                return worldpayOrderRequestBuilder;
            };

    enum Strategy {
        WITH_ALL_AUTHORISATION_ORDER_DETAILS(ADD_3DS_REQUIRED, ADD_SESSION_ID, ADD_EXEMPTION_ENGINE, ADD_TRANSACTION_ID, 
                ADD_MERCHANT_CODE, ADD_DESCRIPTION, ADD_AMOUNT, ADD_AUTHORISATION_DETAILS, ADD_PAYER_IP_ADDRESS),
        
        WITH_ALL_AUTHORISATION_ORDER_MINUS_EXEMPTION_ENGINE_DETAILS(ADD_3DS_REQUIRED, ADD_SESSION_ID, ADD_TRANSACTION_ID,
                ADD_MERCHANT_CODE, ADD_DESCRIPTION, ADD_AMOUNT, ADD_AUTHORISATION_DETAILS, ADD_PAYER_IP_ADDRESS),
        ;

        private List<BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder>> functions;

        Strategy(BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder>... functions) {
            this.functions = List.of(functions);
        }

        private List<BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder>> getFunctions() {
            return functions;
        }
    }
    
    static GatewayOrder buildAuthoriseOrder(CardAuthorisationGatewayRequest request, Strategy strategy) {
        WorldpayOrderRequestBuilder worldpayOrderRequestBuilder = aWorldpayAuthoriseOrderRequestBuilder();
        for (BiFunction<WorldpayOrderRequestBuilder, CardAuthorisationGatewayRequest, WorldpayOrderRequestBuilder> function : strategy.getFunctions()) {
            worldpayOrderRequestBuilder = function.apply(worldpayOrderRequestBuilder, request);
        }
        return  worldpayOrderRequestBuilder.build();
    }
}
