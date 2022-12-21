package utils;

public enum StatusType {

    EDITING(1),
    MUST_SAVE(2),
    CORRUPTED(3),
    MUST_FORCE_SAVE(6),
    CORRUPTED_FORCE_SAVE(7);
    private final int code;

    StatusType(final int codeParam) {
        this.code = codeParam;
    }

    public int getCode() {
       return code;
    }
}
