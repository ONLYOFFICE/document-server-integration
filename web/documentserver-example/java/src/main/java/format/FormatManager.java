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

package format;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import entities.FileType;

public final class FormatManager {
    public List<String> fillableExtensions() throws URISyntaxException,
                                                    IOException,
                                                    JsonSyntaxException {
        return this
            .fillable()
            .stream()
            .map(format -> format.extension())
            .collect(Collectors.toList());
    }

    public List<Format> fillable() throws URISyntaxException,
                                          IOException,
                                          JsonSyntaxException {
        return this
            .all()
            .stream()
            .filter(format -> format.getActions().contains("fill"))
            .collect(Collectors.toList());
    }

    public List<String> viewableExtensions() throws URISyntaxException,
                                                    IOException,
                                                    JsonSyntaxException {
        return this
            .viewable()
            .stream()
            .map(format -> format.extension())
            .collect(Collectors.toList());
    }

    public List<Format> viewable() throws URISyntaxException,
                                          IOException,
                                          JsonSyntaxException {
        return this
            .all()
            .stream()
            .filter(format -> format.getActions().contains("view"))
            .collect(Collectors.toList());
    }

    public List<String> editableExtensions() throws URISyntaxException,
                                                    IOException,
                                                    JsonSyntaxException {
        return this
            .editable()
            .stream()
            .map(format -> format.extension())
            .collect(Collectors.toList());
    }

    public List<Format> editable()  throws URISyntaxException,
                                           IOException,
                                           JsonSyntaxException {
        return this
            .all()
            .stream()
            .filter(format -> (
                format.getActions().contains("edit")
                || format.getActions().contains("lossy-edit")
            ))
            .collect(Collectors.toList());
    }

    public List<String> convertibleExtensions() throws URISyntaxException,
                                                       IOException,
                                                       JsonSyntaxException {
        return this
            .convertible()
            .stream()
            .map(format -> format.extension())
            .collect(Collectors.toList());
    }

    public List<Format> convertible() throws URISyntaxException,
                                             IOException,
                                             JsonSyntaxException {
        return this
            .all()
            .stream()
            .filter(format -> (
                format.getType() == FileType.Cell && format.getConvert().contains("xlsx")
                || format.getType() == FileType.Slide && format.getConvert().contains("pptx")
                || format.getType() == FileType.Word && format.getConvert().contains("docx")
            ))
            .collect(Collectors.toList());
    }

    public List<String> spreadsheetExtensions() throws URISyntaxException,
                                                       IOException,
                                                       JsonSyntaxException {
        return this
            .spreadsheets()
            .stream()
            .map(format -> format.extension())
            .collect(Collectors.toList());
    }

    public List<Format> spreadsheets() throws URISyntaxException,
                                              IOException,
                                              JsonSyntaxException {
        return this
            .all()
            .stream()
            .filter(format -> format.getType() == FileType.Cell)
            .collect(Collectors.toList());
    }

    public List<String> presentationExtensions() throws URISyntaxException,
                                                        IOException,
                                                        JsonSyntaxException {
        return this
            .presentations()
            .stream()
            .map(format -> format.extension())
            .collect(Collectors.toList());
    }

    public List<Format> presentations()  throws URISyntaxException,
                                                IOException,
                                                JsonSyntaxException {
        return this
            .all()
            .stream()
            .filter(format -> format.getType() == FileType.Slide)
            .collect(Collectors.toList());
    }

    public List<String> documentExtensions() throws URISyntaxException,
                                                    IOException,
                                                    JsonSyntaxException {
        return this
            .documents()
            .stream()
            .map(format -> format.extension())
            .collect(Collectors.toList());
    }

    public List<Format> documents() throws URISyntaxException,
                                           IOException,
                                           JsonSyntaxException {
        return this
            .all()
            .stream()
            .filter(format -> format.getType() == FileType.Word)
            .collect(Collectors.toList());
    }

    public List<String> allExtensions() throws URISyntaxException,
                                               IOException,
                                               JsonSyntaxException {
        return this
            .all()
            .stream()
            .map(format -> format.extension())
            .collect(Collectors.toList());
    }

    public List<Format> all() throws URISyntaxException,
                                     IOException,
                                     JsonSyntaxException {
        Path path = this.file();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        String contents = String.join(System.lineSeparator(), lines);
        Gson gson = new Gson();
        Format[] formats = gson.fromJson(contents, Format[].class);
        return Arrays.asList(formats);
    }

    private Path file() throws URISyntaxException {
        return this
            .directory()
            .resolve("onlyoffice-docs-formats.json");
    }

    private Path directory() throws URISyntaxException {
        URI uri = Thread
            .currentThread()
            .getContextClassLoader()
            .getResource("assets/document-formats")
            .toURI();
        return Paths.get(uri);
    }
}
