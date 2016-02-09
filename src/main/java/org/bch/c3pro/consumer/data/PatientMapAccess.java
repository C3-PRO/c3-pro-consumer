package org.bch.c3pro.consumer.data;

import org.bch.c3pro.consumer.model.PatientMap;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Class that enables basic hibernate queries over {@link PatientMap} DB
 * @author CHIT-IHL
 */

public class PatientMapAccess {

    private EntityManager em;

    /**
     * Find a PatientMap by id
     * The id is the id of the fhir resource
     * @param id
     * @return the PatientMap object if found. Null otherwise
     */
    public PatientMap findById(String id) {
        em.flush();
        try {
            return em.find(PatientMap.class, id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the list of PatientMap entities whose signature coincide with the input
     * @param signature
     * @return  the list. Returns an empty list if no signature exits
     */
    public List<PatientMap> findBySignature(String signature) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<PatientMap> criteria = cb.createQuery(PatientMap.class);
        Root<PatientMap> patientMap = criteria.from(PatientMap.class);
        Predicate p1 = cb.equal(patientMap.get("signature"), signature);
        criteria.select(patientMap).where(p1);
        List<PatientMap> out = em.createQuery(criteria).getResultList();
        return out;
    }

    /**
     * Set the entity manager to be used
     * @param em The entoty manager
     */
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }
}
