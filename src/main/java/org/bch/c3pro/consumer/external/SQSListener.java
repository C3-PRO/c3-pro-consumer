package org.bch.c3pro.consumer.external;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.data.PatientMapAccess;
import org.bch.c3pro.consumer.data.ResourceAccess;
import org.bch.c3pro.consumer.exception.C3PROException;
import org.bch.c3pro.consumer.model.PatientMap;
import org.bch.c3pro.consumer.model.Resource;
import org.bch.c3pro.consumer.util.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import org.json.JSONException;

/**
 * Class that implements a listener of the Amazon SQS
 * @author CHIP-IHL
 */
@Default
public class SQSListener implements MessageListener, Serializable {
	private Map<String, PrivateKey> privateKeyMap = new HashMap<>();

    private final static String FHIR_PATIENT = "Patient";
    private final static String FHIR_CONTRACT = "Contract";
    private final static String FHIR_RESOURCE_TYPE = "resourceType";

    private final static String FHIR_CONTRACT_SIGNER = "signer";
    private final static String FHIR_CONTRACT_APPLIES = "applies";
    private final static String FHIR_CONTRACT_APPLIES_START = "start";
    private final static String FHIR_CONTRACT_SIGNER_SIGNATURE= "signature";
    private final static String FHIR_CONTRACT_SIGNER_TYPE = "type";
    private final static String FHIR_CONTRACT_SIGNER_TYPE_CODE = "code";
    private final static String FHIR_CONTRACT_SIGNER_TYPE_SYSTEM = "system";
    private final static String FHIR_CONTRACT_SIGNER_TYPE_DISPLAY = "display";

    private final static String FHIR_RESOURCE_ID = "id";
    private final static String FHIR_RESOURCE_SUBJECT = "subject";
    private final static String FHIR_RESOURCE_REFERENCE = "reference";

    private final static String FHIR_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SS";

    private EntityManager em = null;


    Log log = LogFactory.getLog(SQSListener.class);

    @javax.annotation.Resource
    UserTransaction tx;

    @Inject
    protected I2B2FHIRCell fhirCell;

    @Inject
    protected PatientMapAccess patientMapAccess;

    @Inject
    protected ResourceAccess resourceAccess;

