package org.bch.c3pro.consumer.external;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.codec.binary.Base64;
import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.exception.C3PROException;

public class SQSListener implements MessageListener{
	Logger log = Logger.getAnonymousLogger();
	@Override
	public void onMessage(Message message) {
		try {
			TextMessage txtMessage = ( TextMessage ) message;
	        System.out.println( "\t" + txtMessage.getText() );
	        // Get the body message 
			byte [] messageEnc = Base64.decodeBase64(txtMessage.getText());
			// Get the symetric key as metadata
			String symKeyBase64 = message.getStringProperty(AppConfig.getProp(AppConfig.SECURITY_METADATAKEY));
			byte [] symKeyEnc = Base64.decodeBase64(symKeyBase64);
			
			// Get the private rsa key to decrypt the symetric key
			Path path = Paths.get(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_FILE));
			byte[] privateKeyBin = Files.readAllBytes(path);
			X509EncodedKeySpec privateSpec = new X509EncodedKeySpec(privateKeyBin);
            KeyFactory keyFactory = KeyFactory.getInstance(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_BASEALG));
            PrivateKey publicKey = keyFactory.generatePrivate(privateSpec);

			
			message.acknowledge();
		} catch (JMSException e) {
			log.severe("Error processing message from SQS:" + e.getMessage());
		} catch (C3PROException e) {
			log.severe("error processing message from SQS:" + e.getMessage());
		} catch (IOException e) {
			log.severe("error processing message from SQS:" + e.getMessage());
		} catch (InvalidKeySpecException e) {
			log.severe("error processing message from SQS:" + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			log.severe("error processing message from SQS:" + e.getMessage());
		}
	}
	

}
