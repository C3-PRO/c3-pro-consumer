package org.bch.c3pro.consumer.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Entity model for the DB resource_table table
 * It captures the raw messages consumed from the queue.
 */
@Entity
@Table(name="resource_table")
public class Resource {
private static final long serialVersionUID = 1L;
	
	@Id
    @Column(name = "uuid", unique = true)
    private String UUID;

	/**
	 * The encrypted fhir resource message
	 */
	@Lob
	private String json;

	/**
	 * The encrypted symmetric key used for AES
	 */
	@Lob
	private String key;

	/**
	 * The public-private key id used to encrypt the AES symmetic key
	 */
    @Column(name = "key_id")
    private String keyId;

	/**
	 * If null, the resource has been proceesed correctly and it's in i2b2
	 * Otherwise, the column will contain the error message.
	 */
	@Column(name = "processed", nullable=true)
	private String processed = null;

	/**
	 * The date when the resource was received
	 */
	@Column(name = "start_date", nullable=false)
	private Date startDate = new Date();

	@Column(name = "fhir_version", nullable = false)
	private String fhirVersion;

	/**
	 * Returns the uuid of the row
	 * @return The id
	 */
	public String getUUID() {
		return UUID;
	}

	/**
	 * Returns the date
	 * @return the date
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * Sets the receiving date
	 * @param startDate The new date
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * Sets the uuid
	 * @param uUID The new UUID
	 */
	public void setUUID(String uUID) {
		UUID = uUID;
	}

	/**
	 * Returns the encrypted json
	 * @return The json
	 */
	public String getJson() {
		return json;
	}

	/**
	 * Sets the encrypted json
	 * @param json The new json
	 */
	public void setJson(String json) {
		this.json = json;
	}

	/**
	 * Returns the encrypted symmetric key
	 * @return The key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Sets the encrypted symmetric key
	 * @param key The new key
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Returns the key id (a UUID). It's use to enable multiple key support
	 * @return The key id
	 */
    public String getKeyId() {
        return this.keyId;
    }

	/**
	 * Sets the key id
	 * @param keyId
	 */
    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

	/**
	 * Returs the processed error message. Null if no error.
	 * @return The message
	 */
    public String getProcessed() {
        return processed;
    }

	/**
	 * Sets the processed error message. Should be set to null if no error.
	 * @param processed The new processed message
	 */
    public void setProcessed(String processed) {
        this.processed = processed;
    }

	/**
	 * Get the fhir version of the encrypted resource (e.i. 0.9.0, 1.0.2)
	 * @return The version
	 */
	public String getFhirVersion() {
		return fhirVersion;
	}

	/**
	 * Sets the fhir version of the encrypted resource. (e.i. 0.9.0 or 1.0.2)
	 * @param fhirVersion The new version
	 */
	public void setFhirVersion(String fhirVersion) {
		this.fhirVersion = fhirVersion;
	}
}
