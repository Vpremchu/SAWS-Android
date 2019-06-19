package domain;

public class MessageIO {

    private String username;
    private String message;
    private int color;

    public MessageIO(String username, String message, int color) {
        this.username = username;
        this.message = message;
        this.color = color;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
