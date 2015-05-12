package org.bch.c3pro.consumer.config;

import org.apache.commons.io.IOUtils;
import org.bch.c3pro.consumer.exception.C3PROException;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;

/**
 * Configuration file
 * Created by CH176656 on 3/20/2015.
 */
public class AppConfig {

    public static String CONFIG_PROPERTIES_FILE=    "config.properties";

    public static String AWS_SQS_URL =              "app.aws.sqs.url";
    public static String AWS_SQS_PROFILE =          "app.aws.sqs.profile";
    public static String AWS_SQS_REGION =           "app.aws.sqs.region";
    public static String AWS_SQS_NAME = 			"app.aws.sqs.name";


    // The key posted in the metadata part of the message to SQS. The value will contain the ncrypted symetric key
    // to decrypt the message
    public static String SECURITY_METADATAKEY =         "app.security.metadatakey";

    public static String SECURITY_PRIVATEKEY_ALG =      "app.security.privatekey.algorithm";
    public static String SECURITY_PRIVATEKEY_BASEALG =   "app.security.privatekey.basealgorithm";
    public static String SECURITY_PRIVATEKEY_FILE = 	"app.security.privatekey.file";
    public static String SECURITY_SECRETKEY_BASEALG = 	"app.security.secretkey.basealgorithm";
    public static String SECURITY_SECRETKEY_ALG = 		"app.security.secretkey.algorithm";
    public static String SECURITY_PRIVATEKEY_SIZE =     "app.security.secretkey.size";

    public static String UTF = "UTF-8";
    // Whether we encrypt the messages when sent to the queue; yes - no
    public static String SECURITY_ENCRYPTION_ENABLED = "app.security.encryption.enabled";

    public static int HTTP_TRANSPORT_BUFFER_SIZE = 500;
    private static Properties prop = new Properties();
    /**
     * Upload the configuration from config.properties files
     */
    private static void uploadConfiguration() throws C3PROException {
        InputStream input = null;

        try {
            String filename = CONFIG_PROPERTIES_FILE;
            input = AppConfig.class.getResourceAsStream(filename);
            if (input == null) {
                throw new C3PROException("No " + filename + " found!");
            }
            prop.load(input);

        } catch (IOException ex) {
            throw new C3PROException("", ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    throw new C3PROException("", e);
                }
            }
        }
    }


    public static String getProp(String key) throws C3PROException {
        if (prop.isEmpty()) {
            uploadConfiguration();
        }
        return prop.getProperty(key);
    }

    public static String getAuthCredentials(String key) throws IOException, C3PROException {
        String path = getProp(key);
        String finalPath = path;
        int i = path.indexOf("[");
        int j = path.indexOf("]");
        if (i<0 && j>=0) throw new C3PROException("Missing [ in " + key);
        if (i>=0) {
            if (j<0) throw new C3PROException("Missing ] in " + key);
            String var = path.substring(i+1,j);
            String aux = System.getenv(var);
            if (aux == null) aux = "";
            finalPath = path.replaceAll("\\[" + var + "\\]", aux);
        }
        FileInputStream inputStream = new FileInputStream(finalPath);
        String out=null;
        try {
            out = IOUtils.toString(inputStream).trim();
        } finally {
            inputStream.close();
        }
        return out;
    }
}
