package uk.gov.pay.connector.gateway.worldpay;

import io.dropwizard.setup.Environment;
import uk.gov.pay.connector.app.ConnectorConfiguration;
import uk.gov.pay.connector.charge.service.Worldpay3dsFlexJwtService;
import uk.gov.pay.connector.gateway.ClientFactory;
import uk.gov.pay.connector.gateway.worldpay.exception.ThreeDsFlexDdcServiceUnavailableException;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccount;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.gatewayaccount.model.Worldpay3dsFlexCredentials;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.pay.connector.gateway.PaymentGatewayName.WORLDPAY;

public class Worldpay3dsFlexCredentialsValidationService {

    private static final List<Integer> ACCEPTABLE_RESPONSE_CODES_FROM_3DS_FLEX_URL = List.of(SC_OK, SC_BAD_REQUEST);
    
    private final Client client;
    private final Worldpay3dsFlexJwtService worldpay3dsFlexJwtService;
    private final Map<String, String> threeDsFlexDdcUrls;

    @Inject
    public Worldpay3dsFlexCredentialsValidationService(ClientFactory clientFactory,
                                                       Environment environment,
                                                       Worldpay3dsFlexJwtService worldpay3dsFlexJwtService,
                                                       ConnectorConfiguration connectorConfiguration) {
        this.worldpay3dsFlexJwtService = worldpay3dsFlexJwtService;
        client = clientFactory.createWithDropwizardClient(WORLDPAY, environment.metrics());
        threeDsFlexDdcUrls = connectorConfiguration.getWorldpayConfig().getThreeDsFlexDdcUrls();
    }

    public boolean validateCredentials(GatewayAccountEntity gatewayAccountEntity, Worldpay3dsFlexCredentials flexCredentials) {
        String ddcToken = worldpay3dsFlexJwtService.generateDdcToken(GatewayAccount.valueOf(gatewayAccountEntity), 
                flexCredentials, ZonedDateTime.now());

        var formData = new MultivaluedHashMap<String, String>();
        formData.add("JWT", ddcToken);

        String url = threeDsFlexDdcUrls.get(gatewayAccountEntity.getType());

        Response response = null;
        try {
            response = client.target(url).request().post(Entity.form(formData));
        } catch (ProcessingException e) {
            throw new ThreeDsFlexDdcServiceUnavailableException(e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        
        if (!ACCEPTABLE_RESPONSE_CODES_FROM_3DS_FLEX_URL.contains(response.getStatus())) {
            throw new ThreeDsFlexDdcServiceUnavailableException();
        }
        
        return response.getStatus() == SC_OK;
    }
}