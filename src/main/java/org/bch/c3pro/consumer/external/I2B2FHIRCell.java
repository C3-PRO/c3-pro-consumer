package org.bch.c3pro.consumer.external;

import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.exception.C3PROException;
import org.bch.c3pro.consumer.util.HttpRequest;
import org.bch.c3pro.consumer.util.Response;
import org.bch.c3pro.consumer.util.Utils;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Wrapper class to interact with i2b2 through the fhir cell
 * @author CHIP-IHL
 */
public class I2B2FHIRCell {

    private static final String HTTP_TYPE_CONSUMES = "application/json";

    @Inject
    private HttpRequest httpRequest;

    /**
     * Performs a PUT call over a FHIR resource to the FHIR Cell
     * @param resource      The payoff resource
     * @param resourceName  The resource type, i.e. Patient
     * @param version       The FHIR version, i.e. DSTU2-1.0.2, DSTU2-0.9.0
     * @param idResource    The id of the resource. This will be part of the
     * @return
     * @throws C3PROException   In case the properties needed to build the enpoint are not informed
     * @throws IOException  In case of input/ouptut http problems
     */
    public Response putResource(String resource, String resourceName, String version, String idResource)
            throws C3PROException, IOException {
        String endPoint = buildEndPointwithVersion(resourceName, version);
        return put(resource, endPoint, "/"+idResource);
    }

    /**
     * Performs a PUT call over a FHIR resource to the FHIR Cell
     * @param resource      The payoff resource
     * @param resourceName  The resource type, i.e. Patient
     * @param version       The FHIR version, i.e. DSTU2-1.0.2, DSTU2-0.9.0
     * @return
     * @throws C3PROException   In case the properties needed to build the enpoint are not informed
     * @throws IOException  In case of input/ouptut http problems
     */
    public Response postResource(String resource, String resourceName, String version)
            throws C3PROException, IOException {
        String endPoint = buildEndPointwithVersion(resourceName, version);
        return post(resource, endPoint);
    }

    private String buildEndPointwithVersion(String resourceName, String version) throws C3PROException {
        if (version == null) {
            version = "";
        }
        String endPointPattern = AppConfig.getProp(AppConfig.ENDPOINT_FHIR_I2B2_ROOT);
        return String.format(endPointPattern, version) + "/" + resourceName;
    }

    private Response put(String resource, String endPoint, String extra) throws C3PROException, IOException {
        String url = generateURL(endPoint) + extra;
        Response resp = this.httpRequest.doPostGeneric(url, resource, "", HTTP_TYPE_CONSUMES, "PUT");
        return resp;
    }

    private Response post(String resource, String endPoint) throws C3PROException, IOException {
        String url = generateURL(endPoint);
        Response resp = this.httpRequest.doPostGeneric(url, resource, "", HTTP_TYPE_CONSUMES);
        return resp;
    }

    private String generateURL(String endPoint) throws C3PROException {
        return Utils.generateURL(
                AppConfig.getProp(AppConfig.PROTOCOL_FHIR_I2B2),
                AppConfig.getProp(AppConfig.HOST_FHIR_I2B2),
                AppConfig.getProp(AppConfig.PORT_FHIR_I2B2),
                endPoint);
    }

}
