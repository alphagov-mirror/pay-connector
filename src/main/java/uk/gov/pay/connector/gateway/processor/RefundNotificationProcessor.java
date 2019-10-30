package uk.gov.pay.connector.gateway.processor;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.connector.gateway.PaymentGatewayName;
import uk.gov.pay.connector.gateway.epdq.EpdqNotification;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.refund.model.domain.RefundEntity;
import uk.gov.pay.connector.refund.model.domain.RefundStatus;
import uk.gov.pay.connector.refund.service.ChargeRefundService;
import uk.gov.pay.connector.usernotification.service.UserNotificationService;

import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class RefundNotificationProcessor {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private ChargeRefundService refundService;
    private UserNotificationService userNotificationService;

    @Inject
    RefundNotificationProcessor(ChargeRefundService refundService,
                                UserNotificationService userNotificationService) {
        this.refundService = refundService;
        this.userNotificationService = userNotificationService;
    }

    public void invoke(PaymentGatewayName gatewayName, RefundStatus newStatus, EpdqNotification notification) {
        if (isBlank(notification.getReference())) {
            logger.warn("{} refund notification could not be used to update charge (missing reference)",
                    gatewayName);
            return;
        }

        Optional<RefundEntity> optionalRefundEntity = refundService.findByProviderAndReference(gatewayName.getName(), notification.getReference());
        if (!optionalRefundEntity.isPresent()) {
            logger.warn("{} notification '{}' could not be used to update refund (associated refund entity not found for reference)",
                    gatewayName, notification);
            return;
        }

        RefundEntity refundEntity = optionalRefundEntity.get();
        RefundStatus oldStatus = refundEntity.getStatus();

        refundService.transitionRefundState(refundEntity, newStatus);

        if (RefundStatus.REFUNDED.equals(newStatus)) {
            userNotificationService.sendRefundIssuedEmail(refundEntity);
        }

        GatewayAccountEntity gatewayAccount = refundEntity.getChargeEntity().getGatewayAccount();
        logger.info("Notification received for refund. Updating refund - charge_external_id={}, refund_reference={}, transaction_id={}, status={}, "
                        + "status_to={}, account_id={}, provider={}, provider_type={}",
                refundEntity.getChargeEntity().getExternalId(),
                notification.getReference(),
                notification.getTransactionId(),
                oldStatus,
                newStatus,
                gatewayAccount.getId(),
                gatewayAccount.getGatewayName(),
                gatewayAccount.getType());
    }
}
