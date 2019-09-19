package uk.gov.pay.connector.charge.validation.telephone;

import org.junit.BeforeClass;
import org.junit.Test;
import uk.gov.pay.connector.charge.model.telephone.PaymentOutcome;
import uk.gov.pay.connector.charge.model.telephone.Supplemental;
import uk.gov.pay.connector.charge.model.telephone.TelephoneChargeCreateRequest;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.pay.connector.util.NumberMatcher.isNumber;

public class CardExpiryValidatorTest {
    
    private static TelephoneChargeCreateRequest.Builder telephoneRequestBuilder = new TelephoneChargeCreateRequest.Builder();

    private static Validator validator;

    @BeforeClass
    public static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        telephoneRequestBuilder
                .withAmount(1200L)
                .withDescription("Some description")
                .withReference("Some reference")
                .withProcessorId("1PROC")
                .withProviderId("1PROV")
                .withLastFourDigits("1234")
                .withFirstSixDigits("123456")
                .withCardType("visa");
    }

    @Test
    public void failsValidationForInvalidMonth00AndStatusOfSuccess() {

        TelephoneChargeCreateRequest telephoneChargeCreateRequest = telephoneRequestBuilder
                .withCardExpiry("00/99")
                .withPaymentOutcome(new PaymentOutcome("success"))
                .build();

        Set<ConstraintViolation<TelephoneChargeCreateRequest>> constraintViolations = validator.validate(telephoneChargeCreateRequest);

        assertThat(constraintViolations.size(), is(1));
        assertThat(constraintViolations.iterator().next().getMessage(), is("Field [card_expiry] must have valid MM/YY"));
    }

    @Test
    public void failsValidationForInvalidMonth99AndStatusOfSuccess() {

        TelephoneChargeCreateRequest telephoneChargeCreateRequest = telephoneRequestBuilder
                .withCardExpiry("99/99")
                .withPaymentOutcome(new PaymentOutcome("success"))
                .build();

        Set<ConstraintViolation<TelephoneChargeCreateRequest>> constraintViolations = validator.validate(telephoneChargeCreateRequest);

        assertThat(constraintViolations.size(), isNumber(1));
        assertThat(constraintViolations.iterator().next().getMessage(), is("Field [card_expiry] must have valid MM/YY"));
    }

    @Test
    public void failsValidationForInvalidMonth00AndStatusOfFailed() {

        TelephoneChargeCreateRequest telephoneChargeCreateRequest = telephoneRequestBuilder
                .withCardExpiry("00/99")
                .withPaymentOutcome(new PaymentOutcome("failed", "P0050", new Supplemental()))
                .build();

        Set<ConstraintViolation<TelephoneChargeCreateRequest>> constraintViolations = validator.validate(telephoneChargeCreateRequest);

        assertThat(constraintViolations.size(), is(1));
        assertThat(constraintViolations.iterator().next().getMessage(), is("Field [card_expiry] must have valid MM/YY"));
    }

    @Test
    public void failsValidationForInvalidMonth99AndStatusOfFailed() {

        TelephoneChargeCreateRequest telephoneChargeCreateRequest = telephoneRequestBuilder
                .withCardExpiry("99/99")
                .withPaymentOutcome(new PaymentOutcome("failed", "P0050", new Supplemental()))
                .build();

        Set<ConstraintViolation<TelephoneChargeCreateRequest>> constraintViolations = validator.validate(telephoneChargeCreateRequest);

        assertThat(constraintViolations.size(), isNumber(1));
        assertThat(constraintViolations.iterator().next().getMessage(), is("Field [card_expiry] must have valid MM/YY"));
    }

    @Test
    public void passesValidationForValidCardExpiryAndStatusOfSuccess() {

        TelephoneChargeCreateRequest telephoneChargeCreateRequest = telephoneRequestBuilder
                .withCardExpiry("01/99")
                .withPaymentOutcome(new PaymentOutcome("success"))
                .build();

        Set<ConstraintViolation<TelephoneChargeCreateRequest>> constraintViolations = validator.validate(telephoneChargeCreateRequest);

        assertThat(constraintViolations.isEmpty(), is(true));
    }

    @Test
    public void passesValidationForValidCardExpiryAndStatusOfFailed() {

        TelephoneChargeCreateRequest telephoneChargeCreateRequest = telephoneRequestBuilder
                .withCardExpiry("01/99")
                .withPaymentOutcome(new PaymentOutcome("failed", "P0050", new Supplemental()))
                .build();

        Set<ConstraintViolation<TelephoneChargeCreateRequest>> constraintViolations = validator.validate(telephoneChargeCreateRequest);

        assertThat(constraintViolations.isEmpty(), is(true));
    }

    @Test
    public void passesValidationForNullCardExpiryAndStatusOfFailed() {

        TelephoneChargeCreateRequest telephoneChargeCreateRequest = telephoneRequestBuilder
                .withCardExpiry(null)
                .withPaymentOutcome(new PaymentOutcome("failed", "P0050", new Supplemental()))
                .build();

        Set<ConstraintViolation<TelephoneChargeCreateRequest>> constraintViolations = validator.validate(telephoneChargeCreateRequest);

        assertThat(constraintViolations.isEmpty(), is(true));
    }

    @Test
    public void failsValidationForNullCardExpiryAndStatusOfSuccess() {

        TelephoneChargeCreateRequest telephoneChargeCreateRequest = telephoneRequestBuilder
                .withCardExpiry(null)
                .withPaymentOutcome(new PaymentOutcome("success"))
                .build();

        Set<ConstraintViolation<TelephoneChargeCreateRequest>> constraintViolations = validator.validate(telephoneChargeCreateRequest);

        assertThat(constraintViolations.size(), isNumber(1));
        assertThat(constraintViolations.iterator().next().getMessage(), is("Field [card_expiry] must have valid MM/YY"));
    }
}
