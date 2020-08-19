package uk.gov.pay.connector.northamericaregion;

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CanadaPostalcodeToProvinceOrTerritoryMapperTest {

    @Test
    public void shouldReturnTheCorrectStateForNonXPostalCode() {
        Optional<CanadaProvinceOrTerritory> canadaProvinceTerritory = CanadaPostalcodeToProvinceOrTerritoryMapper.getProvinceOrTerritory("A1A1A1");

        assertThat(canadaProvinceTerritory.isPresent(), is (true));
        assertThat(canadaProvinceTerritory.get().getAbbreviation(), is (CanadaProvinceOrTerritory.NEWFOUNDLAND_AND_LABRADOR.getAbbreviation()));
        assertThat(canadaProvinceTerritory.get().getFullName(), is (CanadaProvinceOrTerritory.NEWFOUNDLAND_AND_LABRADOR.getFullName()));
    }

    @Test
    public void shouldReturnTheCorrectStateForNunavutPostalCode() {
        Optional<CanadaProvinceOrTerritory> canadaProvinceTerritory = CanadaPostalcodeToProvinceOrTerritoryMapper.getProvinceOrTerritory("X0A0A0");

        assertThat(canadaProvinceTerritory.isPresent(), is (true));
        assertThat(canadaProvinceTerritory.get().getAbbreviation(), is (CanadaProvinceOrTerritory.NUNAVUT.getAbbreviation()));
        assertThat(canadaProvinceTerritory.get().getFullName(), is (CanadaProvinceOrTerritory.NUNAVUT.getFullName()));
    }

    @Test
    public void shouldReturnTheCorrectStateForNorthwestTerritoriesPostalCode() {
        Optional<CanadaProvinceOrTerritory> canadaProvinceTerritory = CanadaPostalcodeToProvinceOrTerritoryMapper.getProvinceOrTerritory("X0E1Z0");

        assertThat(canadaProvinceTerritory.isPresent(), is (true));
        assertThat(canadaProvinceTerritory.get().getAbbreviation(), is (CanadaProvinceOrTerritory.NORTHWEST_TERRITORIES.getAbbreviation()));
        assertThat(canadaProvinceTerritory.get().getFullName(), is (CanadaProvinceOrTerritory.NORTHWEST_TERRITORIES.getFullName()));
    }
    
    @Test
    public void shouldNotReturnAStateForSantaPostalCode() {
        Optional<CanadaProvinceOrTerritory> canadaProvinceTerritory = CanadaPostalcodeToProvinceOrTerritoryMapper.getProvinceOrTerritory("H0H0H0");
        
        assertThat(canadaProvinceTerritory.isEmpty(), is (true));
    }

}
