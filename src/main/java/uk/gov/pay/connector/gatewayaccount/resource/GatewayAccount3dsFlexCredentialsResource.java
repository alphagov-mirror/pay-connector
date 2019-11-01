package uk.gov.pay.connector.gatewayaccount.resource;

import uk.gov.pay.connector.gatewayaccount.model.WorldpayUpdate3dsFlexCredentialsRequest;
import uk.gov.pay.connector.gatewayaccount.service.GatewayAccountService;
import uk.gov.pay.connector.gatewayaccount.service.Worldpay3dsFlexCredentialsService;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static uk.gov.pay.connector.util.ResponseUtil.notFoundResponse;

@Path("/")
public class GatewayAccount3dsFlexCredentialsResource {

    private final GatewayAccountService gatewayAccountService;
    private final Worldpay3dsFlexCredentialsService worldpay3DsFlexCredentialsService;

    @Inject
    public GatewayAccount3dsFlexCredentialsResource(GatewayAccountService gatewayAccountService,
                                                    Worldpay3dsFlexCredentialsService worldpay3DsFlexCredentialsService) {
        this.gatewayAccountService = gatewayAccountService;
        this.worldpay3DsFlexCredentialsService = worldpay3DsFlexCredentialsService;
    }

    @POST
    @Path("/v1/api/accounts/{accountId}/3ds-flex-credentials")
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    public Response createOrUpdateWorldpay3dsCredentials(@PathParam("accountId") Long gatewayAccountId,
                                                         @Valid WorldpayUpdate3dsFlexCredentialsRequest worldpay3dsCredentials) {

        return gatewayAccountService.getGatewayAccount(gatewayAccountId)
                .map((gatewayAccountEntity) -> {
                    if (gatewayAccountEntity.getGatewayName().equalsIgnoreCase("worldpay")) {
                        worldpay3DsFlexCredentialsService.setGatewayAccountWorldpay3ds2Credentials(worldpay3dsCredentials,
                                gatewayAccountEntity);
                        return Response.ok().build();
                    } else {
                        return notFoundResponse("Not a Worldpay gateway account");
                    }
                })
                .orElseGet(() -> notFoundResponse("Not a Worldpay gateway account"));
    }
}
