package org.bch.c3pro.consumer.rest;


import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.exception.C3PROException;
import org.bch.c3pro.consumer.external.SQSAccess;
import org.bch.c3pro.consumer.external.SQSListener;
import org.bch.c3pro.consumer.util.HttpRequest;
import org.bch.c3pro.consumer.util.Response;
import org.json.JSONObject;
import javax.sql.DataSource;
import org.junit.Ignore;
import org.junit.Test;

import javax.naming.InitialContext;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

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

    public static StringBuffer uuid = new StringBuffer();
    public static StringBuffer json = new StringBuffer();
    public static StringBuffer key = new StringBuffer();


    @Ignore
    public void generalTestIT() throws Exception {
        HttpRequest http = new HttpRequest();
        String jsonIn =readTextFile("q1.json");
        String conentTypeHeader = "application/json";
        String url = "http://ec2-52-11-82-72.us-west-2.compute.amazonaws.com:8080/c3pro/fhir/Questionnaire";
        Response resp = http.doPostGeneric(url,jsonIn,null,conentTypeHeader);
        assertEquals(201, resp.getResponseCode());

        SQSAccess sqsAccess = SQSAccess.getInstance();
        SQSListener listener = new SQSListenerTest(false);
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

    /**
     * Validates the following points:
     * - ClientId/Secret key are correctly set
     * - Oauth protocol provides a valid bearer token for ClientId
     * - Doing POST with the given token works
     * - We can read from the SQS
     * - We can decrypt the message from the SQS using the private key of the system and recuperate the original
     * resource
     * @throws Exception
     */
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
        SQSListener listener = new SQSListenerTest(false);
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

    /**
     * Validates similar points and include the storage access
     * - ClientId/Secret key are correctly set
     * - Oauth protocol provides a valid bearer token for ClientId
     * - Doing POST with the given token works
     * - We can read from the SQS
     * - We can store the encrypted message in the DB. So, that DB connection works
     * - We can read the from DB the same exact encrypted info that we received from the Queue
     *
     * NOTE: It must be executed with an empty table and an empty Queue
     * @throws Exception
     */
    @Ignore("Broken")
    public void endToEndTest_IT() throws Exception {
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
        SQSListener listener = new SQSListenerTest(true);
        sqsAccess.startListening(listener);
        System.out.println("Waiting for 7 seconds to let the consumer process the message");
        Thread.sleep(7000);
        if (sb.length()==0) {
            fail("Timeout: waiting for 4 seconds to read from queue");
        } else {
            String infoDB = AppConfig.getAuthCredentials(AppConfig.C3PRO_CONSUMER_DATASOURCE);
            String [] infoDBParse = infoDB.split(",");
            Class.forName("oracle.jdbc.driver.OracleDriver");
            Connection conn = DriverManager.getConnection(infoDBParse[0], infoDBParse[1], infoDBParse[2]);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("Select json, key from resource_table");
            if (!rs.next()) fail();
            String jsonDB = rs.getString("json");
            String keyDB = rs.getString("key");
            String uuidDB = rs.getString("uuid");
            assertEquals(ActionsIT.json.toString(), jsonDB);
            assertEquals(ActionsIT.key.toString(), keyDB);
            assertEquals(ActionsIT.uuid.toString(), uuidDB);
            rs.deleteRow();
            conn.close();

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
        private boolean withDB = false;
        public SQSListenerTest(boolean withDB) {
            this.withDB=withDB;
        }
        @Override
        protected void saveMessage(String messageString) {
            ActionsIT.sb.append(messageString);
        }

        @Override
        protected void saveRawMessage(String uuid, String message, String key, String keyId) throws C3PROException {
            ActionsIT.json.append(message);
            ActionsIT.uuid.append(uuid);
            ActionsIT.key.append(key);
            if (this.withDB) {
                super.saveRawMessage(uuid, message, key, null);
            }
        }
    }
}
