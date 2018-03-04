package informationaesthetics.com.powerhawkrealtime.Objects;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ImageButton;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import informationaesthetics.com.powerhawkrealtime.Data.FirebaseDatabaseData;
import informationaesthetics.com.powerhawkrealtime.Data.UserData;
import informationaesthetics.com.powerhawkrealtime.Fragments.LoadingFragment;
import informationaesthetics.com.powerhawkrealtime.Fragments.ServerFragment;
import informationaesthetics.com.powerhawkrealtime.Interfaces.DataLoded;
import informationaesthetics.com.powerhawkrealtime.MainActivity;
import informationaesthetics.com.powerhawkrealtime.R;
import informationaesthetics.com.powerhawkrealtime.Utilities.Utilities;

import static android.net.wifi.WifiConfiguration.Status.strings;

/**
 * Created by quinnfagan on 2018-01-31.
 */

public class Server implements DataLoded{

    int statusImageId;
    private Object lodedData;
    String sKey;
    String sName;
    private String date;
    Context mContext;
    int mIndex;

    public int getRemoveId() {
        return removeId;
    }

    public void setRemoveId(int removeId) {
        this.removeId = removeId;
    }

    int removeId;
    String sIP;
    int rowId;

    public Server(){}
    /*
     Getters and setters
     */

    public String getsName() {
        return sName;
    }

    public void setsName(String sName) {
        this.sName = sName;
    }

    public String getsIP() {
        return sIP;
    }

    public void setsIP(String sIP) {
        this.sIP = sIP;
    }

    public String getsKey() {
        return sKey;
    }

    public void setsKey(String sKey) {
        this.sKey = sKey;
    }

    public int getStatusImageId() {
        return statusImageId;
    }

    public void setStatusImageId(int statusImageId) {
        this.statusImageId = statusImageId;
    }

    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public int getIndex() {
        return mIndex;
    }

    public void setIndex(int index){
        mIndex = index;
    }

    public void setDate(String newDate){
        date = newDate;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (date == null) {
                Utilities.getInstance().getServerFragment().setServerBlinker(0, statusImageId);
            } else {
                Utilities.getInstance().getServerFragment().setServerBlinker(getStatus(date), statusImageId);
            }
        }
    }

    private int getStatus(String date){
        String curDate = DateFormat.getDateTimeInstance().format(new Date());
        return 1;
    }

    public String getDate(){
        return date;
    }

    @Override
    public void dataLoaded(Object lodedData){
        String date = null;
        Object server = lodedData;
        HashMap<String, Object> serverObjects = (HashMap<String, Object>)server;
        if(serverObjects == null){
            setDate(null);
            return;
        }
        for(String key: serverObjects.keySet().toArray(new String[serverObjects.size()])){
            HashMap<String,Object> unit = (HashMap<String, Object>) serverObjects.get(key);
            String otherDate = (String) unit.get("date");
            if(date != null) {
                int count = 0;
                String[] cd = date.split("/");
                for (String ds : otherDate.split("/")) {
                    if(Integer.valueOf(ds) > Integer.valueOf(cd[count])){
                        date = otherDate;
                    } else if (Integer.valueOf(ds) < Integer.valueOf(cd[count])){
                        break;
                    }
                    count++;
                }
            } else{
                date = otherDate;
            }
        }
        setDate(date);
    }

}
