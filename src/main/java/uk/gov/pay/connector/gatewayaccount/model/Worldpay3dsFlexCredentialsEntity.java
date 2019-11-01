package uk.gov.pay.connector.gatewayaccount.model;

import uk.gov.pay.connector.common.model.domain.AbstractVersionedEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@SequenceGenerator(name="gateway_account_id_seq",
        sequenceName = "charges_charge_id_seq", allocationSize = 1)
@Table(name = "worldpay_3ds_flex_credentials")
public class Worldpay3dsFlexCredentialsEntity extends AbstractVersionedEntity {

    @Id
    @GeneratedValue(generator = "gateway_account_id_seq")
    @Column(name = "gateway_account_id")
    private Long gatewayAccountId;

    @Column
    private String issuer;

    @Column(name = "organisational_unit_id")
    private String organisationalUnitId;

    @Column(name = "jwt_mac_key")
    private String jwtMacKey;

    public Worldpay3dsFlexCredentialsEntity() {
        super();
    }

    public Worldpay3dsFlexCredentialsEntity(Long gatewayAccountId, String issuer, String organisationalUnitId, String jwtMacKey) {
        super();
        this.gatewayAccountId = gatewayAccountId;
        this.issuer = issuer;
        this.organisationalUnitId = organisationalUnitId;
        this.jwtMacKey = jwtMacKey;
    }

    public Long getGatewayAccountId() {
        return gatewayAccountId;
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

    public void setGatewayAccountId(Long gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setOrganisationalUnitId(String organisationalUnitId) {
        this.organisationalUnitId = organisationalUnitId;
    }

    public void setJwtMacKey(String jwtMacKey) {
        this.jwtMacKey = jwtMacKey;
    }

    public static final class Worldpay3ds2CredentialsEntityBuilder {
        private Long gatewayAccountId;
        private String issuer;
        private String organisationalUnitId;
        private String jwtMacKey;
        private Long version;

        private Worldpay3ds2CredentialsEntityBuilder() {
        }

        public static Worldpay3ds2CredentialsEntityBuilder aWorldpay3ds2CredentialsEntity() {
            return new Worldpay3ds2CredentialsEntityBuilder();
        }

        public Worldpay3ds2CredentialsEntityBuilder withGatewayAccountId(Long gatewayAccountId) {
            this.gatewayAccountId = gatewayAccountId;
            return this;
        }

        public Worldpay3ds2CredentialsEntityBuilder withIssuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Worldpay3ds2CredentialsEntityBuilder withOrganisationalUnitId(String organisationalUnitId) {
            this.organisationalUnitId = organisationalUnitId;
            return this;
        }

        public Worldpay3ds2CredentialsEntityBuilder withJwtMacKey(String jwtMacKey) {
            this.jwtMacKey = jwtMacKey;
            return this;
        }

        public Worldpay3ds2CredentialsEntityBuilder withVersion(Long version) {
            this.version = version;
            return this;
        }

        public Worldpay3dsFlexCredentialsEntity build() {
            Worldpay3dsFlexCredentialsEntity worldpay3DsFlexCredentialsEntity = new Worldpay3dsFlexCredentialsEntity();
            worldpay3DsFlexCredentialsEntity.jwtMacKey = this.jwtMacKey;
            worldpay3DsFlexCredentialsEntity.gatewayAccountId = this.gatewayAccountId;
            worldpay3DsFlexCredentialsEntity.organisationalUnitId = this.organisationalUnitId;
            worldpay3DsFlexCredentialsEntity.issuer = this.issuer;
            return worldpay3DsFlexCredentialsEntity;
        }
    }
}
