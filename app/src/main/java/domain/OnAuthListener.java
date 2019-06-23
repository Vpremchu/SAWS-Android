package domain;

public interface OnAuthListener {
    void onSuccess(String certificate, String privateKey, String publicKey);
    void onFailure(int code, String message);
}
