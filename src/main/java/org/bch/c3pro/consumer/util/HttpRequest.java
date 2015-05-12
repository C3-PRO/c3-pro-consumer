package org.bch.c3pro.consumer.util;

import org.bch.c3pro.consumer.config.AppConfig;

import javax.ejb.Stateless;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Abstract functionality for REST calls
 * Created by CH176656 on 3/20/2015.
 */
@Stateless
public class HttpRequest {

    public Response doPostGeneric(String urlStr,  String headerAuth) throws IOException {
        return doPostGeneric(urlStr, null, headerAuth, null);
    }

    public Response doPostGeneric(String urlStr, String content, String headerAuth,
                                  String headerContentType) throws IOException {
        return doPostGeneric(urlStr, content, headerAuth, headerContentType, "POST");
    }

    public Response doPostGeneric(String urlStr, String content, String headerAuth,
                                  String headerContentType, String operation) throws IOException {

        OutputStream out;
        HttpURLConnection con;

        URL url = new URL(urlStr);
        con = (HttpURLConnection) url.openConnection();

        con.setAllowUserInteraction(false);
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestMethod(operation);
        if (content!=null) {
            con.setRequestProperty("Content-length", String.valueOf(content.length()));
        }
        if (headerContentType != null) {
            con.setRequestProperty("Content-type", headerContentType);
        }
        if (headerAuth!=null) {
            con.setRequestProperty("Authorization", headerAuth);
        }
        if (content!=null) {
            out = con.getOutputStream();
            out.write(content.getBytes("UTF-8"));
            out.flush();
            out.close();
        }

        Response resp = new ResponseJava(con);
        con.disconnect();
        return resp;
    }

    public static class ResponseJava implements Response {
        private HttpURLConnection con;
        private String content;
        private int status;

        ResponseJava(HttpURLConnection con) throws IOException {
            BufferedReader in;
            String response = "";
            this.con = con;
            this.status = con.getResponseCode();
            System.out.println("RETURN CODE:" + this.status);
            if (this.status >= 400) {
                this.content=null;
                return;
            }

            in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            char[] buffer = new char[AppConfig.HTTP_TRANSPORT_BUFFER_SIZE + 1];
            while (true) {
                int numCharRead = in.read(buffer, 0, AppConfig.HTTP_TRANSPORT_BUFFER_SIZE);
                if (numCharRead == -1) {
                    break;
                }
                String line = new String(buffer, 0, numCharRead);
                response += line;
            }
            in.close();
            this.content = response;
        }

        @Override
        public int getResponseCode() {
            return this.status;
        }

        @Override
        public String getContent() {
            return this.content;
        }
    }
}

