package uk.gov.pay.connector.northamericaregion;

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class UsZipCodeToStateMapperTest {

    @Test
    public void shouldReturnTheCorrectStateForValidZipCode() {
        Optional<UsState> usState = UsZipCodeToStateMapper.getState("05910");

        assertThat(usState.isPresent(), is (true));
        assertThat(usState.get().getAbbreviation(), is (UsState.VERMONT.getAbbreviation()));
        assertThat(usState.get().getFullName(), is (UsState.VERMONT.getFullName()));
    }

    @Test
    public void shouldReturnTheCorrectStateForValidZipCodePlusFour() {
        Optional<UsState> usState = UsZipCodeToStateMapper.getState("05910-1234");

        assertThat(usState.isPresent(), is (true));
        assertThat(usState.get().getAbbreviation(), is (UsState.VERMONT.getAbbreviation()));
        assertThat(usState.get().getFullName(), is (UsState.VERMONT.getFullName()));
    }

    @Test
    public void shouldReturnTheCorrectStateForValidStateAndZipCode() {
        Optional<UsState> usState = UsZipCodeToStateMapper.getState("VT05910");

        assertThat(usState.isPresent(), is (true));
        assertThat(usState.get().getAbbreviation(), is (UsState.VERMONT.getAbbreviation()));
        assertThat(usState.get().getFullName(), is (UsState.VERMONT.getFullName()));
    }

    @Test
    public void shouldReturnTheCorrectStateForValidStateAndZipCodeFourPlus() {
        Optional<UsState> usState = UsZipCodeToStateMapper.getState("VT05910-1234");

        assertThat(usState.isPresent(), is (true));
        assertThat(usState.get().getAbbreviation(), is (UsState.VERMONT.getAbbreviation()));
        assertThat(usState.get().getFullName(), is (UsState.VERMONT.getFullName()));
    }

    @Test
    public void shouldNotReturnStateForInvalidStateAndZipCodeFormat() {
        Optional<UsState> usState = UsZipCodeToStateMapper.getState("XX05910");
        assertThat(usState.isEmpty(), is (true));
    }

    @Test
    public void shouldNotReturnStateForInvalidStateAndZipCodePlusFourFormat() {
        Optional<UsState> usState = UsZipCodeToStateMapper.getState("XX05910-1234");
        assertThat(usState.isEmpty(), is (true));
    }

    @Test
    public void shouldNotReturnStateForZipCodeNotInUse() {
        Optional<UsState> usState = UsZipCodeToStateMapper.getState("00000");
        assertThat(usState.isEmpty(), is (true));
    }

    @Test
    public void shouldNotReturnStateForInvalidZipCodeFormat() {
        Optional<UsState> usState = UsZipCodeToStateMapper.getState("xxxxx");
        assertThat(usState.isEmpty(), is (true));
    }
}
