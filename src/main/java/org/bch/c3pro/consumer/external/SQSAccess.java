package org.bch.c3pro.consumer.external;

import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import org.apache.commons.codec.binary.Base64;
import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.exception.C3PROException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.logging.Logger;

/**
 * Created by CH176656 on 5/1/2015.
 */
public class SQSAccess {

	private SQSConnection connection;
    Logger log = Logger.getAnonymousLogger();

    public void startListening() throws JMSException, C3PROException {
    	Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
    	MessageConsumer consumer = session.createConsumer( session.createQueue(AppConfig.getProp(AppConfig.AWS_SQS_URL)));
    	SQSListener listener = new SQSListener();
        consumer.setMessageListener( listener);
        connection.start();
        
    }
    
    private void setUpConnection() throws C3PROException, JMSException {
    	Region region = Region.getRegion(Regions.fromName(AppConfig.getProp(AppConfig.AWS_SQS_REGION)));
    	AWSCredentials credentials = null;
        try {
            System.setProperty("aws.profile", AppConfig.getProp(AppConfig.AWS_SQS_PROFILE));
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new C3PROException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that the credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in valid format.",
                    e);
        }
    	SQSConnectionFactory connectionFactory = 
                SQSConnectionFactory.builder()
                    .withRegion(region)
                    .withAWSCredentialsProvider((AWSCredentialsProvider) credentials)
                    .build();
    	SQSConnection connection = connectionFactory.createConnection();
    	
    }
}
