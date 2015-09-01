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

    /**
     * Checks that c3pro db is accessible
     * @throws Exception
     */
    @Test
    public void checkDB_C3PRO_IT() throws Exception {
        String infoDB = AppConfig.getAuthCredentials(AppConfig.C3PRO_CONSUMER_DATASOURCE);
        String [] infoDBParse = infoDB.split(",");
        Class.forName("oracle.jdbc.driver.OracleDriver");
        Connection conn = DriverManager.getConnection(infoDBParse[0], infoDBParse[1], infoDBParse[2]);
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("Select count(*) from resource_table");
        if (!rs.next()) fail();
        rs.close();
        rs = stmt.executeQuery("Select count(*) from patient_map");
        if (!rs.next()) fail();
        rs.close();
        stmt.close();
        conn.close();
    }

    /**
     * Checks that fhir cell i2b2 is up
     * @throws Exception
     */
    @Test
    public void checkDB_FHIR_CELL_I2B2_IT() throws Exception {
        HttpRequest http = new HttpRequest();

        String url = AppConfig.getProp(AppConfig.PROTOCOL_FHIR_I2B2) + "://" +
                AppConfig.getProp(AppConfig.HOST_FHIR_I2B2) + ":" +
                AppConfig.getProp(AppConfig.PORT_FHIR_I2B2) +
                "/fhir-i2b2/";

        Response resp = http.doPostGeneric(url,null,null,null,"GET");
        assertTrue(resp.getResponseCode() >= 200 && resp.getResponseCode() <= 204);
    }

}
