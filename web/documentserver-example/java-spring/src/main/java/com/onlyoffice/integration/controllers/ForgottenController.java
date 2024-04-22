/**
 *
 * (c) Copyright Ascensio System SIA 2024
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

package com.onlyoffice.integration.controllers;

import com.onlyoffice.integration.documentserver.util.file.FileUtility;
import com.onlyoffice.integration.documentserver.managers.callback.CallbackManager;
import com.onlyoffice.integration.dto.ForgottenFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;


@CrossOrigin("*")
@Controller
public class ForgottenController {
    @Autowired
    private FileUtility fileUtility;

    @Value("${server.version}")
    private String serverVersion;

    @Value("${enable-forgotten}")
    private String enableForgotten;

    @Autowired
    private CallbackManager callbackManager;

    @GetMapping("${url.forgotten}")
    public String index(final Model model) {
        if (!forgottenEnabled()) {
            model.addAttribute("error", "The forgotten page is disabled");
            return "error.html";
        }

        model.addAttribute("files", getForgottenFiles());
        model.addAttribute("serverVersion", serverVersion);

        return "forgotten.html";
    }

    private ArrayList<ForgottenFile> getForgottenFiles() {
        ArrayList<ForgottenFile> files = new ArrayList<ForgottenFile>();
        try {
            JSONObject forgottenList = callbackManager.commandRequest("getForgottenList", null, null);
            JSONArray keys = (JSONArray) forgottenList.get("keys");
            for (int i = 0; i < keys.size(); i++) {
                JSONObject result = callbackManager.commandRequest("getForgotten",
                                                            String.valueOf(keys.get(i)),
                                                            null);
                ForgottenFile file = new ForgottenFile(
                    result.get("key").toString(),
                    fileUtility
                        .getDocumentType(result.get("url").toString())
                        .toString()
                        .toLowerCase(),
                    result.get("url").toString()
                );
                files.add(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    private boolean forgottenEnabled() {
        return Boolean.valueOf(enableForgotten);
    }

    @DeleteMapping("/forgotten/{filename}")
    public ResponseEntity<String> delete(@PathVariable("filename") final String filename) {
        if (!forgottenEnabled()) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        try {
            callbackManager.commandRequest("deleteForgotten", filename, null);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
