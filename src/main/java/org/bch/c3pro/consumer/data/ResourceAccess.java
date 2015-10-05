package org.bch.c3pro.consumer.data;

import org.bch.c3pro.consumer.model.Resource;

import javax.persistence.EntityManager;

/**
 * Created by ipinyol on 10/5/15.
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

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }
}
