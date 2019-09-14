package nurzhands.kxtt.models;

public class Media {
    private String url;
    private boolean isVideo;
    private long timestamp;
    private String owner;

    public Media() {}

    public Media(String url, boolean isVideo, String owner) {
        this.url = url;
        this.isVideo = isVideo;
        this.timestamp = System.currentTimeMillis();
        this.owner = owner;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isVideo() {
        return isVideo;
    }

    public void setVideo(boolean video) {
        isVideo = video;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
