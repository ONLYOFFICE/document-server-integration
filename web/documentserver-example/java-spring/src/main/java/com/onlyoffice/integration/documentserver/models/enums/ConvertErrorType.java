package com.onlyoffice.integration.documentserver.models.enums;

public enum ConvertErrorType {
    EMPTY_ERROR(0, ""),
    CONVERTATION_UNKNOWN(-1, "Error convertation unknown"),
    CONVERTATION_TIMEOUT(-2, "Error convertation timeout"),
    CONVERTATION_ERROR(-3, "Error convertation error"),
    DOWNLOAD_ERROR(-4, "Error download error"),
    UNEXPECTED_GUID_ERROR(-5, "Error unexpected guid"),
    DATABASE_ERROR(-6, "Error database"),
    DOCUMENT_REQUEST_ERROR(-7, "Error document request"),
    DOCUMENT_VKEY_ERROR(-8, "Error document VKey");

    private final int code;
    private final String label;

    ConvertErrorType(final int codeParam, final String labelParam) {
        this.code = codeParam;
        this.label = labelParam;
    }

    public static String labelOfCode(final int code) {
        for (ConvertErrorType convertErrorType : values()) {
            if (convertErrorType.code == code) {
                return convertErrorType.label;
            }
        }
        return "ErrorCode = " + code;
    }
}
