package uk.gov.pay.connector.client.ledger.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.client.ledger.model.LedgerTransaction;
import uk.gov.pay.connector.client.ledger.model.RefundTransactionsForPayment;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.LEDGER_GET_REFUNDS_FOR_PAYMENT;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.LEDGER_GET_TRANSACTION;
import static uk.gov.pay.connector.util.TestTemplateResourceLoader.load;

@RunWith(MockitoJUnitRunner.class)
public class LedgerServiceTest {

    private ObjectMapper objectMapper = new ObjectMapper();
    private LedgerService ledgerService;
    private Response mockResponse;
    private Client mockClient;
    private ConnectorConfiguration mockConnectorConfiguration;

    @Before
    public void setUp() {
        mockClient = mock(Client.class);
        mockConnectorConfiguration = mock(ConnectorConfiguration.class);
        WebTarget mockWebTarget = mock(WebTarget.class);
        Invocation.Builder mockBuilder = mock(Invocation.Builder.class);
        mockResponse = mock(Response.class);

        when(mockConnectorConfiguration.getLedgerBaseUrl()).thenReturn("http://ledgerUrl");
        when(mockClient.target(any(UriBuilder.class))).thenReturn(mockWebTarget);
        when(mockWebTarget.request()).thenReturn(mockBuilder);
        when(mockBuilder.accept(APPLICATION_JSON)).thenReturn(mockBuilder);
        when(mockBuilder.get()).thenReturn(mockResponse);

        when(mockResponse.getStatus()).thenReturn(SC_OK);
        ledgerService = new LedgerService(mockClient, mockConnectorConfiguration);
    }

    @Test
    public void getTransaction_shouldSerialiseLedgerTransaction() throws JsonProcessingException {
        when(mockResponse.readEntity(LedgerTransaction.class)).thenReturn(objectMapper.readValue(load(LEDGER_GET_TRANSACTION), LedgerTransaction.class));

        String externalId = "external-id";
        Optional<LedgerTransaction> mayBeTransaction = ledgerService.getTransaction("external-id");

        assertThat(mayBeTransaction.isPresent(), is(true));
        LedgerTransaction transaction = mayBeTransaction.get();
        assertThat(transaction.getTransactionId(), is(externalId));
        assertThat(transaction.getAmount(), is(12000L));
        assertThat(transaction.getGatewayAccountId(), is("3"));
        assertThat(transaction.getExternalMetaData(), is(notNullValue()));
    }

    @Test
    public void getRefundsFromLedgerShouldSerialiseResponseCorrectly() throws JsonProcessingException {
        when(mockResponse.readEntity(RefundTransactionsForPayment.class)).
                thenReturn(objectMapper.readValue(load(LEDGER_GET_REFUNDS_FOR_PAYMENT), RefundTransactionsForPayment.class));

        String externalId = "650516the13q5jpfo435f1m1fm";
        Optional<RefundTransactionsForPayment> mayBeRefundsForPayment =
                ledgerService.getRefundsForPayment(152L, externalId);

        assertThat(mayBeRefundsForPayment.isPresent(), is(true));
        RefundTransactionsForPayment refundTransactionsForPayment = mayBeRefundsForPayment.get();
        assertThat(refundTransactionsForPayment.getParentTransactionId(), is(externalId));

        List<LedgerTransaction> transactions = refundTransactionsForPayment.getTransactions();

        assertThat(transactions.get(0).getTransactionId(), is("nklfm1pk9flpu91j815kp2835o"));
        assertThat(transactions.get(0).getGatewayAccountId(), is("152"));
        assertThat(transactions.get(0).getAmount(), is(100L));
        assertThat(transactions.get(0).getState().getStatus(), is("success"));

        assertThat(transactions.get(1).getTransactionId(), is("migtkmlt6gvm16sim5h0p7oeje"));
        assertThat(transactions.get(1).getAmount(), is(110L));
        assertThat(transactions.get(1).getGatewayAccountId(), is("152"));
        assertThat(transactions.get(1).getState().getStatus(), is("failed"));
    }

    @Test
    public void  getRefundsFromLedgerShouldReturnAnEmptyOptionalForNon2xxResponse() {
        when(mockResponse.getStatus()).thenReturn(SC_NOT_FOUND);

        String externalId = "650516the13q5jpfo435f1m1fm";
        Optional<RefundTransactionsForPayment> mayBeRefundsForPayment =
                ledgerService.getRefundsForPayment(152L, externalId);

        assertThat(mayBeRefundsForPayment.isPresent(), is(false));
    }
}
