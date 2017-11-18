package informationaesthetics.powerhawkrt;


import android.app.NotificationManager;
import android.content.Context;
import java.io.*;
import java.net.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.DecimalFormat;
import java.util.ArrayList;

import static java.lang.Math.min;

public class URLHandlerActivity extends AppCompatActivity implements View.OnClickListener {

    //decides how often we pull from website, in seconds
    public static int TIMER_UPDATE_RATE = 5;

    //Static things i need for design
    public static int BUTTON_BACK_COLOR = R.color.colorBackGroundNewForManya;
    public static int BUTTON_TEXT_COLOR = R.color.colorBackGround;
    public static int[] COLORS_ARRAY = {R.color.colorOne, R.color.colorTwo, R.color.colorThree, R.color.colorFour, R.color.colorTen , R.color.colorFive, R.color.colorSix,R.color.colorNine, R.color.colorSeven, R.color.colorEight};

    //gets screen size for making dynamic interface
    DisplayMetrics displaymetrics = new DisplayMetrics();
    int screen_height;
    int screen_width;

    //the matrix which holds all collected data
    MatrixArray theMatrix;

    //an array holding info about which items are selected
    ArrayList<String> selected_items = new ArrayList<>();
    int row_view_col_view_custom_view = 1; // 1 = row, 2= column, 3 = custom
    ArrayList<Integer> rowsorcols = new ArrayList<>();// says which rows/cols are selected
    ArrayList<String> current_alarms = new ArrayList<>();

    //Interface objects
    TextView output_text;
    TextView title_text;
    GraphView graph;
    ScrollView tableScrollView;
    //notification zone
    EditText enter_number;
    TextView display_text1;
    TextView display_text2;
    TextView display_text3;
    ArrayList<Integer> cur_meter_spinner_pos;
    ArrayList<Integer> cur_stat_spinner_pos;
    Button confirmButton;

    //Arrays for titles
    ArrayList<String> MeterTitles = new ArrayList<>();
    ArrayList<String> StatTitles = new ArrayList<>();

    //text formats
    DecimalFormat time_format = new DecimalFormat("#.####");
    DecimalFormat df = new DecimalFormat("#.####");

    //intent key names
    private static final String get_url = "POWERHAWK_URL";
    private static final String get_serverip = "POWERHAWK_SERVER";
    private static final String get_title = "POWERHAWK_TITLE";
    private static final String get_init_input = "POWERHAWK_OUTPUT";
    private static final String get_rows = "POWERHAWK_ROWS";
    private static final String get_alarms = "ALARMS";
    private static final String get_columns = "POWERHAWK_COLUMNS";
    private static final String get_kwd = "KWHD_INDEX";
    private static final String USER_DISPLAY = "quinn.LoginActivity.user_email";
    private static final String USER_ID = "quinn.LoginActivity.user_id";


    //to be collected from intent
    String url;
    String initial_input;
    String row_headers;
    String column_headers;
    String getting_alarms;
    String meter_titles;
    String UserId;
    String UserDisplay;
    String ServerIP;
    int kwdindex;

    //notification timer rate
    long cur_time = 0;
    int min_between_notification = 30;

    //temporary collection
    String output_string = "";
    String collectedString = "";

    //other collected data
    Double total_session_kwhd = 0.0;
    int total_run_time = 0;
    int current_table_display = 0;

