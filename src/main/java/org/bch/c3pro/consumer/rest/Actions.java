package org.bch.c3pro.consumer.rest;

import org.bch.c3pro.consumer.external.SQSAccess;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Created by CH176656 on 5/11/2015.
 */
@Path("/actions")
@RequestScoped
@Stateful
public class Actions {
    @POST
    @Path("/start")
    public Response startService() {
        SQSAccess sqs = SQSAccess.getInstance();
        try {
            sqs.startListening();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/stop")
    public Response stopService() {
        SQSAccess sqs = SQSAccess.getInstance();
        try {
            sqs.stopListening();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        return Response.status(Response.Status.OK).build();
    }
}
