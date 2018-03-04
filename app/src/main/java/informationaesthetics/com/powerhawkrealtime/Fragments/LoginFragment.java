package informationaesthetics.com.powerhawkrealtime.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executor;

import informationaesthetics.com.powerhawkrealtime.Data.UserData;
import informationaesthetics.com.powerhawkrealtime.MainActivity;
import informationaesthetics.com.powerhawkrealtime.Utilities.FragmentUtilities;
import informationaesthetics.com.powerhawkrealtime.Utilities.FragmentUtilities.*;
import informationaesthetics.com.powerhawkrealtime.R;
import informationaesthetics.com.powerhawkrealtime.Utilities.Utilities;

public class LoginFragment extends Fragment implements Executor{

    private static final String TAG = "Login";

    private FirebaseAuth mAuth;
    private UserData userData;
    private OnFragmentInteractionListener mListener;
    private Utilities utils = new Utilities();

    @Override
    public void onDetach() {
        FragmentUtilities.getInstance().getCommunicator().fragmentDetached(R.id.login_fragment_id);
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userData = UserData.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);

        final EditText emailField = rootView.findViewById(R.id.login_fragment_email_field);
        final EditText passwordField = rootView.findViewById(R.id.login_fragment_password_field);

        Button loginButton = rootView.findViewById(R.id.login_fragment_signin_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doLogin(emailField.getText().toString(), passwordField.getText().toString());
            }
        });

        Button signupButton = rootView.findViewById(R.id.login_fragment_signup_button);
        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSignup(emailField.getText().toString(), passwordField.getText().toString());
            }
        });
        return rootView;
    }

    public void doLogin(String email, String password){
        if(email.equals("") || password.equals("")){
            utils.getInstance().makeToast((MainActivity) getActivity(), "Invalid email or password");
            return;
        }
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener((Executor) this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            userData.setName(user.getDisplayName());
                            userData.setEmail(user.getEmail());
                            userData.setTransitFlag(R.integer.LOGIN);
                            onDetach();

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            utils.getInstance().makeToast((MainActivity) getActivity(), "Invalid email or password");
                        }
                    }
                });
    }

    public void doSignup(String email, String password){
        if(email.equals("") || password.equals("")){
            utils.getInstance().makeToast((MainActivity) getActivity(), "Invalid email or password");
            return;
        }
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            userData.setName(user.getDisplayName());
                            userData.setEmail(user.getEmail());
                            userData.setTransitFlag(R.integer.SIGNUP);
                            onDetach();

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            utils.getInstance().makeToast((MainActivity) getActivity(), "Invalid email or password");
                        }
                    }
                });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void execute(@NonNull Runnable command) {
        new Thread(command).start();
    }


}