    //Async task
    private ParseURL mTask;
    private Handler sTask = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_urlhandler);

        //setting up interface objects
        output_text = (TextView) findViewById(R.id.textView);
        title_text = (TextView) findViewById(R.id.textViewTitle);
        title_text.setOnClickListener(this);
        tableScrollView = (ScrollView) findViewById(R.id.scroll_Viewxx);
        enter_number = new EditText(this);

        //gets screen sizes
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        screen_height = displaymetrics.heightPixels;
        screen_width = displaymetrics.widthPixels;

        //should collect intent info here
        Intent intent = getIntent();
        url = intent.getStringExtra(get_url);
        initial_input = intent.getStringExtra(get_init_input);
        row_headers = intent.getStringExtra(get_rows);
        column_headers = intent.getStringExtra(get_columns);
        meter_titles = intent.getStringExtra(get_title);
        kwdindex = intent.getIntExtra(get_kwd, -2);
        getting_alarms = intent.getStringExtra(get_alarms);
        UserId = intent.getStringExtra(USER_ID);
        UserDisplay = intent.getStringExtra(USER_DISPLAY);
        ServerIP = intent.getStringExtra(get_serverip);

        if(!(getting_alarms == "" || getting_alarms == null)) {
            for (String i : getting_alarms.split("@")) {
               if (i == "" ){}else{ // This is dumb and shouldn't be needed but for some reason the if above this doesnt work
                    //maybe its because i dont know De morgans law, but im pretty sure i do and this is supposed to work
                current_alarms.add(i);}
            }
        }


        //build title arrays
        for(String element : row_headers.split(";")){
            if(element!=""){
                MeterTitles.add(element);
            }
        }
        for(String element : column_headers.split(";")){
            if(element!=""){
                StatTitles.add(element);
            }
        }

        //set up matrix
        try {
            String[] inputs = initial_input.split("!");
            theMatrix = new MatrixArray(inputs[0]);
            int count = 1;
            while (count < min(inputs.length - 1, 100)){
                theMatrix.add_matrix(inputs[count]);
                count++;
            }
        } catch (Exception e){
            theMatrix = new MatrixArray(initial_input);
        }

        //set up graph
        graph = (GraphView) findViewById(R.id.urlgraph);
        graph.setPadding(0,0,0,0);
        graph.setBackgroundColor(getResources().getColor(R.color.colorBackGroundNew));
        graph.getViewport().setMaxX(0);
        graph.getViewport().setMinX(-120);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalable(true);

        updateInterface("       > total run time = 0 seconds \n       > total session kwh delivered = 0");

        mTask = (ParseURL) new ParseURL().execute(url);
    }

    @Override
    public void onClick(View view) {

    /*    if (view.getId() == 197+1) {
            //temp conenct button
            TextView temp = (TextView) findViewById(199-2);
            temp.setText(collectedString);

            new connectToServer().execute();


        } else*/ if (view.getId() >= 200 && view.getId() < 300) {
            //this is for titles of each column
            int column = view.getId() - 200;

            if (row_view_col_view_custom_view == 2) { //checks if in column select mode
                if (rowsorcols.contains(column)) { //if it as and contains this column remove it
                    rowsorcols.remove((Integer) column);
                    for (int i = 0; i < theMatrix.getHeight(); i++) {
                        selected_items.remove(i + ";" + column);
                    }
                } else {  // otherwise add it
                    rowsorcols.add(column);
                    for (int i = 0; i < theMatrix.getHeight(); i++) {
                        selected_items.add(i + ";" + column);
                    }
                }
            } else { //if its not makes it col select mose and adds selected column
                row_view_col_view_custom_view = 2;
                selected_items = new ArrayList<>();
                rowsorcols = new ArrayList<>();
                rowsorcols.add(column);
                for (int i = 0; i < theMatrix.getHeight(); i++) {
                    selected_items.add(i + ";" + column);
                }
            }


        } else if (view.getId() >= 300 && view.getId() < 400) {
            //this is for titles of each row
            int row = view.getId() - 300;

            if (row_view_col_view_custom_view == 1) { // checks if in row mode
                if (rowsorcols.contains(row)) { // if it contains this row remove it
                    rowsorcols.remove((Integer) row);
                    for (int i = 0; i < theMatrix.getWidth(); i++) {
                        selected_items.remove(row + ";" + i);
                    }
                } else { //otherwise add this row
                    rowsorcols.add(row);
                    for (int i = 0; i < theMatrix.getWidth(); i++) {
                        selected_items.add(row + ";" + i);
                    }

                }
            } else { // if not clear and add row and make it row mode
                row_view_col_view_custom_view = 1;
                selected_items = new ArrayList<>();
                rowsorcols = new ArrayList<>();
                rowsorcols.add(row);
                for (int i = 0; i < theMatrix.getWidth(); i++) {
                    selected_items.add(row + ";" + i);
                }
            }

        }else if (view.getId()==700+1){
            // confirm button in settings
            ArrayList<Integer> meters = cur_meter_spinner_pos;
            ArrayList<Integer> stats = cur_stat_spinner_pos;
            String range = String.valueOf(enter_number.getText());

            String outString = "";

            //make alarms here
            try {
                for(int meter : meters) {
                    for (int stat : stats) {
                        String[] ss = range.split("-");
                        Double range1 = Double.valueOf(ss[0]);
                        Double range2 = Double.valueOf(ss[1]);

                        outString += range + ";";

                        outString += String.valueOf(meter) + ";";
                        outString += String.valueOf(stat);

                        current_alarms.add(outString);
                        new connectToServer().execute(ServerIP, "alarm", outString);
                        outString="";
                    }
                }
                makeSettings();
            } catch (Exception e){

            }



        }else if(view.getId() >= 800 && view.getId() < 850){
            //select meters for alarms
            Button temp = (Button) findViewById(view.getId());
            if(cur_meter_spinner_pos.contains(view.getId()-800)){
                cur_meter_spinner_pos.remove(new Integer(view.getId()-800));
                temp.setBackgroundColor(getResources().getColor(R.color.colorOne));
            }else {
                cur_meter_spinner_pos.add(view.getId() - 800);
                temp.setBackgroundColor(getResources().getColor(R.color.colorFour));
            }

        }else if(view.getId() < 900 && view.getId() >= 850){
            //select stats for alarms
            Button temp = (Button) findViewById(view.getId());
            if(cur_stat_spinner_pos.contains(view.getId()-850)){
                cur_stat_spinner_pos.remove(new Integer(view.getId()-850));
                temp.setBackgroundColor(getResources().getColor(R.color.colorOne));
            }else {
                cur_stat_spinner_pos.add(view.getId() - 850);
                temp.setBackgroundColor(getResources().getColor(R.color.colorFour));
            }

        }else if(view.getId() >= 1000 && view.getId() < 100000){
            //this is for each individual matrix item
            int row = (view.getId()/1000) - 1;
            int column = view.getId()%1000;

            if(row_view_col_view_custom_view != 3){
                selected_items = new ArrayList<>();
            }
            row_view_col_view_custom_view = 3;

            if(selected_items.contains(row + ";" + column)){
                selected_items.remove(row + ";" + column);
            }else{
                selected_items.add(row + ";" + column);
            }

        } else if (view.getId() >= 100000) {
            //title bar for settings or exit settings button
            if(view.getId() == R.id.textViewTitle){
                //current_table_display = (current_table_display+1)%2;
                buildNotification("test");
                if(current_table_display==1){
                    makeSettings();
                }
            }else {

                current_alarms.remove(view.getId() - 100000);
                makeSettings();
            }

        }else{ // corner button
            //current_table_display = (current_table_display+1)%2;
            //if(current_table_display==1){
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                //makeSettings();
            //}
        }

        //updates interface on button click
        Period period = new Period(total_run_time * 1000L);
        String time = PeriodFormat.getDefault().print(period);


        updateInterface("       > total run time = " + time + "\n       > total session kwh delivered = " + df.format(total_session_kwhd));
    }

    private class ParseURL extends AsyncTask<String, Void, String> {
        // Gets all doubles from inside table and outputs them to the display in the form: 11,12,13,;21,22,23,;31,32,33,;

        @Override
        protected String doInBackground(String... strings) {

            try {
                Log.d("JSwa", "Connecting to [" + strings[0] + "]");
                Document doc = Jsoup.connect(strings[0]).followRedirects(true).get();
                Log.d("JSwa", "Connected to [" + strings[0] + "]");

                Element table = doc.select("TABLE").get(0); //select the first table.
                Elements rows = table.select("tr");

                String Matrix_input = "";

                int row_counter = 0;
                for (Element row : rows){
                    if(row_counter!=0) { // 0 is headers row so we skip it
                        Elements columns = row.select("td");
                        int column_counter = 0;
                        for (Element column : columns) {
                            if (column_counter != 0) { //0 is the title column so we skip it
                                if (column.text().matches("-?\\d*\\.?\\d+")) {
                                    Matrix_input += column.text() + ","; // signifies end of column to matrix
                                }
                            }
                            column_counter++;
                        }
                        Matrix_input += ";"; // signifies end of row to matrix
                    }
                    row_counter++;
                }

                theMatrix.add_matrix(Matrix_input);
            } catch (Throwable t) {
                output_string = String.valueOf(t);
            } finally {
                // Maybe do something here. I dunno yet.
                // Updating interface seems good but may move this to just be in timer once im just
                // Reading from matrix for graph
            }

            return "Connected! Loading information...";

        }
    }

    //Timer code
    int delay = TIMER_UPDATE_RATE * 1000;
    Runnable runnable;
    @Override
    protected void onStart() {
        //start handler as activity become visible

        sTask.postDelayed(new Runnable() {
            public void run() {
                if(!Thread.interrupted()) {
                    mTask = (ParseURL) new ParseURL().execute(url);
                }else{
                    onPause();
                }

                //keep track of total time and total kwh delivered
                if(theMatrix.get_all_items(0,0).size() > 1){
                    for(int i = 0; i < theMatrix.getHeight(); i++ ) {
                        ArrayList<Double> temp = theMatrix.get_all_items(i, kwdindex);
                        total_session_kwhd += ((temp.get(temp.size() - 1) - temp.get(temp.size() - 2)));
                    }
                    total_run_time += TIMER_UPDATE_RATE;
                }

                Period period = new Period(total_run_time * 1000L);
                String time = PeriodFormat.getDefault().print(period);

                //makes notifications
                for (String s : current_alarms){
                    String[] ss = s.split(";");
                    if (Double.valueOf(ss[0].split("-")[0]) > theMatrix.get_recent_item(Integer.valueOf(ss[1]), Integer.valueOf(ss[2]))
                            || Double.valueOf(ss[0].split("-")[1]) < theMatrix.get_recent_item(Integer.valueOf(ss[1]), Integer.valueOf(ss[2]))){
                        buildNotification(MeterTitles.get(Integer.valueOf(ss[1])) + "'s "+ StatTitles.get(Integer.valueOf(ss[2])) +" went outside of its set range!");
                        break;
                    }
                }

                updateInterface(collectedString + "       > total run time = " + time + "\n       > total session kwh delivered = " + df.format(total_session_kwhd));

                runnable=this;

                sTask.postDelayed(runnable, delay);
            }
        }, delay);

        super.onStart();
    }

    @Override
    protected void onPause() {
        sTask.removeCallbacks(runnable); //stop handler when activity not visible
        super.onPause();
    }

    //Back button
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (Integer.parseInt(android.os.Build.VERSION.SDK) > 5
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            Log.d("CDA", "onKeyDown Called");
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        Log.d("CDA", "onBackPressed Called");
        mTask.cancel(true);
        Intent setIntent = new Intent(this, StartupActivity.class);
        setIntent.putExtra(USER_DISPLAY, UserDisplay);
        setIntent.putExtra(USER_ID, UserId);
        setIntent.putExtra(get_serverip, ServerIP);
        startActivity(setIntent);
    }

    //Updating the display
    protected void updateInterface(String output){
        title_text.setTextSize(20);
        title_text.setTextColor(Color.BLACK);
        title_text.setBackgroundColor(getResources().getColor(R.color.colorTwelve));
        String text = "<bold><font color=#000000>Meter : </font></bold> <font color=#ffffff>"+meter_titles+"</font>";
        title_text.setText(Html.fromHtml(text));
        title_text.setGravity(Gravity.CENTER);
        output_text.setText(output);
        output_text.setBackgroundColor(getResources().getColor(R.color.colorTwelve));
        output_text.setTextColor(Color.LTGRAY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            output_text.setForeground(getResources().getDrawable(R.drawable.headerbottomborder));
            title_text.setForeground(getResources().getDrawable(R.drawable.headertopborder));
        }

        if(current_table_display==0) {
            buildTable();
            updateGraph();
        }
    }

    protected void buildTable(){
            TableLayout ll = (TableLayout) findViewById(R.id.thetable);
            ll.removeAllViews();


            TableRow row = new TableRow(this);
            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp);

            //Makes first corner button of table
            Button temp_button = new Button(this);
            temp_button.setBackgroundColor(getResources().getColor(BUTTON_TEXT_COLOR));
            temp_button.setText("\u2699");
            temp_button.setOnClickListener(this);
            temp_button.setTextColor(Color.BLACK);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                temp_button.setForeground(getResources().getDrawable(R.drawable.tableimageborder));
            }
            row.addView(temp_button);
            //adds the titles to each column
            int title_count = 0;
            for (String title : column_headers.split(";")) {
                if (title != "") {
                    temp_button = new Button(this);
                    temp_button.setText(title);
                    temp_button.setId(200 + title_count); //title will be in 200's
                    temp_button.setOnClickListener(this);
                    temp_button.setTextColor(getResources().getColor(BUTTON_TEXT_COLOR));
                    temp_button.setBackgroundColor(getResources().getColor(BUTTON_BACK_COLOR));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        temp_button.setForeground(getResources().getDrawable(R.drawable.tableimageborder));
                    }
                    row.addView(temp_button);
                    title_count++;
                }
            }
            ll.addView(row, 0);

            for (int i = 0; i < theMatrix.getHeight(); i++) {

                row = new TableRow(this);
                lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
                row.setLayoutParams(lp);

                for (int j = -1; j < theMatrix.getWidth(); j++) {
                    if (j == -1) {
                        // adds a title to each row
                        temp_button = new Button(this);
                        try {
                            temp_button.setText(row_headers.split(";")[i]);
                        } catch (Exception e) {
                            temp_button.setText("lost data");
                        }
                        temp_button.setId(300 + i); // row titles will be in the 300's
                        temp_button.setOnClickListener(this);
                        temp_button.setTextColor(getResources().getColor(BUTTON_TEXT_COLOR));
                        temp_button.setBackgroundColor(getResources().getColor(BUTTON_BACK_COLOR));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            temp_button.setForeground(getResources().getDrawable(R.drawable.tableimageborder));
                        }
                        row.addView(temp_button);
                    } else {//Adds the main items of the matrix

                        temp_button = new Button(this);
                        if (j != kwdindex || theMatrix.get_all_items(i, j).size() == 1 || url.contains("reactive")) {
                            temp_button.setText(String.valueOf(theMatrix.get_recent_item(i, j)));
                        } else { // we set different text for kwd
                            ArrayList<Double> temp = theMatrix.get_all_items(i, j);
                            temp_button.setText(df.format(temp.get(temp.size() - 1) - temp.get(temp.size() - 2)));
                        }
                        temp_button.setId((i + 1) * (1000) + j); //all matrix buttons will take up the thousands
                        temp_button.setOnClickListener(this);
                        temp_button.setTextColor(Color.BLACK);
                        temp_button.setBackgroundColor(getResources().getColor(BUTTON_TEXT_COLOR));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            temp_button.setForeground(getResources().getDrawable(R.drawable.tableimageborder));
                        }
                        row.addView(temp_button);
                    }
                }
                ll.addView(row, i + 1);
            }
    }

    public LineGraphSeries make_series(ArrayList<Double> data){

        int i = 0;
        int x = -TIMER_UPDATE_RATE*(data.size()-1);
        DataPoint[] values = new DataPoint[data.size()];

        while (i < data.size()){  //maybe debug here?

            values[i] = new DataPoint(x, (data.get(i)));
            x = x+TIMER_UPDATE_RATE;
            i++;
        }

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(values);
        return series;
    }

    public void updateGraph(){

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(screen_width,0,1.0f);
        graph.setLayoutParams(scrollParams);

        graph.setVisibility(View.VISIBLE);
        graph.removeAllSeries();

        GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
        //gridLabel.setVerticalAxisTitle(sectionHeaders.get(set_stat));
        gridLabel.setHorizontalAxisTitle("Seconds");


        for (String dataPoint : selected_items) {
            //gets cur data point
            int row = Integer.valueOf(dataPoint.split(";")[0]);
            int col = Integer.valueOf(dataPoint.split(";")[1]);

            //sets table button bg colour
            if(current_table_display == 0) {
                Button temp = (Button) findViewById((row + 1) * 1000 + col);
                temp.setBackgroundColor(getResources().getColor(COLORS_ARRAY[(row + col) % 10]));
            }

            LineGraphSeries series;
            series = make_series(theMatrix.get_all_items(row, col));
            series.setTitle(row_headers.split(";")[row] + ", " + column_headers.split(";")[col]);
            series.setColor(getResources().getColor(COLORS_ARRAY[(row+col)%10]));
            graph.addSeries(series);
        }
    }

    //notification code
    protected void buildNotification(String s) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        String strRingtonePreference = preference.getString("notifications_new_message_ringtone", "DEFAULT_SOUND");
        Boolean isEnabled = preference.getBoolean("notifications_new_message", false);
        Boolean isVibrate = preference.getBoolean("notifications_new_message_vibrate", true);
        if(isEnabled) {
            long temp_time = System.currentTimeMillis();
            if(cur_time + (long)(60000*min_between_notification) < temp_time ){
                cur_time = temp_time;

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
                mBuilder.setSmallIcon(R.drawable.icon);
                mBuilder.setContentTitle("One of your meter points has gone outside it's set range");
                mBuilder.setContentText(s);

                mBuilder.setSound(Uri.parse(strRingtonePreference));

                if (isVibrate) {
                    mBuilder.setVibrate(new long[]{99999, 50, 50, 50, 1000});
                } else {
                    mBuilder.setVibrate(new long[]{});
                }

                mBuilder.setLights(Color.GREEN, 3000, 3000);

                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                // notificationID allows you to update the notification later on.
                mNotificationManager.notify(1, mBuilder.build());

            }
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

                if(strings[1] == "alarm") {

                    DataOutputStream dout = new DataOutputStream(soc.getOutputStream());
                    //sends all the things needed too keep track of alarm
                    dout.writeUTF("alarm_"  + UserId + "_" + strings[2] + "_" + url);
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

                    dout.close();
                    in.close();
                    soc.close();
                }
            } catch (Exception e) {
                dataString += e;
                e.printStackTrace();
            }

            collectedString = dataString;
            return ("MESSAGE: " + dataString);
        }


    }

    //builds the settings page and hide the graph
    //TODO: clicking a button on then off crashes app, fix that - also make it a batter ui
    protected void makeSettings() {

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.0f);
        graph.setLayoutParams(scrollParams);

        TableLayout ll = (TableLayout) findViewById(R.id.thetable);
        ll.removeAllViews();
        try {

            ViewGroup oldll = (ViewGroup) enter_number.getParent();
            oldll.removeAllViews();

        }catch (Exception e){

        }

        TableRow row= new TableRow(this);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(lp);

        display_text1 = new TextView(this);
        display_text2 = new TextView(this);
        display_text3 = new TextView(this);
        confirmButton = new Button(this);


        display_text1.setText("Select which meter(s): \n");
        display_text2.setText("Select which stat(s): \n");
        display_text3.setText("Enter a range (eg. 200-300): ");

        confirmButton.setText("Confirm");
        confirmButton.setId(700+1);
        confirmButton.setOnClickListener(this);

        Button backButton = new Button(this);
        backButton.setText("back");
        backButton.setId(22+1);
        backButton.setOnClickListener(this);
        row.addView(backButton);
        ll.addView(row);

        //Select the meters
        row = new TableRow(this);
        row.setLayoutParams(lp);
        row.addView(display_text1);
        int index = 0;
        cur_meter_spinner_pos = new ArrayList<>();
        cur_stat_spinner_pos = new ArrayList<>();
        ll.addView(row);
        row = new TableRow(this);
        row.setLayoutParams(lp);
        for(String meter : MeterTitles){
            Button Manyaisthebest = new Button(this);
            Manyaisthebest.setText(meter);
            Manyaisthebest.setId(800+index);
            Manyaisthebest.setOnClickListener(this);
            Manyaisthebest.setBackgroundColor(getResources().getColor(R.color.colorOne));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Manyaisthebest.setForeground(getResources().getDrawable(R.drawable.tableimageborder));
            }
            row.addView(Manyaisthebest);
            index++;
        }
        ll.addView(row);

        //select the stats
        row = new TableRow(this);
        row.setLayoutParams(lp);
        row.addView(display_text2);
        ll.addView(row);
        row = new TableRow(this);
        row.setLayoutParams(lp);
        index = 0;
        for(String stat : StatTitles){
            Button Manyaisthebest = new Button(this);
            Manyaisthebest.setText(stat);
            Manyaisthebest.setBackgroundColor(getResources().getColor(R.color.colorOne));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Manyaisthebest.setForeground(getResources().getDrawable(R.drawable.tableimageborder));
            }
            Manyaisthebest.setId(850+index);
            Manyaisthebest.setOnClickListener(this);
            row.addView(Manyaisthebest);
            index++;
        }
        ll.addView(row);

        row = new TableRow(this);
        row.setLayoutParams(lp);
        row.addView(display_text3);
        row.addView(enter_number);
        row.addView(confirmButton);
        ll.addView(row);

        int i = 0;
        for(String element : current_alarms) {
            Button delete_button = new Button(this);
            delete_button.setId(100000+i);
            delete_button.setOnClickListener(this);
            delete_button.setWidth(10);
            delete_button.setText("Delete");
            row = new TableRow(this);
            row.setLayoutParams(lp);
            display_text1 = new TextView(this);
            display_text1.setText(element);
            row.addView(display_text1);
            row.addView(delete_button);
            ll.addView(row);
            i++;
        }
/*
        row = new TableRow(this);
        row.setLayoutParams(lp);
        Button temp = new Button(this);
        TextView temp_text = new TextView(this);
        temp_text.setId(199-2);
        temp.setText("get server info");
        temp.setId(199-1);
        temp.setOnClickListener(this);
        row.addView(temp);
        row.addView(temp_text);
        ll.addView(row);
*/
    }


}
