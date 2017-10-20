package informationaesthetics.powerhawkrealtime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.security.Provider;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class ServerActivity extends AppCompatActivity implements View.OnClickListener {

    //Prefs
    private static final String SHARED_PREFS = "POWERHAWK_URL_SAVED_PREFS";

    private static final String TAG = "msm";
    private static String USER_DISPLAY = "quinn.LoginActivity.user_email";
    private static String USER_ID = "quinn.LoginActivity.user_id";

    //cloud messaging token
    String token;

    //intent key names
    private static final String get_url = "POWERHAWK_URL";
    private static final String get_serverip = "POWERHAWK_SERVER";
    private static final String get_title = "POWERHAWK_TITLE";
    private static final String get_init_input = "POWERHAWK_OUTPUT";
    private static final String get_rows = "POWERHAWK_ROWS";
    private static final String get_columns = "POWERHAWK_COLUMNS";
    private static final String get_kwd = "KWHD_INDEX";
    private static final String get_alarms = "ALARMS";
    private static final String get_string_input = "SERVER_SENT_STRING";

    //to be collected from intent
    String UserId;
    String UserDisplay;
    String ServerIP = "";
    String stringFromServer;
    String row_headers = "";
    String column_headers = "";
    String alarms_string = "";
    String initial_input = "";
    String meter_title = "";
    int kwdindex = 0;
    String url;

    //this is because i dont know why the fucking interface wont update
    int clickcount = 0;
    Boolean isServerRead = false;
    Boolean isReactive = false;

    //othervariables
    String headURL = "";
    ArrayList<Integer> ignoredColumns = new ArrayList<>();
    String[] urlTypes = {"initial","active","matrixinfo","headerinfo","gettitle"};
    int numPages= 2;
    int totalMeters = 0;
    int readcount=5;

    ArrayList<String> actives = new ArrayList<>();
    ArrayList<String> reactives = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        //Intent receive_notifications = new Intent(this, FirebaseMessageService.class);
        //startService(receive_notifications);

        //get intent from login, check for previous saved data
        Intent received_intent = getIntent();
        UserDisplay = received_intent.getStringExtra(USER_DISPLAY);
        UserId = received_intent.getStringExtra(USER_ID);
        ServerIP = received_intent.getStringExtra(get_serverip);
        stringFromServer = received_intent.getStringExtra(get_string_input);

        //new connectToServer().execute(ServerIP, "sendtoken");

        parseString(stringFromServer); // parses the sent string and sets up appropriate buttons
    }

    @Override
    public void onClick(View view){
        /*
        ImageView loading = new ImageView(this);
        if(Build.VERSION.SDK_INT > 15) {
            loading.setBackground(getResources().getDrawable(R.drawable.loadingimg));
            setContentView(loading);
        }
        */

        if (view.getId() >= 200) {
            //reactive button
            launchActivity(reactives.get(view.getId() - 200));
        } else {
            //active button
            launchActivity(actives.get(view.getId() - 100));
        }

    }


    protected void parseString(String input){
        //disects the string received from the server
        String[] temp_string_array = input.split(";");
        ArrayList<String> urls = new ArrayList<>();
        String kwd = "";
        String watts = "";

        for(String item : temp_string_array){
            urls.add(item.split("^")[0]);

            if(urls.size() == numPages){
                //second time around we make the buttons for the url
                actives.add(urls.get(0));
                reactives.add(urls.get(1));
                setupButtons(urls.get(0).split("//")[1].split("/")[0], kwd, watts);
                urls.remove(0);
                urls.remove(0); // clear urls
            }else{
                //we only get kwd from active which is the first page
                kwd = item.split("\\^")[1];
                watts = item.split("\\^")[2];
            }
        }

    }

    private void getAllSharedPrefs(String urlx){
        //gets all the previously saved things from specified URL
        SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        row_headers = sharedPref.getString(get_rows + urlx, "");
        column_headers = sharedPref.getString(get_columns + urlx, "");
        kwdindex = sharedPref.getInt(get_kwd + urlx, -1);
        meter_title = sharedPref.getString(get_title + urlx, "");
        if(initial_input == null || initial_input == ""){
            initial_input = sharedPref.getString(get_init_input + urlx, "");
        }
    }

    private void saveAllSharedPrefs(String urlx){
        SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFS,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString(get_rows + urlx, row_headers);
        editor.putString(get_columns + urlx, column_headers);
        editor.putInt(get_kwd + urlx, kwdindex);
        editor.putString(get_title + urlx, meter_title);
        editor.putString(get_init_input + urlx, initial_input);

        editor.commit();
    }

    protected void setupButtons(String url, String kwd, String watts){
        //format for numbers
        DecimalFormat df = new DecimalFormat("#.####");

        //build the interface for the Activity
        LinearLayout meterViewLayout = new LinearLayout(this);
        meterViewLayout.setBackgroundColor(getResources().getColor(R.color.colorBackGroundNewForManya));
        meterViewLayout.setOrientation(LinearLayout.VERTICAL);
        meterViewLayout.setPadding(50,50,50,50);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            meterViewLayout.setForeground(getResources().getDrawable(R.drawable.tableimageborder));
        }

        TextView meterTitle = new TextView(this);
        meterTitle.setText("Unit: " + url);
        meterTitle.setTextColor(Color.WHITE);

        TextView meterKwd = new TextView(this);
        meterKwd.setText("Total Kwh Delivered: " + df.format(Double.valueOf(kwd)));
        meterKwd.setTextColor(Color.WHITE);

        TextView meterWatts = new TextView(this);
        meterWatts.setText("Current Total Watts: " + watts);
        meterWatts.setTextColor(Color.WHITE);

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setBackgroundColor(getResources().getColor(R.color.colorBackGroundNewForManya));
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button activeB = new Button(this);
        activeB.setId(100 + totalMeters);
        activeB.setOnClickListener(this);
        activeB.setText("Active");

        Button reactiveB = new Button(this);
        reactiveB.setId(200 + totalMeters);
        reactiveB.setOnClickListener(this);
        reactiveB.setText("Reactive");

        meterViewLayout.addView(meterTitle);
        meterViewLayout.addView(meterKwd);
        meterViewLayout.addView(meterWatts);
        buttonLayout.addView(activeB);
        buttonLayout.addView(reactiveB);
        meterViewLayout.addView(buttonLayout);

        LinearLayout thisView = (LinearLayout) findViewById(R.id.serverid);
        thisView.setBackgroundColor(getResources().getColor(R.color.colorBackGround));
        thisView.addView(meterViewLayout);
        thisView.setPadding(50,50,50,50);

        totalMeters ++;
    }

    protected void launchActivity(String selectedUrl){
        url = selectedUrl;

        if (!isServerRead) {
            setLoading();
            new connectToServer().execute(ServerIP, "init");
            readcount = 0;
        }else {
            SharedPreferences tempSp = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);

            String urlPlaceholder = url.split("\\^")[0];
            String tempCheckIfBeenSaved = tempSp.getString((get_rows + urlPlaceholder), "");
            if (tempCheckIfBeenSaved.equals("")) {
                //first time setup we read row and column headers as well as meter_title
                if (url.contains("reactive") || isReactive) {
                    if (readcount < 5) {
                        isReactive = true;
                        new ParseURL().execute("http://" + url.split("/")[2], "reactive");
                        return;
                    }
                } else {
                    if (readcount < 5) {
                        new ParseURL().execute("http://" + url.split("/")[2], "");
                        return;
                    }
                }
                saveAllSharedPrefs(url);
            } else {
                //otherwise we just load it from Shared prefs
                url = url.split("\\^")[0];
                getAllSharedPrefs(url);
            }

            //add saved prefs for meter
            newSharedPref(row_headers, url, "m");
            newSharedPref(column_headers, url, "s");

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
            intent.putExtra(get_alarms, alarms_string);
            // for debug la_textview.setText(initial_input);
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(this, StartupActivity.class);
        intent.putExtra(USER_DISPLAY, UserDisplay);
        intent.putExtra(USER_ID, UserId);
        startActivity(intent);

    }

    public int setLoading(){
        LinearLayout thisView = (LinearLayout) findViewById(R.id.serverid);
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

    //connect to python server
    private class connectToServer extends AsyncTask<String,Void,String> {

        @Override
        protected String doInBackground(String... strings) {
            String dataString = "";

            try {
                //connection refused because its not local host its an emulator, try connecting via my IP
                Socket soc = new Socket(strings[0], 9002);

                //requests to see which urls are hosted on the server
                if (strings[1] == "init"){
                    DataOutputStream dout=new DataOutputStream(soc.getOutputStream());
                    dout.writeUTF("init_" + UserId + "_" + url.split("\\^")[0]);
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
                isServerRead = true;
                launchActivity(url);

                return ("MESSAGE: " + dataString);
            }
        }
    }

    private class ParseURL extends AsyncTask<String, Void, String> { // Gets all doubles from inside table and outputs them to the display in the form: 11,12,13,;21,22,23,;31,32,33,;

        //needed variables
        String url_read_type = urlTypes[readcount];
        Document doc;
        Elements links;
        Element table;
        Elements rows;
        int row_counter;
        String oldUrl;


        @Override
        protected String doInBackground(String... strings) {

            try {
                switch (url_read_type){
                    case "initial": //finds the nav page which contains info about the active/reactive URL
                        Log.d("JSwa", "Connecting to [" + strings[0] + "]");
                        doc = Jsoup.connect(strings[0]).get();
                        Log.d("JSwa", "Connected to [" + strings[0] + "]");

                        links = doc.select("FRAME");

                        for (Element link : links) {
                            String[] split = link.toString().split("src=\"");
                            String[] split2 = split[1].split("\"");
                            if(split2[0].toLowerCase().contains("nav")){
                                url = strings[0] + "/"+split2[0]; //sets url to the navigation tab URL
                            }else if(split2[0].toLowerCase().contains("header")){
                                headURL = strings[0] +"/"+ split2[0]; //we also get header URL to collect meter info
                            }

                        }
                        break;
                    case "active":
                        Log.d("JSwa", "Connecting to [" + strings[0] + "]");
                        doc = Jsoup.connect(url).get();
                        Log.d("JSwa", "Connected to [" + strings[0] + "]");

                        links = doc.select("a[href]");

                        for (Element link : links) {
                            if(strings[1] == "reactive"){
                                if (link.attr("abs:href").toLowerCase().contains("reactive")) {
                                    url = link.attr("abs:href");
                                }
                            }else {
                                if (link.attr("abs:href").toLowerCase().contains("active") && !link.attr("abs:href").toLowerCase().contains("reactive")) {
                                    url = link.attr("abs:href");
                                }
                            }
                        }
                        break;
                    case "matrixinfo":
                        Log.d("JSwa", "Connecting to [" + strings[0] + "]");
                        doc = Jsoup.connect(url).get();
                        Log.d("JSwa", "Connected to [" + strings[0] + "]");
                        if (String.valueOf(doc).split("url=")[1].split(".htm")[0].contains("active")){ //catches redirect
                            if(strings[1]=="reactive"){
                                url = url.split("reactive.htm")[0] + String.valueOf(doc).split("url=")[1].split(".htm")[0] + ".htm"; //sets url to redirect link
                            }else {
                                url = url.split("active.htm")[0] + String.valueOf(doc).split("url=")[1].split(".htm")[0] + ".htm"; //sets url to redirect link
                            }
                            Log.d("JSwa", "Connecting to [" + strings[0] + "]");
                            doc = Jsoup.connect(url).get();
                            Log.d("JSwa", "Connected to [" + strings[0] + "]");
                        }

                        table = doc.select("TABLE").get(0); //select the first table.
                        rows = table.select("tr");

                        String Matrix_input = "";

                        row_counter = 0;
                        for (Element row : rows){
                            if(row_counter!=0) { // 0 is headers row so we skip it
                                Elements columns = row.select("td");
                                int column_counter = 0;
                                for (Element column : columns) {
                                    if (column_counter != 0) { //0 is the title column so we skip it
                                        if (column.text().matches("-?\\d*\\.?\\d+")) { //if the column only contains a double
                                            Matrix_input += column.text() + ","; // signifies end of column to matrix
                                        }else{
                                            ignoredColumns.add(column_counter);
                                        }
                                    }
                                    column_counter++;
                                }
                                Matrix_input += ";"; // signifies end of row to matrix
                            }
                            row_counter++;
                        }
                        //initial_input = Matrix_input;

                        break;
                    case "headerinfo":
                        Log.d("JSwa", "Connecting to [" + strings[0] + "]");
                        doc = Jsoup.connect(url).get();
                        Log.d("JSwa", "Connected to [" + strings[0] + "]");

                        table = doc.select("TABLE").get(0); //select the first table.
                        rows = table.select("tr");
                        Elements col_headers = rows.get(0).select("th");

                        int counter = 0;
                        int counter_x = 0; //not including ignored columns
                        for (Element header : col_headers){
                            if(!ignoredColumns.contains(counter) && counter > 1) {
                                column_headers += " " + header.text() + " " + ";";
                                if (header.text().toLowerCase().contains("kwh delivered")){
                                    kwdindex = counter_x;
                                }
                                counter_x++;
                            }
                            counter ++;
                        }

                        for (Element row : rows) {
                            Elements columns = row.select("td");
                            counter = 0;
                            for (Element column : columns) {
                                if (counter == 1){
                                    row_headers += column.text() + ";";
                                }
                                counter++;
                            }
                        }
                        break;
                    case "gettitle":
                        Log.d("JSwa", "Connecting to [" + strings[0] + "]");
                        doc = Jsoup.connect(headURL).get();
                        Log.d("JSwa", "Connected to [" + strings[0] + "]");

                        Element table = doc.select("TABLE").get(0);
                        Elements rows = table.select("p");

                        String[] parts = rows.get(0).toString().split(">");
                        String[] parts2 = parts[2].split("<");

                        meter_title = parts2[0];
                        break;

                }
            } catch (Throwable t) {
                readcount = 99;
                Log.d("JSwa", t.toString());
            } finally {
                readcount ++;
                launchActivity(url);
            }

            return "Connected! Loading information...";
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

            }
        }
    }

}
