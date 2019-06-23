/*
    LoginResult.java - Holds loginresult data
 */
package domain;

public class AuthResult {
    private int code;
    private String message;
    private String certificate;
    private String privateKey;
    private String publicKey;

    //Constructor
    public AuthResult(int code) {
        this.code = code;
    }

    //Get
    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getCertificate() {
        return certificate;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    //Set
    public void setMessage(String message) {
        this.message = message;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
