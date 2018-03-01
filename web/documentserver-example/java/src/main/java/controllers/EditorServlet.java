/*
 *
 * (c) Copyright Ascensio System Limited 2010-2018
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
*/


package controllers;

import helpers.ConfigManager;
import helpers.DocumentManager;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import entities.FileModel;


@WebServlet(name = "EditorServlet", urlPatterns = {"/EditorServlet"})
public class EditorServlet extends HttpServlet
{
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String fileName = "";
        if (request.getParameterMap().containsKey("fileName"))
        {
             fileName = request.getParameter("fileName");
        }

        String fileExt = null;
        if (request.getParameterMap().containsKey("fileExt"))
        {
             fileExt = request.getParameter("fileExt");
        }

        if (fileExt != null)
        {
            try
            {
                DocumentManager.Init(request, response);
                fileName = DocumentManager.CreateDemo(fileExt);
            }
            catch (Exception ex)
            {
                response.getWriter().write("Error: " + ex.getMessage());    
            }
        }

        String mode = "";
        if (request.getParameterMap().containsKey("mode"))
        {
             mode = request.getParameter("mode");
        }
        Boolean desktopMode = !"embedded".equals(mode);
        
        FileModel file = new FileModel();
        file.SetTypeDesktop(desktopMode);
        file.SetFileName(fileName);
        
        request.setAttribute("file", file);
        request.setAttribute("mode", mode);
        request.setAttribute("type", desktopMode ? "desktop" : "embedded");
        request.setAttribute("docserviceApiUrl", ConfigManager.GetProperty("files.docservice.url.api"));
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
