package uk.gov.pay.connector.service;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.persist.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import uk.gov.pay.connector.dao.ChargeDao;
import uk.gov.pay.connector.exception.ChargeNotFoundRuntimeException;
import uk.gov.pay.connector.exception.ConflictRuntimeException;
import uk.gov.pay.connector.exception.GenericGatewayRuntimeException;
import uk.gov.pay.connector.exception.OperationAlreadyInProgressRuntimeException;
import uk.gov.pay.connector.model.domain.*;
import uk.gov.pay.connector.model.gateway.AuthorisationGatewayRequest;
import uk.gov.pay.connector.model.gateway.GatewayResponse;

import javax.inject.Inject;
import javax.persistence.OptimisticLockException;
import java.util.Optional;
import java.util.function.Supplier;

import static uk.gov.pay.connector.model.domain.ChargeStatus.*;
import static uk.gov.pay.connector.service.CardExecutorService.ExecutionStatus;

public class CardAuthoriseService extends CardAuthoriseBaseService<AuthorisationDetails> {

    private final Auth3dsDetailsFactory auth3dsDetailsFactory;

    @Inject
    public CardAuthoriseService(ChargeDao chargeDao,
                                PaymentProviders providers,
                                CardExecutorService cardExecutorService,
                                Auth3dsDetailsFactory auth3dsDetailsFactory,
                                MetricRegistry metricRegistry) {
        super(chargeDao, providers, cardExecutorService, metricRegistry);

        this.auth3dsDetailsFactory = auth3dsDetailsFactory;
    }

    public GatewayResponse<BaseAuthoriseResponse> operation(ChargeEntity chargeEntity, AuthorisationDetails authorisationDetails) {
        return getPaymentProviderFor(chargeEntity)
                .authorise(AuthorisationGatewayRequest.valueOf(chargeEntity, authorisationDetails));
    }

    @Override
    protected ChargeStatus[] getLegalStates() {
        return new ChargeStatus[]{
                ENTERING_CARD_DETAILS
        };
    }

    @Transactional
    public GatewayResponse<BaseAuthoriseResponse> postOperation(ChargeEntity chargeEntity, AuthorisationDetails authorisationDetails, GatewayResponse<BaseAuthoriseResponse> operationResponse) {
        ChargeEntity reloadedCharge = chargeDao.merge(chargeEntity);

        ChargeStatus status = operationResponse.getBaseResponse()
                .map(BaseAuthoriseResponse::authoriseStatus)
                .map(BaseAuthoriseResponse.AuthoriseStatus::getMappedChargeStatus)
                .orElse(ChargeStatus.AUTHORISATION_ERROR);

        String transactionId = operationResponse.getBaseResponse()
                .map(BaseAuthoriseResponse::getTransactionId).orElse("");

        logger.info("AuthorisationDetails authorisation response received - charge_external_id={}, operation_type={}, transaction_id={}, status={}",
                chargeEntity.getExternalId(), OperationType.AUTHORISATION.getValue(), transactionId, status);

        GatewayAccountEntity account = chargeEntity.getGatewayAccount();

        metricRegistry.counter(String.format("gateway-operations.%s.%s.%s.authorise.result.%s", account.getGatewayName(), account.getType(), account.getId(), status.toString())).inc();

        reloadedCharge.setStatus(status);
        operationResponse.getBaseResponse().ifPresent(response -> auth3dsDetailsFactory.create(response).ifPresent(reloadedCharge::set3dsDetails));

        if (StringUtils.isBlank(transactionId)) {
            logger.warn("AuthorisationDetails authorisation response received with no transaction id. -  charge_external_id={}", reloadedCharge.getExternalId());
        } else {
            reloadedCharge.setGatewayTransactionId(transactionId);
        }

        appendCardDetails(reloadedCharge, authorisationDetails);
        chargeDao.mergeAndNotifyStatusHasChanged(reloadedCharge, Optional.empty());
        return operationResponse;
    }

    private void appendCardDetails(ChargeEntity chargeEntity, AuthorisationDetails authorisationDetails) {
        CardDetailsEntity detailsEntity = new CardDetailsEntity();
        detailsEntity.setCardBrand(authorisationDetails.getCardBrand());
        detailsEntity.setBillingAddress(new AddressEntity(authorisationDetails.getAddress()));
        detailsEntity.setCardHolderName(authorisationDetails.getCardHolder());
        detailsEntity.setExpiryDate(authorisationDetails.getEndDate());
        detailsEntity.setLastDigitsCardNumber(StringUtils.right(authorisationDetails.getCardNo(), 4));
        chargeEntity.setCardDetails(detailsEntity);
        logger.info("Stored confirmation details for charge - charge_external_id={}", chargeEntity.getExternalId());
    }
}
