package informationaesthetics.powerhawkrealtime;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.security.KeyStore;

public class FirebaseLoginActivity extends AppCompatActivity implements View.OnClickListener{

    private static String TAG = "loginactivity";

    //Prefs
    private static final String SHARED_PREFS = "FORGE_SAVED_PREFS";


    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private FirebaseDatabase database;

    Button SignInButton;
    Button SignUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        SignInButton = (Button)findViewById(R.id.SignInButton);
        SignUpButton = (Button)findViewById(R.id.SignUpButton);

        SignInButton.setOnClickListener(this);
        SignUpButton.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser() != null) {
            //Starts loading the activity immidiatley if we are already signed in
            Intent load = new Intent(getBaseContext(), LoadFromDatabase.class);
            startActivity(load);
        }

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };


    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    public void onClick(View view){

        EditText emailText = (EditText)findViewById(R.id.UserEditText);
        String email = String.valueOf(emailText.getText());

        EditText passwordText = (EditText) findViewById(R.id.PasswordEditText);
        String password = String.valueOf(passwordText.getText());

        if(email.equals("")|| password.equals("")){
            Toast.makeText(FirebaseLoginActivity.this, R.string.auth_failed,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        switch (view.getId()){
            case R.id.SignInButton:
                //Signs in an existing user
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {
                                    Log.w(TAG, "signInWithEmail:failed", task.getException());
                                    Toast.makeText(FirebaseLoginActivity.this, R.string.auth_failed,
                                            Toast.LENGTH_SHORT).show();
                                }else{

                                    Intent load = new Intent(getBaseContext(), LoadFromDatabase.class);
                                    startActivity(load);

                                }

                                // ...
                            }
                        });

                break;
            case R.id.SignUpButton:
                //Signs up a new user
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());

                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {
                                    Toast.makeText(FirebaseLoginActivity.this, R.string.auth_failed,
                                            Toast.LENGTH_SHORT).show();
                                }else{
                                    //verify email
                                    FirebaseAuth.getInstance().getCurrentUser().sendEmailVerification();

                                    //instantiate database
                                    database = FirebaseDatabase.getInstance();

                                    Intent load = new Intent(getBaseContext(), LoadFromDatabase.class);
                                    startActivity(load);
                                }
                            }
                        });
                break;
        }
    }

    public void setStat(String stat, String value, String uID){
        // sets a specific statistic for a user
        DatabaseReference statData = database.getReference("/users/" + uID + "/" + stat);
        statData.setValue(value);
    }


}
