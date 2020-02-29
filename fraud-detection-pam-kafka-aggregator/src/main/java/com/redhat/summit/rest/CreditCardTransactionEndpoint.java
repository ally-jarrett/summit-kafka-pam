package com.redhat.summit.rest;

import java.util.List;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.redhat.summit.model.GetAggregatedTransactionsResult;
import com.redhat.summit.stream.InteractiveQuery;
import com.redhat.summit.stream.PipelineMetadata;

@ApplicationScoped
@Path("/transactions")
public class CreditCardTransactionEndpoint {
    
    @Inject
    InteractiveQuery interactiveQueries;

    @GET
    @Path("/meta-data")
    @Produces(MediaType.APPLICATION_JSON)
    public List<PipelineMetadata> getMetaData() {
        return interactiveQueries.getMetaData();
    }

    @GET
    @Path("/data/{credit-card-number}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAggregatedTransactionsForCardNumber(@PathParam("credit-card-number") String creditCardNumber) {
        final GetAggregatedTransactionsResult result = interactiveQueries.getAggregatedTransactions(creditCardNumber);

        if (result.getResult().isPresent()) {
            return Response.ok(result.getResult().get()).build();
        } else if (result.getHost().isPresent()) {
            final URI otherUri = getOtherUri(result.getHost().get(), result.getPort().getAsInt(), creditCardNumber);
            return Response.seeOther(otherUri).build();
        } else {
            return Response.status(Status.NOT_FOUND.getStatusCode(), "No data found for weather station " + creditCardNumber).build();
        }
    }

    private URI getOtherUri(String host, int port, String creditCardNumber) {
        try {
            return new URI("http://" + host + ":" + port + "/transactions/data/" + creditCardNumber);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}