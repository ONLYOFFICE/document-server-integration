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