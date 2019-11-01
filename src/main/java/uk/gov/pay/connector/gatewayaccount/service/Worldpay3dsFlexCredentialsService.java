package uk.gov.pay.connector.gatewayaccount.service;

import uk.gov.pay.connector.gatewayaccount.dao.Worldpay3dsFlexCredentialsDao;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.gatewayaccount.model.WorldpayUpdate3dsFlexCredentialsRequest;

import javax.inject.Inject;

import static uk.gov.pay.connector.gatewayaccount.model.Worldpay3dsFlexCredentialsEntity.Worldpay3ds2CredentialsEntityBuilder.aWorldpay3ds2CredentialsEntity;

public class Worldpay3dsFlexCredentialsService {

    private Worldpay3dsFlexCredentialsDao worldpay3DsFlexCredentialsDao;

    @Inject
    public Worldpay3dsFlexCredentialsService(Worldpay3dsFlexCredentialsDao worldpay3DsFlexCredentialsDao) {
        this.worldpay3DsFlexCredentialsDao = worldpay3DsFlexCredentialsDao;
    }

    public void setGatewayAccountWorldpay3ds2Credentials(WorldpayUpdate3dsFlexCredentialsRequest worldpayUpdate3DsFlexCredentialsRequest, GatewayAccountEntity gatewayAccountEntity) {
        worldpay3DsFlexCredentialsDao.findById(gatewayAccountEntity.getId()).ifPresentOrElse(worldpay3dsFlexCredentialsEntity -> {
            worldpay3dsFlexCredentialsEntity.setIssuer(worldpayUpdate3DsFlexCredentialsRequest.getIssuer());
            worldpay3dsFlexCredentialsEntity.setJwtMacKey(worldpayUpdate3DsFlexCredentialsRequest.getJwtMacKey());
            worldpay3dsFlexCredentialsEntity.setOrganisationalUnitId(worldpayUpdate3DsFlexCredentialsRequest.getOrganisationalUnitId());
            worldpay3DsFlexCredentialsDao.merge(worldpay3dsFlexCredentialsEntity);
        }, () -> {
            var storedEntity = aWorldpay3ds2CredentialsEntity()
                    .withGatewayAccountId(gatewayAccountEntity.getId())
                    .withIssuer(worldpayUpdate3DsFlexCredentialsRequest.getIssuer())
                    .withJwtMacKey(worldpayUpdate3DsFlexCredentialsRequest.getJwtMacKey())
                    .withOrganisationalUnitId(worldpayUpdate3DsFlexCredentialsRequest.getOrganisationalUnitId())
                    .build();
            worldpay3DsFlexCredentialsDao.merge(storedEntity);
        });
    }
}
