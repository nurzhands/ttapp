package nurzhands.kxtt.models;

public class Media {
    private String url;
    private boolean isVideo;
    private long timestamp;
    private String owner;
    private String ownerId;

    public Media() {}

    public Media(String url, boolean isVideo, String owner, String ownerId) {
        this.url = url;
        this.isVideo = isVideo;
        this.owner = owner;
        this.ownerId = ownerId;
        this.timestamp = System.currentTimeMillis();
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

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
}
