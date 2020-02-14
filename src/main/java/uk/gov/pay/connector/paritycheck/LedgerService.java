package uk.gov.pay.connector.paritycheck;

import uk.gov.pay.connector.app.ConnectorConfiguration;

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

    public Optional<LedgerTransaction> getTransactionForGatewayAccount(String id, Long gatewayAccountId) {
        var uri = UriBuilder
                .fromPath(ledgerUrl)
                .path(format("/v1/transaction/%s", id))
                .queryParam("account_id", gatewayAccountId);

        return getTransactionFromLedger(uri);
    }

    private Optional<LedgerTransaction> getTransactionFromLedger(UriBuilder uri) {
        Response response = client
                .target(uri)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get();

        if (response.getStatus() == SC_OK) {
            return Optional.of(response.readEntity(LedgerTransaction.class));
        }

        return Optional.empty();
    }

}
