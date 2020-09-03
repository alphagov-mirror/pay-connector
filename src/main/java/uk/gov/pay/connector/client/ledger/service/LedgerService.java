package uk.gov.pay.connector.client.ledger.service;

import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.client.ledger.model.LedgerTransaction;
import uk.gov.pay.connector.client.ledger.model.RefundTransactionsForPayment;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.Optional;

import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;

public class LedgerService {

    private final Client client;
    private final String ledgerUrl;

    @Inject
    public LedgerService(Client client, ConnectorConfiguration configuration) {
        this.client = client;
        this.ledgerUrl = configuration.getLedgerBaseUrl();
    }

    public Optional<LedgerTransaction> getTransaction(String id) {
        var uri = UriBuilder
                .fromPath(ledgerUrl)
                .path(format("/v1/transaction/%s", id))
                .queryParam("override_account_id_restriction", "true");

        return getTransactionFromLedger(uri);
    }

    public Optional<LedgerTransaction> getTransactionForProviderAndGatewayTransactionId(String paymentGatewayName,
                                                                                        String gatewayTransactionId) {
        var uri = UriBuilder
                .fromPath(ledgerUrl)
                .path(format("/v1/transaction/gateway-transaction/%s", gatewayTransactionId))
                .queryParam("payment_provider", paymentGatewayName);

        return getTransactionFromLedger(uri);
    }

    public Optional<LedgerTransaction> getTransactionForGatewayAccount(String id, Long gatewayAccountId) {
        var uri = UriBuilder
                .fromPath(ledgerUrl)
                .path(format("/v1/transaction/%s", id))
                .queryParam("account_id", gatewayAccountId);

        return getTransactionFromLedger(uri);
    }

    public Optional<RefundTransactionsForPayment> getRefundsForPayment(Long gatewayAccountId, String paymentExternalId) {
        var uri = UriBuilder
                .fromPath(ledgerUrl)
                .path(format("/v1/transaction/%s/transaction", paymentExternalId))
                .queryParam("gateway_account_id", gatewayAccountId);

        Response response = getResponse(uri);

        if (response.getStatus() == SC_OK) {
            return Optional.of(response.readEntity(RefundTransactionsForPayment.class));
        }

        return Optional.empty();
    }

    private Optional<LedgerTransaction> getTransactionFromLedger(UriBuilder uri) {
        Response response = getResponse(uri);

        if (response.getStatus() == SC_OK) {
            return Optional.of(response.readEntity(LedgerTransaction.class));
        }

        return Optional.empty();
    }

    private Response getResponse(UriBuilder uri) {
        return client
                .target(uri)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
    }

}
