/**
 *
 * (c) Copyright Ascensio System SIA 2021
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

var lockDict = {};

function getLockObject(filePath) {
    return lockDict[filePath];
}

function clearLockTimeout(lockObject) {
    if (lockObject && lockObject.timeout) {
        clearTimeout(lockObject.timeout);
    }
}

function getLockValue(filePath) {
    let lock = getLockObject(filePath);
    if (lock) return lock.value;
    return "";
}

function hasLock(filePath) {
    return !!getLockObject(filePath);
}

function lock(filePath, lockValue) {
    let oldLock = getLockObject(filePath);
    clearLockTimeout(oldLock);

    lockDict[filePath] = {
        value: lockValue,
        timeout: setTimeout(unlock, 1000 * 60 * 30, filePath) // set lock for 30 minutes
    }
}

function unlock(filePath) {
    let lock = getLockObject(filePath);
    clearLockTimeout(lock);
    delete lockDict[filePath];
}

module.exports = {
    hasLock: hasLock,
    getLock: getLockValue,
    lock: lock,
    unlock: unlock
}