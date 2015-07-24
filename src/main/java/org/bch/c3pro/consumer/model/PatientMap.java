package org.bch.c3pro.consumer.model;

/**
 * Created with IntelliJ IDEA.
 * User: CH176656
 * Date: 7/24/15
 * Time: 5:58 AM
 * To change this template use File | Settings | File Templates.
 */
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="patient_map")
public class PatientMap {
    @Id
    @Column(unique = true)
    private String id;

    private String signature;

    @Column(name = "subject_id")
    private String subjectId;

    @Column(name = "start_date")
    private Date startDate;

    private String system;

    private String code;

    private String display;

    @Column(name ="version_id")
    private String versionId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }
}

