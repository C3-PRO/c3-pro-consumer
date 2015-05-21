package org.bch.c3pro.consumer.external;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.inject.Default;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;

import org.apache.commons.codec.binary.Base64;
import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.exception.C3PROException;
import org.bch.c3pro.consumer.model.Resource;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Default
public class SQSListener implements MessageListener, Serializable {
	private PrivateKey privateKey = null;
	private EntityManager em = null;

	Logger log = LoggerFactory.getLogger(SQSListener.class);

    @javax.annotation.Resource
    UserTransaction tx;

	@Override
	public void onMessage(Message messageWrapper) {
		try {
			TextMessage txtMessage = ( TextMessage ) messageWrapper;

	        // Get the body message 
			byte [] messageEnc = Base64.decodeBase64(txtMessage.getText());

			// Get the symetric key as metadata
			String symKeyBase64 = messageWrapper.getStringProperty(AppConfig.getProp(AppConfig.SECURITY_METADATAKEY));
            saveRawMessage(null, txtMessage.getText(), symKeyBase64 );

			// We decrypt the secret key of the message using the private key
			byte [] secretKeyEnc = Base64.decodeBase64(symKeyBase64);
			byte [] secretKey = decryptSecretKey(secretKeyEnc);

			// We decrypt the message using the secret key
			byte [] message = decryptMessage(messageEnc, secretKey);
			String messageString = new String(message, AppConfig.UTF);
			saveMessage(messageString);

            // We send acknowledge notification
			messageWrapper.acknowledge();
		} catch (JMSException e) {
			log.error("JMSException Error processing message from SQS:" + e.getMessage());
		} catch (C3PROException e) {
			log.error("C3PROException Error processing message from SQS:" + e.getMessage());
		} catch (IOException e) {
			log.error("IOException error processing message from SQS:" + e.getMessage());
		} catch (InvalidKeySpecException e) {
			log.error("InvalidKeySpecException error processing message from SQS:" + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			log.error("NoSuchAlgorithmException error processing message from SQS:" + e.getMessage());
		} catch (BadPaddingException e) {
			log.error("BadPaddingException error processing message from SQS:" + e.getMessage());
		} catch (GeneralSecurityException e) {
			log.error("GeneralSecurityException error processing message from SQS:" + e.getMessage());
		}
	}
	

    protected void saveMessage(String messageString) {
        log.info("Resource Processed");
	}

    protected void saveRawMessage(String uuid, String message, String key) throws C3PROException {
        String newUUID = uuid;
        if (this.em == null) {
            log.warn("EntityManager is null. The raw message will not be stored.");
        }
        if (uuid == null) {
            newUUID = UUID.randomUUID().toString();
        }
        Resource res = new Resource();
        res.setUUID(newUUID);
        res.setJson(message);
        res.setKey(key);
        try {
            tx.begin();
            em.persist(res);
            em.flush();
            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                tx.rollback();
            } catch (Exception ee) {
                e.printStackTrace();
            }
            throw new C3PROException("Error storing raw data: " + e.getMessage(), e);
        }
    }

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

	private static byte [] decryptMessage(byte [] messageEnc, byte[] secretKeyBytes) throws GeneralSecurityException,
            C3PROException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes,
                AppConfig.getProp(AppConfig.SECURITY_SECRETKEY_BASEALG));
        int size = Integer.parseInt(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_SIZE));
        SecretKey secretKey = secretKeySpec;
		Cipher cipher = null;
		cipher = Cipher.getInstance(AppConfig.getProp(AppConfig.SECURITY_SECRETKEY_ALG));
        cipher.init(Cipher.DECRYPT_MODE, secretKey,  new IvParameterSpec(new byte[size]));
        return cipher.doFinal(messageEnc);
        
	}
	private byte [] decryptSecretKey(byte [] symKeyEnc) throws C3PROException, IOException, NoSuchAlgorithmException,
            InvalidKeySpecException {
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

        return out;
	}
	
	private void loadPrivateKey() throws C3PROException, IOException, NoSuchAlgorithmException,
            InvalidKeySpecException {
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
