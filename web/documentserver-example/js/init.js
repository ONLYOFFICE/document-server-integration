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

function initEditor(docKey, mode, type) {
    //mode for editor
    window.mode = window.mode || mode || "view";
    mode = window.mode;

    //mode for editor
    window.type = window.type || type || "desktop";
    type = window.type;

    //url for document
    window.docUrl = document.getElementById("documentUrl").value;

    //key for chaching and collaborate editing
    window.docKey = window.docKey || docKey || key(docUrl);
    docKey = window.docKey;

    //type for document
    var docType = docUrl.substring(docUrl.lastIndexOf(".") + 1).trim().toLowerCase();
    //type for editor
    var documentType = getDocumentType(docType);

    //creating object editing
    new DocsAPI.DocEditor("placeholder",
        {
            type: type,
            width: (type == "desktop" ? "100%" : undefined),
            height: (type == "desktop" ? "100%" : undefined),
            documentType: documentType,
            document: {
                title: docUrl,
                url: docUrl,
                fileType: docType,
                key: docKey,
                permissions: {
                    edit: true
                }
            },
            editorConfig: {
                mode: mode,
            }
        });
}

function key(k) {
    var result = k.replace(new RegExp("[^0-9-.a-zA-Z_=]", "g"), "_") + (new Date()).getTime();
    return result.substring(result.length - Math.min(result.length, 20));
};

var getDocumentType = function (ext) {
    if (".doc.docx.docm.dot.dotx.dotm.odt.fodt.ott.rtf.txt.html.htm.mht.xml.pdf.djvu.fb2.epub.xps.oxps".indexOf(ext) != -1) return "text";
    if (".xls.xlsx.xlsm.xlsb.xlt.xltx.xltm.ods.fods.ots.csv".indexOf(ext) != -1) return "spreadsheet";
    if (".pps.ppsx.ppsm.ppt.pptx.pptm.pot.potx.potm.odp.fodp.otp".indexOf(ext) != -1) return "presentation";
    return null;
};