package uk.gov.pay.connector.northamericaregion;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

public class UsZipCodeToStateMapper {

    private final static Pattern WELL_FORMED_STATE_WITH_ZIP_CODE = Pattern.compile("[A-Z]{2}[0-9]{5}");
    private final static Pattern WELL_FORMED_STATE_WITH_ZIP_FOUR_PLUS = Pattern.compile("[A-Z]{2}[0-9]{5}-[0-9]{4}");
    private final static Pattern WELL_FORMED_ZIP_CODE = Pattern.compile("[0-9]{5}");
    private final static Pattern WELL_FORMED_ZIP_PLUS_FOUR = Pattern.compile("[0-9]{5}-[0-9]{4}");
    
    private final static Map<String, UsState> US_STATE_ABBREVIATIONS_MAPPING = Arrays.stream(UsState.values()).collect(
            toUnmodifiableMap(UsState::getAbbreviation, identity())); 
    
    public static Optional<UsState> getState(String normalisedZipCode) {
        
        if (WELL_FORMED_STATE_WITH_ZIP_CODE.matcher(normalisedZipCode).matches() || WELL_FORMED_STATE_WITH_ZIP_FOUR_PLUS.matcher(normalisedZipCode).matches()) {
            return getStateFromZipCodeAndState(normalisedZipCode);
        } else if (WELL_FORMED_ZIP_CODE.matcher(normalisedZipCode).matches() || WELL_FORMED_ZIP_PLUS_FOUR.matcher(normalisedZipCode).matches()) {
            return getStateFromZipCode(normalisedZipCode.substring(0, 3));
        }
        
        return Optional.empty();
    }
    
    private static Optional<UsState> getStateFromZipCodeAndState(String zipCodeWithState) {
        String initialTwoChars = zipCodeWithState.substring(0, 2);
        return Optional.ofNullable(US_STATE_ABBREVIATIONS_MAPPING.get(initialTwoChars));
    }
    
    private static Optional<UsState> getStateFromZipCode(String zipCode) {
        String stateAbbreviation = zipCode.substring(0, 3);
        return Optional.ofNullable(UsZipCodeToStateMap.ZIP_CODE_TO_US_STATE_ABBREVIATIONS.get(stateAbbreviation));
    }

}
