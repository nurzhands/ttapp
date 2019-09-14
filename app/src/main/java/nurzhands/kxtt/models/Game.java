package nurzhands.kxtt.models;

public class Game {
    private String firstName;
    private String secondName;
    private String firstUid;
    private String secondUid;
    private int firstGames;
    private int secondGames;
    private String secondToken;
    private long timestamp;

    public Game() {}

    public Game(String firstName, String secondName, String firstUid, String secondUid, int firstGames, int secondGames, String secondToken) {
        this.firstName = firstName;
        this.secondName = secondName;
        this.firstUid = firstUid;
        this.secondUid = secondUid;
        this.firstGames = firstGames;
        this.secondGames = secondGames;
        this.secondToken = secondToken;
    }

    public int getFirstGames() {
        return firstGames;
    }

    public void setFirstGames(int firstGames) {
        this.firstGames = firstGames;
    }

    public int getSecondGames() {
        return secondGames;
    }

    public void setSecondGames(int secondGames) {
        this.secondGames = secondGames;
    }

    public String getFirstUid() {
        return firstUid;
    }

    public void setFirstUid(String firstUid) {
        this.firstUid = firstUid;
    }

    public String getSecondUid() {
        return secondUid;
    }

    public void setSecondUid(String secondUid) {
        this.secondUid = secondUid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public String getSecondToken() {
        return secondToken;
    }

    public void setSecondToken(String secondToken) {
        this.secondToken = secondToken;
    }
}
