package informationaesthetics.powerhawkrealtime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.inputmethodservice.ExtractEditText;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;

public class FirstTimeSetupActivity extends AppCompatActivity implements View.OnClickListener{

    // key names for getting information in shared prefs and DB
    private static final String SHARED_PREFS = "POWERHAWK_URL_SAVED_PREFS";
    private static final String get_title = "POWERHAWK_TITLE";
    private static final String get_init_input = "POWERHAWK_OUTPUT";
    private static final String get_rows = "POWERHAWK_ROWS";
    private static final String get_columns = "POWERHAWK_COLUMNS";

    //Variables

    int readcount = 0; // keeps track of the pages being read
    int urlcount = 0; // keeps track of which unit we are reading

    Boolean isReactive = false; // checks if page is reactive or active URL

    String[] urls; // All the URL's needed to be read
    String url; // always set to the URL being read

    String headURL = ""; // Keeps URL for the header pager

    ArrayList<Integer> ignoredColumns = new ArrayList<>(); // Ignore columns that don't contain numbers

    String[] urlTypes = {"initial","active","matrixinfo","headerinfo","gettitle"}; // tells ParseURL how to read the URL

    String[] initial_arrays; // holds arrays of data

    String[] row_headers; // holds the names of all the rows
    String[] column_headers; // holds the names of all the columns
    String[] unit_titles; // holds the titles of each unit
    String alarms_string; // all the notifications that areset - may remove this

    String server; // IP address of the server

    String user_email;

    FirebaseAuth auth;

