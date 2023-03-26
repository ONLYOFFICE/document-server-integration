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

using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Web.Configuration;
using System.Web.Helpers;
using System.Web.Script.Serialization;

namespace OnlineEditorsExampleMVC.Helpers
{
    /// <summary>
    /// Class service api conversion
    /// </summary>
    public static class ServiceConverter
    {
        /// <summary>
        /// Static constructor
        /// </summary>
        static ServiceConverter()
        {
            DocumentConverterUrl = (WebConfigurationManager.AppSettings["files.docservice.url.site"] ?? "") + (WebConfigurationManager.AppSettings["files.docservice.url.converter"] ?? "");

            Int32.TryParse(WebConfigurationManager.AppSettings["files.docservice.timeout"], out ConvertTimeout);
            ConvertTimeout = ConvertTimeout > 0 ? ConvertTimeout : 120000;
        }

        #region private fields

        /// <summary>
        /// Timeout to request conversion
        /// </summary>
        private static readonly int ConvertTimeout;

        /// <summary>
        /// Url to the service of conversion
        /// </summary>
        private static readonly string DocumentConverterUrl;

        #endregion

        #region public method

        /// <summary>
        ///     The method is to convert the file to the required format
        /// </summary>
        /// <param name="documentUri">Uri for the document to convert</param>
        /// <param name="fromExtension">Document extension</param>
        /// <param name="toExtension">Extension to which to convert</param>
        /// <param name="documentRevisionId">Key for caching on service</param>
        /// <param name="isAsync">Perform conversions asynchronously</param>
        /// <param name="convertedDocumentData">Uri and file type of the converted document</param>
        /// <returns>The percentage of conversion completion</returns>
        /// <example>
        /// Dictionary<string, string> convertedDocumentData;
        /// GetConvertedData("http://helpcenter.onlyoffice.com/content/GettingStarted.pdf", ".pdf", ".docx", "http://helpcenter.onlyoffice.com/content/GettingStarted.pdf", false, out convertedDocumentData);
        /// </example>
        /// <exception>
        /// </exception>
        public static int GetConvertedData(string documentUri,
                                          string fromExtension,
                                          string toExtension,
                                          string documentRevisionId,
                                          bool isAsync,
                                          out Dictionary<string, string> convertedDocumentData,
                                          string filePass = null,
                                          string lang = null)
        {
            convertedDocumentData = new Dictionary<string, string>();

            // check if the fromExtension parameter is defined; if not, get it from the document url
            fromExtension = string.IsNullOrEmpty(fromExtension) ? Path.GetExtension(documentUri).ToLower() : fromExtension;

            // check if the file name parameter is defined; if not, get random uuid for this file
            var title = Path.GetFileName(documentUri);
            title = string.IsNullOrEmpty(title) ? Guid.NewGuid().ToString() : title;

            // get document key
            documentRevisionId = string.IsNullOrEmpty(documentRevisionId)
                                     ? documentUri
                                     : documentRevisionId;
            documentRevisionId = GenerateRevisionId(documentRevisionId);

            // specify request parameters
            var request = (HttpWebRequest)WebRequest.Create(DocumentConverterUrl);
            request.Method = "POST";
            request.ContentType = "application/json";
            request.Accept = "application/json";
            request.Timeout = ConvertTimeout;

            // write all the necessary parameters to the body object
            var body = new Dictionary<string, object>() {
                { "async", isAsync },
                { "filetype", fromExtension.Trim('.') },
                { "key", documentRevisionId },
                { "outputtype", toExtension.Trim('.') },
                { "title", title },
                { "url", documentUri },
                { "password", filePass },
                { "region", lang }
            };

            if (JwtManager.Enabled && JwtManager.SignatureUseForRequest)
            {
                // create payload object
                var payload = new Dictionary<string, object>
                    {
                        { "payload", body }
                    };

                var payloadToken = JwtManager.Encode(payload);  // encode the payload object to the payload token
                var bodyToken = JwtManager.Encode(body);  // encode the body object to the body token
                // create header token
                string JWTheader = string.IsNullOrEmpty(WebConfigurationManager.AppSettings["files.docservice.header"]) ? "Authorization" : WebConfigurationManager.AppSettings["files.docservice.header"];
                request.Headers.Add(JWTheader, "Bearer " + payloadToken);

                body.Add("token", bodyToken);
            }

            var bytes = Encoding.UTF8.GetBytes(new JavaScriptSerializer().Serialize(body));
            request.ContentLength = bytes.Length;
            using (var requestStream = request.GetRequestStream())  // get the request stream
            {
                requestStream.Write(bytes, 0, bytes.Length);  // and write the serialized body object to it
            }

            DocManagerHelper.VerifySSL();

            string dataResponse;
            using (var response = request.GetResponse())
            using (var stream = response.GetResponseStream())  // get the response stream
            {
                if (stream == null) throw new Exception("Response is null");

                using (var reader = new StreamReader(stream))
                {
                    dataResponse = reader.ReadToEnd();  // and read it
                }
            }

            return GetResponseData(dataResponse, out convertedDocumentData);
        }

