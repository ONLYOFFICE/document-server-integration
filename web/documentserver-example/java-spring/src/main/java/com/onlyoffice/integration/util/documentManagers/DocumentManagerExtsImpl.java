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
package com.onlyoffice.integration.util.documentManagers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class DocumentManagerExtsImpl implements DocumentManagerExts {
    @Value("${files.docservice.viewed-docs}")
    private String docserviceViewedDocs;

    @Value("${files.docservice.edited-docs}")
    private String docserviceEditedDocs;

    @Value("${files.docservice.convert-docs}")
    private String docserviceConvertDocs;

    public List<String> getFileExts() {
        List<String> res = new ArrayList<>();

        res.addAll(getViewedExts());
        res.addAll(getEditedExts());
        res.addAll(getConvertExts());

        return res;
    }

    public List<String> getViewedExts()
    {
        return Arrays.asList(docserviceViewedDocs.split("\\|"));
    }

    public List<String> getEditedExts()
    {
        return Arrays.asList(docserviceEditedDocs.split("\\|"));
    }

    public List<String> getConvertExts()
    {
        return Arrays.asList(docserviceConvertDocs.split("\\|"));
    }
}
