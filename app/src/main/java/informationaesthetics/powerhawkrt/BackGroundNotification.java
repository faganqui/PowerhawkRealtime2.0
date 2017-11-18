package informationaesthetics.powerhawkrt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.io.DataOutputStream;
import java.net.Socket;

import static android.content.ContentValues.TAG;

/**
 * Created by Quinn on 2017-10-09.
 */

public class BackGroundNotification extends FirebaseInstanceIdService {

    //Prefs
    private static final String SHARED_PREFS = "POWERHAWK_URL_SAVED_PREFS";

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        Log.d(TAG, "refreshedToken = " + refreshedToken);

        sendRegistrationToServer(refreshedToken);
    }

    public void sendRegistrationToServer(String token){
        //send token to server here
        SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);

        new connectToServer().execute(sharedPref.getString("curserver", ""),"send_token", token, sharedPref.getString("userid", ""));

    }

    //connect to python server
    private class connectToServer extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            String dataString = "";

            try {
                //connection refused because its not local host its an emulator, try connecting via my IP
                Socket soc = new Socket(strings[0], 9002);

                //requests to see which urls are hosted on the server
                if (strings[1] == "sendtoken") {


                    DataOutputStream dout=new DataOutputStream(soc.getOutputStream());
                    dout.writeUTF("sendtoken_" + strings[2] + "_SPLITHERE" + strings[3] + "SPLITHERE");
                    dout.flush();
                    dout.close();

                }
                soc.close();
            } catch (Exception e) {
                dataString += e;
                e.printStackTrace();
            }

            return "winner";
        }
    }
}
