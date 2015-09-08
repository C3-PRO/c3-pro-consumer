package org.bch.c3pro.consumer.external;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.persistence.EntityManager;
import javax.transaction.UserTransaction;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.ResourceMetadataKeyEnum;
import ca.uhn.fhir.model.dstu2.resource.Contract;
import ca.uhn.fhir.model.dstu2.resource.Observation;
import ca.uhn.fhir.model.dstu2.resource.Patient;
import ca.uhn.fhir.model.dstu2.resource.QuestionnaireAnswers;
import ca.uhn.fhir.model.primitive.IdDt;
import org.apache.commons.codec.binary.Base64;
import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.data.PatientMapAccess;
import org.bch.c3pro.consumer.exception.C3PROException;
import org.bch.c3pro.consumer.model.PatientMap;
import org.bch.c3pro.consumer.model.Resource;
import org.bch.c3pro.consumer.util.Response;
import org.json.JSONObject;

import org.json.JSONException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

@Default
public class SQSListener implements MessageListener, Serializable {
	private Map<String, PrivateKey> privateKeyMap = new HashMap<>();

    private final static String FHIR_OBSERVATION = "Observation";
    private final static String FHIR_QA = "QuestionnaireAnswers";
    private final static String FHIR_CON = "Contract";
    private final static String FHIR_PATIENT = "Patient";
    private final static String FHIR_RESOURCE_TYPE = "resourceType";

    private EntityManager em = null;

    protected FhirContext ctx = FhirContext.forDstu2();

	Logger log = LoggerFactory.getLogger(SQSListener.class);

    @javax.annotation.Resource
    UserTransaction tx;

    @Inject
    protected I2B2FHIRCell fhirCell;

    @Inject
    protected PatientMapAccess patientMapAccess;