        /// <summary>
        /// Translation key to a supported form.
        /// </summary>
        /// <param name="expectedKey">Expected key</param>
        /// <returns>Supported key</returns>
        public static string GenerateRevisionId(string expectedKey)
        {
            // if the expected key length is greater than 20, it is hashed and a fixed length value is stored in the string format 
            if (expectedKey.Length > 20) expectedKey = expectedKey.GetHashCode().ToString();
            var key = Regex.Replace(expectedKey, "[^0-9-.a-zA-Z_=]", "_");
            return key.Substring(key.Length - Math.Min(key.Length, 20));  // the resulting key length is 20 or less
        }

        #endregion

        #region private method

        /// <summary>
        /// Processing document received from the editing service
        /// </summary>
        /// <param name="jsonDocumentResponse">The resulting json from editing service</param>
        /// <param name="responseData">Uri and file type of the converted document</param>
        /// <returns>The percentage of conversion completion</returns>
        private static int GetResponseData(string jsonDocumentResponse, out Dictionary<string, string> responseData)
        {
            if (string.IsNullOrEmpty(jsonDocumentResponse)) throw new ArgumentException("Invalid param", "jsonDocumentResponse");

            var responseFromService = Json.Decode(jsonDocumentResponse);
            if (jsonDocumentResponse == null) throw new WebException("Invalid answer format");

            var errorElement = responseFromService.error;  // if an error occurs
            if (errorElement != null) ProcessResponseError(Convert.ToInt32(errorElement));  // then get an error message

            // check if the conversion is completed and save the result to a variable
            var isEndConvert = responseFromService.endConvert;

            int resultPercent;
            responseData = new Dictionary<string, string>(); 
            var responseUri = string.Empty;
            var responseFileType = string.Empty;
            if (isEndConvert)  // if the conversion is completed
            {
                responseUri = responseFromService.fileUrl;  // get the file url
                responseFileType = responseFromService.fileType;  // get the file type
                responseData.Add("fileUrl", responseUri);
                responseData.Add("fileType", responseFileType);
                resultPercent = 100;
            }
            else  // if the conversion isn't completed
            {
                responseData.Add("fileUrl", "");
                resultPercent = responseFromService.percent;  // get the percentage value
                if (resultPercent >= 100) resultPercent = 99;
            }

            return resultPercent;
        }

        /// <summary>
        /// Generate an error code table
        /// </summary>
        /// <param name="errorCode">Error code</param>
        private static void ProcessResponseError(int errorCode)
        {
            var errorMessage = string.Empty;
            const string errorMessageTemplate = "Error occurred in the ConvertService.ashx: {0}";

            switch (errorCode)
            {
                case -8:
                    // public const int c_nErrorFileVKey = -8;
                    errorMessage = String.Format(errorMessageTemplate, "Error document VKey");
                    break;
                case -7:
                    // public const int c_nErrorFileRequest = -7;
                    errorMessage = String.Format(errorMessageTemplate, "Error document request");
                    break;
                case -6:
                    // public const int c_nErrorDatabase = -6;
                    errorMessage = String.Format(errorMessageTemplate, "Error database");
                    break;
                case -5:
                    // public const int c_nErrorUnexpectedGuid = -5;
                    errorMessage = String.Format(errorMessageTemplate, "Incorrect password");
                    break;
                case -4:
                    // public const int c_nErrorDownloadError = -4;
                    errorMessage = String.Format(errorMessageTemplate, "Error download error");
                    break;
                case -3:
                    // public const int c_nErrorConvertationError = -3;
                    errorMessage = String.Format(errorMessageTemplate, "Error convertation error");
                    break;
                case -2:
                    // public const int c_nErrorConvertationTimeout = -2;
                    errorMessage = String.Format(errorMessageTemplate, "Error convertation timeout");
                    break;
                case -1:
                    // public const int c_nErrorUnknown = -1;
                    errorMessage = String.Format(errorMessageTemplate, "Error convertation unknown");
                    break;
                case 0:
                    // public const int c_nErrorNo = 0;
                    break;
                default:
                    errorMessage = "ErrorCode = " + errorCode;
                    break;
            }

            throw new Exception(errorMessage);
        }

        #endregion
    }
}