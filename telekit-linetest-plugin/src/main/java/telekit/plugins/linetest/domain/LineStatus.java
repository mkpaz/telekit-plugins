package telekit.plugins.linetest.domain;

public enum LineStatus {

    UNKNOWN(""),
    ON_HOOK("on-hook"),
    OFF_HOOK("off-hook"),
    RINGING("ringing"),
    CONNECTED("connected");

    private final String title;

    LineStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}