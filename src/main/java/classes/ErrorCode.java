package classes;

public enum ErrorCode {
    HOSTED_NETWORK_START(0, "can't create a JShare Network."),
    HOSTED_NETWORK_STOP(1, "can't stop JShare Network.");

    private final int code;
    private final String description;

    private ErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return code + ": " + description;
    }
}
