package informationaesthetics.powerhawkrt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class LoadFromDatabase extends AppCompatActivity {

    //Prefs
    private static final String SHARED_PREFS = "POWERHAWK_URL_SAVED_PREFS";
    // key names for getting information in shared prefs and DB
    private static final String get_title = "POWERHAWK_TITLE";
    private static final String get_init_input = "POWERHAWK_OUTPUT";
    private static final String get_init_input_minutes = "POWERHAWK_OUTPUT_MINS";
    private static final String get_init_input_hours = "POWERHAWK_OUTPUT_HOURS";
    private static final String get_rows = "POWERHAWK_ROWS";
    private static final String get_columns = "POWERHAWK_COLUMNS";

    String[] stat_array = new String[4];
    String[] urls;

    //get database objects
    FirebaseDatabase database;

    //server ip
    String server;

    String server_mac;

    //objects for temporary data collection
    String result = "";
    int index = 0;
    int url_index = -1;
    Boolean hasNextStat = false;

    //User stats
    String userId;

    //shared preferences
    SharedPreferences sharedPref;

    //Text view for showing load percent
    TextView loadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_from_database);

        //Loading text
        loadingText = (TextView)findViewById(R.id.loading_text);
        loadingText.setText("pinging server");

        //get prefs
        sharedPref = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        server = sharedPref.getString("server", "");

        //todo : remove this
        server = "99.227.134.44";

        new connectToServer().execute(server);

        stat_array[0] = "DATA";
        stat_array[1] = "DATAMINUTES";
        stat_array[2] = "DATAHOURS";
        stat_array[3] = "HEADERNAMES";

        // Get database instance
        database = FirebaseDatabase.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    }

    public String[] remove_dupes(String[] og){
        // removes any duplicate string in og
        Set<String> set = new HashSet<String>();
        for (String num : og) {
            set.add(num);
        }

        String[] urls = set.toArray(new String[set.size()]);

        return urls;
    }

    public void collectAllData(){
        //display % of download completed
        String x= result;
        urls = (result.split("URLSPLIT").length > 1) ? remove_dupes(result.split("URLSPLIT")) : urls;

        loadingText.setText(String.format("Session started\nloading data - %1$.2f",(Double.valueOf(index)/Double.valueOf(stat_array.length) * Double.valueOf(urls.length - 1)) *100) + "%");

        //breaks if we've collected all the data
        if(index == stat_array.length){
            if(url_index < urls.length-1){
                url_index++;
                index = 0;
            }else {
                finishLoading();
                return;
            }
        }

        //placeStatInPrefs
        if (hasNextStat){
            SharedPreferences.Editor editor = sharedPref.edit();
            if(url_index == -1){
                //first time is urls we do this special
                String url_string = "";
                for(String url : urls){
                    url_string += url + ";";
                }
                editor.putString("URLS", url_string);
                url_index++;
            }else {
                //puts the read value in shared pref
                editor.putString(stripper(urls[url_index]) + stat_array[index], result);
                editor.apply();
                index++;
                hasNextStat = false;
            }
        }

        //retrieves each stat value from database
        if(index < stat_array.length) {
            getStat(stat_array[index], urls[url_index]);
        } else {
            finishLoading();
        }
    }

    public String stripper(String og){
        //returns og without the following characters:
        // . / :
        return og.replace(":", "").replace(".", "").replace("/", "");
    }

    public void finishLoading(){
        Intent battle = new Intent(getBaseContext(), MainActivity.class);
        startActivity(battle);
    }

    public void getStat(String stat, String url){
        //sets result to the current read data
        // sets a specific statistic for a user
        DatabaseReference statData = database.getReference("/" + server.replace(".","") + server_mac + "/" + stripper(url) + "/" + stat);

        // Attach a listener to read the data at our posts reference
        statData.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                result = value;
                //calls to collect data after previous data is read
                hasNextStat = true;
                collectAllData();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });

    }

    private class connectToServer extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            String dataString = "";

            try {

                //connection refused because its not local host its an emulator, try connecting via my IP
                Socket soc = new Socket(strings[0], 9002);


                DataOutputStream dout = new DataOutputStream(soc.getOutputStream());
                //sends all the things needed too keep track of alarm
                dout.writeUTF("SPLITHERE" + safeSendTokenFormatter(FirebaseInstanceId.getInstance().getToken()) + "SPLITHERE" + FirebaseAuth.getInstance().getCurrentUser().getEmail() + "SPLITHERE");
                dout.flush();

                BufferedReader in = new BufferedReader(new InputStreamReader(
                        soc.getInputStream()));

                BufferedReader stdIn =
                        new BufferedReader(
                                new InputStreamReader(System.in));

                String userInput;

                StringBuilder sb = new StringBuilder();
                while ((userInput = in.readLine()) != null) {
                    sb.append(userInput);
                }
                userInput = sb.toString();

                dataString += userInput;

                server_mac = dataString.substring(3);

                dout.close();
                in.close();
                soc.close();

            } catch (Exception e) {
                dataString += e;
                e.printStackTrace();
            } finally {
                getStat("URLS","urlinfo");
            }

            //collectedString = dataString;
            return ("MESSAGE: " + dataString);
        }
    }

    public static String safeSendTokenFormatter(String x){
        String result = "";
        for(int i = 0; i<x.length()-1; i++){
            result += x.substring(i, i+1) + "\\";
        }
        return result+",";
    }

}



