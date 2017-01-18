/*
 *
 * (c) Copyright Ascensio System Limited 2010-2017
 *
 * This program is freeware. You can redistribute it and/or modify it under the terms of the GNU 
 * General Public License (GPL) version 3 as published by the Free Software Foundation (https://www.gnu.org/copyleft/gpl.html). 
 * In accordance with Section 7(a) of the GNU GPL its Section 15 shall be amended to the effect that 
 * Ascensio System SIA expressly excludes the warranty of non-infringement of any third-party rights.
 *
 * THIS PROGRAM IS DISTRIBUTED WITHOUT ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR
 * FITNESS FOR A PARTICULAR PURPOSE. For more details, see GNU GPL at https://www.gnu.org/copyleft/gpl.html
 *
 * You can contact Ascensio System SIA by email at sales@onlyoffice.com
 *
 * The interactive user interfaces in modified source and object code versions of ONLYOFFICE must display 
 * Appropriate Legal Notices, as required under Section 5 of the GNU GPL version 3.
 *
 * Pursuant to Section 7 § 3(b) of the GNU GPL you must retain the original ONLYOFFICE logo which contains 
 * relevant author attributions when distributing the software. If the display of the logo in its graphic 
 * form is not reasonably feasible for technical reasons, you must include the words "Powered by ONLYOFFICE" 
 * in every copy of the program you distribute. 
 * Pursuant to Section 7 § 3(e) we decline to grant you any rights under trademark law for use of our trademarks.
 *
*/

