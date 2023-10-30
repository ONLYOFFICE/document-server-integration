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

const supportedFormats = require('../public/assets/document-formats/onlyoffice-docs-formats.json'); // eslint-disable-line

const fileUtility = {};

// get file name from the given url
fileUtility.getFileName = function getFileName(url, withoutExtension) {
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
fileUtility.getFileExtension = function getFileExtension(url, withoutDot) {
  if (!url) return null;

  const fileName = fileUtility.getFileName(url); // get file name from the given url

  const parts = fileName.toLowerCase().split('.');

  return withoutDot ? parts.pop() : `.${parts.pop()}`; // get the extension from the file name with or without dot
};

// get file type from the given url
fileUtility.getFileType = function getFileType(url) {
  const ext = fileUtility.getFileExtension(url, true); // get the file extension from the given url

  for (let i = 0; i < supportedFormats.length; i++) {
    if (supportedFormats[i].name === ext) return supportedFormats[i].type;
  }

  return fileUtility.fileType.word; // the default file type is word
};

fileUtility.fileType = {
  word: 'word',
  cell: 'cell',
  slide: 'slide',
};

fileUtility.getSuppotredExtensions = function getSuppotredExtensions() {
  return supportedFormats.reduce((extensions, format) => [...extensions, format.name], []);
};

fileUtility.getViewExtensions = function getViewExtensions() {
  return supportedFormats.filter(
    (format) => format.actions.includes('view'),
  ).reduce((extensions, format) => [...extensions, format.name], []);
};

fileUtility.getEditExtensions = function getEditExtensions() {
  return supportedFormats.filter(
    (format) => format.actions.includes('edit') || format.actions.includes('lossy-edit'),
  ).reduce((extensions, format) => [...extensions, format.name], []);
};

fileUtility.getFillExtensions = function getFillExtensions() {
  return supportedFormats.filter(
    (format) => format.actions.includes('fill'),
  ).reduce((extensions, format) => [...extensions, format.name], []);
};

fileUtility.getConvertExtensions = function getConvertExtensions() {
  return supportedFormats.filter(
    (format) => format.actions.includes('auto-convert'),
  ).reduce((extensions, format) => [...extensions, format.name], []);
};

// get url parameters
// eslint-disable-next-line no-unused-vars
const getUrlParams = function getUrlParams(url) {
  try {
    const query = url.split('?').pop(); // take all the parameters which are placed after ? sign in the file url
    const params = query.split('&'); // parameters are separated by & sign
    const map = {}; // write parameters and their values to the map dictionary
    for (let i = 0; i < params.length; i++) {
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
