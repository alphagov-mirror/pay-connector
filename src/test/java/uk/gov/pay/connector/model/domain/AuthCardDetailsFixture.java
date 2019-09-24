package uk.gov.pay.connector.model.domain;

import org.apache.commons.lang3.StringUtils;
import uk.gov.pay.connector.cardtype.model.domain.CardTypeEntity;
import uk.gov.pay.connector.charge.model.AddressEntity;
import uk.gov.pay.connector.charge.model.CardDetailsEntity;
import uk.gov.pay.connector.charge.model.FirstDigitsCardNumber;
import uk.gov.pay.connector.charge.model.LastDigitsCardNumber;
import uk.gov.pay.connector.common.model.domain.Address;
import uk.gov.pay.connector.gateway.model.AuthCardDetails;
import uk.gov.pay.connector.gateway.model.PayersCardType;
import uk.gov.pay.connector.gateway.model.PayersCardPrepaidStatus;

public final class AuthCardDetailsFixture {
    private String cardNo = "4242424242424242";
    private String cardHolder = "Mr Test";
    private String cvc = "123";
    private String endDate = "12/99";
    private Address address = AddressFixture.anAddress().build();
    private String cardBrand = "visa";
    private String userAgentHeader = "Mozilla/5.0";
    private String acceptHeader = "text/html";
    private PayersCardType payersCardType = PayersCardType.DEBIT;
    private PayersCardPrepaidStatus payersCardPrepaidStatus = PayersCardPrepaidStatus.UNKNOWN;
    private Boolean corporateCard = Boolean.FALSE;
    private String worldpay3dsFlexDdcResult;

    private AuthCardDetailsFixture() {
    }

    public static AuthCardDetailsFixture anAuthCardDetails() {
        return new AuthCardDetailsFixture();
    }

    public AuthCardDetailsFixture withCardNo(String cardNo) {
        this.cardNo = cardNo;
        return this;
    }

    public AuthCardDetailsFixture withCardHolder(String cardHolder) {
        this.cardHolder = cardHolder;
        return this;
    }

    public AuthCardDetailsFixture withCvc(String cvc) {
        this.cvc = cvc;
        return this;
    }

    public AuthCardDetailsFixture withEndDate(String endDate) {
        this.endDate = endDate;
        return this;
    }

    public AuthCardDetailsFixture withAddress(Address address) {
        this.address = address;
        return this;
    }

    public AuthCardDetailsFixture withCardBrand(String cardBrand) {
        this.cardBrand = cardBrand;
        return this;
    }

    public AuthCardDetailsFixture withUserAgentHeader(String userAgentHeader) {
        this.userAgentHeader = userAgentHeader;
        return this;
    }

    public AuthCardDetailsFixture withAcceptHeader(String acceptHeader) {
        this.acceptHeader = acceptHeader;
        return this;
    }

    public AuthCardDetailsFixture withCardType(PayersCardType payersCardType) {
        this.payersCardType = payersCardType;
        return this;
    }

    public AuthCardDetailsFixture withCorporateCard(Boolean corporateCard) {
        this.corporateCard = corporateCard;
        return this;
    }

    public AuthCardDetailsFixture withPayersCardPrepaidStatus(PayersCardPrepaidStatus payersCardPrepaidStatus) {
        this.payersCardPrepaidStatus = payersCardPrepaidStatus;
        return this;
    }
    
    public AuthCardDetailsFixture withWorldpay3dsFlexDdcResult(String worldpay3dsFlexDdcResult) {
        this.worldpay3dsFlexDdcResult = worldpay3dsFlexDdcResult;
        return this;
    }

    public CardDetailsEntity getCardDetailsEntity() {
        CardDetailsEntity cardDetailsEntity = new CardDetailsEntity();
        cardDetailsEntity.setCardBrand(cardBrand);
        cardDetailsEntity.setCardHolderName(cardHolder);
        cardDetailsEntity.setExpiryDate(endDate);
        if(cardNo.length() > 6) {
            cardDetailsEntity.setFirstDigitsCardNumber(FirstDigitsCardNumber.of(StringUtils.left(cardNo, 6)));
        }

        cardDetailsEntity.setLastDigitsCardNumber(LastDigitsCardNumber.of(StringUtils.right(cardNo, 4)));

        if(address != null) {
            cardDetailsEntity.setBillingAddress(new AddressEntity(address));
        }

        CardTypeEntity cardType = new CardTypeEntity();
        cardType.setBrand("visa");
        cardType.setLabel("Visa");
        cardDetailsEntity.setCardType(cardType);

        return cardDetailsEntity;
    }

    public AuthCardDetails build() {
        AuthCardDetails authCardDetails = new AuthCardDetails();
        authCardDetails.setCardNo(cardNo);
        authCardDetails.setCardHolder(cardHolder);
        authCardDetails.setCvc(cvc);
        authCardDetails.setEndDate(endDate);
        authCardDetails.setAddress(address);
        authCardDetails.setCardBrand(cardBrand);
        authCardDetails.setUserAgentHeader(userAgentHeader);
        authCardDetails.setAcceptHeader(acceptHeader);
        authCardDetails.setPayersCardType(payersCardType);
        authCardDetails.setPayersCardPrepaidStatus(payersCardPrepaidStatus);
        authCardDetails.setCorporateCard(corporateCard);
        authCardDetails.setWorldpay3dsFlexDdcResult(worldpay3dsFlexDdcResult);
        return authCardDetails;
    }
}