	@Override
	public void onMessage(Message messageWrapper) {
		try {
			TextMessage txtMessage = ( TextMessage ) messageWrapper;

	        // Get the body message 
			byte [] messageEnc = Base64.decodeBase64(txtMessage.getText());

			// Get the symetric key as metadata
			String symKeyBase64 = messageWrapper.getStringProperty(AppConfig.getProp(AppConfig.SECURITY_METADATAKEY));
            String publicKeyId = messageWrapper.getStringProperty(AppConfig.getProp(AppConfig.SECURITY_METADATAKEY_ID));


            publicKeyId = publicKeyId.trim();
			// We decrypt the secret key of the message using the private key
			byte [] secretKeyEnc = Base64.decodeBase64(symKeyBase64);
			byte [] secretKey = decryptSecretKey(secretKeyEnc, publicKeyId);

			// We decrypt the message using the secret key
			byte [] message = decryptMessage(messageEnc, secretKey);
			String messageString = new String(message, AppConfig.UTF);
            String processed = null;
            try {
                saveMessage(messageString);
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                processed = e.getMessage();
            }
            saveRawMessage(null, txtMessage.getText(), symKeyBase64, publicKeyId, processed);
            if (processed!=null) {
                log.warn("Raw message stored but an error occurred while processing the message. Please, check logs");
            }
            // We send acknowledge notification
			messageWrapper.acknowledge();
		} catch (JMSException e) {
            e.printStackTrace();
			log.error("JMSException Error processing message from SQS:" + e.getMessage());
		} catch (C3PROException e) {
            e.printStackTrace();
			log.error("C3PROException Error processing message from SQS:" + e.getMessage());
		} catch (IOException e) {
            e.printStackTrace();
			log.error("IOException error processing message from SQS:" + e.getMessage());
		} catch (InvalidKeySpecException e) {
            e.printStackTrace();
			log.error("InvalidKeySpecException error processing message from SQS:" + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
			log.error("NoSuchAlgorithmException error processing message from SQS:" + e.getMessage());
		} catch (BadPaddingException e) {
            e.printStackTrace();
			log.error("BadPaddingException error processing message from SQS:" + e.getMessage());
		} catch (GeneralSecurityException e) {
            e.printStackTrace();
			log.error("GeneralSecurityException error processing message from SQS:" + e.getMessage());
		}
	}

    protected void saveMessage(String messageString) throws C3PROException {
        System.out.println(messageString);
        String resourceType = this.getResourceType(messageString);
        Response resp = null;

        try {
            log.info("Saving " + resourceType + " fhir resource");
            switch(resourceType) {
                case FHIR_QA:
                    messageString = replaceSubjectIdQA(messageString);
                    resp = fhirCell.postQuestionnaireAnswers(messageString);
                    break;
                case FHIR_CON:
                    storeContract(messageString);
                    //resp = fhirCell.postContract(messageString);
                    break;
                case FHIR_OBSERVATION:
                    messageString = replaceSubjectIdObs(messageString);
                    resp = fhirCell.postObservation(messageString);
                    break;
                case FHIR_PATIENT:
                    Patient patient = replaceSubjectIdPatient(messageString);
                    messageString = ctx.newJsonParser().encodeResourceToString(patient);
                    resp = fhirCell.putPatient(messageString, patient.getId().getIdPart());
                    break;
                default:
                    throw new C3PROException("FHIR Resource " + resourceType + " not supported");
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new C3PROException(e.getMessage(), e);
        }
        if (resp!=null) {
            int code = resp.getResponseCode();
            log.info(""+code);
            if (code>=400) throw new C3PROException("Error storing data into i2b2");
        }
	}

    protected String replaceSubjectIdQA(String message) throws C3PROException {
        QuestionnaireAnswers qa = (QuestionnaireAnswers) ctx.newJsonParser().parseResource(message);
        String subject = qa.getSubject().getReference().getIdPart();
        if (subject == null) {
            log.warn("Subject is null in a QuestionnaireAnswers Resource");
            return message;
        }
        String i2b2Subject = findI2B2Subject(subject);

        qa.getSubject().setReference("Patient/"+i2b2Subject);
        return ctx.newJsonParser().encodeResourceToString(qa);

    }

    protected Patient replaceSubjectIdPatient(String message) throws C3PROException {
        Patient patient = (Patient) ctx.newJsonParser().parseResource(message);
        IdDt iddt = patient.getId();
        String subject = iddt.getIdPart();
        String i2b2Subject = findI2B2Subject(subject);
        IdDt newId = new IdDt("Patient", i2b2Subject , "1");
        patient.setId(newId);
        return patient;
    }

    protected String replaceSubjectIdObs(String message) throws C3PROException {
        Observation obs = (Observation) ctx.newJsonParser().parseResource(message);
        String subject = obs.getSubject().getReference().getIdPart();
        if (subject == null) {
            log.warn("Subject is null in a Observation Resource");
            return message;
        }
        String i2b2Subject = findI2B2Subject(subject);

        obs.getSubject().setReference("Patient/"+i2b2Subject);
        return ctx.newJsonParser().encodeResourceToString(obs);

    }

    protected String findI2B2Subject(String signature) throws C3PROException {
        try {
            List<PatientMap> maps = this.patientMapAccess.findBySignature(signature);
            if (maps.size()==0) {
                log.warn("No mapping correspondance for signature " + signature);
                return null;
            } else if (maps.size()>1) {
                log.warn("More than one mapping found for signature " + signature + ". Getting the first one");
            }
            return maps.get(0).getSubjectId();

        } catch (Exception e) {
            e.printStackTrace();
            throw new C3PROException(e.getMessage(), e);
        }
    }

    protected void storeContract(String contractJson) throws C3PROException {
        String id = null;
        String version = null;
        Date startDate = null;
        String system = null;
        String code = null;
        String display = null;
        String signature = null;

        try {
            Contract contract = (Contract) ctx.newJsonParser().parseResource(contractJson);
            id = contract.getId().getIdPart();
            //version = contract.getResourceMetadata().get(ResourceMetadataKeyEnum.VERSION).toString();
            //version = (String) contract.getResourceMetadata().get("versionId");
            version = "0";
            startDate = contract.getApplies().getStart();
            system = contract.getSigner().
                    get(0).
                    getType().
                    get(0).getSystem();

            code = contract.getSigner().
                    get(0).
                    getType().
                    get(0).getCode();

            display = contract.getSigner().
                    get(0).
                    getType().
                    get(0).getDisplay();

            signature = contract.getSigner().
                    get(0).getSignature();


        } catch (Exception e) {
            e.printStackTrace();
            throw new C3PROException(e.getMessage(), e);
        }
        System.out.println("id:" + id);
        System.out.println("version:" + version);
        System.out.println("startDate:" + startDate.toString());
        System.out.println("system:" + system);
        System.out.println("code:" + code);
        System.out.println("display:" + display);
        System.out.println("signature:" + signature);

        PatientMap patientMap = new PatientMap();
        patientMap.setId(id);
        patientMap.setVersionId(version);
        patientMap.setCode(code);
        patientMap.setDisplay(display);
        patientMap.setSignature(signature);
        patientMap.setStartDate(startDate);
        patientMap.setSystem(system);

        // finally, we generate the subject id that will
        patientMap.setSubjectId(UUID.randomUUID().toString());
        try {
            tx.begin();
            em.persist(patientMap);
            em.flush();
            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                tx.rollback();
            } catch (Exception ee) {
                e.printStackTrace();
            }
            throw new C3PROException("Error storing consent data: " + e.getMessage(), e);
        }

    }

    protected String getResourceType(String msg) throws C3PROException {
        try {
            JSONObject json = new JSONObject(msg);
            String resourceType = json.getString(FHIR_RESOURCE_TYPE);
            return resourceType;
        } catch (JSONException e) {
            e.printStackTrace();
            throw new C3PROException(e.getMessage(), e);

        }

    }

    protected void saveRawMessage(String uuid, String message, String key, String keyId, String processed) throws C3PROException {
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
        res.setKeyId(keyId);
        res.setProcessed(processed);
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
        this.patientMapAccess.setEntityManager(em);
    }

	public static byte [] decryptMessage(byte [] messageEnc, byte[] secretKeyBytes) throws GeneralSecurityException,
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
	public byte [] decryptSecretKey(byte [] symKeyEnc, String keyId) throws C3PROException, IOException, NoSuchAlgorithmException,
            InvalidKeySpecException {
		PrivateKey privateKey = loadPrivateKey(keyId);
		Cipher cipher = null;
        byte [] out = null;
        try {
            cipher = Cipher.getInstance(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_ALG));
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            out = cipher.doFinal(symKeyEnc);
        } catch (Exception e) {
            e.printStackTrace();
            throw new C3PROException(e.getMessage(), e);
        }

        return out;
	}
	
	public PrivateKey loadPrivateKey(String keyId) throws C3PROException, IOException, NoSuchAlgorithmException,
            InvalidKeySpecException {
		// Get the private rsa key to decrypt the symetric key
        if (!this.privateKeyMap.containsKey(keyId)) {
            String fullPath = AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_BASEPATH);
            fullPath = fullPath + keyId + "/" + AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_FILENAME);
            Path path = Paths.get(fullPath);
            byte[] privateKeyBin = Files.readAllBytes(path);
            PKCS8EncodedKeySpec privateSpec = new PKCS8EncodedKeySpec(privateKeyBin);
            KeyFactory keyFactory = KeyFactory.getInstance(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_BASEALG));
            this.privateKeyMap.put(keyId,keyFactory.generatePrivate(privateSpec));
        }
		return this.privateKeyMap.get(keyId);
	}
}
