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

import com.google.gson.Gson;
import entities.FileModel;
import entities.User;
import helpers.ConfigManager;
import helpers.CookieManager;
import helpers.DocumentManager;
import helpers.FileUtility;
import helpers.Users;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@WebServlet(name = "EditorServlet", urlPatterns = {"/EditorServlet"})
public class EditorServlet extends HttpServlet {
    // process request
    protected void processRequest(final HttpServletRequest request,
                                  final HttpServletResponse response) throws ServletException, IOException {
        DocumentManager.init(request, response);

        String fileName = FileUtility.getFileName(request.getParameter("fileName"));
        String fileExt = request.getParameter("fileExt");
        String sample = request.getParameter("sample");
        Boolean isEnableDirectUrl = Boolean.valueOf(request.getParameter("directUrl"));

        // check if there is sample data in the request
        Boolean sampleData = (sample == null || sample.isEmpty()) ? false : sample.toLowerCase().equals("true");

        CookieManager cm = new CookieManager(request);
        User user = Users.getUser(cm.getCookie("uid"));

        if (fileExt != null) {
            try {
                // create demo document
                fileName = DocumentManager.createDemo(fileExt, sampleData, user);

                // redirect the request
                response.sendRedirect("EditorServlet?fileName=" + URLEncoder.encode(fileName, "UTF-8"));
                return;
            } catch (Exception ex) {
                response.getWriter().write("Error: " + ex.getMessage());
            }
        }

        // create file model (get all the necessary parameters from cookies)
        FileModel file = new FileModel(fileName, cm.getCookie("ulang"), request.getParameter("actionLink"),
                user, isEnableDirectUrl);
        // change type parameter if needed
        file.changeType(request.getParameter("mode"), request.getParameter("type"), user, fileName);

        // an image that will be inserted into the document
        Map<String, Object> dataInsertImage = new HashMap<>();
        dataInsertImage.put("fileType", "png");
        dataInsertImage.put("url", DocumentManager.getServerUrl(true) + "/css/img/logo.png");
        if (isEnableDirectUrl) {
            dataInsertImage.put("directUrl", DocumentManager.getServerUrl(false) + "/css/img/logo.png");
        }

        // a document that will be compared with the current document
        Map<String, Object> dataDocument = new HashMap<>();
        dataDocument.put("fileType", "docx");
        dataDocument.put("url", DocumentManager.getServerUrl(true) + "/IndexServlet?type=assets&"
                + "name=sample.docx");
        if (isEnableDirectUrl) {
            dataDocument.put("directUrl", DocumentManager.getServerUrl(false) + "/IndexServlet?"
                    + "type=assets&name=sample.docx");
        }

        // recipients data for mail merging
        Map<String, Object> dataSpreadsheet = new HashMap<>();
        dataSpreadsheet.put("fileType", "csv");
        dataSpreadsheet.put("url", DocumentManager.getServerUrl(true) + "/IndexServlet?"
                + "type=csv");
        if (isEnableDirectUrl) {
            dataSpreadsheet.put("directUrl", DocumentManager.getServerUrl(false)
                    + "/IndexServlet?type=csv");
        }

        // users data for mentions
        List<Map<String, Object>> usersForMentions = Users.getUsersForMentions(user.getId());

        // check if the document token is enabled
        if (DocumentManager.tokenEnabled()) {
            file.buildToken();  // generate document token

            // create token from the dataInsertImage object
            dataInsertImage.put("token", DocumentManager.createToken(dataInsertImage));

            // create token from the dataDocument object
            dataDocument.put("token", DocumentManager.createToken(dataDocument));

            // create token from the dataSpreadsheet object
            dataSpreadsheet.put("token", DocumentManager.createToken(dataSpreadsheet));
        }

        Gson gson = new Gson();
        request.setAttribute("file", file);
        request.setAttribute("docserviceApiUrl", ConfigManager.getProperty("files.docservice.url.site")
                + ConfigManager.getProperty("files.docservice.url.api"));
        request.setAttribute("dataInsertImage",  gson.toJson(dataInsertImage)
                .substring(1, gson.toJson(dataInsertImage).length() - 1));
        request.setAttribute("dataDocument",  gson.toJson(dataDocument));
        request.setAttribute("dataSpreadsheet", gson.toJson(dataSpreadsheet));
        request.setAttribute("usersForMentions", !user.getId()
                .equals("uid-0") ? gson.toJson(usersForMentions) : null);
        request.getRequestDispatcher("editor.jsp").forward(request, response);
    }

    // create get request
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException,
            IOException {
        processRequest(request, response);
    }

    // create post request
    @Override
    protected void doPost(final HttpServletRequest request,
                          final HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    // get servlet information
    @Override
    public String getServletInfo() {
        return "Editor page";
    }
}
