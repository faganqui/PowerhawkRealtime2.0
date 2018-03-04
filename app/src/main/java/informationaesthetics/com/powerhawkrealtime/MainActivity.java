package informationaesthetics.com.powerhawkrealtime;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTabHost;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import informationaesthetics.com.powerhawkrealtime.Data.FirebaseDatabaseData;
import informationaesthetics.com.powerhawkrealtime.Data.UnitData;
import informationaesthetics.com.powerhawkrealtime.Data.UserData;
import informationaesthetics.com.powerhawkrealtime.Fragments.AddServerFragment;
import informationaesthetics.com.powerhawkrealtime.Fragments.LoadingFragment;
import informationaesthetics.com.powerhawkrealtime.Fragments.SelectedMeterFragment;
import informationaesthetics.com.powerhawkrealtime.Fragments.SelectedServerFragment;
import informationaesthetics.com.powerhawkrealtime.Fragments.SelectedUnitFragment;
import informationaesthetics.com.powerhawkrealtime.Fragments.ServerFragment;
import informationaesthetics.com.powerhawkrealtime.Fragments.SettingsFragment;
import informationaesthetics.com.powerhawkrealtime.Objects.Server;
import informationaesthetics.com.powerhawkrealtime.Utilities.MenuActions;
import informationaesthetics.com.powerhawkrealtime.Utilities.MenuActions.*;
import informationaesthetics.com.powerhawkrealtime.Fragments.LoginFragment;
import informationaesthetics.com.powerhawkrealtime.Utilities.FragmentUtilities;
import informationaesthetics.com.powerhawkrealtime.Utilities.FragmentUtilities.*;
import informationaesthetics.com.powerhawkrealtime.Utilities.Utilities;

public class MainActivity extends AppCompatActivity implements FragmentCommunicator, OnFragmentInteractionListener, View.OnClickListener {

    public UserData userData;

    FragmentManager fragmentManager;
    ArrayList<Fragment> oldFragments = new ArrayList<>();
    FragmentTransaction fragmentTransaction;
    public Utilities utils = new Utilities();

    //fragments
    LoginFragment loginFragment;
    ServerFragment serverFragment;
    LoadingFragment loading;

    Toolbar myToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myToolbar  = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.setTitle(R.string.empty);
        myToolbar.setLogo(R.drawable.icon);
        setSupportActionBar(myToolbar);

        fragmentManager = getSupportFragmentManager();
        Utilities.getInstance().setFragmentManager(fragmentManager);
        Utilities.getInstance().setMainActivity(this);
        fragmentTransaction = fragmentManager.beginTransaction();

        userData = UserData.getInstance();

