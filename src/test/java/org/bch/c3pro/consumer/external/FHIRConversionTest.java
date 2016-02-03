package org.bch.c3pro.consumer.external;

import org.bch.c3pro.consumer.exception.C3PROException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: CH176656
 * Date: 7/24/15
 * Time: 7:48 AM
 * To change this template use File | Settings | File Templates.
 */
public class FHIRConversionTest {
    String goodSubjectId = UUID.randomUUID().toString();

    String baseFHIRObs = "{\n" +
            "    \"resourceType\":\"Observation\",\n" +
            "    \"id\":\"91f47758-8856-4081-9374-c55d5b0f3b56\",\n" +
            "     \"meta\":{\n" +
            "         \"versionId\":\"0\",\n" +
            "         \"lastUpdated\":\"2015-07-20T19:10:34.944+00:00\"\n" +
            "     },\n" +
            "     \"code\":{\n" +
            "         \"coding\":[\n" +
            "             {\n" +
            "                \"system\":\"http://loinc.org\",\n" +
            "                 \"code\":\"8302-2\"\n" +
            "             }\n" +
            "         ]\n" +
            "     },\n" +
            "     \"valueQuantity\":{\n" +
            "         \"value\":182.88,\n" +
            "         \"units\":\"cm\"\n" +
            "     },\n" +
            "     \"status\":\"final\",\n" +
            "     \"subject\":{\n" +
            "         \"reference\":\"Patient/3C42AF26-4C0B-4FC9-A154-9D1484672DA9\"\n" +
            "     }\n" +
            " }";

    String baseFHIROsGood = "{" +
            "\"resourceType\":\"Observation\"," +
            "\"id\":\"91f47758-8856-4081-9374-c55d5b0f3b56\"," +
            "\"meta\":{" +
            "\"versionId\":\"0\"," +
            "\"lastUpdated\":\"2015-07-20T19:10:34.944+00:00\"" +
            "}," +
            "\"code\":{" +
            "\"coding\":[" +
            "{" +
            "\"system\":\"http://loinc.org\"," +
            "\"code\":\"8302-2\"" +
            "}" +
            "]" +
            "}," +
            "\"valueQuantity\":{" +
            "\"value\":182.88," +
            "\"units\":\"cm\"" +
            "}," +
            "\"status\":\"final\"," +
            "\"subject\":{" +
            "\"reference\":\"Patient/" + goodSubjectId + "\"" +
            "}" +
            "}";

    String baseFHIRQA = "{\n" +
            "  \"contained\": [\n" +
            "    {\n" +
            "      \"id\": \"period\",\n" +
            "      \"period\": {\n" +
            "        \"end\": \"2015-07-09T16:16:34.0388Z\",\n" +
            "        \"start\": \"2015-07-02T16:16:34.0388Z\"\n" +
            "      },\n" +
            "      \"resourceType\": \"Encounter\",\n" +
            "      \"status\": \"finished\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"encounter\": {\n" +
            "    \"reference\": \"#period\"\n" +
            "  },\n" +
            "  \"subject\": {\n" +
            "    \"reference\": \"Patient/3C42AF26-4C0B-4FC9-A154-9D1484672DA9\"\n" +
            "  },\n" +
            "  \"group\": {\n" +
            "    \"linkId\": \"org.chip.c3-pro.activity\",\n" +
            "    \"question\": [\n" +
            "      {\n" +
            "        \"answer\": [\n" +
            "          {\n" +
            "            \"valueQuantity\": {\n" +
            "              \"units\": \"minute\",\n" +
            "              \"value\": 366\n" +
            "            }\n" +
            "          }\n" +
            "        ],\n" +
            "        \"linkId\": \"motion-coprocessor.light\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"answer\": [\n" +
            "          {\n" +
            "            \"valueQuantity\": {\n" +
            "              \"units\": \"minute\",\n" +
            "              \"value\": 154\n" +
            "            }\n" +
            "          }\n" +
            "        ],\n" +
            "        \"linkId\": \"motion-coprocessor.moderate\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"resourceType\": \"QuestionnaireAnswers\",\n" +
            "  \"status\": \"completed\"\n" +
            "}";

    String baseFHIRQAGood = "{\"resourceType\":\"QuestionnaireAnswers\",\"contained\":[{\"resourceType\":\"Encounter\"," +
            "\"id\":\"period\",\"status\":\"finished\",\"period\":{\"start\":\"2015-07-02T16:16:34.0388Z\"," +
            "\"end\":\"2015-07-09T16:16:34.0388Z\"}}],\"status\":\"completed\",\"subject\":{\"reference\":" +
            "\"Patient/" + goodSubjectId + "\"},\"encounter\":{\"reference\":\"#period\"}," +
            "\"group\":{\"linkId\":\"org.chip.c3-pro.activity\",\"question\":[{\"linkId\":\"motion-coprocessor.light\"" +
            ",\"answer\":[{\"valueQuantity\":{\"value\":366,\"units\":\"minute\"}}]},{\"linkId\":" +
            "\"motion-coprocessor.moderate\",\"answer\":[{\"valueQuantity\":{\"value\":154,\"units\":\"minute\"}}]}]}}";

    
    @Test
    public void replaceSubjectIdObsTest() throws Exception {
        SQSListenerTest test = new SQSListenerTest();
        test.subjectId=this.goodSubjectId;
        String result = test.replaceSubjectReference(baseFHIRObs);
        JSONObject resultJSON = new JSONObject(result);
        JSONAssert.assertEquals(baseFHIROsGood, resultJSON, false);

    }

    @Test
    public void replaceSubjectIdQATest() throws Exception {
        SQSListenerTest test = new SQSListenerTest();
        test.subjectId=this.goodSubjectId;
        String result = test.replaceSubjectReference(baseFHIRQA);
        System.out.println(result);
        System.out.println(baseFHIRQAGood);
        JSONObject resultJSON = new JSONObject(result);
        JSONAssert.assertEquals(baseFHIRQAGood, resultJSON, false);
    }

    private class SQSListenerTest extends SQSListener {
        public String subjectId;

        public String replaceSubjectReferenceTest(String message) throws C3PROException, JSONException {
            return this.replaceSubjectReference(message);
        }

        @Override
        protected String findI2B2Subject(String signature) throws C3PROException {
            return this.subjectId;

        }
    }
}
