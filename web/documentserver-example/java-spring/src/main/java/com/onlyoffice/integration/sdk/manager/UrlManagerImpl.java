/**
 *
 * (c) Copyright Ascensio System SIA 2025
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

package com.onlyoffice.integration.sdk.manager;

import com.onlyoffice.integration.documentserver.storage.FileStoragePathBuilder;
import com.onlyoffice.manager.document.DocumentManager;
import com.onlyoffice.manager.settings.SettingsManager;
import com.onlyoffice.manager.url.DefaultUrlManager;
import com.onlyoffice.model.documenteditor.config.document.DocumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;

@Component
public class UrlManagerImpl extends DefaultUrlManager implements UrlManager {
    @Value("${url.index}")
    private String indexMapping;
    @Value("${url.download}")
    private String downloadUrl;
    @Value("${url.track}")
    private String trackUrl;

    @Autowired
    private FileStoragePathBuilder storagePathBuilder;
    @Autowired
    private DocumentManager documentManager;

    public UrlManagerImpl(final SettingsManager settingsManager) {
        super(settingsManager);
    }

    @Override
    public String getFileUrl(final String fileId) {
        return getDownloadUrl(fileId, true);
    }

    @Override
    public String getCreateUrl(final String fileId) {
        return getCreateUrl(fileId, false);
    }

    @Override
    public String getCreateSampleUrl(final String fileId) {
        return getCreateUrl(fileId, true);
    }

    @Override
    public String getCallbackUrl(final String fileId) {
        String serverPath = storagePathBuilder.getServerUrl(true);
        String storageAddress = storagePathBuilder.getStorageLocation();
        try {
            String query = trackUrl + "?fileName="
                    + URLEncoder.encode(fileId, java.nio.charset.StandardCharsets.UTF_8.toString())
                    + "&userAddress=" + URLEncoder
                    .encode(storageAddress, java.nio.charset.StandardCharsets.UTF_8.toString());
            return serverPath + query;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    @Override
    public String getGobackUrl(final String fileId) {
        return storagePathBuilder.getServerUrl(false) + indexMapping;
    }

    // get file URL
    public String getHistoryFileUrl(final String fileName, final Integer version, final String file,
                                    final Boolean forDocumentServer) {
        try {
            String serverPath = storagePathBuilder.getServerUrl(forDocumentServer);  // get server URL
            String hostAddress = storagePathBuilder.getStorageLocation();  // get the storage directory
            String filePathDownload = !fileName.contains(InetAddress.getLocalHost().getHostAddress()) ? fileName
                    : fileName.substring(fileName.indexOf(InetAddress.getLocalHost().getHostAddress())
                    + InetAddress.getLocalHost().getHostAddress().length() + 1);
            String userAddress = forDocumentServer ? "&userAddress" + URLEncoder
                    .encode(hostAddress, java.nio.charset.StandardCharsets.UTF_8.toString()) : "";
            String filePath = serverPath + "/downloadhistory?fileName=" + URLEncoder
                    .encode(filePathDownload, java.nio.charset.StandardCharsets.UTF_8.toString())
                    + "&ver=" + version + "&file=" + file
                    + userAddress;
            return filePath;
        } catch (UnsupportedEncodingException | UnknownHostException e) {
            return "";
        }
    }

    public String getTemplateImageUrl(final String fileName) {
        DocumentType documentType = documentManager.getDocumentType(fileName);  // get the file type
        String path = storagePathBuilder.getServerUrl(true);  // get server URL
        switch (documentType) {
            case WORD: // get URL to the template image for the word document type
                return path + "/css/img/file_docx.svg";
            case SLIDE: // get URL to the template image for the slide document type
                return path + "/css/img/file_pptx.svg";
            case CELL: // get URL to the template image for the cell document type
                return path + "/css/img/file_xlsx.svg";
            default: // get URL to the template image for the default document type (word)
                return path + "/css/img/file_docx.svg";
        }
    }

    // get URL to download a file
    private String getDownloadUrl(final String fileName, final Boolean isServer) {
        String serverPath = storagePathBuilder.getServerUrl(isServer);
        String storageAddress = storagePathBuilder.getStorageLocation();
        try {
            String userAddress = isServer ? "&userAddress=" + URLEncoder
                    .encode(storageAddress, java.nio.charset.StandardCharsets.UTF_8.toString()) : "";
            String query = downloadUrl + "?fileName="
                    + URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8.toString())
                    + userAddress;

            return serverPath + query;
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    // get URL to the created file
    private String getCreateUrl(final String fileName, final Boolean sample) {
        String fileExt = documentManager.getExtension(fileName);
        String url = storagePathBuilder.getServerUrl(true)
                + "/create?fileExt=" + fileExt + "&sample=" + sample;
        return url;
    }
}
