package uk.gov.pay.connector.gateway.epdq.payload;

import org.apache.http.NameValuePair;
import uk.gov.pay.commons.model.SupportedLanguage;
import uk.gov.pay.connector.common.model.domain.Address;
import uk.gov.pay.connector.gateway.epdq.EpdqTemplateData;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static uk.gov.pay.connector.gateway.epdq.payload.EpdqParameterBuilder.newParameterBuilder;

public class EpdqPayloadDefinitionForNew3ds2Order extends EpdqPayloadDefinitionForNew3dsOrder {

    public final static String BROWSER_COLOR_DEPTH = "browserColorDepth";
    public final static String BROWSER_LANGUAGE = "browserLanguage";
    public final static String BROWSER_SCREEN_HEIGHT = "browserScreenHeight";
    public final static String BROWSER_SCREEN_WIDTH = "browserScreenWidth";
    public final static String BROWSER_TIMEZONE = "browserTimezone";
    public final static String BROWSER_ACCEPT_HEADER = "browserAcceptHeader";
    public final static String BROWSER_USER_AGENT = "browserUserAgent";
    public final static String BROWSER_JAVA_ENABLED = "browserJavaEnabled";
    public final static String ECOM_BILLTO_POSTAL_STREET_LINE1 = "ECOM_BILLTO_POSTAL_STREET_LINE1";
    public final static String ECOM_BILLTO_POSTAL_STREET_LINE2 = "ECOM_BILLTO_POSTAL_STREET_LINE2";
    public final static String ECOM_BILLTO_POSTAL_CITY = "ECOM_BILLTO_POSTAL_CITY";
    public final static String ECOM_BILLTO_POSTAL_COUNTRYCODE = "ECOM_BILLTO_POSTAL_COUNTRYCODE";
    public final static String ECOM_BILLTO_POSTAL_POSTALCODE = "ECOM_BILLTO_POSTAL_POSTALCODE";
    public final static String REMOTE_ADDR = "REMOTE_ADDR";

    public final static String DEFAULT_BROWSER_COLOR_DEPTH = "24";
    public final static String DEFAULT_BROWSER_SCREEN_HEIGHT = "480";
    public final static String DEFAULT_BROWSER_SCREEN_WIDTH = "320";

    public static final int BROWSER_ACCEPT_MAX_LENGTH = 2048;
    public static final int BROWSER_USER_AGENT_MAX_LENGTH = 2048;
    public static final int BROWSER_LANGUAGE_MAX_LENGTH = 8;
    
    private final static Pattern NUMBER_FROM_0_TO_999999 = Pattern.compile("0|[1-9][0-9]{0,5}");
    private final static Pattern NUMBER_FROM_MINUS_999_TO_999 = Pattern.compile("-[1-9][0-9]{0,2}|0|[1-9][0-9]{0,2}");
    private final static Set<String> VALID_SCREEN_COLOR_DEPTHS = Set.of("1", "2", "4", "8", "15", "16", "24", "32");
    
    private final boolean sendPayerIpAddressToGateway;
    private final SupportedLanguage paymentLanguage;
    private final Clock clock;

    public EpdqPayloadDefinitionForNew3ds2Order(String frontendUrl, boolean sendPayerIpAddressToGateway, SupportedLanguage paymentLanguage, Clock clock) {
        super(frontendUrl);
        this.sendPayerIpAddressToGateway = sendPayerIpAddressToGateway;
        this.paymentLanguage = paymentLanguage;
        this.clock = clock;
    }

    @Override
    public List<NameValuePair> extract(EpdqTemplateData templateData) {
        List<NameValuePair> nameValuePairs = super.extract(templateData);
        EpdqParameterBuilder parameterBuilder = newParameterBuilder(nameValuePairs)
                .add(BROWSER_COLOR_DEPTH, getBrowserColorDepth(templateData))
                .add(BROWSER_LANGUAGE, getBrowserLanguage(templateData))
                .add(BROWSER_SCREEN_HEIGHT, getBrowserScreenHeight(templateData))
                .add(BROWSER_SCREEN_WIDTH, getBrowserScreenWidth(templateData))
                .add(BROWSER_TIMEZONE, getBrowserTimezone(templateData))
                .add(BROWSER_ACCEPT_HEADER, getBrowserAcceptHeader(templateData))
                .add(BROWSER_USER_AGENT, getBrowserUserAgent(templateData))
                .add(BROWSER_JAVA_ENABLED, "false");
        
        templateData.getAuthCardDetails().getAddress().map(Address::getCity).ifPresent(city -> parameterBuilder.add(ECOM_BILLTO_POSTAL_CITY, city));
        templateData.getAuthCardDetails().getAddress().map(Address::getCountry).ifPresent(country -> parameterBuilder.add(ECOM_BILLTO_POSTAL_COUNTRYCODE, country));
        templateData.getAuthCardDetails().getAddress().map(Address::getLine1).ifPresent(addressLine1 -> parameterBuilder.add(ECOM_BILLTO_POSTAL_STREET_LINE1, addressLine1));
        templateData.getAuthCardDetails().getAddress().map(Address::getLine2).ifPresent(addressLine2 -> parameterBuilder.add(ECOM_BILLTO_POSTAL_STREET_LINE2, addressLine2));
        templateData.getAuthCardDetails().getAddress().map(Address::getPostcode).ifPresent(addressPostCode -> parameterBuilder.add(ECOM_BILLTO_POSTAL_POSTALCODE, addressPostCode));
        
        if (sendPayerIpAddressToGateway) {
            templateData.getAuthCardDetails().getIpAddress().ifPresent(ipAddress -> parameterBuilder.add(REMOTE_ADDR, ipAddress));
        }

        return parameterBuilder.build();
    }