    //Interface objects
    EditText server_entry;
    TextView loadText;
    Button continue_button;
    int background;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_time_setup);

        //get user info
        auth = FirebaseAuth.getInstance();
        user_email = auth.getCurrentUser().getEmail();

        //set up interface objects
        continue_button = (Button) findViewById(R.id.connectToServer);
        server_entry = (EditText) findViewById(R.id.editServerText);

        background = Color.GRAY;

        continue_button.setOnClickListener(this);
    }

    @Override
    public void onBackPressed(){
        //we do nothing to make sure it can't be broken
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case (R.id.connectToServer):
                //Load everything into shared prefs
                server = server_entry.getText().toString();
                setLoadingScreen();
                setUpEverything(1, server);
        }
    }

    public void setLoadingScreen(){
        LinearLayout layout = (LinearLayout) findViewById(R.id.first_set_up);
        layout.setBackgroundColor(background);
        layout.removeAllViews();
        layout.setGravity(Gravity.CENTER);

        loadText = new TextView(this);
        loadText.setTextColor(Color.WHITE);
        loadText.setGravity(Gravity.CENTER);
        layout.addView(loadText);
    }

    public void setUpEverything(int phase, String server){
        //called until everythingis read

        // go through the phases of loading
        switch (phase) {
            case 1:
                //first thing is to get the URLS from the server
                loadText.setText("Performing first-time setup\nGetting meter info from server");
                new connectToServer().execute(server, "url");
                break;
            case 2:
                //loadText.setText("Performing first-time setup\nSetting up data structures for\n Meter "  + urlcount + " of " + urls.length);
                if (urlcount < urls.length) {
                    setUpHeaders(urls[urlcount]);
                } else {
                    urlcount = 0;
                    setUpEverything(3, server);
                }
                break;
            case 3:
                //loadText.setText("Performing first-time setup\nFilling data structures for\n Meter "  + urlcount + " of " + urls.length);
                if (urlcount < urls.length) {
                    new connectToServer().execute(server, "init", String.valueOf(urlcount), urls[urlcount]);
                } else {
                    setUpEverything(4, server);
                }
                break;
            case 4:
                //loadText.setText("Saving structs");
                int i = 0;
                do {
                    saveAllSharedPrefs(urls[i], i);
                    i++;
                } while (i < urls.length);

                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);

                break;
        }

    }

    // collect information about row and column headers from scraping HTMl
    public void setUpHeaders(String url) {
        //first time setup we read row and column headers as well as meter_title
        if (url.contains("reactive") || isReactive) {
            if (readcount < 5) {
                // read till we have all info we need
                isReactive = true;
                new ParseURL().execute("http://" + url.split("/")[2], "reactive");
                return;
            } else {
                //then call again to go to next URL
                ignoredColumns = new ArrayList<>();
                isReactive = false;
                readcount = 0;
                urlcount++;
                setUpEverything(2, server);
            }
        } else {
            if (readcount < 5) {
                new ParseURL().execute("http://" + url.split("/")[2], "");
                return;
            } else {
                ignoredColumns = new ArrayList<>();
                isReactive = false;
                urlcount++;
                readcount = 0;
                setUpEverything(2, server);
            }
        }
    }

    // save all collected data in shared preferences
    private void saveAllSharedPrefs(String urlx, int count){
        SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if(count == 0){
            //we only need to dave this once
            String url_String = "";

            for (String url : urls){
                url_String += url + "URLSPLIT";
            }

            editor.putString("urls", url_String);
            editor.putString("server", server);
        }

        editor.putString(get_rows + urlx, row_headers[count]);
        editor.putString(get_columns + urlx, column_headers[count]);
        editor.putString(get_title + urlx, unit_titles[count]);
        editor.putString(get_init_input + urlx, initial_arrays[count]);

        editor.commit();
    }

    // read from HMTL
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
                        doc = Jsoup.connect(urls[urlcount]).get();
                        Log.d("JSwa", "Connected to [" + strings[0] + "]");

                        table = doc.select("TABLE").get(0); //select the first table.
                        rows = table.select("tr");
                        Elements col_headers = rows.get(0).select("th");

                        int counter = 0;

                        column_headers[urlcount] = "";

                        for (Element header : col_headers){
                            if(!ignoredColumns.contains(counter) && counter > 1) {
                                column_headers[urlcount] += " " + header.text() + " " + ";";
                            }
                            counter ++;
                        }

                        row_headers[urlcount] = "";
                        for (Element row : rows) {
                            Elements columns = row.select("td");
                            counter = 0;
                            for (Element column : columns) {
                                if (counter == 1){
                                    row_headers[urlcount] += column.text() + ";";
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

                        unit_titles[urlcount] = parts2[0];
                        break;

                }
            } catch (Throwable t) {
                readcount = 99;
                Log.d("JSwa", t.toString());
            } finally {
                readcount ++;
                setUpHeaders(url);
            }

            return "Connected! Loading information...";
        }
    }

    // connect to python server
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

                    String userInput;

                    StringBuilder sb=  new StringBuilder();
                    while ((userInput = in.readLine())!= null)
                    {
                        sb.append(userInput + ";");
                    }
                    userInput = sb.toString();

                    urls = userInput.split(";");

                    for (int i = 0; i < urls.length; i++){
                        urls[i] = urls[i].split("\\^")[0];
                    }

                    row_headers = new String[urls.length]; // holds the names of all the rows
                    column_headers = new String[urls.length];// holds the names of all the columns
                    unit_titles = new String[urls.length]; // holds the titles of each unit
                    initial_arrays = new String[urls.length];

                    dout.close();
                    in.close();

                }else{
                    DataOutputStream dout=new DataOutputStream(soc.getOutputStream());
                    dout.writeUTF("init_" + user_email + "_" + strings[3]);
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

                    initial_arrays[Integer.valueOf(strings[2])] = dataString;
                }
                soc.close();
            } catch (Exception e) {
                dataString += e;
                e.printStackTrace();
            } finally {
                if(strings[1] == "url") {
                    setUpEverything(2, strings[0]);
                }else{
                    urlcount++;
                    setUpEverything(3, strings[0]);
                }
                return ("MESSAGE: " + dataString);
            }
        }
    }

}
