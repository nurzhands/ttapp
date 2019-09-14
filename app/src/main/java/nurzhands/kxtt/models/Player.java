package nurzhands.kxtt.models;

public class Player {
    private String name;
    private String photoUrl;
    private long timestamp;

    public Player() {}

    public Player(String name, String photoUrl, long timestamp) {
        this.name = name;
        this.photoUrl = photoUrl;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
