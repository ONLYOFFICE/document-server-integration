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

using System;
using System.IO;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Web.Configuration;
using System.Web.Helpers;

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
            DocumentConverterUrl = WebConfigurationManager.AppSettings["files.docservice.url.converter"] ?? "";

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
        /// <param name="convertedDocumentUri">Uri to the converted document</param>
        /// <returns>The percentage of completion of conversion</returns>
        /// <example>
        /// string convertedDocumentUri;
        /// GetConvertedUri("http://helpcenter.onlyoffice.com/content/GettingStarted.pdf", ".pdf", ".docx", "http://helpcenter.onlyoffice.com/content/GettingStarted.pdf", false, out convertedDocumentUri);
        /// </example>
        /// <exception>
        /// </exception>
        public static int GetConvertedUri(string documentUri,
                                          string fromExtension,
                                          string toExtension,
                                          string documentRevisionId,
                                          bool isAsync,
                                          out string convertedDocumentUri)
        {
            convertedDocumentUri = string.Empty;

            fromExtension = string.IsNullOrEmpty(fromExtension) ? Path.GetExtension(documentUri) : fromExtension;

            var title = Path.GetFileName(documentUri);
            title = string.IsNullOrEmpty(title) ? Guid.NewGuid().ToString() : title;

            documentRevisionId = string.IsNullOrEmpty(documentRevisionId)
                                     ? documentUri
                                     : documentRevisionId;
            documentRevisionId = GenerateRevisionId(documentRevisionId);

            var request = (HttpWebRequest)WebRequest.Create(DocumentConverterUrl);
            request.Method = "POST";
            request.ContentType = "application/json";
            request.Accept = "application/json";
            request.Timeout = ConvertTimeout;

            var bodyString = string.Format("{{\"async\": {0},\"filetype\": \"{1}\",\"key\": \"{2}\",\"outputtype\": \"{3}\",\"title\": \"{4}\",\"url\": \"{5}\"}}",
                                           isAsync.ToString().ToLower(),
                                           fromExtension.Trim('.'),
                                           documentRevisionId,
                                           toExtension.Trim('.'),
                                           title,
                                           documentUri);

            var bytes = Encoding.UTF8.GetBytes(bodyString);
            request.ContentLength = bytes.Length;
            using (var requestStream = request.GetRequestStream())
            {
                requestStream.Write(bytes, 0, bytes.Length);
            }

            string dataResponse;
            using (var response = request.GetResponse())
            using (var stream = response.GetResponseStream())
            {
                if (stream == null) throw new Exception("Response is null");

                using (var reader = new StreamReader(stream))
                {
                    dataResponse = reader.ReadToEnd();
                }
            }

            return GetResponseUri(dataResponse, out convertedDocumentUri);
        }

        /// <summary>
        /// Translation key to a supported form.
        /// </summary>
        /// <param name="expectedKey">Expected key</param>
        /// <returns>Supported key</returns>
        public static string GenerateRevisionId(string expectedKey)
        {
            if (expectedKey.Length > 20) expectedKey = expectedKey.GetHashCode().ToString();
            var key = Regex.Replace(expectedKey, "[^0-9-.a-zA-Z_=]", "_");
            return key.Substring(key.Length - Math.Min(key.Length, 20));
        }

        #endregion

        #region private method

        /// <summary>
        /// Processing document received from the editing service
        /// </summary>
        /// <param name="jsonDocumentResponse">The resulting json from editing service</param>
        /// <param name="responseUri">Uri to the converted document</param>
        /// <returns>The percentage of completion of conversion</returns>
        private static int GetResponseUri(string jsonDocumentResponse, out string responseUri)
        {
            if (string.IsNullOrEmpty(jsonDocumentResponse)) throw new ArgumentException("Invalid param", "jsonDocumentResponse");

            var responseFromService = Json.Decode(jsonDocumentResponse);
            if (jsonDocumentResponse == null) throw new WebException("Invalid answer format");

            var errorElement = responseFromService.error;
            if (errorElement != null) ProcessResponseError(Convert.ToInt32(errorElement));

            var isEndConvert = responseFromService.endConvert;

            int resultPercent;
            responseUri = string.Empty;
            if (isEndConvert)
            {
                responseUri = responseFromService.fileUrl;
                resultPercent = 100;
            }
            else
            {
                resultPercent = responseFromService.percent;
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
                    errorMessage = String.Format(errorMessageTemplate, "Error unexpected guid");
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