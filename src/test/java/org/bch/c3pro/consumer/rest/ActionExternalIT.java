package org.bch.c3pro.consumer.rest;

import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.external.SQSAccess;
import org.bch.c3pro.consumer.external.SQSListener;
import org.bch.c3pro.consumer.util.HttpRequest;
import org.bch.c3pro.consumer.util.Response;
import org.json.JSONObject;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by ipinyol on 8/31/15.
 */
public class ActionExternalIT {
    // It requires c3pro-server running serving to the same queue as c3pro-consumer,
    // an empty queue and the public key of the c3pro-consumer installed in the c3pro-server
    public static StringBuffer sb = new StringBuffer();

    public static StringBuffer uuid = new StringBuffer();
    public static StringBuffer json = new StringBuffer();
    public static StringBuffer key = new StringBuffer();


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
     * NOTE: It requires the consumer to be running externally!
     * @throws Exception
     */
    @Test
    public void endToEndTestuestionnaireAnswers_IT() throws Exception {
        HttpRequest http = new HttpRequest();
        String jsonIn =readTextFile("mainQuestionnaireAnswer.json");
        String contentTypeHeader = "application/json";

        String urlAuth = AppConfig.getProp(AppConfig.C3PRO_SERVER_TRANS) + "://" +
                AppConfig.getProp(AppConfig.C3PRO_SERVER_HOST) + ":" +
                AppConfig.getProp(AppConfig.C3PRO_SERVER_PORT) +
                "/c3pro/oauth?grant_type=client_credentials";

        String url = AppConfig.getProp(AppConfig.C3PRO_SERVER_TRANS) + "://" +
                AppConfig.getProp(AppConfig.C3PRO_SERVER_HOST) + ":" +
                AppConfig.getProp(AppConfig.C3PRO_SERVER_PORT) +
                "/c3pro/fhir/QuestionnaireAnswers";

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
        //SQSAccess sqsAccess = SQSAccess.getInstance();
        SQSListener listener = new SQSListener();


        //sqsAccess.startListening(listener);
        System.out.println("Waiting for 7 seconds to let the consumer process the message");
        Thread.sleep(7000);

        // We read the DB to see that the info coincides with what it has been sent
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

        //JSONObject jsonObjIn = new JSONObject(jsonIn);
        //JSONObject jsonObjOut = new JSONObject(sb.toString());
        //JSONObject qIn = jsonObjIn.getJSONObject("group");
        //JSONObject qOut = jsonObjOut.getJSONObject("group");
        //assertEquals(qIn.toString(), qOut.toString());
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

}
