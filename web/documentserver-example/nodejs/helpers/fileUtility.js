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

const fileUtility = {};

// get file name from the given url
fileUtility.getFileName = function (url, withoutExtension) {
  if (!url) return '';

  let parts = url.split('\\');
  parts = parts.pop();
  parts = parts.split('/');
  let fileName = parts.pop(); // get the file name from the last part of the url
  [fileName] = fileName.split('?');

  // get file name without extension
  if (withoutExtension) {
    return fileName.substring(0, fileName.lastIndexOf('.'));
  }

  return fileName;
};

// get file extension from the given url
fileUtility.getFileExtension = function (url, withoutDot) {
  if (!url) return null;

  const fileName = fileUtility.getFileName(url); // get file name from the given url

  const parts = fileName.toLowerCase().split('.');

  return withoutDot ? parts.pop() : `.${parts.pop()}`; // get the extension from the file name with or without dot
};

// get file type from the given url
fileUtility.getFileType = function (url) {
  const ext = fileUtility.getFileExtension(url); // get the file extension from the given url

  // word type for document extensions
  if (fileUtility.documentExts.indexOf(ext) !== -1) return fileUtility.fileType.word;
  // cell type for spreadsheet extensions
  if (fileUtility.spreadsheetExts.indexOf(ext) !== -1) return fileUtility.fileType.cell;
  // slide type for presentation extensions
  if (fileUtility.presentationExts.indexOf(ext) !== -1) return fileUtility.fileType.slide;

  return fileUtility.fileType.word; // the default file type is word
};

fileUtility.fileType = {
  word: 'word',
  cell: 'cell',
  slide: 'slide'
};

// the document extension list
fileUtility.documentExts = ['.doc', '.docx', '.oform', '.docm', '.dot', '.dotx', '.dotm', '.odt',
  '.fodt', '.ott', '.rtf', '.txt', '.html', '.htm', '.mht', '.xml', '.pdf', '.djvu', '.fb2', '.epub', '.xps', '.oxps'];

// the spreadsheet extension list
fileUtility.spreadsheetExts = ['.xls', '.xlsx', '.xlsm', '.xlsb', '.xlt',
  '.xltx', '.xltm', '.ods', '.fods', '.ots', '.csv'];

// the presentation extension list
fileUtility.presentationExts = ['.pps', '.ppsx', '.ppsm', '.ppt', '.pptx', '.pptm', '.pot',
  '.potx', '.potm', '.odp', '.fodp', '.otp'];

// get url parameters
// eslint-disable-next-line no-unused-vars
const getUrlParams = function (url) {
  try {
    const query = url.split('?').pop(); // take all the parameters which are placed after ? sign in the file url
    const params = query.split('&'); // parameters are separated by & sign
    const map = {}; // write parameters and their values to the map dictionary
    for (let i = 0; i < params.length; i += 1) {
      // eslint-disable-next-line no-undef
      const parts = param.split('=');
      [, map[parts[0]]] = parts;
    }
    return map;
  } catch (ex) {
    return null;
  }
};

// save all the functions to the fileUtility module to export it later in other files
module.exports = fileUtility;