    private String getBrowserTimezone(EpdqTemplateData templateData) {
        return templateData.getAuthCardDetails().getJsTimezoneOffsetMins()
                .filter(timezoneOffsetMins -> NUMBER_FROM_MINUS_999_TO_999.matcher(timezoneOffsetMins).matches())
                .map(Integer::parseInt)
                .filter(timezoneOffsetMins -> timezoneOffsetMins >= -840 && timezoneOffsetMins <= 720)
                .map(timezoneOffsetMins -> Integer.toString(timezoneOffsetMins))
                .orElseGet(this::getDefaultBrowserOffsetInMinutes);
    }

    private String getBrowserScreenWidth(EpdqTemplateData templateData) {
        return templateData.getAuthCardDetails().getJsScreenWidth()
                .filter(screenWidth -> NUMBER_FROM_0_TO_999999.matcher(screenWidth).matches())
                .map(Integer::parseInt)
                .filter(screenWidth -> screenWidth >= 0 && screenWidth <= 999999)
                .map(screenWidth -> Integer.toString(screenWidth))
                .orElse(DEFAULT_BROWSER_SCREEN_WIDTH);
    }

    private String getBrowserScreenHeight(EpdqTemplateData templateData) {
        return templateData.getAuthCardDetails().getJsScreenHeight()
                .filter(screenHeight -> NUMBER_FROM_0_TO_999999.matcher(screenHeight).matches())
                .map(Integer::parseInt)
                .filter(screenHeight -> screenHeight >= 0 && screenHeight <= 999999)
                .map(screenHeight -> Integer.toString(screenHeight))
                .orElse(DEFAULT_BROWSER_SCREEN_HEIGHT);
    }

    private String getBrowserLanguage(EpdqTemplateData templateData) {
        return templateData
                .getAuthCardDetails()
                .getJsNavigatorLanguage()
                .map(Locale::forLanguageTag)
                .map(Locale::toLanguageTag)
                .filter(languageTag -> languageTag.length() <= BROWSER_LANGUAGE_MAX_LENGTH)
                .orElse(getDefaultBrowserLanguage());
    }

    private String getDefaultBrowserLanguage() {
        if (paymentLanguage == SupportedLanguage.ENGLISH) {
            return "en-GB";
        }
        return paymentLanguage.toString();
    }

    private String getBrowserColorDepth(EpdqTemplateData templateData) {
        return templateData.getAuthCardDetails().getJsScreenColorDepth()
                .filter(VALID_SCREEN_COLOR_DEPTHS::contains).orElse(DEFAULT_BROWSER_COLOR_DEPTH);
    }

    private String getDefaultBrowserOffsetInMinutes() {
        ZoneOffset currentUkOffset = ZoneId.of("Europe/London").getRules().getOffset(clock.instant());
        int currentUkOffsetMinsInJavaFormatWithAheadOfUtcPositive = currentUkOffset.getTotalSeconds() / 60;
        int currentUkOffsetMinsInJavaScriptFormatWithAheadOfUtcNegative = -currentUkOffsetMinsInJavaFormatWithAheadOfUtcPositive;
        return String.valueOf(currentUkOffsetMinsInJavaScriptFormatWithAheadOfUtcNegative);
    }
    
    String getBrowserAcceptHeader(EpdqTemplateData templateData) {
        String acceptHeader = super.getBrowserAcceptHeader(templateData);
        if (acceptHeader.length() > BROWSER_ACCEPT_MAX_LENGTH) {
            return DEFAULT_BROWSER_ACCEPT_HEADER;
        }
        return super.getBrowserAcceptHeader(templateData);
    }

    String getBrowserUserAgent(EpdqTemplateData templateData) {
        String userAgent = super.getBrowserUserAgent(templateData);
        if (userAgent.length() > BROWSER_USER_AGENT_MAX_LENGTH) {
            return DEFAULT_BROWSER_USER_AGENT;
        }
        return super.getBrowserUserAgent(templateData);
    }
}
