package uk.gov.pay.connector.util;

import uk.gov.pay.connector.gatewayaccount.model.EmailCollectionMode;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;

import java.util.Map;

import static uk.gov.pay.connector.gatewayaccount.model.EmailCollectionMode.MANDATORY;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity.Type.TEST;

public class AddGatewayAccountParams {
    private String accountId;
    private String paymentGateway;
    private Map<String, String> credentials;
    private String serviceName;
    private GatewayAccountEntity.Type type;
    private String description;
    private String analyticsId;
    private EmailCollectionMode emailCollectionMode;
    private long corporateCreditCardSurchargeAmount;
    private long corporateDebitCardSurchargeAmount;
    private long corporatePrepaidCreditCardSurchargeAmount;
    private long corporatePrepaidDebitCardSurchargeAmount;
    private int integrationVersion3ds;
    private boolean allowMoto;
    private boolean motoMaskCardNumberInput;
    private boolean motoMaskCardSecurityCodeInput;
    private boolean allowApplePay;
    private boolean allowGooglePay;
    private boolean requires3ds;

    public int getIntegrationVersion3ds() {
        return integrationVersion3ds;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getPaymentGateway() {
        return paymentGateway;
    }

    public Map<String, String> getCredentials() {
        return credentials;
    }

    public String getServiceName() {
        return serviceName;
    }

    public GatewayAccountEntity.Type getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getAnalyticsId() {
        return analyticsId;
    }

    public EmailCollectionMode getEmailCollectionMode() {
        return emailCollectionMode;
    }

    public long getCorporateCreditCardSurchargeAmount() {
        return corporateCreditCardSurchargeAmount;
    }

    public long getCorporateDebitCardSurchargeAmount() {
        return corporateDebitCardSurchargeAmount;
    }

    public long getCorporatePrepaidCreditCardSurchargeAmount() {
        return corporatePrepaidCreditCardSurchargeAmount;
    }

    public long getCorporatePrepaidDebitCardSurchargeAmount() {
        return corporatePrepaidDebitCardSurchargeAmount;
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

    public boolean isAllowApplePay() {
        return allowApplePay;
    }

    public boolean isAllowGooglePay() {
        return allowGooglePay;
    }

    public boolean isRequires3ds() {
        return requires3ds;
    }

    public static final class AddGatewayAccountParamsBuilder {
        private String accountId;
        private String paymentGateway = "provider";
        private Map<String, String> credentials;
        private String serviceName = "service name";
        private GatewayAccountEntity.Type type = TEST;
        private String description = "description";
        private String analyticsId;
        private EmailCollectionMode emailCollectionMode = MANDATORY;
        private long corporateCreditCardSurchargeAmount;
        private long corporateDebitCardSurchargeAmount;
        private long corporatePrepaidCreditCardSurchargeAmount;
        private long corporatePrepaidDebitCardSurchargeAmount;
        private int integrationVersion3ds = 2;
        private boolean allowMoto;
        private boolean motoMaskCardNumberInput;
        private boolean motoMaskCardSecurityCodeInput;
        private boolean allowApplePay;
        private boolean allowGooglePay;
        private boolean requires3ds;

        private AddGatewayAccountParamsBuilder() {
        }

        public static AddGatewayAccountParamsBuilder anAddGatewayAccountParams() {
            return new AddGatewayAccountParamsBuilder();
        }

        public AddGatewayAccountParamsBuilder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public AddGatewayAccountParamsBuilder withPaymentGateway(String paymentGateway) {
            this.paymentGateway = paymentGateway;
            return this;
        }

        public AddGatewayAccountParamsBuilder withCredentials(Map<String, String> credentials) {
            this.credentials = credentials;
            return this;
        }

        public AddGatewayAccountParamsBuilder withServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public AddGatewayAccountParamsBuilder withType(GatewayAccountEntity.Type type) {
            this.type = type;
            return this;
        }

        public AddGatewayAccountParamsBuilder withDescription(String description) {
            this.description = description;
            return this;
        }

        public AddGatewayAccountParamsBuilder withAnalyticsId(String analyticsId) {
            this.analyticsId = analyticsId;
            return this;
        }

        public AddGatewayAccountParamsBuilder withEmailCollectionMode(EmailCollectionMode emailCollectionMode) {
            this.emailCollectionMode = emailCollectionMode;
            return this;
        }

        public AddGatewayAccountParamsBuilder withCorporateCreditCardSurchargeAmount(long corporateCreditCardSurchargeAmount) {
            this.corporateCreditCardSurchargeAmount = corporateCreditCardSurchargeAmount;
            return this;
        }

        public AddGatewayAccountParamsBuilder withCorporateDebitCardSurchargeAmount(long corporateDebitCardSurchargeAmount) {
            this.corporateDebitCardSurchargeAmount = corporateDebitCardSurchargeAmount;
            return this;
        }

        public AddGatewayAccountParamsBuilder withCorporatePrepaidCreditCardSurchargeAmount(long corporatePrepaidCreditCardSurchargeAmount) {
            this.corporatePrepaidCreditCardSurchargeAmount = corporatePrepaidCreditCardSurchargeAmount;
            return this;
        }

        public AddGatewayAccountParamsBuilder withCorporatePrepaidDebitCardSurchargeAmount(long corporatePrepaidDebitCardSurchargeAmount) {
            this.corporatePrepaidDebitCardSurchargeAmount = corporatePrepaidDebitCardSurchargeAmount;
            return this;
        }
        
        public AddGatewayAccountParamsBuilder withAllowMoto(boolean allowMoto) {
            this.allowMoto = allowMoto;
            return this;
        }

        public AddGatewayAccountParamsBuilder withMotoMaskCardNumberInput(boolean motoMaskCardNumberInput) {
            this.motoMaskCardNumberInput = motoMaskCardNumberInput;
            return this;
        }

        public AddGatewayAccountParamsBuilder withMotoMaskCardSecurityCodeInput(boolean motoMaskCardSecurityCodeInput) {
            this.motoMaskCardSecurityCodeInput = motoMaskCardSecurityCodeInput;
            return this;
        }
        
        public AddGatewayAccountParamsBuilder withAllowApplePay(boolean allowApplePay) {
            this.allowApplePay = allowApplePay;
            return this;
        }
        
        public AddGatewayAccountParamsBuilder withAllowGooglePay(boolean allowGooglePay) {
            this.allowGooglePay = allowGooglePay;
            return this;
        }
        
        public AddGatewayAccountParamsBuilder withRequires3ds(boolean requires3ds) {
            this.requires3ds = requires3ds;
            return this;
        }

        public AddGatewayAccountParams build() {
            AddGatewayAccountParams addGatewayAccountParams = new AddGatewayAccountParams();
            addGatewayAccountParams.accountId = this.accountId;
            addGatewayAccountParams.paymentGateway = this.paymentGateway;
            addGatewayAccountParams.corporatePrepaidDebitCardSurchargeAmount = this.corporatePrepaidDebitCardSurchargeAmount;
            addGatewayAccountParams.analyticsId = this.analyticsId;
            addGatewayAccountParams.corporatePrepaidCreditCardSurchargeAmount = this.corporatePrepaidCreditCardSurchargeAmount;
            addGatewayAccountParams.type = this.type;
            addGatewayAccountParams.credentials = this.credentials;
            addGatewayAccountParams.description = this.description;
            addGatewayAccountParams.serviceName = this.serviceName;
            addGatewayAccountParams.corporateCreditCardSurchargeAmount = this.corporateCreditCardSurchargeAmount;
            addGatewayAccountParams.emailCollectionMode = this.emailCollectionMode;
            addGatewayAccountParams.corporateDebitCardSurchargeAmount = this.corporateDebitCardSurchargeAmount;
            addGatewayAccountParams.integrationVersion3ds = this.integrationVersion3ds;
            addGatewayAccountParams.allowMoto = this.allowMoto;
            addGatewayAccountParams.motoMaskCardNumberInput = this.motoMaskCardNumberInput;
            addGatewayAccountParams.motoMaskCardSecurityCodeInput = this.motoMaskCardSecurityCodeInput;
            addGatewayAccountParams.allowApplePay = this.allowApplePay;
            addGatewayAccountParams.allowGooglePay = this.allowGooglePay;
            addGatewayAccountParams.requires3ds = this.requires3ds;
            return addGatewayAccountParams;
        }

        public AddGatewayAccountParamsBuilder withIntegrationVersion3ds(int integrationVersion3ds) {
            this.integrationVersion3ds = integrationVersion3ds;
            return this;
        }
    }
}
