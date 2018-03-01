<?php
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
?>

<?php

require_once( dirname(__FILE__) . '/config.php' );


function DoUpload($fileUri) {
    $_fileName = GetCorrectName($fileUri);

    $ext = strtolower('.' . pathinfo($_fileName, PATHINFO_EXTENSION));
    if (!in_array($ext, getFileExts()))
    {
        throw new Exception("File type is not supported");
    }

    if(!@copy($fileUri, getStoragePath($_fileName)))
    {
        $errors= error_get_last();
        $err = "Copy file error: " . $errors['type'] . "<br />\n" . $errors['message'];
        throw new Exception($err);
    }

    return $_fileName;
}


/**
* Generate an error code table
*
* @param string $errorCode   Error code
*
* @return null
*/
function ProcessConvServResponceError($errorCode) {
    $errorMessageTemplate = "Error occurred in the document service: ";
    $errorMessage = '';

    switch ($errorCode)
    {
        case -8:
            $errorMessage = $errorMessageTemplate . "Error document VKey";
            break;
        case -7:
            $errorMessage = $errorMessageTemplate . "Error document request";
            break;
        case -6:
            $errorMessage = $errorMessageTemplate . "Error database";
            break;
        case -5:
            $errorMessage = $errorMessageTemplate . "Error unexpected guid";
            break;
        case -4:
            $errorMessage = $errorMessageTemplate . "Error download error";
            break;
        case -3:
            $errorMessage = $errorMessageTemplate . "Error convertation error";
            break;
        case -2:
            $errorMessage = $errorMessageTemplate . "Error convertation timeout";
            break;
        case -1:
            $errorMessage = $errorMessageTemplate . "Error convertation unknown";
            break;
        case 0:
            break;
        default:
            $errorMessage = $errorMessageTemplate . "ErrorCode = " . $errorCode;
            break;
    }

    throw new Exception($errorMessage);
}


/**
* Translation key to a supported form.
*
* @param string $expected_key  Expected key
*
* @return Supported key
*/
function GenerateRevisionId($expected_key) {
    if (strlen($expected_key) > 20) $expected_key = crc32( $expected_key);
    $key = preg_replace("[^0-9-.a-zA-Z_=]", "_", $expected_key);
    $key = substr($key, 0, min(array(strlen($key), 20)));
    return $key;
}


/**
* Request for conversion to a service
*
* @param string $document_uri            Uri for the document to convert
* @param string $from_extension          Document extension
* @param string $to_extension            Extension to which to convert
* @param string $document_revision_id    Key for caching on service
* @param bool   $is_async                Perform conversions asynchronously
*
* @return Document request result of conversion
*/
function SendRequestToConvertService($document_uri, $from_extension, $to_extension, $document_revision_id, $is_async) {
    if (empty($from_extension))
    {
        $path_parts = pathinfo($document_uri);
        $from_extension = $path_parts['extension'];
    }

    $title = basename($document_uri);
    if (empty($title)) {
        $title = guid();
    }

    if (empty($document_revision_id)) {
        $document_revision_id = $document_uri;
    }

    $document_revision_id = GenerateRevisionId($document_revision_id);

    $urlToConverter = $GLOBALS['DOC_SERV_CONVERTER_URL'];

    $data = json_encode(
        array(
            "async" => $is_async,
            "url" => $document_uri,
            "outputtype" => trim($to_extension,'.'),
            "filetype" => trim($from_extension, '.'),
            "title" => $title,
            "key" => $document_revision_id
        )
    );

    $opts = array('http' => array(
                'method'  => 'POST',
                'timeout' => $GLOBALS['DOC_SERV_TIMEOUT'],
                'header'=> "Content-type: application/json\r\n" . 
                            "Accept: application/json\r\n",
                'content' => $data
            )
        );

    if (substr($urlToConverter, 0, strlen("https")) === "https") {
        $opts['ssl'] = array( 'verify_peer'   => FALSE );
    }
 
    $context  = stream_context_create($opts);
    $response_data = file_get_contents($urlToConverter, FALSE, $context);

    return $response_data;
}


/**
* The method is to convert the file to the required format
*
* Example:
* string convertedDocumentUri;
* GetConvertedUri("http://helpcenter.onlyoffice.com/content/GettingStarted.pdf", ".pdf", ".docx", "http://helpcenter.onlyoffice.com/content/GettingStarted.pdf", false, out convertedDocumentUri);
* 
* @param string $document_uri            Uri for the document to convert
* @param string $from_extension          Document extension
* @param string $to_extension            Extension to which to convert
* @param string $document_revision_id    Key for caching on service
* @param bool   $is_async                Perform conversions asynchronously
* @param string $converted_document_uri  Uri to the converted document
*
* @return The percentage of completion of conversion
*/
function GetConvertedUri($document_uri, $from_extension, $to_extension, $document_revision_id, $is_async, &$converted_document_uri) {
    $converted_document_uri = "";
    $responceFromConvertService = SendRequestToConvertService($document_uri, $from_extension, $to_extension, $document_revision_id, $is_async);

    $errorElement = $responceFromConvertService->Error;
    if ($errorElement != NULL && $errorElement != "") ProcessConvServResponceError($errorElement);

    $isEndConvert = $responceFromConvertService->EndConvert;
    $percent = $responceFromConvertService->Percent . "";

    if ($isEndConvert != NULL && strtolower($isEndConvert) == "true")
    {
        $converted_document_uri = $responceFromConvertService->FileUrl;
        $percent = 100;
    }
    else if ($percent >= 100)
        $percent = 99;

    return $percent;
}


/**
* Processing document received from the editing service.
*
* @param string $document_response     The result from editing service
* @param string $response_uri          Uri to the converted document
*
* @return The percentage of completion of conversion
*/
function GetResponseUri($document_response, &$response_uri) {
    $response_uri = "";
    $resultPercent = 0;

    if (!$document_response) {
        $errs = "Invalid answer format";
    }

    $errorElement = $document_response->Error;
    if ($errorElement != NULL && $errorElement != "") ProcessConvServResponceError($document_response->Error);

    $endConvert = $document_response->EndConvert;
    if ($endConvert != NULL && $endConvert == "") throw new Exception("Invalid answer format");

    if ($endConvert != NULL && strtolower($endConvert) == true)
    {
        $fileUrl = $document_response->FileUrl;
        if ($fileUrl == NULL || $fileUrl == "") throw new Exception("Invalid answer format");

        $response_uri = $fileUrl;
        $resultPercent = 100;
    }
    else
    {
        $percent = $document_response->Percent;

        if ($percent != NULL && $percent != "")
            $resultPercent = $percent;
        if ($resultPercent >= 100)
            $resultPercent = 99;
    }

    return $resultPercent;
}

?>