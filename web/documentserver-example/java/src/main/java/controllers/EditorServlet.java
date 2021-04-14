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

import com.google.gson.Gson;
import helpers.ConfigManager;
import helpers.CookieManager;
import helpers.DocumentManager;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import entities.FileModel;
import helpers.FileUtility;


@WebServlet(name = "EditorServlet", urlPatterns = {"/EditorServlet"})
public class EditorServlet extends HttpServlet
{
    // process request
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        DocumentManager.Init(request, response);

        String fileName = FileUtility.GetFileName(request.getParameter("fileName"));
        String fileExt = request.getParameter("fileExt");
        String sample = request.getParameter("sample");

        // check if there is sample data in the request
        Boolean sampleData = (sample == null || sample.isEmpty()) ? false : sample.toLowerCase().equals("true");

        CookieManager cm = new CookieManager(request);

        if (fileExt != null)
        {
            try
            {
                // create demo document
                fileName = DocumentManager.CreateDemo(fileExt, sampleData, cm.getCookie("uid"), cm.getCookie("uname"));
                response.sendRedirect("EditorServlet?fileName=" + URLEncoder.encode(fileName, "UTF-8"));  // redirect the request
                return;
            }
            catch (Exception ex)
            {
                response.getWriter().write("Error: " + ex.getMessage());    
            }
        }

        // create file model (get all the necessary parameters from cookies)
        FileModel file = new FileModel(fileName, cm.getCookie("ulang"), cm.getCookie("uid"), cm.getCookie("uname"), request.getParameter("actionLink"));
        // change type parameter if needed
        file.changeType(request.getParameter("mode"), request.getParameter("type"));

        // an image that will be inserted into the document
        Map<String, Object> dataInsertImage = new HashMap<>();
        dataInsertImage.put("fileType", "png");
        dataInsertImage.put("url", DocumentManager.GetServerUrl(true) + "/css/img/logo.png");

        // a document that will be compared with the current document
        Map<String, Object> dataCompareFile = new HashMap<>();
        dataCompareFile.put("fileType", "docx");
        dataCompareFile.put("url", DocumentManager.GetServerUrl(true) + "/IndexServlet?type=assets&name=sample.docx");

        // recipients data for mail merging
        Map<String, Object> dataMailMergeRecipients = new HashMap<>();
        dataMailMergeRecipients.put("fileType", "csv");
        dataMailMergeRecipients.put("url", DocumentManager.GetServerUrl(true) + "/IndexServlet?type=csv");

        // check if the document token is enabled
        if (DocumentManager.TokenEnabled())
        {
            file.BuildToken();  // generate document token
            dataInsertImage.put("token", DocumentManager.CreateToken(dataInsertImage));  // create token from the dataInsertImage object
            dataCompareFile.put("token", DocumentManager.CreateToken(dataCompareFile));  // create token from the dataCompareFile object
            dataMailMergeRecipients.put("token", DocumentManager.CreateToken(dataMailMergeRecipients));  // create token from the dataMailMergeRecipients object
        }

        Gson gson = new Gson();
        request.setAttribute("file", file);
        request.setAttribute("docserviceApiUrl", ConfigManager.GetProperty("files.docservice.url.site") + ConfigManager.GetProperty("files.docservice.url.api"));
        request.setAttribute("dataInsertImage",  gson.toJson(dataInsertImage).substring(1, gson.toJson(dataInsertImage).length()-1));
        request.setAttribute("dataCompareFile",  gson.toJson(dataCompareFile));
        request.setAttribute("dataMailMergeRecipients", gson.toJson(dataMailMergeRecipients));
        request.getRequestDispatcher("editor.jsp").forward(request, response);
    }

    @Override
    // create get request
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    @Override
    // create post request
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    @Override
    // get servlet information
    public String getServletInfo()
    {
        return "Editor page";
    }
}