        if(userData.getUser() == null) {
            loginFragment = new LoginFragment();
            goToFragment(loginFragment);
        } else {
            UserData.getInstance().setTransitFlag(R.integer.GO_TO_SERVER);
            goToFragment(loading = new LoadingFragment());
        }
    }

    @Override
    public void fragmentDetached(int frag_id) {
        switch (frag_id){
            case R.id.login_fragment_id:
                // this should never happen - we only leave login if signed in success
                if(userData.getUser() == null) return;
                if(userData.getTransitFlag() == R.integer.LOGIN) {
                    UserData.getInstance().setTransitFlag(R.integer.GO_TO_SERVER);
                    goToFragment(loading = new LoadingFragment());
                }else{
                    setUpServerFragment();
                }
                break;
            case R.id.server_fragment_id:
                switch (UserData.getInstance().getTransitFlag()){
                    case R.integer.SELECT_SERVER:
                        userData.setTransitFlag(R.integer.GO_TO_SELECTED_SERVER);
                        goToFragment(loading = new LoadingFragment());
                        break;
                    case R.integer.ADD_SERVER:
                        setUpAddServerFragment();
                        break;
                    case R.integer.REMOVE_SERVER:
                        setUpServerFragment();
                        break;
                }
                break;
            case R.id.add_server_fragment_id:
                setUpServerFragment();
                break;
            case R.id.loading_fragment_id:
                switch (UserData.getInstance().getTransitFlag()) {
                    case R.integer.GO_TO_SERVER:
                        ArrayList<HashMap<String, String>> servers = (ArrayList<HashMap<String, String>>) FirebaseDatabaseData.getInstance().getLoadedData();
                        try {
                            for (HashMap<String, String> server : servers) {
                                UserData.getInstance().addToServerList(server.get("sName"), server.get("sIP"));
                            }
                        } catch (Exception e) {
                            Log.d("LOAD", "LOADING DATA NULL" + e);
                        }
                        setUpServerFragment();
                        break;
                    case R.integer.GO_TO_SELECTED_SERVER:
                        Object units = loading.getLodedData();
                        HashMap<String, HashMap<String,Object>> unitArray = (HashMap<String, HashMap<String,Object>>) units;
                        UnitData.getInstance().initilizeData(unitArray);
                        setUpSelectedServerFragment();
                        break;
                }
                break;
            case R.id.selected_server_fragment_id:
                setUpSelectedUnitFragment();
                break;
            case R.id.selected_unit_fragment_id:
                setUpSelectedMeterFragment();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.action_bar, menu);
        return true;
    }

    @Override
    public void onClick(View view){
        if (view != null) {
            switch (view.getId()) {
                case R.id.watts_button_switch:
                    UnitData.getInstance().setStatType(0);
                    break;
                case R.id.var_button_switch:
                    UnitData.getInstance().setStatType(1);
                    break;
                case R.id.va_button_switch:
                    UnitData.getInstance().setStatType(2);
                    break;

            }
        }

    }

    public void refreshPages(){
        if (oldFragments.get(oldFragments.size()-1).getClass() == SelectedServerFragment.class) {
            ((SelectedServerFragment)oldFragments.get(oldFragments.size() - 1)).updateTable();
        } else if (oldFragments.get(oldFragments.size()-1).getClass() == SelectedUnitFragment.class){
            ((SelectedUnitFragment)oldFragments.get(oldFragments.size() - 1)).updateTable();
        } else if (oldFragments.get(oldFragments.size()-1).getClass() == SelectedMeterFragment.class){
            ((SelectedMeterFragment)oldFragments.get(oldFragments.size() - 1)).buildGraphView();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return MenuActions.getInstance().performAction(MainActivity.this , getApplicationContext(), item.getItemId());
    }

    private void setUpAddServerFragment(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myToolbar.setTitle(getString(R.string.toolbar_add_server_fragment));
                myToolbar.setLogo(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
            }
        });

        AddServerFragment addServerFragment = new AddServerFragment();
        goToFragment(addServerFragment);
    }

    public void setUpSettingsFragment(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myToolbar.setTitle(getString(R.string.toolbar_settings_fragment));
                myToolbar.setLogo(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
            }
        });

        SettingsFragment settingsFragment = new SettingsFragment();
        if (oldFragments.size() == 0 || !(oldFragments.get(oldFragments.size()-1).getClass() == SettingsFragment.class)) {
            oldFragments.add(settingsFragment);
        }
        goToFragment(settingsFragment);
    }


    public void setUpSelectedServerFragment(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myToolbar.setTitle(getString(R.string.toolbar_selected_server_fragment) + UserData.getInstance().getSelectedServer().getsName());
                myToolbar.setLogo(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
            }
        });

        SelectedServerFragment selectedServerFragment = new SelectedServerFragment();
        if (oldFragments.size() == 0 || !(oldFragments.get(oldFragments.size()-1).getClass() == SelectedServerFragment.class)) {
            oldFragments.add(selectedServerFragment);
        }
        goToFragment(selectedServerFragment);
    }

    public void setUpSelectedUnitFragment(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myToolbar.setTitle(getString(R.string.toolbar_selected_unit_fragment) + UserData.getInstance().getSelectedUnit().getKeyName());
                myToolbar.setLogo(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
            }
        });

        SelectedUnitFragment selectedUnitFragment = new SelectedUnitFragment();
        if (oldFragments.size() == 0 || !(oldFragments.get(oldFragments.size()-1).getClass() == SelectedUnitFragment.class)) {
            oldFragments.add(selectedUnitFragment);
        }
        goToFragment(selectedUnitFragment);
    }

    public void setUpSelectedMeterFragment(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myToolbar.setTitle(getString(R.string.toolbar_selected_meter_fragment) + UserData.getInstance().getSelectedMeter().getName());
                myToolbar.setLogo(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
            }
        });

        SelectedMeterFragment selectedMeterFragment = new SelectedMeterFragment();
        if (oldFragments.size() == 0 || !(oldFragments.get(oldFragments.size()-1).getClass() == SelectedMeterFragment.class)) {
            oldFragments.add(selectedMeterFragment);
        }
        goToFragment(selectedMeterFragment);
    }


    private void setUpServerFragment(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myToolbar.setTitle(getString(R.string.server_fragment_name));
                myToolbar.setLogo(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
            }
        });

        serverFragment = new ServerFragment();
        if (oldFragments.size() == 0 || !(oldFragments.get(oldFragments.size()-1).getClass() == ServerFragment.class)) {
            oldFragments.add(serverFragment);
        }
        goToFragment(serverFragment);
    }

    public void goToFragment(final Fragment fragment){
        fragmentTransaction = fragmentManager.beginTransaction();

        FragmentUtilities.getInstance().setCommunicator(this);
        try {
            fragmentTransaction.replace(R.id.fragment_holder, fragment);
            fragmentTransaction.addToBackStack(null);
        } catch (Exception e){
            fragmentTransaction.add(R.id.fragment_holder, fragment);
        }
        fragmentTransaction.commit();
    }

    public ServerFragment getServerFragment(){
        return serverFragment;
    }

    @Override
    public void onBackPressed(){
        oldFragments.remove(oldFragments.size()-1);
        if (oldFragments.size() == 0){
            setUpServerFragment();
        }else{
            if(oldFragments.get(oldFragments.size()-1).getClass() == ServerFragment.class){
                setUpServerFragment();
            }else if(oldFragments.get(oldFragments.size()-1).getClass() == SettingsFragment.class){
                setUpSettingsFragment();
            }else if(oldFragments.get(oldFragments.size()-1).getClass() == SelectedServerFragment.class){
                setUpSelectedServerFragment();
            }else if(oldFragments.get(oldFragments.size()-1).getClass() == SelectedUnitFragment.class){
                setUpSelectedUnitFragment();
            } else if(oldFragments.get(oldFragments.size()-1).getClass() == SelectedMeterFragment.class){
                setUpSelectedMeterFragment();
            }
        }
    }

    /*
    Stuff that i don't currently use but is needed anyway
     */

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }


}
