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

package com.onlyoffice.integration.documentserver.managers.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.primeframework.jwt.Signer;
import org.primeframework.jwt.Verifier;
import org.primeframework.jwt.domain.JWT;
import org.primeframework.jwt.hmac.HMACSigner;
import org.primeframework.jwt.hmac.HMACVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DefaultJwtManager implements JwtManager {
    @Value("${files.docservice.secret}")
    private String tokenSecret;
    @Value("${files.docservice.token-use-for-request}")
    private String tokenUseForRequest;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JSONParser parser;

    // create document token
    public String createToken(final Map<String, Object> payloadClaims) {
        try {
            // build a HMAC signer using a SHA-256 hash
            Signer signer = HMACSigner.newSHA256Signer(tokenSecret);
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet()) {  // run through all the keys from the payload
                jwt.addClaim(key, payloadClaims.get(key));  // and write each claim to the jwt
            }
            return JWT.getEncoder().encode(jwt, signer);  // sign and encode the JWT to a JSON string representation
        } catch (Exception e) {
            return "";
        }
    }

    // check if the token is enabled
    public boolean tokenEnabled() {
        return tokenSecret != null && !tokenSecret.isEmpty();
    }

    public boolean tokenUseForRequest() {
        return Boolean.parseBoolean(tokenUseForRequest) && !tokenUseForRequest.isEmpty();
    }

    // read document token
    public JWT readToken(final String token) {
        try {
            // build a HMAC verifier using the token secret
            Verifier verifier = HMACVerifier.newVerifier(tokenSecret);

            // verify and decode the encoded string JWT to a rich object
            return JWT.getDecoder().decode(token, verifier);
        } catch (Exception exception) {
            return null;
        }
    }

    // parse the body
    public JSONObject parseBody(final String payload, final String header) {
        JSONObject body;
        try {
            Object obj = parser.parse(payload);  // get body parameters by parsing the payload
            body = (JSONObject) obj;
        } catch (Exception ex) {
            throw new RuntimeException("{\"error\":1,\"message\":\"JSON Parsing error\"}");
        }
        if (tokenEnabled() && tokenUseForRequest()) {  // check if the token is enabled
            String token = (String) body.get("token");  // get token from the body
            if (token == null) {  // if token is empty
                if (header != null && !header.isBlank()) {  // and the header is defined

                    // get token from the header (it is placed after the Bearer prefix if it exists)
                    token = header.startsWith("Bearer ") ? header.substring("Bearer ".length()) : header;
                }
            }
            if (token == null || token.isBlank()) {
                throw new RuntimeException("{\"error\":1,\"message\":\"JWT expected\"}");
            }

            JWT jwt = readToken(token);  // read token
            if (jwt == null) {
                throw new RuntimeException("{\"error\":1,\"message\":\"JWT validation failed\"}");
            }
            if (jwt.getObject("payload") != null) {  // get payload from the token and check if it is not empty
                try {
                    @SuppressWarnings("unchecked") LinkedHashMap<String, Object> jwtPayload =
                            (LinkedHashMap<String, Object>) jwt.getObject("payload");

                    jwt.claims = jwtPayload;
                } catch (Exception ex) {
                    throw new RuntimeException("{\"error\":1,\"message\":\"Wrong payload\"}");
                }
            }
            try {
                Object obj = parser.parse(objectMapper.writeValueAsString(jwt.claims));
                body = (JSONObject) obj;
            } catch (Exception ex) {
                throw new RuntimeException("{\"error\":1,\"message\":\"Parsing error\"}");
            }
        }

        return body;
    }
}