using System;
using System.IO;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Web;
using System.Web.Configuration;
using System.Xml;
using System.Xml.Linq;

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
            DocumentStorageUrl = WebConfigurationManager.AppSettings["files.docservice.url.storage"] ?? "";

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

        /// <summary>
        /// Url to the service of storage
        /// </summary>
        private static readonly string DocumentStorageUrl;

        /// <summary>
        /// The parameters for the query conversion
        /// </summary>
        private const string ConvertParams = "?url={0}&outputtype={1}&filetype={2}&title={3}&key={4}";

        /// <summary>
        /// Number of tries request conversion
        /// </summary>
        private const int MaxTry = 3;

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
            var responceFromConvertService =
                SendRequestToConvertService(documentUri, fromExtension, toExtension, documentRevisionId, isAsync)
                    .Root;

            var errorElement = responceFromConvertService.Element("Error");
            if (errorElement != null)
                ProcessConvertServiceResponceError(Convert.ToInt32(errorElement.Value));

            var isEndConvert = Convert.ToBoolean(responceFromConvertService.Element("EndConvert").Value);
            var percent = Convert.ToInt32(responceFromConvertService.Element("Percent").Value);

            if (isEndConvert)
            {
                convertedDocumentUri = responceFromConvertService.Element("FileUrl").Value;
                percent = 100;
            }
            else
            {
                percent = percent >= 100 ? 99 : percent;
            }

            return percent;
        }

        /// <summary>
        /// Placing the document in the storage service
        /// </summary>
        /// <param name="fileStream">Stream of document</param>
        /// <param name="contentLength">Length of stream</param>
        /// <param name="contentType">Mime type</param>
        /// <param name="documentRevisionId">Key for caching on service, whose used in editor</param>
        /// <returns>Uri to document in the storage</returns>
        public static string GetExternalUri(
            Stream fileStream,
            long contentLength,
            string contentType,
            string documentRevisionId)
        {
            var urlDocumentService = DocumentStorageUrl + ConvertParams;
            var urlTostorage = String.Format(urlDocumentService,
                                             string.Empty,
                                             string.Empty,
                                             string.Empty,
                                             string.Empty,
                                             documentRevisionId);

            var request = (HttpWebRequest)WebRequest.Create(urlTostorage);
            request.Method = "POST";
            request.ContentType = contentType;
            request.ContentLength = contentLength;

            const int bufferSize = 2048;
            var buffer = new byte[bufferSize];
            int readed;
            while ((readed = fileStream.Read(buffer, 0, bufferSize)) > 0)
            {
                request.GetRequestStream().Write(buffer, 0, readed);
            }

            using (var response = request.GetResponse())
            using (var stream = response.GetResponseStream())
            {
                if (stream == null) throw new WebException("Could not get an answer");
                var xDocumentResponse = XDocument.Load(new XmlTextReader(stream));
                string externalUri;
                GetResponseUri(xDocumentResponse, out externalUri);
                return externalUri;
            }
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
        /// Request for conversion to a service
        /// </summary>
        /// <param name="documentUri">Uri for the document to convert</param>
        /// <param name="fromExtension">Document extension</param>
        /// <param name="toExtension">Extension to which to convert</param>
        /// <param name="documentRevisionId">Key for caching on service</param>
        /// <param name="isAsync">Perform conversions asynchronously</param>
        /// <returns>Xml document request result of conversion</returns>
        private static XDocument SendRequestToConvertService(string documentUri, string fromExtension, string toExtension, string documentRevisionId, bool isAsync)
        {
            fromExtension = string.IsNullOrEmpty(fromExtension) ? Path.GetExtension(documentUri) : fromExtension;

            var title = Path.GetFileName(documentUri);
            title = string.IsNullOrEmpty(title) ? Guid.NewGuid().ToString() : title;

            documentRevisionId = string.IsNullOrEmpty(documentRevisionId)
                                     ? documentUri
                                     : documentRevisionId;
            documentRevisionId = GenerateRevisionId(documentRevisionId);

            var req = (HttpWebRequest)WebRequest.Create(DocumentConverterUrl);
            req.Method = "POST";
            req.ContentType = "text/json";
            req.Timeout = ConvertTimeout;

            var bodyString = string.Format("{{\"async\": {0},\"filetype\": \"{1}\",\"key\": \"{2}\",\"outputtype\": \"{3}\",\"title\": \"{4}\",\"url\": \"{5}\"}}",
                                           isAsync.ToString().ToLower(),
                                           fromExtension.Trim('.'),
                                           documentRevisionId,
                                           toExtension.Trim('.'),
                                           title,
                                           documentUri);

            var bytes = Encoding.UTF8.GetBytes(bodyString);
            req.ContentLength = bytes.Length;
            using (var requestStream = req.GetRequestStream())
            {
                requestStream.Write(bytes, 0, bytes.Length);
            }

            Stream stream = null;
            var countTry = 0;
            while (countTry < MaxTry)
            {
                try
                {
                    countTry++;
                    stream = req.GetResponse().GetResponseStream();
                    break;
                }
                catch (WebException ex)
                {
                    if (ex.Status != WebExceptionStatus.Timeout)
                    {
                        throw new HttpException((int) HttpStatusCode.BadRequest, "Bad Request", ex);
                    }
                }
            }
            if (countTry == MaxTry)
            {
                throw new WebException("Timeout", WebExceptionStatus.Timeout);
            }

            return XDocument.Load(new XmlTextReader(stream));
        }

        /// <summary>
        /// Generate an error code table
        /// </summary>
        /// <param name="errorCode">Error code</param>
        private static void ProcessConvertServiceResponceError(int errorCode)
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

        /// <summary>
        /// Processing document received from the editing service
        /// </summary>
        /// <param name="xDocumentResponse">The resulting xml from editing service</param>
        /// <param name="responseUri">Uri to the converted document</param>
        /// <returns>The percentage of completion of conversion</returns>
        private static int GetResponseUri(XDocument xDocumentResponse, out string responseUri)
        {
            var responceFromConvertService = xDocumentResponse.Root;
            if (responceFromConvertService == null) throw new WebException("Invalid answer format");

            var errorElement = responceFromConvertService.Element("Error");
            if (errorElement != null) ProcessConvertServiceResponceError(Convert.ToInt32(errorElement.Value));

            var endConvert = responceFromConvertService.Element("EndConvert");
            if (endConvert == null) throw new WebException("Invalid answer format");
            var isEndConvert = Convert.ToBoolean(endConvert.Value);

            var resultPercent = 0;
            responseUri = string.Empty;
            if (isEndConvert)
            {
                var fileUrl = responceFromConvertService.Element("FileUrl");
                if (fileUrl == null) throw new WebException("Invalid answer format");

                responseUri = fileUrl.Value;
                resultPercent = 100;
            }
            else
            {
                var percent = responceFromConvertService.Element("Percent");
                if (percent != null)
                    resultPercent = Convert.ToInt32(percent.Value);
                resultPercent = resultPercent >= 100 ? 99 : resultPercent;
            }

            return resultPercent;
        }

        #endregion
    }
}