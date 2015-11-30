package org.bch.c3pro.consumer.external;

import org.bch.c3pro.consumer.config.AppConfig;
import org.bch.c3pro.consumer.exception.C3PROException;
import org.bch.c3pro.consumer.util.HttpRequest;
import org.bch.c3pro.consumer.util.Response;
import org.bch.c3pro.consumer.util.Utils;

import javax.inject.Inject;
import java.io.IOException;

/**
 * Created by ipinyol on 7/10/15.
 */
public class I2B2FHIRCell {

    // TODO: Consider version to determine the end-point
    private static final String HTTP_TYPE_CONSUMES = "application/json";

    @Inject
    private HttpRequest httpRequest;

    public Response postQuestionnaireAnswers(String qa) throws C3PROException, IOException {
        return postResource(qa, AppConfig.getProp(AppConfig.ENDPOINT_FHIR_I2B2_QA));
    }

    // TODO: update endpoint properly
    public Response postQuestionnaireResponse(String qr) throws C3PROException, IOException {
        return postResource(qr, AppConfig.getProp(AppConfig.ENDPOINT_FHIR_I2B2_QA));
    }

    public Response postObservation(String obs) throws C3PROException, IOException {
        return postResource(obs, AppConfig.getProp(AppConfig.ENDPOINT_FHIR_I2B2_OBS));
    }

    public Response postContract(String con) throws C3PROException, IOException {
        return postResource(con, AppConfig.getProp(AppConfig.ENDPOINT_FHIR_I2B2_CON));
    }

    public Response putPatient(String patient, String patientId) throws C3PROException, IOException {
        return putResource(patient, AppConfig.getProp(AppConfig.ENDPOINT_FHIR_I2B2_PAT), "/"+patientId);
    }

    private Response postResource(String resource, String endPoint) throws C3PROException, IOException {
        String url = generateURL(endPoint);
        Response resp = this.httpRequest.doPostGeneric(url, resource, "", HTTP_TYPE_CONSUMES);
        return resp;

    }

    private Response putResource(String resource, String endPoint, String extra) throws C3PROException, IOException {
        String url = generateURL(endPoint) + extra;
        Response resp = this.httpRequest.doPostGeneric(url, resource, "", HTTP_TYPE_CONSUMES, "PUT");
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
