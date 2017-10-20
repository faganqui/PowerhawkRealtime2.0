package informationaesthetics.powerhawkrealtime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;

public class StartupActivity extends AppCompatActivity implements View.OnClickListener {

    //Interface options
    Button start_button;
    EditText url_input;
    ImageView loading_image;
    TextView la_textview;

    //Prefs
    private static final String SHARED_PREFS = "POWERHAWK_URL_SAVED_PREFS";

    //intent key names
    private static final String get_url = "POWERHAWK_URL";
    private static final String get_serverip = "POWERHAWK_SERVER";
    private static final String get_title = "POWERHAWK_TITLE";
    private static final String get_init_input = "POWERHAWK_OUTPUT";
    private static final String get_rows = "POWERHAWK_ROWS";
    private static final String get_columns = "POWERHAWK_COLUMNS";
    private static final String get_kwd = "KWHD_INDEX";
    private static final String get_string_input = "SERVER_SENT_STRING";
    private static final String get_alarms = "ALARMS";
    private static String USER_DISPLAY = "quinn.LoginActivity.user_email";
    private static String USER_ID = "quinn.LoginActivity.user_id";
    private static String STRING_FROM_SERVER = "SERVER_SENT_STRING";

    //to be collected from intent
    String UserId;
    String UserDisplay;
    String ServerIP = "";

    //cloud messaging token
    String token;
    Boolean hasToken = false;

    //For saving prefs
    int numSavedURLS =0;
    ArrayList<String> savedURLS = new ArrayList<>();

    //to be given to intent
    String url;
    String alarms_string;
    String initial_input;
    String row_headers = "";
    String column_headers = "";
    String meter_title = "";
    String stringFromServer;
    int kwdindex;

