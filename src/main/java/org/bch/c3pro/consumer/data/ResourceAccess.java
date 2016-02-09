package org.bch.c3pro.consumer.data;

import org.bch.c3pro.consumer.model.PatientMap;
import org.bch.c3pro.consumer.model.Resource;

import javax.persistence.EntityManager;

/**
 * Class that enables basic hibernate queries over {@link ResourceAccess} DB
 * @author CHIT-IHL
 */
public class ResourceAccess {
    private EntityManager em;

    /**
     * Find a Resource by id
     * The id is the id of the fhir resource
     * @param id
     * @return the Resource object if found. Null otherwise
     */
    public Resource findById(String id) {
        em.flush();
        try {
            return em.find(Resource.class, id);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Sets the entity manager object
     * @param em The entity manager object
     */
    public void setEntityManager(EntityManager em) {
        this.em = em;
    }
}
