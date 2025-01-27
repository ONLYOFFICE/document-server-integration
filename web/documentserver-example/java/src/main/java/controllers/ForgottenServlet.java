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

package controllers;


import helpers.TrackManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import entities.ForgottenFile;
import helpers.FileUtility;
import helpers.ConfigManager;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

@WebServlet(name = "ForgottenServlet", urlPatterns = {"/ForgottenServlet"})
@MultipartConfig
public class ForgottenServlet extends HttpServlet {
    protected void processRequest(final HttpServletRequest request,
                                  final HttpServletResponse response) throws ServletException, IOException {
        // create a variable to display information about the application and error messages
        PrintWriter writer = response.getWriter();

        if (!Boolean.valueOf(ConfigManager.getProperty("enable-forgotten"))) {
            writer.write("{ \"error\": \"The forgotten page is disabled\"}");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // get the type parameter from the request
        String action = request.getParameter("type");

        if (action == null) {
            // forward the request and response objects to the forgotten.jsp
            request.setAttribute("files", getFiles());
            request.getRequestDispatcher("forgotten.jsp").forward(request, response);
            return;
        }

        // define functions for each type of operation
        switch (action.toLowerCase()) {
            case "delete":
                delete(request, response, writer);
                break;
            default:
                break;
        }
    }

    private ArrayList<ForgottenFile> getFiles() {
        ArrayList<ForgottenFile> files = new ArrayList<ForgottenFile>();
        try {
            JSONObject forgottenList = TrackManager.commandRequest("getForgottenList", null, null);
            JSONArray keys = (JSONArray) forgottenList.get("keys");
            for (int i = 0; i < keys.size(); i++) {
                JSONObject result = TrackManager.commandRequest("getForgotten",
                                                            String.valueOf(keys.get(i)),
                                                            null);
                ForgottenFile file = new ForgottenFile(
                    result.get("key").toString(),
                    result.get("url").toString(),
                    FileUtility.getFileType(result.get("url").toString())
                        .toString()
                        .toLowerCase()
                );
                files.add(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return files;
    }

    private static void delete(final HttpServletRequest request,
                               final HttpServletResponse response,
                               final PrintWriter writer) {
        try {
            String filename = request.getParameter("filename");
            if (filename != null && !filename.isEmpty()) {
                TrackManager.commandRequest("deleteForgotten", filename, null);
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        } catch (Exception e) {
            writer.write("{ \"error\": \"" + e.getMessage() + "\"}");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // process get request
    @Override
    protected void doGet(final HttpServletRequest request,
                         final HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    // process delete request
    @Override
    protected void doDelete(final HttpServletRequest request,
                          final HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    // get servlet information
    @Override
    public String getServletInfo() {
        return "Forgotten Files";
    }
}
