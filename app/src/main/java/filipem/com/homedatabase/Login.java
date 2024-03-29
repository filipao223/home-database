package filipem.com.homedatabase;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseUiException;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

public class Login extends Activity {

    private static final int RC_SIGN_IN = 123;
    private static final String TAG = "Login";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // User is signed in
            //Toast.makeText(this, "Already signed in", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, Home.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("User", user);
            startActivity(intent);
            finish();
            return;
        } else {
            // User is signed out
            Log.d(TAG, "onAuthStateChanged:signed_out");
        }

        if (isNetworkConnected()){
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                   new AuthUI.IdpConfig.EmailBuilder().build(),
                   new AuthUI.IdpConfig.GoogleBuilder().build());

            startActivityForResult(
                   AuthUI.getInstance()
                           .createSignInIntentBuilder()
                           .setAvailableProviders(providers)
                           .build(),
                   RC_SIGN_IN);
        }
        else{
           Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                Toast.makeText(this, "Signed in succesfully", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(this, Home.class);
                intent.putExtra("User", user);
                startActivity(intent);
                finish();

            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                if (response == null) finish();
                else{
                    Toast.makeText(this, R.string.login_error, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

}
