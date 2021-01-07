package uk.gov.pay.connector.gatewayaccount.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import uk.gov.pay.connector.usernotification.model.domain.EmailNotificationEntity;
import uk.gov.pay.connector.usernotification.model.domain.EmailNotificationType;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class GatewayAccountResourceDTO {

    @JsonProperty("gateway_account_id")
    private long accountId;

    @JsonProperty("external_id")
    private String externalId;

    @JsonProperty("payment_provider")
    private String paymentProvider;

    private GatewayAccountType type;

    private String description;

    @JsonProperty("service_name")
    private String serviceName;

    @JsonProperty("analytics_id")
    private String analyticsId;

    @JsonProperty("corporate_credit_card_surcharge_amount")
    private long corporateCreditCardSurchargeAmount;

    @JsonProperty("corporate_debit_card_surcharge_amount")
    private long corporateDebitCardSurchargeAmount;

    @JsonProperty("_links")
    private Map<String, Map<String, URI>> links = new HashMap<>();

    @JsonProperty("allow_apple_pay")
    private boolean allowApplePay;

    @JsonProperty("allow_google_pay")
    private boolean allowGooglePay;

    @JsonProperty("block_prepaid_cards")
    private boolean blockPrepaidCards;

    @JsonProperty("corporate_prepaid_credit_card_surcharge_amount")
    private long corporatePrepaidCreditCardSurchargeAmount;

    @JsonProperty("corporate_prepaid_debit_card_surcharge_amount")
    private long corporatePrepaidDebitCardSurchargeAmount;

    @JsonProperty("email_notifications")
    private Map<EmailNotificationType, EmailNotificationEntity> emailNotifications = new HashMap<>();

    @JsonProperty("email_collection_mode")
    private EmailCollectionMode emailCollectionMode = EmailCollectionMode.MANDATORY;

    @JsonProperty("requires3ds")
    private boolean requires3ds;

    @JsonProperty("allow_zero_amount")
    private boolean allowZeroAmount;

    @JsonProperty("integration_version_3ds")
    private int integrationVersion3ds;

    @JsonProperty("allow_moto")
    private boolean allowMoto;

    @JsonProperty("allow_telephone_payment_notifications")
    private boolean allowTelephonePaymentNotifications;

    @JsonProperty("moto_mask_card_number_input")
    private boolean motoMaskCardNumberInput;

    @JsonProperty("moto_mask_card_security_code_input")
    private boolean motoMaskCardSecurityCodeInput;

    public GatewayAccountResourceDTO() {
    }

    public GatewayAccountResourceDTO(long accountId,
                                     String externalId,
                                     String paymentProvider,
                                     GatewayAccountType type,
                                     String description,
                                     String serviceName,
                                     String analyticsId,
                                     long corporateCreditCardSurchargeAmount,
                                     long corporateDebitCardSurchargeAmount,
                                     boolean allowApplePay,
                                     boolean allowGooglePay,
                                     boolean blockPrepaidCards,
                                     long corporatePrepaidCreditCardSurchargeAmount,
                                     long corporatePrepaidDebitCardSurchargeAmount,
                                     Map<EmailNotificationType, EmailNotificationEntity> emailNotifications,
                                     EmailCollectionMode emailCollectionMode,
                                     boolean requires3ds,
                                     boolean allowZeroAmount,
                                     int integrationVersion3ds,
                                     boolean allowMoto,
                                     boolean motoMaskCardNumberInput,
                                     boolean motoMaskCardSecurityCodeInput,
                                     boolean allowTelephonePaymentNotifications) {
        this.accountId = accountId;
        this.externalId = externalId;
        this.paymentProvider = paymentProvider;
        this.type = type;
        this.description = description;
        this.serviceName = serviceName;
        this.analyticsId = analyticsId;
        this.corporateCreditCardSurchargeAmount = corporateCreditCardSurchargeAmount;
        this.corporateDebitCardSurchargeAmount = corporateDebitCardSurchargeAmount;
        this.allowApplePay = allowApplePay;
        this.allowGooglePay = allowGooglePay;
        this.blockPrepaidCards = blockPrepaidCards;
        this.corporatePrepaidCreditCardSurchargeAmount = corporatePrepaidCreditCardSurchargeAmount;
        this.corporatePrepaidDebitCardSurchargeAmount = corporatePrepaidDebitCardSurchargeAmount;
        this.emailNotifications = emailNotifications;
        this.emailCollectionMode = emailCollectionMode;
        this.requires3ds = requires3ds;
        this.allowZeroAmount = allowZeroAmount;
        this.integrationVersion3ds = integrationVersion3ds;
        this.allowMoto = allowMoto;
        this.motoMaskCardNumberInput = motoMaskCardNumberInput;
        this.motoMaskCardSecurityCodeInput = motoMaskCardSecurityCodeInput;
        this.allowTelephonePaymentNotifications = allowTelephonePaymentNotifications;
    }

    public static GatewayAccountResourceDTO fromEntity(GatewayAccountEntity gatewayAccountEntity) {
        return new GatewayAccountResourceDTO(
                gatewayAccountEntity.getId(),
                gatewayAccountEntity.getExternalId(),
                gatewayAccountEntity.getGatewayName(),
                GatewayAccountType.fromString(gatewayAccountEntity.getType()),
                gatewayAccountEntity.getDescription(),
                gatewayAccountEntity.getServiceName(),
                gatewayAccountEntity.getAnalyticsId(),
                gatewayAccountEntity.getCorporateNonPrepaidCreditCardSurchargeAmount(),
                gatewayAccountEntity.getCorporateNonPrepaidDebitCardSurchargeAmount(),
                gatewayAccountEntity.isAllowApplePay(),
                gatewayAccountEntity.isAllowGooglePay(),
                gatewayAccountEntity.isBlockPrepaidCards(),
                gatewayAccountEntity.getCorporatePrepaidCreditCardSurchargeAmount(),
                gatewayAccountEntity.getCorporatePrepaidDebitCardSurchargeAmount(),
                gatewayAccountEntity.getEmailNotifications(),
                gatewayAccountEntity.getEmailCollectionMode(),
                gatewayAccountEntity.isRequires3ds(),
                gatewayAccountEntity.isAllowZeroAmount(),
                gatewayAccountEntity.getIntegrationVersion3ds(),
                gatewayAccountEntity.isAllowMoto(),
                gatewayAccountEntity.isMotoMaskCardNumberInput(),
                gatewayAccountEntity.isMotoMaskCardSecurityCodeInput(),
                gatewayAccountEntity.isAllowTelephonePaymentNotifications()
        );
    }

    public long getAccountId() {
        return accountId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public String getType() {
        return type.toString();
    }

    public String getDescription() {
        return description;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getAnalyticsId() {
        return analyticsId;
    }

    public long getCorporateCreditCardSurchargeAmount() {
        return corporateCreditCardSurchargeAmount;
    }

    public long getCorporateDebitCardSurchargeAmount() {
        return corporateDebitCardSurchargeAmount;
    }

    public Map<String, Map<String, URI>> getLinks() {
        return links;
    }

    public void addLink(String key, URI uri) {
        links.put(key, ImmutableMap.of("href", uri));
    }

    public long getCorporatePrepaidCreditCardSurchargeAmount() {
        return corporatePrepaidCreditCardSurchargeAmount;
    }

    public long getCorporatePrepaidDebitCardSurchargeAmount() {
        return corporatePrepaidDebitCardSurchargeAmount;
    }

    public boolean isAllowApplePay() {
        return allowApplePay;
    }

    public boolean isAllowGooglePay() {
        return allowGooglePay;
    }

    public boolean isBlockPrepaidCards() {
        return blockPrepaidCards;
    }

    public Map<EmailNotificationType, EmailNotificationEntity> getEmailNotifications() {
        return emailNotifications;
    }

    public EmailCollectionMode getEmailCollectionMode() {
        return emailCollectionMode;
    }

    public boolean isRequires3ds() {
        return requires3ds;
    }

    public boolean isAllowZeroAmount() {
        return allowZeroAmount;
    }

    public int getIntegrationVersion3ds() {
        return integrationVersion3ds;
    }

    public boolean isAllowMoto() {
        return allowMoto;
    }

    public boolean isMotoMaskCardNumberInput() {
        return motoMaskCardNumberInput;
    }

    public boolean isMotoMaskCardSecurityCodeInput() {
        return motoMaskCardSecurityCodeInput;
    }

    public boolean isAllowTelephonePaymentNotifications() {
        return allowTelephonePaymentNotifications;
    }
}
