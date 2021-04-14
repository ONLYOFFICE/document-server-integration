/**
 *
 * (c) Copyright Ascensio System SIA 2020
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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class GlobalServletContextListener implements ServletContextListener
{
    @Override
    // destroy ServletContextListener interface
    public void contextDestroyed(ServletContextEvent arg0)
    {
        System.out.println("ServletContextListener destroyed");
    }

    @Override
    // start ServletContextListener interface
    public void contextInitialized(ServletContextEvent arg0)
    {
        TrustManager[] trustAllCerts = new TrustManager[]
        {
            new X509TrustManager()
            {
                @Override
                // return an array of certificates which are trusted
                public java.security.cert.X509Certificate[] getAcceptedIssuers()
                {
                    return null;
                }

                @Override
                // check whether the X509 certificate chain can be validated and is trusted for client authentication
                public void checkClientTrusted(X509Certificate[] certs, String authType)
                {
                }

                @Override
                // check whether the X509 certificate chain can be validated and is trusted for server authentication
                public void checkServerTrusted(X509Certificate[] certs, String authType)
                {
                }
            }
        };

        SSLContext sc;

        try
        {
            // register the all-trusting trust manager for HTTPS
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }
        catch (NoSuchAlgorithmException | KeyManagementException ex)
        {
        }

        // create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier()
        {
            @Override
            public boolean verify(String hostname, SSLSession session)
            {
                return true;
            }
        };

        // install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        System.out.println("ServletContextListener started");
    }
}