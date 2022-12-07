package com.onlyoffice.integration.documentserver.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    public static final Integer MAX_FILE_SIZE = 5 * 1024 * 1024;
    public static final Integer CONVERT_TIMEOUT_MS = 120000;
    public static final String CONVERTATION_ERROR_MESSAGE_TEMPLATE = "Error occurred in the ConvertService: ";
    public static final Long PERCENT_99 = 99L;
    public static final Long PERCENT_100 = 100L;
    public static final Integer FILE_SAVE_TIMEOUT = 5000;
    public static final Integer KILOBYTE = 1024;
    public static final Integer KEY_LENGHT = 20;
    public static final Integer FOUR = 4;
    public static final Integer SEVEN = 4;
}
