const requestType = Object.freeze({
    "None": 0,

    "CheckFileInfo": 1,
    "PutRelativeFile": 2,

    "Lock": 3,
    "GetLock": 4,
    "Unlock": 5,
    "RefreshLock": 6,
    "UnlockAndRelock": 7,

    "ExecuteCobaltRequest": 8,

    "DeleteFile": 9,
    "ReadSecureStore": 10,
    "GetRestrictedLink": 11,
    "RevokeRestrictedLink": 12,

    "CheckFolderInfo": 13,

    "GetFile": 14,
    "PutFile": 16,

    "EnumerateChildren": 16,

    "RenameFile": 17,
    "PutUserInfo": 18,
});

const requestHeaders = Object.freeze({
    "RequestType": "X-WOPI-Override",
    "ItemVersion": "X-WOPI-ItemVersion",

    "Lock": "X-WOPI-Lock",
    "OldLock": "X-WOPI-OldLock",
    "LockFailureReason": "X-WOPI-LockFailureReason",
    "LockedByOtherInterface": "X-WOPI-LockedByOtherInterface",

    "SuggestedTarget": "X-WOPI-SuggestedTarget",
    "RelativeTarget": "X-WOPI-RelativeTarget",
    "OverwriteRelativeTarget": "X-WOPI-OverwriteRelativeTarget",
});

module.exports = {
    requestType: requestType,
    requestHeaders: requestHeaders,
}