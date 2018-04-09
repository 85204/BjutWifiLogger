package com.android.bjutwifilogger;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;

import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity {

    /**
     * A dummy authentication store containing known user names and passwords.
     */
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private WifiManager wifiManager;
    private WifiInfo wifiInfo;

    static final String USER_NAME = "USER_NAME";
    static final String PASS_WORD = "PASS_WORD";
    //    static final String FIRST_RUN = "FIRST_RUN";
    static final String AUTO_LOGIN = "AUTO_LOGIN";
    static final String SAVE_PASSWORD = "SAVE_PASSWORD";

    private String loginType;


    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private CheckBox mAutoLoginView;
    private CheckBox mSavePasswordView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getPreferences(Activity.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        setContentView(R.layout.activity_login);
        // Set up the login for
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mEmailView.setText(sharedPreferences.getString(USER_NAME, ""));
        mPasswordView.setText(sharedPreferences.getString(PASS_WORD, ""));

        mSavePasswordView = (CheckBox) findViewById(R.id.save_password_checkbox);
        mSavePasswordView.setChecked(sharedPreferences.getBoolean(SAVE_PASSWORD, false));
        mSavePasswordView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    mAutoLoginView.setChecked(false);
                }
            }
        });
        mAutoLoginView = (CheckBox) findViewById(R.id.auto_login_checkbox);
        mAutoLoginView.setChecked(sharedPreferences.getBoolean(AUTO_LOGIN, false));
        mAutoLoginView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSavePasswordView.setChecked(true);
                }
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        if (mEmailSignInButton != null) {
            mEmailSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptLogin();
                }
            });
        }

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        if (sharedPreferences.getBoolean(AUTO_LOGIN, false)) {
            attemptLogin();
        }

    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        wifiInfo = wifiManager.getConnectionInfo();
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "请打开wifi", Toast.LENGTH_SHORT).show();
            return;
        } else if (wifiInfo.getSSID().contains("bjut_wifi")) {
            loginType = "bjutwifi";
        } else if (wifiInfo.getSSID().contains("Tushuguan")) {
            loginType = "tushuguan";
        } else {
            Toast.makeText(getApplicationContext(), "请连接校园wifi", Toast.LENGTH_SHORT).show();
        }

        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_password_required));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_username_required));
            focusView = mEmailView;
            cancel = true;
//        } else if (!isEmailValid(email)) {
//            mEmailView.setError(getString(R.string.error_invalid_email));
//            focusView = mEmailView;
//            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


    }

//    private boolean isEmailValid(String email) {
//        return email.contains("@");
//    }

//    private boolean isPasswordValid(String password) {
//        return password.length() > 4;
//    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Integer> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (loginType.equals("bjutwifi")) {

                try {
                    URL url = new URL("https://wlgn.bjut.edu.cn/");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");

                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream(), "utf-8");
                    BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                    bufferedWriter.write("DDDDD=" + mEmail + "&upass=" + mPassword + "&6MKKey=123");
                    bufferedWriter.flush();

                    InputStream inputStream = connection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        System.out.println(line);
                        if (line.contains("You have successfully logged in")) {
                            return 0;
                        } else if (line.contains("msga='In use !'")) {
                            return 1;
                        } else if (line.contains("msga='ldap auth error'")) {
                            return 2;
                        }
                    }
                    bufferedReader.close();
                    inputStreamReader.close();
                    inputStream.close();

                    // Simulate network access.
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (loginType.equals("tushuguan")) {
                try {
                    URL url = new URL("http://172.24.39.253/portal/logon.cgi");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");

                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(connection.getOutputStream(), "utf-8");
                    BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
                    bufferedWriter.write("PtUser=" + mEmail + "&PtPwd=" + mPassword + "&PtButton=%B5%C7%C2%BC");
                    bufferedWriter.flush();

                    InputStream inputStream = connection.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        System.out.println(line);
                        if (line.contains("Authentication passed!")) {
                            break;
                        } else if (line.contains("This user has connected!")) {
                            return 1;
                        } else if (line.contains("Authentication failed!")) {
                            return 2;
                        }
                    }

                    url = new URL("https://lgn.bjut.edu.cn/");
                    connection = (HttpURLConnection) url.openConnection();

                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestMethod("POST");

                    outputStreamWriter = new OutputStreamWriter(connection.getOutputStream(), "utf-8");
                    bufferedWriter = new BufferedWriter(outputStreamWriter);

                    bufferedWriter.write("DDDDD=" + mEmail + "&upass=" + mPassword + "&v46s=1&v6ip=&0MKKey=");
                    bufferedWriter.flush();
                    inputStream = connection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream, "utf-8");
                    bufferedReader = new BufferedReader(inputStreamReader);
                    while ((line = bufferedReader.readLine()) != null) {
                        System.out.println(line);
                        if (line.contains("You have successfully logged in")) {
                            return 0;
                        } else if (line.contains("msga='In use !'")) {
                            return 1;
                        } else if (line.contains("msga='ldap auth error'")) {
                            return 2;
                        }
                    }

                    bufferedReader.close();
                    inputStreamReader.close();
                    inputStream.close();

                    // Simulate network access.
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            return -1;

        }

        @Override
        protected void onPostExecute(final Integer success) {
            mAuthTask = null;
            showProgress(false);

            switch (success) {
                case 0:
                    editor.putString(USER_NAME, mEmail);
                    if (mSavePasswordView.isChecked()) {
                        editor.putString(PASS_WORD, mPassword);
                    } else {
                        editor.putString(PASS_WORD, "");
                    }
                    editor.putBoolean(SAVE_PASSWORD, mSavePasswordView.isChecked());
                    editor.putBoolean(AUTO_LOGIN, mAutoLoginView.isChecked());
                    editor.commit();
                    Toast.makeText(getApplicationContext(), R.string.login_success, Toast.LENGTH_LONG).show();
                    finish();
                    break;
                case 1:
                    mPasswordView.setError(getString(R.string.error_in_use));
                    mPasswordView.requestFocus();
                    break;
                case 2:
                    mPasswordView.setError(getString(R.string.error_incorrect_password));
                    mPasswordView.requestFocus();
                    break;
                default:
                    mPasswordView.setError(getString(R.string.unknown_error));
                    mPasswordView.requestFocus();
                    break;
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
