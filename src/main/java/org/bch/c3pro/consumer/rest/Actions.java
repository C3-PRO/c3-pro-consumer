package org.bch.c3pro.consumer.rest;

import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.external.SQSAccess;
import org.bch.c3pro.consumer.external.SQSListener;
import org.jboss.resteasy.util.Base64;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.Consumes;
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

    /**
     * Just for testing purposes!
     * json:
     * {
     *     "id":{{UUID of the key}},
     *     "symetric_key": {{BASE64(key)}}
     *     "message":{{Encrypted message}}
     * }
     * @return
     */
    @POST
    @Path("/decrypt")
    @Consumes("application/json")
    public Response decrypt(String json) {
        log.info("REST /decrypt");
        String messageString=null;
        try {
            JSONObject jsonObj = new JSONObject(json);
            String publicKeyId = jsonObj.getString("id");
            String symKey64 = jsonObj.getString("symetric_key");
            String message64 = null;
            try {
                message64 = jsonObj.getString("message");
            } catch (Exception e){}

            byte [] secretKeyEnc = Base64.decode(symKey64);
            SQSListener sqsListener = new SQSListener();
            byte [] secretKey = sqsListener.decryptSecretKey(secretKeyEnc, publicKeyId);

            if (message64!= null) {

                byte[] messageEnc = Base64.decode(message64);
                byte[] message = sqsListener.decryptMessage(messageEnc, secretKey);
                messageString = new String(message, "UTF-8");
            } else {
                messageString = new String(secretKey, "UTF-8");
            }


        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
        return Response.status(Response.Status.OK).entity(messageString).build();
    }

}