    /**
     * Method that activates when a message is received int eh queue.
     * Stores the raw message in the DB, decrypts the message and sends the fhir resource to the fhir i2b2 cell
     * @param messageWrapper The message
     */
	@Override
	public void onMessage(Message messageWrapper) {
		try {
			TextMessage txtMessage = ( TextMessage ) messageWrapper;

	        // Get the body message 
			byte [] messageEnc = Base64.decodeBase64(txtMessage.getText().getBytes());

			// Get the symetric key as metadata
			String symKeyBase64 = messageWrapper.getStringProperty(AppConfig.getProp(AppConfig.SECURITY_METADATAKEY));
            String publicKeyId = messageWrapper.getStringProperty(AppConfig.getProp(AppConfig.SECURITY_METADATAKEY_ID));

            // Get the FHIR version. If property is not informed, null will be returned
            String version = messageWrapper.getStringProperty(AppConfig.getProp(AppConfig.FHIR_METADATA_VERSION));
            if (version == null) {
                version = AppConfig.getProp(AppConfig.FHIR_VERSION_DEFAULT);
            }

            publicKeyId = publicKeyId.trim();
			// We decrypt the secret key of the message using the private key
			byte [] secretKeyEnc = Base64.decodeBase64(symKeyBase64.getBytes());
			byte [] secretKey = decryptSecretKey(secretKeyEnc, publicKeyId);

			// We decrypt the message using the secret key
			byte [] message = decryptMessage(messageEnc, secretKey);
			String messageString = new String(message, AppConfig.UTF);
            String processed = null;
            try {
                saveMessage(messageString, version);
            } catch (Exception e) {
                e.printStackTrace();
                log.error(e.getMessage());
                processed = e.getMessage();
            }
            saveRawMessage(null, txtMessage.getText(), symKeyBase64, publicKeyId, processed, version);
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

    protected void saveMessage(String messageString, String version) throws C3PROException {
        System.out.println(messageString);
        String resourceType = this.getResourceType(messageString);
        Response resp = null;

        try {
            log.info("Saving " + resourceType + " fhir resource");
            if (resourceType.equals(FHIR_PATIENT)) {
                // If it's a patient resource, we PUT the resource to the fhir cell
                String [] ret = replaceSubjectIdPatient(messageString);
                String resource = ret[0];
                String idResource = ret[1];
                resp = fhirCell.putResource(resource, resourceType, version, idResource);
            } else if (resourceType.equals(FHIR_CONTRACT)) {
                // If we have a contract resource, we store it in the internal db
                storeContract(messageString);
            } else {
                // Otherwise, we POST the resource to the fhir cell
                String resource = replaceSubjectReference(messageString);
                resp = fhirCell.postResource(resource, resourceType, version);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new C3PROException(e.getMessage(), e);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new C3PROException(e.getMessage(), e);
        }
        if (resp!=null) {
            int code = resp.getResponseCode();
            log.info(""+code);
            if (code>=400) throw new C3PROException("Error storing data into i2b2");
        }
	}

    protected String [] replaceSubjectIdPatient(String message) throws C3PROException, JSONException {
        JSONObject patientJSON = new JSONObject(message);
        String subjectId = patientJSON.getString(FHIR_RESOURCE_ID);
        String i2b2Subject = findI2B2Subject(subjectId);
        patientJSON.put(FHIR_RESOURCE_ID, i2b2Subject);
        String [] ret = new String [2];
        ret[0] = patientJSON.toString();
        ret[1] = i2b2Subject;
        return ret;
    }

    protected String replaceSubjectReference(String message) throws C3PROException, JSONException {
        JSONObject obsJSON = new JSONObject(message);
        JSONObject subjectJSON = obsJSON.getJSONObject(FHIR_RESOURCE_SUBJECT);
        String subjectRef = subjectJSON.getString(FHIR_RESOURCE_REFERENCE);
        String [] parts = subjectRef.split("/");
        if (parts.length<2) {
            log.error("Subject reference missing:" + subjectRef);
            throw new C3PROException("Subject reference not informed properly!");
        }
        String i2b2Subject = findI2B2Subject(parts[1]);
        subjectJSON.put(FHIR_RESOURCE_REFERENCE, parts[0]+"/"+i2b2Subject);
        obsJSON.put(FHIR_RESOURCE_SUBJECT, subjectJSON);
        return obsJSON.toString();
    }

    /**
     * Reprocess a message that has been stored in the DB given its id.
     * Each raw encrypted message is stored in a DB. Each row is identifier with a unique UUID that acts as the
     * the identifier of the row. This method allows to reprocess the message identified by its id
     * @param id The id
     * @throws Exception if any probelm occurs
     */
    public void reprocess(String id) throws Exception {
        Resource resource = this.resourceAccess.findById(id);
        byte [] messageEnc = Base64.decodeBase64(resource.getJson().getBytes());

        // Get the symetric key as metadata
        String symKeyBase64 =  resource.getKey();
        String publicKeyId = resource.getKeyId();


        publicKeyId = publicKeyId.trim();
        // We decrypt the secret key of the message using the private key
        byte [] secretKeyEnc = Base64.decodeBase64(symKeyBase64.getBytes());
        byte [] secretKey = decryptSecretKey(secretKeyEnc, publicKeyId);

        // We decrypt the message using the secret key
        byte [] message = decryptMessage(messageEnc, secretKey);
        String messageString = new String(message, AppConfig.UTF);
        String processed = null;
        try {
            saveMessage(messageString, resource.getFhirVersion());
            //tx.begin();
            resource.setProcessed(null);
            em.persist(resource);
            em.flush();
            //tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            throw e;
        }

    }

    protected String findI2B2Subject(String signature) throws C3PROException {
        try {
            List<PatientMap> maps = this.patientMapAccess.findBySignature(signature);
            if (maps.size()==0) {
                log.warn("No mapping correspondence for signature " + signature+". Probably the contract has not been " +
                        "processed yet. We create an entrance anyway and update it when needed");
                String subjectId=UUID.randomUUID().toString();
                PatientMap patientMap = new PatientMap();
                patientMap.setSubjectId(subjectId);
                patientMap.setId(UUID.randomUUID().toString());
                patientMap.setSignature(signature);
                patientMap.setVersionId(null);
                patientMap.setCode(null);
                patientMap.setDisplay(null);
                patientMap.setStartDate(new Date());
                patientMap.setSystem(null);

                // We store the info. The rest of the contract information will be processed when the contract is
                // received
                storeMappingInfo(patientMap, false);
                return subjectId;

            } else if (maps.size()>1) {
                log.warn("More than one mapping found for signature " + signature + ". Getting the first one");
            }
            return maps.get(0).getSubjectId();

        } catch (Exception e) {
            e.printStackTrace();
            throw new C3PROException(e.getMessage(), e);
        }
    }

    private void storeMappingInfo(PatientMap patientMap, boolean b) throws C3PROException {
        try {
            if (!b) {
                tx.begin();
            }
            em.persist(patientMap);
            em.flush();
            tx.commit();
        } catch (Exception e) {
            // If exception it might indicate that we are already in a transactional block. So, we try to persist again
            // without the transaction
            try {
                em.persist(patientMap);
                em.flush();
            } catch (Exception e2) {
                // if this also fails, we are in troubles and raise an exception!
                e2.printStackTrace();
                throw new C3PROException("Error storing consent data: " + e.getMessage(), e);
            }
        }
    }

    protected void storeContract(String message) throws C3PROException {
        String id = null;
        String version = null;
        Date startDate = null;
        String system = null;
        String code = null;
        String display = null;
        String signature = null;

        try {
            JSONObject contractJSON = new JSONObject(message);
            // Get resource id

            id = contractJSON.optString(FHIR_RESOURCE_ID, null);

            // Get applies date
            JSONObject applies = contractJSON.getJSONObject(FHIR_CONTRACT_APPLIES);
            String startDateStr = applies.getString(FHIR_CONTRACT_APPLIES_START);
            SimpleDateFormat sdf = new SimpleDateFormat(FHIR_DATE_FORMAT);
            startDate = sdf.parse(startDateStr);
            version = "0";

            // Get signature (subject UUID stored in device)
            JSONArray signer = contractJSON.getJSONArray(FHIR_CONTRACT_SIGNER);
            JSONObject signer0 = signer.getJSONObject(0);

            signature = signer0.getString(FHIR_CONTRACT_SIGNER_SIGNATURE);

            // Get type information. THey are optional!

            try {
                JSONArray type = signer0.getJSONArray(FHIR_CONTRACT_SIGNER_TYPE);
                JSONObject type0 = type.getJSONObject(0);
                system = type0.getString(FHIR_CONTRACT_SIGNER_TYPE_SYSTEM);
                code = type0.getString(FHIR_CONTRACT_SIGNER_TYPE_CODE);
                display = type0.getString(FHIR_CONTRACT_SIGNER_TYPE_DISPLAY);
            } catch (Exception e) {
                log.warn("type, code, system or display elements not present on CONTRACT Resource");
            }
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

        // Check if the mapping already exists
        List<PatientMap> patientMaps = patientMapAccess.findBySignature(signature);
        PatientMap patientMap = null;
        boolean b=false;
        if (patientMaps.size()==0) {
            patientMap = new PatientMap();
            if (id==null) {
                id = UUID.randomUUID().toString();
            }
            patientMap.setId(id);
            patientMap.setSignature(signature);
            patientMap.setSubjectId(UUID.randomUUID().toString());
        } else {
            try {
                tx.begin();
                b=true;
            } catch (Exception e) {}
            patientMap=patientMapAccess.findById(patientMaps.get(0).getId());
        }

        patientMap.setVersionId(version);
        patientMap.setCode(code);
        patientMap.setDisplay(display);
        patientMap.setStartDate(startDate);
        patientMap.setSystem(system);

        // finally, we generate the subject id that will
        storeMappingInfo(patientMap, b);

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

    protected void saveRawMessage(String uuid, String message, String key, String keyId, String processed,
                                  String version) throws C3PROException {
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
        res.setFhirVersion(version);
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
        this.resourceAccess.setEntityManager(em);
    }


    // testing purposes
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
