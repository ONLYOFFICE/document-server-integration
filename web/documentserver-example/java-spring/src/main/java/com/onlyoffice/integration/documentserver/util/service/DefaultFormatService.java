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

package com.onlyoffice.integration.documentserver.util.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.onlyoffice.integration.documentserver.models.Format;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DefaultFormatService implements FormatService {
    private List<Format> formats;
    @Autowired
    public DefaultFormatService(
            @Value("classpath:assets/document-formats/onlyoffice-docs-formats.json") final Resource resourceFile,
            final ObjectMapper objectMapper
    ) {
        try {
            File targetFile = resourceFile.getFile();
            this.formats = objectMapper.readValue(targetFile, new TypeReference<List<Format>>() { });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Format> getFormats() {
        return this.formats;
    }

    public List<Format> getFormatsByAction(final String action) {
        return this
                .formats
                .stream()
                .filter(format -> format.getActions().contains(action))
                .collect(Collectors.toList());
    }

    public List<String> allExtensions() {
        return this
                .formats
                .stream()
                .map(format -> format.getName())
                .collect(Collectors.toList());
    }

    public List<String> fillableExtensions() {
        return this
                .getFormatsByAction("fill")
                .stream()
                .map(format -> format.getName())
                .collect(Collectors.toList());
    }

    public List<String> viewableExtensions() {
        return this
                .getFormatsByAction("view")
                .stream()
                .map(format -> format.getName())
                .collect(Collectors.toList());
    }

    public List<String> editableExtensions() {
        return Stream
                .of(this.getFormatsByAction("edit"), this.getFormatsByAction("lossy-edit"))
                .flatMap(x -> x.stream())
                .map(format -> format.getName())
                .collect(Collectors.toList());
    }

    public List<String> autoConvertExtensions() {
        return this
                .getFormatsByAction("auto-convert")
                .stream()
                .map(format -> format.getName())
                .collect(Collectors.toList());
    }
}
