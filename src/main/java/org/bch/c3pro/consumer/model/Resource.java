package org.bch.c3pro.consumer.model;

import javax.persistence.*;
import java.util.Date;

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

	public String getUUID() {
		return UUID;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public void setUUID(String uUID) {
		UUID = uUID;
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

    public String getKeyId() {
        return this.keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getProcessed() {
        return processed;
    }

    public void setProcessed(String processed) {
        this.processed = processed;
    }

	public String getFhirVersion() {
		return fhirVersion;
	}

	public void setFhirVersion(String fhirVersion) {
		this.fhirVersion = fhirVersion;
	}
}