    //temporary variables
    String headURL;
    ArrayList<Integer> ignoredColumns = new ArrayList<>();
    String[] urlTypes = {"initial","active","matrixinfo","headerinfo","gettitle"};
    int readcount = 0;
    int skip_in = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);

        //get intent from login, check for previous saved data
        Intent received_intent = getIntent();
        UserDisplay = received_intent.getStringExtra(USER_DISPLAY);
        UserId = received_intent.getStringExtra(USER_ID);
        ServerIP = received_intent.getStringExtra(get_serverip);

        //save current user_id
        newSharedPref(UserId, "xx", "xx");

        //get any saved info from past sessions
        getSharedPrefs();

        //setting up interface objects
        start_button = (Button) findViewById(R.id.startbutton);
        url_input = (EditText) findViewById(R.id.url_entry);
        loading_image = (ImageView) findViewById(R.id.load_image);
        la_textview = (TextView) findViewById(R.id.savedurlstext);
        la_textview.setText("Welcome "+UserDisplay+",\nClick here for past URL's:");

        /*
        if(ServerIP != "" && ServerIP != null) {
            //makes it so if weve already connected to a server in this session we load server
            //data immediately
            url_input.setText(ServerIP);
            new connectToServer().execute(ServerIP, "url");

            Intent intent = new Intent(this, ServerActivity.class);
            intent.putExtra(USER_DISPLAY, UserDisplay);
            intent.putExtra(USER_ID, UserId);
            intent.putExtra(get_serverip, ServerIP);
            intent.putExtra(STRING_FROM_SERVER,stringFromServer);
            startActivity(intent);

            //makeButtons(savedURLS, 2);
        }else{
            makeButtons(savedURLS, 1);
        }
        */
        makeButtons(savedURLS, 1);

        start_button.setOnClickListener(StartupActivity.this);
    }

    //connect to python server
    private class connectToServerToken extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {

            try {
                //connection refused because its not local host its an emulator, try connecting via my IP
                Socket soc = new Socket(strings[0], 9002);

                //requests to see which urls are hosted on the server
                if (strings[1] == "send_token") {

                    DataOutputStream dout=new DataOutputStream(soc.getOutputStream());
                    dout.writeUTF("sendtoken_" + strings[2] + "_SPLITHERE" + strings[3] + "SPLITHERE");
                    dout.flush();
                    dout.close();

                }
                soc.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                hasToken = true;
                readStuff(strings[0]);
            }

            return "winner";
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.startbutton:
                setLoading();
                //sends token to server
                String temp_url = String.valueOf(url_input.getText());
                readcount = 5;
                newSharedPref(temp_url, "na", "");
                readStuff(temp_url);
                break;
            default:
                setLoading();
                //first set of buttons, connects to listed past URLS
                readcount = 5;
                readStuff(savedURLS.get(view.getId()-100));
                break;
            /*
                //reads the entered URL;
                newSharedPref(temp_url, "na", "na");
                int lock = -1;

                //check if its a server or a meter
                try {
                    if(temp_url.matches("([0-9]+).([0-9]+).([0-9]+).([0-9]+)")){
                        readcount = 99;
                    }

                    //first checks if meter
                    //Calls parse url till we have all needed info
                    while (readcount < 5) {
                        la_textview.setText("Connecting to meter...");
                        if (lock != readcount) {
                            lock = readcount;
                            if (readcount < 5) {
                                new ParseURL().execute("http://" + temp_url, "");
                            }
                        }

                    }

                    //break it if it fails (readcount is sett to 99/100 on fail of reading a url)
                    int executioner = 1/(100-readcount);
                    executioner = 1/(99-readcount);

                    //Begin the main activity
                    Intent intent = new Intent(this, URLHandlerActivity.class);
                    intent.putExtra(get_url, url);
                    intent.putExtra(get_rows, row_headers);
                    intent.putExtra(get_columns, column_headers);
                    intent.putExtra(get_kwd, kwdindex);
                    intent.putExtra(get_title, meter_title);
                    intent.putExtra(USER_ID, UserId);
                    intent.putExtra(USER_DISPLAY, UserDisplay);
                    intent.putExtra(get_init_input, initial_input);
                    // for debug la_textview.setText(initial_input);
                    startActivity(intent);
                    break;
                    //if it fails it trys to connect to server
                } catch (Exception e ){
                    readcount = 5;
                    readStuff(temp_url);
                }

            default:
                if (view.getId() >= 100 && view.getId()<200){
                    //first set of buttons, connects to listed past URLS
                    url_input.setText(savedURLS.get(view.getId()-100));
                    start_button.callOnClick();
                    break;
                } else if(view.getId() >= 200 && view.getId()<300){
                    //set of buttons loaded from server, connects to server at given URL
                    ServerIP = String.valueOf(url_input.getText());

                    url = savedURLS.get(view.getId()-200);
                    String serverip_temp = String.valueOf(url_input.getText());
                    readcount=5;
                    new connectToServer().execute(serverip_temp, "init");
                    while (readcount != 6){ //read count is set to six when connectToServer exits
                        la_textview.setText("reading from server...");
                    }

                    //Calls parse url till we have all needed info
                    lock = -1;
                    readcount = 0;
                    skip_in = 1;
                    if(url.contains("reactive")) {
                        while (readcount < 5) {
                            la_textview.setText("Connecting to meter...");
                            if (lock != readcount) {
                                lock = readcount;
                                if (readcount < 5) {
                                    new ParseURL().execute("http://" + url.split("/")[2], "reactive");
                                }
                            }
                        }
                    }else{
                        while (readcount < 5) {
                            la_textview.setText("Connecting to meter...");
                            if (lock != readcount) {
                                lock = readcount;
                                if (readcount < 5) {
                                    new ParseURL().execute("http://" + url.split("/")[2], "");
                                }
                            }
                        }
                    }

                    //add saved prefs for meter
                    newSharedPref(row_headers,url, "m");
                    newSharedPref(column_headers,url, "s");

                    //Begin the main activity from server
                    Intent intent = new Intent(this, URLHandlerActivity.class);
                    intent.putExtra(get_url, url);
                    intent.putExtra(get_serverip, ServerIP);
                    intent.putExtra(get_rows, row_headers);
                    intent.putExtra(get_columns, column_headers);
                    intent.putExtra(get_kwd, kwdindex);
                    intent.putExtra(get_title, meter_title);
                    intent.putExtra(USER_ID, UserId);
                    intent.putExtra(USER_DISPLAY, UserDisplay);
                    intent.putExtra(get_init_input, initial_input);
                    intent.putExtra(get_alarms,alarms_string);
                    // for debug la_textview.setText(initial_input);
                    startActivity(intent);
                    break;
                }

        }
        //makeButtons(savedURLS, 2);
        */
        }
    }

    public void readStuff(String temp_url){
        try {
            if (!hasToken) {
                token = FirebaseInstanceId.getInstance().getToken();
                Log.d("TAGStartup", "TOKEN: " + token);

                token = token.replace("", "/");
                new connectToServerToken().execute(temp_url, "send_token", UserId, token);
                return;
            } else {
                if (readcount != 6) {
                    ServerIP = temp_url;
                    new connectToServer().execute(temp_url, "url");
                    return;
                } else {
                    Intent intent = new Intent(this, ServerActivity.class);
                    intent.putExtra(USER_DISPLAY, UserDisplay);
                    intent.putExtra(USER_ID, UserId);
                    intent.putExtra(get_serverip, ServerIP);
                    intent.putExtra(STRING_FROM_SERVER, stringFromServer);
                    startActivity(intent);
                }
            }
        } catch (Exception e) {
            readStuff(temp_url);
        }
    }

    private void newSharedPref(String temp_url_input, String url_to_save, String ms){
        //adds a new URL to list of saved ones
        if(temp_url_input != "") {
            if(ms == "xx"){
                SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFS,Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("userid", temp_url_input);
                editor.commit();
                return;
            }
            if(url_to_save != "na"){
                SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(UserId + url_to_save + ms, temp_url_input);
                //temp set "cururl" to url
                editor.putString("cururl", url_to_save);
                editor.putString("curserver", ServerIP);
                editor.commit();

            }else {
                if (!(savedURLS.contains(temp_url_input))) { //we only save if we don't already have it saved
                    numSavedURLS++;
                    SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(UserId + numSavedURLS, temp_url_input);
                    editor.putInt(UserId, numSavedURLS);
                    editor.commit();
                }
            }
        }
    }

    private void getSharedPrefs(){
        SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        numSavedURLS = sharedPref.getInt(UserId, 0);

        for (int i = 0; i < numSavedURLS; i++) {
            savedURLS.add(sharedPref.getString(UserId+ String.valueOf(i+1), ""));
        }
    }

    //connect to python server
    private class connectToServer extends AsyncTask<String,Void,String>{



        @Override
        protected String doInBackground(String... strings) {
            String dataString = "";

            try {
                //connection refused because its not local host its an emulator, try connecting via my IP
                Socket soc = new Socket(strings[0], 9002);

                //requests to see which urls are hosted on the server
                if (strings[1] == "url"){
                    //Send to server
                    DataOutputStream dout=new DataOutputStream(soc.getOutputStream());
                    dout.writeUTF("urls_");
                    dout.flush();

                    //Read response from server
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            soc.getInputStream()));

                    BufferedReader stdIn =
                            new BufferedReader(
                                    new InputStreamReader(System.in));

                    String userInput;

                    StringBuilder sb=  new StringBuilder();
                    while ((userInput = in.readLine())!= null)
                    {
                        sb.append(userInput + ";");
                    }
                    userInput = sb.toString();

                    stringFromServer = userInput;
                    readcount = 6;

                    String[] URLsFromServer = userInput.split(";");
                    savedURLS = new ArrayList<>();
                    numSavedURLS = 0;

                    for (int i = 0; i < URLsFromServer.length; i++){
                        savedURLS.add(URLsFromServer[i]);
                        numSavedURLS++;
                    }

                    dout.close();
                    in.close();

                    //collects info about "url" variable
                }else{
                    DataOutputStream dout=new DataOutputStream(soc.getOutputStream());
                    dout.writeUTF("init_" + UserId + "_" + url);
                    dout.flush();

                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            soc.getInputStream()));

                    BufferedReader stdIn =
                            new BufferedReader(
                                    new InputStreamReader(System.in));

                    String userInput;

                    StringBuilder sb=  new StringBuilder();
                    while ((userInput = in.readLine())!= null)
                    {
                        sb.append(userInput);
                    }
                    userInput = sb.toString();

                    dataString += userInput.split("ALARM")[1];
                    alarms_string = userInput.split("ALARM")[0];

                    dout.close();
                    in.close();

                }
                soc.close();
            } catch (Exception e) {
                dataString += e;
                e.printStackTrace();
            } finally {
                initial_input = dataString;
                readStuff(strings[0]);
                return ("MESSAGE: " + dataString);
            }
        }


    }

    public int setLoading(){
        LinearLayout thisView = (LinearLayout) findViewById(R.id.startupLayout);
        TextView loadText = new TextView(this);
        thisView.removeAllViews();
        thisView.setGravity(Gravity.CENTER);
        loadText.setGravity(Gravity.CENTER);
        loadText.setText("Loading" );
        thisView.addView(loadText);
        thisView.invalidate();
        thisView.requestLayout();
        return 1;
    }

    private void makeButtons(ArrayList<String> la_Buttons, int mode) {

        LinearLayout button_layout = (LinearLayout) findViewById(R.id.newButtons);
        button_layout.removeAllViews();

        //makes the buttons of previously used urls
        int count = 0;
        for (String button_name : la_Buttons){
            Button temp_button = new Button(this);
            temp_button.setText(button_name);
            temp_button.setId((100*mode) + count);
            temp_button.setOnClickListener(this);
            button_layout.addView(temp_button);
            count++;
        }
    }


}