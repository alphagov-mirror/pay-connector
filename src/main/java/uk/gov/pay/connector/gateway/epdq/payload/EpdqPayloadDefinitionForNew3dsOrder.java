package uk.gov.pay.connector.gateway.epdq.payload;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import uk.gov.pay.connector.common.model.domain.Address;
import uk.gov.pay.connector.gateway.model.OrderRequestType;

import java.util.List;

import uk.gov.pay.connector.gateway.epdq.EpdqTemplateData;

import static uk.gov.pay.connector.gateway.epdq.payload.EpdqParameterBuilder.newParameterBuilder;

public class EpdqPayloadDefinitionForNew3dsOrder extends EpdqPayloadDefinitionForNewOrder {

    public static final String ACCEPTURL_KEY = "ACCEPTURL";
    public static final String COMPLUS_KEY = "COMPLUS";
    public static final String DECLINEURL_KEY = "DECLINEURL";
    public static final String EXCEPTIONURL_KEY = "EXCEPTIONURL";
    public static final String FLAG3D_KEY = "FLAG3D";
    public static final String HTTPACCEPT_URL = "HTTP_ACCEPT";
    public static final String HTTPUSER_AGENT = "HTTP_USER_AGENT";
    public static final String LANGUAGE_URL = "LANGUAGE";
    public static final String PARAMPLUS_URL = "PARAMPLUS";
    public static final String WIN3DS_URL = "WIN3DS";
    
    private final String frontendUrl;

    public EpdqPayloadDefinitionForNew3dsOrder(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public List<NameValuePair> extract(EpdqTemplateData templateData) {
        templateData.setFrontendUrl(frontendUrl);
        String frontend3dsIncomingUrl = String.format("%s/card_details/%s/3ds_required_in/epdq", templateData.getFrontendUrl(), templateData.getOrderId());

        EpdqParameterBuilder epdqParameterBuilder = newParameterBuilder()
                .add(ACCEPTURL_KEY, frontend3dsIncomingUrl)
                .add(AMOUNT_KEY, templateData.getAmount())
                .add(CARD_NO_KEY, templateData.getAuthCardDetails().getCardNo())
                .add(CARDHOLDER_NAME_KEY, templateData.getAuthCardDetails().getCardHolder())
                .add(COMPLUS_KEY, "")
                .add(CURRENCY_KEY, "GBP")
                .add(CVC_KEY, templateData.getAuthCardDetails().getCvc())
                .add(DECLINEURL_KEY, frontend3dsIncomingUrl + "?status=declined")
                .add(EXCEPTIONURL_KEY, frontend3dsIncomingUrl + "?status=error")
                .add(EXPIRY_DATE_KEY, templateData.getAuthCardDetails().getEndDate())
                .add(FLAG3D_KEY, "Y")
                .add(HTTPACCEPT_URL, templateData.getAuthCardDetails().getAcceptHeader())
                .add(LANGUAGE_URL, "en_GB")
                .add(OPERATION_KEY, templateData.getOperationType())
                .add(ORDER_ID_KEY, templateData.getOrderId());
        
        if (StringUtils.isBlank(templateData.getAuthCardDetails().getUserAgentHeader())) {
            epdqParameterBuilder.add(HTTPUSER_AGENT, "Mozilla/5.0");
        } else {
            epdqParameterBuilder.add(HTTPUSER_AGENT, templateData.getAuthCardDetails().getUserAgentHeader());
        }

        if (templateData.getAuthCardDetails().getAddress().isPresent()) {
            Address address = templateData.getAuthCardDetails().getAddress().get();
            String addressLines = concatAddressLines(address.getLine1(), address.getLine2());

            epdqParameterBuilder.add(OWNER_ADDRESS_KEY, addressLines)
                    .add(OWNER_COUNTRY_CODE_KEY, address.getCountry())
                    .add(OWNER_TOWN_KEY, address.getCity())
                    .add(OWNER_ZIP_KEY, address.getPostcode());
        }

        epdqParameterBuilder.add(PARAMPLUS_URL, "")
                .add(PSPID_KEY, templateData.getMerchantCode())
                .add(PSWD_KEY, templateData.getPassword())
                .add(USERID_KEY, templateData.getUserId())
                .add(WIN3DS_URL, "MAINW");

        return epdqParameterBuilder.build();
    }

    private static String concatAddressLines(String addressLine1, String addressLine2) {
        return StringUtils.isBlank(addressLine2) ? addressLine1 : addressLine1 + ", " + addressLine2;
    }

    @Override
    protected String getOperationType() {
        return "RES";
    }

    @Override
    protected OrderRequestType getOrderRequestType() {
        return OrderRequestType.AUTHORISE_3DS;
    }
}
