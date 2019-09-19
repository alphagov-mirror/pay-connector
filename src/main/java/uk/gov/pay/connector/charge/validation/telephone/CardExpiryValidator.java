package uk.gov.pay.connector.charge.validation.telephone;

import uk.gov.pay.connector.charge.model.telephone.TelephoneChargeCreateRequest;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class CardExpiryValidator implements ConstraintValidator<ValidCardExpiryDate, TelephoneChargeCreateRequest> {

    private Pattern pattern = Pattern.compile("(0[1-9]|1[0-2])\\/[0-9]{2}");

    @Override
    public boolean isValid(TelephoneChargeCreateRequest telephoneChargeCreateRequest, ConstraintValidatorContext context) {

        final String cardExpiry = telephoneChargeCreateRequest.getCardExpiry().orElse(null);
        final String status = telephoneChargeCreateRequest.getPaymentOutcome().getStatus();

        if (cardExpiry == null) {
            return "failed".equals(status);
        }

        return pattern.matcher(cardExpiry).matches();
    }
}
