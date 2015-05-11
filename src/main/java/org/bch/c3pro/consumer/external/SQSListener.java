package org.bch.c3pro.consumer.external;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.codec.binary.Base64;
import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.exception.C3PROException;

public class SQSListener implements MessageListener{
	private PrivateKey privateKey = null;
	
	Logger log = Logger.getAnonymousLogger();
	@Override
	public void onMessage(Message messageWrapper) {
		try {
			TextMessage txtMessage = ( TextMessage ) messageWrapper;
            log.info("1111111111111");
	        //System.out.println( "\t" + txtMessage.getText() );
	        // Get the body message 
			byte [] messageEnc = Base64.decodeBase64(txtMessage.getText());
            log.info("2222222222222");
			// Get the symetric key as metadata
			String symKeyBase64 = messageWrapper.getStringProperty(AppConfig.getProp(AppConfig.SECURITY_METADATAKEY));
            log.info("33333333333333");
			// We decrypt the secret key of the message using the private key
			byte [] secretKeyEnc = Base64.decodeBase64(symKeyBase64);
            log.info("44444444444444");
			byte [] secretKey = decryptSecretKey(secretKeyEnc);
            log.info("55555555555555");
			// We decrypt the message using the secret key
			byte [] message = decryptMessage(messageEnc, secretKey);
            log.info("66666666666666");
			String messageString = new String(message, AppConfig.UTF);
            log.info("77777777777777");
			saveMessage(messageString);
            log.info("88888888888888");
			messageWrapper.acknowledge();
			
		} catch (JMSException e) {
			log.severe("JMSException Error processing message from SQS:" + e.getMessage());
		} catch (C3PROException e) {
			log.severe("C3PROException Error processing message from SQS:" + e.getMessage());
		} catch (IOException e) {
			log.severe("IOException error processing message from SQS:" + e.getMessage());
		} catch (InvalidKeySpecException e) {
			log.severe("InvalidKeySpecException error processing message from SQS:" + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			log.severe("NoSuchAlgorithmException error processing message from SQS:" + e.getMessage());
		} catch (BadPaddingException e) {
			log.severe("BadPaddingException error processing message from SQS:" + e.getMessage());
		} catch (GeneralSecurityException e) {
			log.severe("GeneralSecurityException error processing message from SQS:" + e.getMessage());
		}
	}
	
	private void saveMessage(String messageString) {
		System.out.println(messageString);
	}

	private byte [] decryptMessage(byte [] messageEnc, byte[] secretKeyBytes) throws GeneralSecurityException, BadPaddingException, C3PROException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, AppConfig.getProp(AppConfig.SECURITY_SECRETKEY_BASEALG));
		//KeyFactory kf = KeyFactory.getInstance(AppConfig.getProp(AppConfig.SECURITY_SECRETKEY_BASEALG));
		//PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(secretKeyBytes);
		//PrivateKey secretKey = kf.generatePrivate(privateSpec);
        int size = Integer.parseInt(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_SIZE));
        SecretKey secretKey = secretKeySpec;
		Cipher cipher = null;
		cipher = Cipher.getInstance(AppConfig.getProp(AppConfig.SECURITY_SECRETKEY_ALG));
        cipher.init(Cipher.DECRYPT_MODE, secretKey,  new IvParameterSpec(new byte[size]));
        return cipher.doFinal(messageEnc);
        
	}
	private byte [] decryptSecretKey(byte [] symKeyEnc) throws C3PROException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		loadPrivateKey();
		Cipher cipher = null;
        byte [] out = null;
        try {
            cipher = Cipher.getInstance(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_ALG));
            cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
            out = cipher.doFinal(symKeyEnc);
        } catch (Exception e) {
            throw new C3PROException(e.getMessage(), e);
        }
        //byte [] outReal = new byte[16];
        //System.arraycopy(out,0,outReal,0,16);
        return out;
	}
	
	private void loadPrivateKey() throws C3PROException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		// Get the private rsa key to decrypt the symetric key
		if (this.privateKey == null) {
			Path path = Paths.get(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_FILE));
			byte[] privateKeyBin = Files.readAllBytes(path);
			PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBin);
	        KeyFactory keyFactory = KeyFactory.getInstance(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_BASEALG));
	        this.privateKey = keyFactory.generatePrivate(privateSpec);
		}
	}
}
