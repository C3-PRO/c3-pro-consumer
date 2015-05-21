package org.bch.c3pro.consumer.rest;


import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.exception.C3PROException;
import org.bch.c3pro.consumer.external.SQSAccess;
import org.bch.c3pro.consumer.external.SQSListener;
import org.bch.c3pro.consumer.util.HttpRequest;
import org.bch.c3pro.consumer.util.Response;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by CH176656 on 5/12/2015.
 */
public class ActionsIT {
    // It requires c3pro-server running serving to the same queue as c3pro-consumer,
    // an empty queue and the public key of the c3pro-consumer installed in the c3pro-server
    public static StringBuffer sb = new StringBuffer();

    @Ignore
    public void generalTestIT() throws Exception {
        HttpRequest http = new HttpRequest();
        String jsonIn =readTextFile("q1.json");
        String conentTypeHeader = "application/json";
        String url = "http://ec2-52-11-82-72.us-west-2.compute.amazonaws.com:8080/c3pro/fhir/Questionnaire";
        Response resp = http.doPostGeneric(url,jsonIn,null,conentTypeHeader);
        assertEquals(201, resp.getResponseCode());

        SQSAccess sqsAccess = SQSAccess.getInstance();
        SQSListener listener = new SQSListenerTest();
        sqsAccess.startListening(listener);
        System.out.println("Waiting for 4 seconds to let the consumer process the message");
        Thread.sleep(4000);
        if (sb.length()==0) {
            fail("Timeout: waiting for 4 seconds to read from queue");
        } else {
            JSONObject jsonObjIn = new JSONObject(jsonIn);
            JSONObject jsonObjOut = new JSONObject(sb.toString());
            JSONObject qIn = jsonObjIn.getJSONObject("group");
            JSONObject qOut = jsonObjOut.getJSONObject("group");
            assertEquals(qIn.toString(), qOut.toString());
        }
    }

    @Test
    public void generalTest_IT() throws Exception {
        HttpRequest http = new HttpRequest();
        String jsonIn =readTextFile("q1.json");
        String contentTypeHeader = "application/json";

        String urlAuth = AppConfig.getProp(AppConfig.C3PRO_SERVER_TRANS) + "://" +
                AppConfig.getProp(AppConfig.C3PRO_SERVER_HOST) + ":" +
                AppConfig.getProp(AppConfig.C3PRO_SERVER_PORT) +
                "/c3pro/oauth?grant_type=client_credentials";

        String url = AppConfig.getProp(AppConfig.C3PRO_SERVER_TRANS) + "://" +
                AppConfig.getProp(AppConfig.C3PRO_SERVER_HOST) + ":" +
                AppConfig.getProp(AppConfig.C3PRO_SERVER_PORT) +
                "/c3pro/fhir/Questionnaire";

        // First we get the access token
        String cred = AppConfig.getAuthCredentials(AppConfig.C3PRO_SERVER_CREDENTIALS);
        String authHeader = "Basic " + new String(javax.xml.bind.DatatypeConverter.printBase64Binary(cred.getBytes()));
        System.out.println(urlAuth);
        System.out.println(authHeader);
        Response resp = http.doPostGeneric(urlAuth,null,authHeader,null);
        assertTrue(resp.getResponseCode() >= 200 && resp.getResponseCode() <= 204);

        String jsonRespStr = resp.getContent();
        JSONObject jsonResp = new JSONObject(jsonRespStr);
        String tokenType = jsonResp.getString("token_type");
        assertEquals("bearer", tokenType);

        String token = jsonResp.getString("access_token");

        // Now we push the questionnaire
        authHeader = "Bearer " + token;
        resp = http.doPostGeneric(url,jsonIn,authHeader,contentTypeHeader);
        assertTrue(resp.getResponseCode() >= 200 && resp.getResponseCode() <= 204);

        // And we check that the questionnaire is received properly
        SQSAccess sqsAccess = SQSAccess.getInstance();
        SQSListener listener = new SQSListenerTest();
        sqsAccess.startListening(listener);
        System.out.println("Waiting for 7 seconds to let the consumer process the message");
        Thread.sleep(7000);
        if (sb.length()==0) {
            fail("Timeout: waiting for 4 seconds to read from queue");
        } else {
            JSONObject jsonObjIn = new JSONObject(jsonIn);
            JSONObject jsonObjOut = new JSONObject(sb.toString());
            JSONObject qIn = jsonObjIn.getJSONObject("group");
            JSONObject qOut = jsonObjOut.getJSONObject("group");
            assertEquals(qIn.toString(), qOut.toString());
        }
    }

    private String readTextFile(String fileName) throws Exception {
        InputStream in = ActionsIT.class.getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuilder sBuffer = new StringBuilder();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                sBuffer.append(line).append('\n');
            }
        } catch(Exception e) {
            e.printStackTrace();

        } finally {
            in.close();
        }
        return sBuffer.toString();
    }

    // Class that extends SQSLIstener and overrides the saveMessage method that is called when the message has
    // been decrypted from the queue
    public static class SQSListenerTest extends SQSListener {
        @Override
        protected void saveMessage(String messageString) {
            ActionsIT.sb.append(messageString);
        }

        @Override
        protected void saveRawMessage(String uuid, String message, String key) throws C3PROException {}
    }
}
