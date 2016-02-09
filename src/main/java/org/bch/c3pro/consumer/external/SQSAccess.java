package org.bch.c3pro.consumer.external;

import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.exception.C3PROException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

/**
 * Class that manages the access to the Amazon SQS.
 * It's a singleton. Th method {@link SQSAccess#getInstance()} should be the only way to ger an instance of the class
 * @author CHIP-IHL
 */
public class SQSAccess {

    private static SQSAccess sqsAccessSingleton = null;

	private SQSConnection connection;
    Log log = LogFactory.getLog(SQSAccess.class);

    // just to defeat instantiation
    protected SQSAccess() {}

    /**
     * Returns the instance of a {@link SQSAccess}
     * @return the object
     */
    public static SQSAccess getInstance() {
        if (sqsAccessSingleton == null) {
            SQSAccess.sqsAccessSingleton = new SQSAccess();
        }
        return SQSAccess.sqsAccessSingleton;
    }

    /**
     * Starts the listener of the queue
     * @throws JMSException In case of SQS problems
     * @throws C3PROException In case the properties that describe the sqs access are missing
     */
    public void startListening() throws JMSException, C3PROException {
        if (this.connection==null) {
            setUpConnection();
            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(
                    session.createQueue(AppConfig.getProp(AppConfig.AWS_SQS_NAME)));
            SQSListener listener = new SQSListener();
            consumer.setMessageListener(listener);
            connection.start();
        }
    }

    /**
     * Starts the listener of the queue passed by parameter
     * @param listener The listener
     * @throws JMSException In case of SQS problems
     * @throws C3PROException In case the properties that describe the sqs access are missing

     */
    public void startListening(SQSListener listener) throws JMSException, C3PROException {
        if (this.connection==null) {
            setUpConnection();
            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(session.createQueue(
                    AppConfig.getProp(AppConfig.AWS_SQS_NAME)));
            consumer.setMessageListener(listener);
            connection.start();
        }
    }

    public void stopListening() throws JMSException {
        if (this.connection!=null) {
            this.connection.close();
            this.connection = null;
        }
    }

    private void setUpConnection() throws C3PROException, JMSException {
    	Region region = Region.getRegion(Regions.fromName(AppConfig.getProp(AppConfig.AWS_SQS_REGION)));
    	AWSCredentialsProvider credentials = null;
        try {
            System.setProperty("aws.profile", AppConfig.getProp(AppConfig.AWS_SQS_PROFILE));
            credentials = new ProfileCredentialsProvider();
            credentials.getCredentials();
        } catch (Exception e) {
            throw new C3PROException(
                    "Cannot load the credentials from the credential profiles file. " +
                            "Please make sure that the credentials file is at the correct " +
                            "location (~/.aws/credentials), and is in a valid format.",
                    e);
        }
    	SQSConnectionFactory connectionFactory = 
                SQSConnectionFactory.builder()
                    .withRegion(region)
                    .withAWSCredentialsProvider(credentials)
                    .build();
    	this.connection = connectionFactory.createConnection();
    	
    }
}
