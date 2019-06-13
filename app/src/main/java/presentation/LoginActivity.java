/*
    LoginActivity.java - Login page functionality
 */
package presentation;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Objects;

import domain.OnLoginListener;
import liveVideoBroadcaster.LiveVideoBroadcasterActivity;
import liveVideoBroadcaster.R;
import logic.LoginManager;

public class LoginActivity extends AppCompatActivity {

    private LoginManager loginManager;
    private OnLoginListener onLoginListener;
    private String username;
    private String password;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        //Change the action bar name
        try {
            getSupportActionBar().setTitle("Seechange Login");
        } catch (Exception e) {
            System.out.println(e);
        }

        this.loginManager = new LoginManager();
        this.onLoginListener = new OnLoginListener() {
            @Override
            public void onSuccess(String token, String username) {
                Intent intent = new Intent(getApplicationContext(), LiveVideoBroadcasterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), "Welkom, " + username, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int code, String message) {
                Snackbar.make(getCurrentFocus(), message, Snackbar.LENGTH_LONG).show();
            }
        };

        final EditText usernameEditText = findViewById(R.id.username);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.login);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                username = usernameEditText.getText().toString().trim();
                password = passwordEditText.getText().toString();

                InputMethodManager inputManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                Objects.requireNonNull(inputManager).hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                loginManager
                        .setLoginDetails(username, password)
                        .setOnLoginListener(onLoginListener)
                        .login();
            }
        });

    }
}

