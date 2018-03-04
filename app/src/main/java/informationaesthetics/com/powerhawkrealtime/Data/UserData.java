package informationaesthetics.com.powerhawkrealtime.Data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.lang.reflect.Array;
import java.net.ConnectException;
import java.security.BasicPermission;
import java.util.ArrayList;
import java.util.Arrays;

import informationaesthetics.com.powerhawkrealtime.MainActivity;
import informationaesthetics.com.powerhawkrealtime.Objects.Meter;
import informationaesthetics.com.powerhawkrealtime.Objects.Server;
import informationaesthetics.com.powerhawkrealtime.Objects.Unit;
import informationaesthetics.com.powerhawkrealtime.Utilities.MenuActions;

/**
 * Created by quinnfagan on 2018-01-29.
 */

public class UserData {

    private static final String PREFERENCE = "com.informationaesthetics.prefs";
    private static final String TAG = "UsrDta";
    private static final String CSV_SERVERS = "Servers";
    private static final String USER_NAME = "Username";
    private static final String USER_EMAIL = "Email";

    private static final String SERVER_DELIMITER = ":";

    private volatile static UserData mInstance;

    private Context mContext;
    private int transitFlag;
    private SharedPreferences mPrefs;
    private FirebaseAuth mAuth;
    private String csv_servers;
    private Server selectedServer;
    private Server[] servers;
    private Unit selectedUnit;
    private Meter selectedMeter;

    private UserData(Context context){
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mAuth = FirebaseAuth.getInstance();
        setServerList(new String[0]);
    }

    public static final UserData getInstance(){
        if(mInstance == null) return null;
        return mInstance;
    }

    public static final void init(Context context){
        mInstance = new UserData(context);
    }

    private String[] getServerListString() {
        if (csv_servers != null && csv_servers != "") return csv_servers.split(",");
        String CSVServers = mPrefs.getString(CSV_SERVERS, null);
        return (CSVServers == null || CSVServers == "") ? null : CSVServers.split(",");
    }

    public Server[] getTempServerList(){
        if (getServerListString() == null) return null;
        Server[] outArray = new Server[getServerListString().length];
        int index = 0;
        for(String server : getServerListString()) {
            Server s = new Server();
            s.setsIP(server.split(SERVER_DELIMITER)[0]);
            s.setsName(server.split(SERVER_DELIMITER)[1]);
            outArray[index] = s;
            index++;
        }
        return outArray;
    }

    public Server[] getServerList(){
        return servers;
    }

    public void setServerList(Server[] servers){
        this.servers = servers;
    }

    public Server getSelectedServer(){
        return selectedServer;
    }

    public void setSelectedServer(Server server){
        selectedServer = server;
    }

    private void setServerList(String[] ServerList){
        String csv_string = "";
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        if (ServerList.length > 0) {
            for (String server : ServerList) {
                csv_string += server + ",";
            }
            csv_string = csv_string.substring(0, csv_string.length() - 1);
        }
        prefsEditor.putString(CSV_SERVERS, csv_string);
        prefsEditor.apply();
        csv_servers = csv_string;
    }

    public void addToServerList(String newServerName, String newServerIP) {
        if (getServerListString() == null){
            setServerList(new String[]{newServerIP + SERVER_DELIMITER + newServerName});
        } else {
            for (String server : getServerListString()) {
                if (server.toLowerCase().split(SERVER_DELIMITER)[0].equals(newServerIP.toLowerCase())) {
                    // Server list already contains server
                    Log.d(TAG, "Duplicate server being added");
                    return;
                }
            }
            ArrayList<String> List = new ArrayList<>(Arrays.asList(getServerListString()));
            String newServer = newServerIP + SERVER_DELIMITER + newServerName;
            List.add(newServer);
            String[] Array = new String[List.size()];
            setServerList(List.toArray(Array));
        }
        return;
    }

    public int removeFromServerList(int index){
        ArrayList<String> List = new ArrayList<>(Arrays.asList(getServerListString()));
        if(List.size() < index) return 0;
        List.remove(index);
        String[] Array = new String[List.size()];
        setServerList(List.toArray(Array));
        return 1;
    }

    public String getName() {
        return mPrefs.getString(USER_NAME, null);
    }

    public void setName(String Name) {
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putString(USER_NAME, Name);
        prefsEditor.apply();
    }

    public FirebaseUser getUser(){
        return mAuth.getCurrentUser();
    }

    public FirebaseAuth getAuth(){
        return mAuth;
    }


    public int getTransitFlag() {
        return transitFlag;
    }

    public void setTransitFlag(int transitFlag) {
        this.transitFlag = transitFlag;
    }

    public String getEmail() {
        return mPrefs.getString(USER_EMAIL, null);
    }

    public void setEmail(String Email) {
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putString(USER_NAME, Email);
        prefsEditor.apply();
    }

    public void setSelectedUnit(Unit selectedUnit) {
        this.selectedUnit = selectedUnit;
    }

    public Unit getSelectedUnit(){
        return this.selectedUnit;
    }

    public void setSelectedMeter(Meter selectedMeter) {
        this.selectedMeter = selectedMeter;
    }

    public Meter getSelectedMeter(){
        return this.selectedMeter;
    }
}
