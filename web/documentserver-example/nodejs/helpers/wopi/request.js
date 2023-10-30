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

// request types
const requestType = Object.freeze({
  None: 0,

  CheckFileInfo: 1,
  PutRelativeFile: 2,

  Lock: 3,
  GetLock: 4,
  Unlock: 5,
  RefreshLock: 6,
  UnlockAndRelock: 7,

  ExecuteCobaltRequest: 8,

  DeleteFile: 9,
  ReadSecureStore: 10,
  GetRestrictedLink: 11,
  RevokeRestrictedLink: 12,

  CheckFolderInfo: 13,

  GetFile: 14,
  PutFile: 16,

  EnumerateChildren: 16,

  RenameFile: 17,
  PutUserInfo: 18,
});

// request headers
const requestHeaders = Object.freeze({
  RequestType: 'X-WOPI-Override',
  ItemVersion: 'X-WOPI-ItemVersion',

  Lock: 'X-WOPI-Lock',
  OldLock: 'X-WOPI-OldLock',
  LockFailureReason: 'X-WOPI-LockFailureReason',
  LockedByOtherInterface: 'X-WOPI-LockedByOtherInterface',

  FileConversion: 'X-WOPI-FileConversion',

  SuggestedTarget: 'X-WOPI-SuggestedTarget',
  RelativeTarget: 'X-WOPI-RelativeTarget',
  OverwriteRelativeTarget: 'X-WOPI-OverwriteRelativeTarget',

  ValidRelativeTarget: 'X-WOPI-ValidRelativeTarget',
});

module.exports = {
  requestType,
  requestHeaders,
};
