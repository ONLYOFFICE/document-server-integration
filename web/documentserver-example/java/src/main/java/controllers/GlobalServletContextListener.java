/**
 *
 * (c) Copyright Ascensio System SIA 2023
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package controllers;

import helpers.ConfigManager;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

public class GlobalServletContextListener implements ServletContextListener {
    // destroy ServletContextListener interface
    @Override
    public void contextDestroyed(final ServletContextEvent arg0) {
        System.out.println("ServletContextListener destroyed");
    }

    // start ServletContextListener interface
    @Override
    public void contextInitialized(final ServletContextEvent arg0) {
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                // return an array of certificates which are trusted
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                // check whether the X509 certificate chain can be validated and is trusted for client authentication
                @Override
                public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
                }

                // check whether the X509 certificate chain can be validated and is trusted for server authentication
                @Override
                public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
                }
            }
        };

        SSLContext sc;

        if (!ConfigManager.getProperty("files.docservice.verify-peer-off").isEmpty()) {
             try {
                // register the all-trusting trust manager for HTTPS
                sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (NoSuchAlgorithmException | KeyManagementException ex) {
            }
        }

        // create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override
            public boolean verify(final String hostname, final SSLSession session) {
                return true;
            }
        };

        // install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        System.out.println("ServletContextListener started");
    }
}
