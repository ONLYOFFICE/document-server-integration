/**
 *
 * (c) Copyright Ascensio System SIA 2021
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
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JSONParser parser;

    public String createToken(Map<String, Object> payloadClaims) {
        try {
            Signer signer = HMACSigner.newSHA256Signer(tokenSecret);
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet()) {
                jwt.addClaim(key, payloadClaims.get(key));
            }
            return JWT.getEncoder().encode(jwt, signer);
        } catch (Exception e) {
            return "";
        }
    }

    public boolean tokenEnabled() {
        return tokenSecret != null && !tokenSecret.isEmpty();
    }

    public JWT readToken(String token) {
        try {
            Verifier verifier = HMACVerifier.newVerifier(tokenSecret);
            return JWT.getDecoder().decode(token, verifier);
        } catch (Exception exception) {
            return null;
        }
    }

    public JSONObject parseBody(String payload, String header) {
        JSONObject body;
        try {
            Object obj = parser.parse(payload);
            body = (JSONObject) obj;
        } catch (Exception ex) {
            throw new RuntimeException("{\"error\":1,\"message\":\"JSON Parsing error\"}");
        }
        if (tokenEnabled()) {
            String token = (String) body.get("token");
            if (token == null) {
                if (header != null && !header.isBlank()) {
                    token = header.startsWith("Bearer ") ? header.substring(7) : header;
                }
            }
            if (token == null || token.isBlank()) {
                throw new RuntimeException("{\"error\":1,\"message\":\"JWT expected\"}");
            }

            JWT jwt = readToken(token);
            if (jwt == null) {
                throw new RuntimeException("{\"error\":1,\"message\":\"JWT validation failed\"}");
            }
            if (jwt.getObject("payload") != null) {
                try {
                    @SuppressWarnings("unchecked") LinkedHashMap<String, Object> jwtPayload =
                            (LinkedHashMap<String, Object>)jwt.getObject("payload");

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
