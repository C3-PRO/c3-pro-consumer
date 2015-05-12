package org.bch.c3pro.consumer.rest;

import org.bch.c3pro.consumer.external.SQSAccess;
import org.bch.c3pro.consumer.external.SQSListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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

    Logger log = LoggerFactory.getLogger(Actions.class);

    @PersistenceContext(unitName="fhir-resource")
    private EntityManager em;

    @Inject @Default
    private SQSListener listener;

    @POST
    @Path("/start")
    public Response startService() {
        log.info("REST /start");
        SQSAccess sqs = SQSAccess.getInstance();
        try {
            listener.setEntityManager(em);
            sqs.startListening(listener);
        } catch (Exception e) {
            log.error("Error starting listener: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/stop")
    public Response stopService() {
        log.info("REST /start");
        SQSAccess sqs = SQSAccess.getInstance();
        try {
            sqs.stopListening();
        } catch (Exception e) {
            log.error("Error stopping listener: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        return Response.status(Response.Status.OK).build();
    }
}
