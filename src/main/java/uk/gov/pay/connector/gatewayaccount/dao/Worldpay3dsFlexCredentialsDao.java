package uk.gov.pay.connector.gatewayaccount.dao;

import com.google.inject.Inject;
import com.google.inject.Provider;
import uk.gov.pay.connector.common.dao.JpaDao;
import uk.gov.pay.connector.gatewayaccount.model.Worldpay3dsFlexCredentialsEntity;

import javax.persistence.EntityManager;
import java.util.Optional;

public class Worldpay3dsFlexCredentialsDao extends JpaDao<Worldpay3dsFlexCredentialsEntity> {
    
    @Inject
    public Worldpay3dsFlexCredentialsDao(Provider<EntityManager> entityManager) {
        super(entityManager);
    }

    public Optional<Worldpay3dsFlexCredentialsEntity> findById(Long accountId) {
        return super.findById(Worldpay3dsFlexCredentialsEntity.class, accountId);
    }

    @Override
    public void persist(Worldpay3dsFlexCredentialsEntity object) {
        super.persist(object);
    }

    @Override
    public Worldpay3dsFlexCredentialsEntity merge(Worldpay3dsFlexCredentialsEntity object) {
        return super.merge(object);
    }
}
