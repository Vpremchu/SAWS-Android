package domain;

public class Profile {
    private String imageUrl;
    private String userName;
    private int satoshiCount;

    public Profile(String imageUrl, String userName, int satoshiCount) {
        this.imageUrl = imageUrl;
        this.userName = userName;
        this.satoshiCount = satoshiCount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getSatoshiCount() {
        return satoshiCount;
    }

    public void setSatoshiCount(int satoshiCount) {
        this.satoshiCount = satoshiCount;
    }
}
