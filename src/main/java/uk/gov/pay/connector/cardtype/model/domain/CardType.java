package uk.gov.pay.connector.cardtype.model.domain;

/**
 * Internal entity used to drive the frontend UI based on the
 * strings stored in the table {@code card_types}. This represents
 * which card types are supported by the service, not what card types
 * are used for payment by the paying user, which is {@code PayersCardType}
 * <p>
 * This should be used only for instances where the type of the card will either be
 * Credit or Debit, but there is no CREDIT_OR_DEBIT state as exists in PayersCardType
 */
public enum CardType {
    CREDIT,
    DEBIT
}
