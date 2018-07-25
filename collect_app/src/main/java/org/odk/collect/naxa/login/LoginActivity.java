package org.odk.collect.naxa.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;


import org.odk.collect.android.R;
import org.odk.collect.android.activities.CollectAbstractActivity;
import org.odk.collect.naxa.common.DialogFactory;
import org.odk.collect.naxa.network.APIEndpoint;
import org.odk.collect.naxa.onboarding.DownloadActivity;

import static org.odk.collect.android.application.Collect.allowClick;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends CollectAbstractActivity implements LoginView {

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    private LoginPresenter loginPresenter;
    private Button mEmailSignInButton;
    private RelativeLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        rootLayout = findViewById(R.id.root_layout_activity_login);

        mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {
                if(allowClick()){
                    hideKeyboardInActivity(LoginActivity.this);
                    attemptLogin();
                }
            }
        });

        mLoginFormView = findViewById(R.id.logo);
        mProgressView = findViewById(R.id.login_progress);


        findViewById(R.id.tv_forgot_pwd).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (allowClick()) {
                    String url = APIEndpoint.PASSWORD_RESET;
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);

                }
            }
        });

        loginPresenter = new LoginPresenterImpl(this);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();


        loginPresenter.validateCredentials(email, password);

    }

    @Override
    public void showProgress(final boolean show) {

        runOnUiThread(() -> {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_longAnimTime);

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

            mEmailSignInButton.setEnabled(!show);
        });

    }

    @Override
    public void showPasswordError(int resourceId) {
        mPasswordView.setError(getString(resourceId));
        mPasswordView.requestFocus();

    }

    @Override
    public void showUsernameError(int resourceId) {
        mEmailView.setError(getString(resourceId));
        mEmailView.requestFocus();
    }

    @Override
    public void successAction() {
        Toast.makeText(this, "Logged In!", Toast.LENGTH_SHORT).show();
        DownloadActivity.start(LoginActivity.this);
    }

    @Override
    public void showError(String msg) {
        showProgress(false);
        showErrorDialog(msg);
    }

    private void showErrorDialog(String msg) {
        Dialog dialog = DialogFactory.createMessageDialog(LoginActivity.this, "Login Failed", msg);
        new Handler().postDelayed(() -> dialog.show(), 500);

    }

    /**
     * Only works from an activity, using getActivity() does not work
     *
     * @param activity
     */
    public void hideKeyboardInActivity(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        }
    }
}

