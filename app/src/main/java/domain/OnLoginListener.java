/*
    OnLoginListener.java - handles response of login task
 */
package domain;

    public interface OnLoginListener {
        void onSuccess(String token, String name);
        void onFailure(int code, String message);

    }

