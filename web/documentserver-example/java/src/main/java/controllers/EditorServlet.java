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
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        DocumentManager.Init(request, response);

        String fileName = FileUtility.GetFileName(request.getParameter("fileName"));
        String fileExt = request.getParameter("fileExt");
        String sample = request.getParameter("sample");

        Boolean sampleData = (sample == null || sample.isEmpty()) ? false : sample.toLowerCase().equals("true");

        CookieManager cm = new CookieManager(request);

        if (fileExt != null)
        {
            try
            {
                fileName = DocumentManager.CreateDemo(fileExt, sampleData, cm.getCookie("uid"), cm.getCookie("uname"));
                response.sendRedirect("EditorServlet?fileName=" + URLEncoder.encode(fileName, "UTF-8"));
                return;
            }
            catch (Exception ex)
            {
                response.getWriter().write("Error: " + ex.getMessage());    
            }
        }

        FileModel file = new FileModel(fileName, cm.getCookie("ulang"), cm.getCookie("uid"), cm.getCookie("uname"), request.getParameter("actionLink"));
        file.changeType(request.getParameter("mode"), request.getParameter("type"));

        Map<String, Object> dataInsertImage = new HashMap<>();
        dataInsertImage.put("fileType", "png");
        dataInsertImage.put("url", DocumentManager.GetServerUrl(true) + "/css/img/logo.png");

        Map<String, Object> dataCompareFile = new HashMap<>();
        dataCompareFile.put("fileType", "docx");
        dataCompareFile.put("url", DocumentManager.GetServerUrl(true) + "/IndexServlet?type=assets&name=sample.docx");

        Map<String, Object> dataMailMergeRecipients = new HashMap<>();
        dataMailMergeRecipients.put("fileType", "csv");
        dataMailMergeRecipients.put("url", DocumentManager.GetServerUrl(true) + "/IndexServlet?type=csv");

        if (DocumentManager.TokenEnabled())
        {
            file.BuildToken();
            dataInsertImage.put("token", DocumentManager.CreateToken(dataInsertImage));
            dataCompareFile.put("token", DocumentManager.CreateToken(dataCompareFile));
            dataMailMergeRecipients.put("token", DocumentManager.CreateToken(dataMailMergeRecipients));
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo()
    {
        return "Editor page";
    }
}
