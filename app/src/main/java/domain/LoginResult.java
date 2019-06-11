/*
    LoginResult.java - Holds loginresult data
 */
package domain;

public class LoginResult {
    private int code;
    private String message;
    private String username;
    private String token;

    //Constructor
    public LoginResult(int code) {
        this.code = code;
    }

    //Get
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    //Set
    public void setMessage(String message) {
        this.message = message;
    }

    public LoginResult setToken(String token) {
        this.token = token;
        return this;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
