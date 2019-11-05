package uk.gov.pay.connector.gatewayaccount.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class WorldpayUpdate3dsFlexCredentialsRequest {

    @JsonProperty("issuer")
    @NotNull(message = "Field [issuer] cannot be null")
    private String issuer;

    @JsonProperty("organisational_unit_id")
    @NotNull(message = "Field [organisational_unit_id] cannot be null")
    private String organisationalUnitId;

    @JsonProperty("jwt_mac_key")
    @NotNull(message = "Field [jwt_mac_key] cannot be null")
    private String jwtMacKey;

    public WorldpayUpdate3dsFlexCredentialsRequest() {
        //Blank Constructor Needed For Instantiation
    }

    private WorldpayUpdate3dsFlexCredentialsRequest(WorldpayUpdate3ds2CredentialsRequestBuilder builder) {
        this.issuer = builder.issuer;
        this.organisationalUnitId = builder.organisationalUnitId;
        this.jwtMacKey = builder.jwtMacKey;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getOrganisationalUnitId() {
        return organisationalUnitId;
    }

    public String getJwtMacKey() {
        return jwtMacKey;
    }

    public static final class WorldpayUpdate3ds2CredentialsRequestBuilder {
        private String issuer;
        private String organisationalUnitId;
        private String jwtMacKey;

        private WorldpayUpdate3ds2CredentialsRequestBuilder() {
        }

        public static WorldpayUpdate3ds2CredentialsRequestBuilder aWorldpayUpdate3ds2CredentialsRequest() {
            return new WorldpayUpdate3ds2CredentialsRequestBuilder();
        }

        public WorldpayUpdate3ds2CredentialsRequestBuilder withIssuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public WorldpayUpdate3ds2CredentialsRequestBuilder withOrganisationalUnitId(String organisationalUnitId) {
            this.organisationalUnitId = organisationalUnitId;
            return this;
        }

        public WorldpayUpdate3ds2CredentialsRequestBuilder withJwtMacKey(String jwtMacKey) {
            this.jwtMacKey = jwtMacKey;
            return this;
        }

        public WorldpayUpdate3dsFlexCredentialsRequest build() {
            return new WorldpayUpdate3dsFlexCredentialsRequest(this);
        }
    }
}

