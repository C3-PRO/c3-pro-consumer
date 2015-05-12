package org.bch.c3pro.consumer.model;

import javax.persistence.*;

@Entity
@Table(name="resource_table")
public class Resource {
private static final long serialVersionUID = 1L;
	
	@Id
    @Column(name = "uuid", unique = true)
    private String UUID;
	
	@Lob
	private String json;
	
	@Lob
	private String key;

	public String getUUID() {
		return UUID;
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
	
	
}
