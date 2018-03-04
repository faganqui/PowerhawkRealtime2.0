package informationaesthetics.com.powerhawkrealtime.Utilities;

import android.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import informationaesthetics.com.powerhawkrealtime.Data.UserData;
import informationaesthetics.com.powerhawkrealtime.MainActivity;
import informationaesthetics.com.powerhawkrealtime.R;

import informationaesthetics.com.powerhawkrealtime.Utilities.Utilities;

/**
 * Created by quinnfagan on 2018-01-31.
 */

public class MenuActions {

    Utilities utils = new Utilities();

    public MenuActions(){

    }

    private void goToFragment(Fragment fragment, FragmentManager fragmentManager){

    }

    public static MenuActions getInstance(){
        return new MenuActions();
    }

    public boolean performAction(MainActivity activity, Context context, int actionId){
        switch (actionId){
            case R.id.action_settings:
                activity.setUpSettingsFragment();
                return true;

            case R.id.action_logout:
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                FirebaseUser user = mAuth.getCurrentUser();
                if(user == null){
                    utils.getInstance().makeToast(activity, "Must be logged in to logout");
                    return false;
                }
                mAuth.signOut();
                utils.getInstance().Restart();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return false;

        }
    }
}
