package informationaesthetics.powerhawkrt;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static java.lang.Math.min;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    //static variables
    public static int[] COLORS_ARRAY = {R.color.colorOne, R.color.colorTwo, R.color.colorThree, R.color.colorFour, R.color.colorTen , R.color.colorFive, R.color.colorSix,R.color.colorNine, R.color.colorSeven, R.color.colorEight};
    public static int TIMER_UPDATE_RATE = 5;
    public static int URL_PAGES_BEING_READ = 2;

    // key names for getting information in shared prefs and DB
    private static final String SHARED_PREFS = "POWERHAWK_URL_SAVED_PREFS";
    private static final String get_title = "POWERHAWK_TITLE";
    private static final String get_init_input = "POWERHAWK_OUTPUT";
    private static final String get_rows = "POWERHAWK_ROWS";
    private static final String get_columns = "POWERHAWK_COLUMNS";

    // Interface objects
    GraphView graph; // the graph
    TextView timeSince; //text view that displays under graph
    LinearLayout[] spinners; // holds each layout which holds the spinners
    LinearLayout[] headers;
    Boolean first_read = true;

    //useful variables for interface
    Map<Integer, Series> currentlyDisplayedSeries = new HashMap<>(); //holds all the series displayed on the graph
    Map<Integer, String> currentlyDisplayedSeriesInfo = new HashMap<>();
    Map<Integer, Series> currentlyDisplayedSeriesMinutes = new HashMap<>();
    Map<Integer, Series> currentlyDisplayedSeriesHours = new HashMap<>();
    Integer labelCount = 0; //keeps track of how of how many labels there are
    //String[] extraStuff; // string representation of labels of graph

    //variables for data
    String[] rows; // holds titles of each row for each URL
    String[] columns; // holds titles for each column
    String[] urls; // holds the addresses of each URL
    String[] titles; // holds the titles for each meter
    String[] data_arrays; // holds all the data temporarily
    String[] data_arrays_minutes; // holds all the data temporarily
    String[] data_arrays_hours; // holds all the data temporarily
    MatrixArray[] matrix_array; // holds all the data in an easier-to-acess kind of way
    MatrixArray[] matrix_array_minutes; // holds all the data in an easier-to-acess kind of way
    MatrixArray[] matrix_array_hours; // holds all the data in an easier-to-acess kind of way


    //variables for database
    int sec_min_hours = 0;
    int index = 0; // to set up all variables
    int stats_index = 0; //also to help set up all variables
    int readCountDatabase = 0;
    String[] stat_array = new String[4];
    String date = ""; // keeps track of last time we read from database
    String stats = "recent_data";
    String server;
    String mac;
    Boolean hasNextStat = false;
    String result;
    SharedPreferences sharedPref;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPref = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
        database = FirebaseDatabase.getInstance();

        //setup stat array
        stat_array[0] = "DATA";
        stat_array[1] = "DATAMINUTES";
        stat_array[2] = "DATAHOURS";
        stat_array[3] = "HEADERNAMES";

        //load the data into preferences
        getSharedPreferences();

        //set up text view
        timeSince = (TextView) findViewById(R.id.time_scale_text);

        //set up matrix
        setUpMatricies();

        //build the layout
        buildGraph();
        buildLayout();

        //update data from firebase
        collectAllData();

        //set time scale spinner
        setUpTimeScaleSpinner();

    }

    public void setUpTimeScaleSpinner(){
        Spinner timeScaleSpinner = (Spinner)findViewById(R.id.time_scale_spinner);

        String[] spinnerObjects = {"Seconds","Minutes","Hours"};

        timeScaleSpinner.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item,spinnerObjects));

        timeScaleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                sec_min_hours = position;
                updateGraph();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onBackPressed(){
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signOut();
        Intent intent = new Intent(this, FirebaseLoginActivity.class);
        startActivity(intent);
    }

    public void setHeader() {
        LinearLayout main = (LinearLayout) findViewById(R.id.main_layout_back);
        //set up header at top
        RelativeLayout.LayoutParams header_lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        LinearLayout actual_header = new LinearLayout(this);
        actual_header.setLayoutParams(header_lp);
        actual_header.setGravity(Gravity.CENTER_VERTICAL);
        actual_header.setOrientation(LinearLayout.HORIZONTAL);
        actual_header.setBackgroundColor(getResources().getColor(R.color.colorBackGroundNewForManya));
        actual_header.setId(100-1);
        //icon for header
        ImageView icon = new ImageView(this);
        icon.setBackgroundResource(R.drawable.icon_official);
        Toolbar.LayoutParams icon_lp = new Toolbar.LayoutParams(100, 100);
        icon.setLayoutParams(icon_lp);
        actual_header.addView(icon);
        //text for header
        TextView headerText = new TextView(this);
        headerText.setText("Powerhawk Real Time");
        headerText.setTextColor(Color.WHITE);
        actual_header.addView(headerText);
        //add header to main
        main.addView(actual_header);
    }

    public Bitmap colorize(Bitmap srcBmp, int dstColor) {

        int width = srcBmp.getWidth();
        int height = srcBmp.getHeight();

        float srcHSV[] = new float[3];
        float dstHSV[] = new float[3];

        Bitmap dstBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int pixel = srcBmp.getPixel(col, row);
                int alpha = Color.alpha(pixel);
                Color.colorToHSV(pixel, srcHSV);
                Color.colorToHSV(dstColor, dstHSV);

                // If it area to be painted set only value of original image
                dstHSV[2] = srcHSV[2];  // value
                dstBitmap.setPixel(col, row, Color.HSVToColor(alpha, dstHSV));
            }
        }

        return dstBitmap;
    }

    protected void buildLayout(){
        //build the layout
        LinearLayout main = (LinearLayout) findViewById(R.id.main_layout);
        main.removeAllViews();
        main.setBackgroundResource(R.color.colorBackGroundNew);

        setHeader();
        timeSince.setText("since " + date);

        for (int i = 0; i < urls.length; i += URL_PAGES_BEING_READ){ //counting by URL_PAGES_BEING_READ since each URL has multiple pages
            // make a linear layout for each URL
            LinearLayout unit_layout = new LinearLayout(this);
            unit_layout.setPadding(70,70,0,0);
            unit_layout.setOrientation(LinearLayout.VERTICAL);
            RelativeLayout.LayoutParams unit_lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            unit_layout.setBackgroundResource(R.drawable.border);
            unit_layout.setLayoutParams(unit_lp);
            unit_layout.setId(100 + i);

            //make the header
            LinearLayout headerLayout = makeHeaderLayout(i);
            headers[i] = headerLayout; //save spinners - we don't remake these

            //makes spinners and controls
            LinearLayout spinnerLayout = makeSpinnerLayout(i);
            spinners[i] = spinnerLayout; //save spinners - we don't remake these

            //makes the extra space to put label headers
            LinearLayout extraSpace = makeExtraStuffLayout(i);

            unit_layout.addView(headerLayout);
            unit_layout.addView(extraSpace);
            unit_layout.addView(spinnerLayout);
            main.addView(unit_layout);
        }

        LinearLayout g_layout = (LinearLayout) findViewById(R.id.main_layout_graph);
        try {
            g_layout.addView(graph);
        }catch (Exception e){
            //graph already on layout
        }
    }

    protected LinearLayout makeExtraStuffLayout(int i){
        LinearLayout extraLayout = new LinearLayout(this);
        extraLayout.setOrientation(LinearLayout.VERTICAL);
        extraLayout.setId(520+i);

        Iterator iterator = currentlyDisplayedSeries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry)iterator.next();

            int cur_label = (Integer) pair.getKey();
            String info_string = currentlyDisplayedSeriesInfo.get(cur_label);
            int row = Integer.valueOf( info_string.split(",")[1] );
            int column = Integer.valueOf(info_string.split(",")[2]);
            int url =Integer.valueOf( info_string.split(",")[0]);

            //if i in url for active or reactive
            if(i == url || i == url-1) {
                LinearLayout item = makeItemOnRow(url, row, column);
                extraLayout.addView(item);
            }

        }

        return extraLayout;
    }

    protected LinearLayout makeHeaderLayout(int i){
        // make a layout for the header of each unit - header will include basic meter info and an image
        TableLayout header_layout = new TableLayout(this);
        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);

        final String[] rowsInHeader = {"Address: ", "Unit: ", "Watts: ", "kwh delivered: "};
        for (String row_text : rowsInHeader){
            TableRow row = new TableRow(this);
            row.setLayoutParams(lp);

            TextView title = new TextView(this);
            TextView content = new TextView(this);

            title.setTextColor(Color.BLACK);

            title.setText(row_text);
            switch (row_text){
                case "Address: ":
                    content.setText(urls[i].split("/")[2]);  //may need to update this for now hides active/reactive label
                    break;
                case "Unit: ":
                    content.setText("temp");//titles[i]);
                    break;
                case "Watts: ":
                    int watt_index = Arrays.asList(columns[i].split(";")).indexOf(" watts ");

                    if (watt_index == -1){
                        watt_index = Arrays.asList(columns[i].split(";")).indexOf(" w ");
                    }

                    int total_watts = 0;
                    for (int j = 0; j < matrix_array[i].getHeight(); j++) {
                        total_watts += matrix_array[i].get_recent_item(j, watt_index);
                    }

                    content.setText(String.valueOf(total_watts));
                    break;
                case "kwh delivered: ":
                    int kwh_index = Arrays.asList(columns[i].split(";")).indexOf(" kwh delivered ");
                    if (kwh_index != -1 && kwh_index < matrix_array[i].getWidth()) {
                        double total_kwh = 0;
                        for (int j = 0; j < matrix_array[i].getHeight(); j++) {
                            total_kwh += matrix_array[i].get_recent_item(j, kwh_index);
                        }
                        DecimalFormat df = new DecimalFormat("#.####");
                        content.setText(df.format(total_kwh));
                    } else {
                        content.setText("not found");
                    }
                    break;
            }

            row.addView(title);
            row.addView(content);
            header_layout.addView(row);
        }
        return header_layout;
    }

    protected LinearLayout makeSpinnerLayout(int i){
        //add controls for each layout
        LinearLayout spinnerLayout = new LinearLayout(this);
        spinnerLayout.setOrientation(LinearLayout.HORIZONTAL);

        final Spinner rowsSpinner = new Spinner(this);
        final Spinner columnsSpinner = new Spinner(this);

        rowsSpinner.setBackgroundResource(R.drawable.spinnerback);
        columnsSpinner.setBackgroundResource(R.drawable.spinnerback);

        rowsSpinner.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item,rows[i].split(";")));

        //add columns from each page
        String[] temp_cols = new String[columns[i].split(";").length + columns[i+1].split(";").length ];

        System.arraycopy(columns[i].split(";"), 0, temp_cols, 0, columns[i].split(";").length);
        System.arraycopy(columns[i+1].split(";"), 0, temp_cols, columns[i].split(";").length, columns[i+1].split(";").length);

        columnsSpinner.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item,temp_cols));

        Button confirmButton = new Button(this);
        confirmButton.setText("+");
        RelativeLayout.LayoutParams unit_lp = new RelativeLayout.LayoutParams(150, 150);
        confirmButton.setLayoutParams(unit_lp);
        confirmButton.setId(420+i);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int url_layout = v.getId() - 420;
                int actual_url = url_layout;
                int row = rowsSpinner.getSelectedItemPosition();
                int column = columnsSpinner.getSelectedItemPosition();

                //check if active or reactive page
                try{
                    //active page
                    String test = columns[url_layout].split(";")[column];
                }catch (Exception e){
                    //reactive page
                    column = column - columns[url_layout].split(";").length;
                    actual_url += 1;
                }

                addSeriesToGraph(actual_url,row,column);
                LinearLayout observedItem = makeItemOnRow(actual_url,row,column);
                LinearLayout extraStuff = (LinearLayout) findViewById(520+url_layout);
                extraStuff.addView(observedItem);

                labelCount++;
            }
        });

        spinnerLayout.addView(confirmButton);
        spinnerLayout.addView(rowsSpinner);
        spinnerLayout.addView(columnsSpinner);
        return spinnerLayout;
    }

    protected LinearLayout makeItemOnRow (int url, int row, int column) {
        LinearLayout observedItem = new LinearLayout(getApplicationContext());
        observedItem.setOrientation(LinearLayout.HORIZONTAL);
        TextView itemText = new TextView(getApplicationContext());
        itemText.setText(rows[url].split(";")[row] + ", " + columns[url].split(";")[column] + ": " + matrix_array[url].get_recent_item(row, column));
        itemText.setTextColor(Color.BLACK);

        Button removeButton = new Button(getApplicationContext());
        removeButton.setText("-");
        RelativeLayout.LayoutParams unit_lp = new RelativeLayout.LayoutParams(150, 150);
        removeButton.setLayoutParams(unit_lp);

        Bitmap background = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.btndefault);

        removeButton.setBackground(new BitmapDrawable(colorize(background, getResources().getColor(COLORS_ARRAY[(url+row+column)%COLORS_ARRAY.length]))));
        removeButton.setId(720 + getUniqueLabel(url,row,column));
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeSeriesFromGraph(v.getId() - 720);
                ((ViewGroup)v.getParent().getParent()).removeView((ViewGroup)v.getParent());
            }
        });

        observedItem.addView(removeButton);
        observedItem.addView(itemText);
        return observedItem;
    }

    protected void setUpMatricies(){

        //set up matrices
        for (int i = 0; i < data_arrays.length; i ++){
            try {
                String[] inputs = data_arrays[i].split("!");
                matrix_array[i] = new MatrixArray(inputs[0]);
                int count = 1;
                while (count < min(inputs.length - 1, 100)){
                    matrix_array[i].add_matrix(inputs[count]);
                    count++;
                }
            } catch (Exception e){
                // theMatrix = new MatrixArray(initial_input);
            }
        }

        //set up matrices
        for (int i = 0; i < data_arrays_minutes.length; i ++){
            try {
                String[] inputs = data_arrays_minutes[i].split("!");
                matrix_array_minutes[i] = new MatrixArray(inputs[0]);
                int count = 1;
                while (count < min(inputs.length - 1, 100)){
                    matrix_array_minutes[i].add_matrix(inputs[count]);
                    count++;
                }
            } catch (Exception e){
                // theMatrix = new MatrixArray(initial_input);
            }
        }

        //set up matrices
        for (int i = 0; i < data_arrays_hours.length; i ++){
            try {
                String[] inputs = data_arrays_hours[i].split("!");
                matrix_array_hours[i] = new MatrixArray(inputs[0]);
                int count = 1;
                while (count < min(inputs.length - 1, 100)){
                    matrix_array_hours[i].add_matrix(inputs[count]);
                    count++;
                }
            } catch (Exception e){
                // theMatrix = new MatrixArray(initial_input);
            }
        }

    }

    private void getSharedPreferences() {
        sharedPref = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        urls = sharedPref.getString("URLS", "").split("URLSPLIT");
        date = sharedPref.getString("date", "");
        mac = sharedPref.getString("SERVERMAC", "");
        server = sharedPref.getString("server", "");

        if (first_read) {
            headers = new LinearLayout[urls.length];
            spinners = new LinearLayout[urls.length];
            rows = new String[urls.length];
            columns = new String[urls.length];
            titles = new String[urls.length];
           // extraStuff = new String[urls.length];
            matrix_array = new MatrixArray[urls.length];
            matrix_array_minutes = new MatrixArray[urls.length];
            matrix_array_hours = new MatrixArray[urls.length];
            data_arrays = new String[urls.length];
            data_arrays_minutes = new String[urls.length];
            data_arrays_hours = new String[urls.length];
            first_read = false;
        }

        for (int i = 0; i < urls.length; i++) {
            for (String stat : stat_array){
                switch (stat) {
                    case "HEADERNAMES":
                        rows[i] = sharedPref.getString(stripper(urls[i]) + stat, "").split(";")[0].replace(",", ";");
                        columns[i] = sharedPref.getString(stripper(urls[i]) + stat, "").split(";")[1];

                        //add acctive/reactive labels for elements
                        String[] tempcol = columns[i].split(",");
                        columns[i] = "";
                        for(int j = 0; j < tempcol.length; j++){
                            if(tempcol[j].toLowerCase().contains("element")){

                                tempcol[j] += (i%2==0 ? " (Watts)" : " (VAR)");
                            }
                            columns[i] += " " + tempcol[j] + " ;";
                        }
                        columns[i] = columns[i].substring(0, columns[i].length() - 1);
                        break;
                    case "DATA":
                        data_arrays[i] = sharedPref.getString(stripper(urls[i]) + stat, "");
                        break;
                    case "DATAMINUTES":
                        data_arrays_minutes[i] = sharedPref.getString(stripper(urls[i]) + stat, "");
                        break;
                    case "DATAHOURS":
                        data_arrays_hours[i] = sharedPref.getString(stripper(urls[i]) + stat, "");
                        break;
                }
            }

            /*
            rows[i] = sharedPref.getString(get_rows + urls[i], "");
            columns[i] = sharedPref.getString(get_columns + urls[i], "");
            titles[i] = sharedPref.getString(get_title + urls[i], "");
            data_arrays[i] = sharedPref.getString(get_init_input + urls[i], "");
            */

        }

    }

    public String stripper(String og){
        //returns og without the following characters:
        // . / :
        return og.replace(":", "").replace(".", "").replace("/", "");
    }

    public LineGraphSeries make_series(ArrayList<Double> data, int type){

        int i = 0;
        int x = -(type==0 ? TIMER_UPDATE_RATE : 1)*(data.size()-1);
        DataPoint[] values = new DataPoint[data.size()];

        while (i < data.size()){  //maybe debug here?

            values[i] = new DataPoint(x, (data.get(i)));
            x = x+ (type==0 ? TIMER_UPDATE_RATE : 1);
            i++;
        }

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(values);
        return series;
    }

    public void buildGraph(){

        GraphView graphView = new GraphView(this);
        graphView.setPadding(0,0,0,0);
        graphView.getViewport().setMaxX(0);
        graphView.getViewport().setMinX(-120);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setScrollable(true);
        graphView.getViewport().setScalable(true);
        graphView.setBackgroundResource(R.drawable.bordergraph);
        graph = graphView;

        //GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
        //gridLabel.setHorizontalAxisTitle("seconds since " + date);
    }

    public void addSeriesToGraph(int url, int row, int column){
        LineGraphSeries series = make_series(matrix_array[url].get_all_items(row,column), 0);
        series.setColor(getResources().getColor(COLORS_ARRAY[(url+row+column)%10]));
        String title = urls[url] + rows[url].split(";")[row] + columns[url].split(";")[column];
        series.setTitle(title);

        LineGraphSeries seriesminutes = make_series(matrix_array_minutes[url].get_all_items(row,column), 1);
        series.setColor(getResources().getColor(COLORS_ARRAY[(url+row+column)%10]));
        series.setTitle(title);

        LineGraphSeries serieshours = make_series(matrix_array_hours[url].get_all_items(row,column), 2);
        series.setColor(getResources().getColor(COLORS_ARRAY[(url+row+column)%10]));
        series.setTitle(title);

        currentlyDisplayedSeries.put(getUniqueLabel(url,row,column), series);
        currentlyDisplayedSeriesMinutes.put(getUniqueLabel(url,row,column), seriesminutes);
        currentlyDisplayedSeriesHours.put(getUniqueLabel(url,row,column), serieshours);
        currentlyDisplayedSeriesInfo.put(getUniqueLabel(url,row,column), url + "," + row + "," + column);
        switch (sec_min_hours){
            case 0:
                graph.addSeries(series);
                break;
            case 1:
                graph.addSeries(seriesminutes);
                break;
            case 2:
                graph.addSeries(serieshours);
                break;
        }

    }

    public void removeSeriesFromGraph(Integer seriesLabel){
        switch (sec_min_hours){
            case 0:
                graph.removeSeries(currentlyDisplayedSeries.get(seriesLabel));
                break;
            case 1:
                graph.removeSeries(currentlyDisplayedSeriesMinutes.get(seriesLabel));
                break;
            case 2:
                graph.removeSeries(currentlyDisplayedSeriesHours.get(seriesLabel));
                break;
        }
        currentlyDisplayedSeries.remove(seriesLabel);
        currentlyDisplayedSeriesMinutes.remove(seriesLabel);
        currentlyDisplayedSeriesHours.remove(seriesLabel);
        currentlyDisplayedSeriesInfo.remove(seriesLabel);
    }

    public int getUniqueLabel(int one, int two, int three){
        return Integer.valueOf(String.valueOf(one) + String.valueOf(two) + String.valueOf(three));
    }

    //methods for continuously updating the layout
    public void updateLayout(){
        // re draws the layout to update numbers
        updateGraph();

        //build the layout
        LinearLayout main = (LinearLayout) findViewById(R.id.main_layout);
        main.removeAllViews();

        setHeader();

        for (int i = 0; i < urls.length; i+= URL_PAGES_BEING_READ) {
            LinearLayout unit_layout = new LinearLayout(this);
            unit_layout.setPadding(70,70,0,0);
            unit_layout.setOrientation(LinearLayout.VERTICAL);
            RelativeLayout.LayoutParams unit_lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            unit_layout.setBackgroundResource(R.drawable.border);
            unit_layout.setLayoutParams(unit_lp);
            unit_layout.setId(100 + i);

            LinearLayout header = makeHeaderLayout(i);

            //makes the extra space to put label headers
            LinearLayout extraSpace = makeExtraStuffLayout(i);

            unit_layout.addView(header);
            unit_layout.addView(extraSpace);
            ((ViewGroup)spinners[i].getParent()).removeView(spinners[i]);
            unit_layout.addView(spinners[i]);
            main.addView(unit_layout);
        }
    }

    public void updateGraph(){
        //re draws the graph
        graph.removeAllSeries();

        GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
        String timescale = "";
        switch (sec_min_hours){
            case 0:
                timescale = "seconds";
                break;
            case 1:
                timescale = "minutes";
                break;
            case 2:
                timescale = "hours";
                break;
        }
        //gridLabel.setHorizontalAxisTitle(timescale + " since " + date);
        timeSince.setText(" since " + date);

        Iterator iterator = currentlyDisplayedSeries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry)iterator.next();

            int cur_label = (Integer) pair.getKey();
            String info_string = currentlyDisplayedSeriesInfo.get(cur_label);
            int row = Integer.valueOf( info_string.split(",")[1] );
            int column = Integer.valueOf(info_string.split(",")[2]);
            int url =Integer.valueOf( info_string.split(",")[0]);
            MatrixArray[] array_to_use;
            switch (sec_min_hours){
                case 0:
                    array_to_use = matrix_array;
                    break;
                case 1:
                    array_to_use = matrix_array_minutes;
                    break;
                default:
                    array_to_use = matrix_array_hours;
                    break;
            }
            LineGraphSeries series = make_series(array_to_use[url].get_all_items(row, column), sec_min_hours);
            series.setColor(getResources().getColor(COLORS_ARRAY[(url+row+column)%10]));
            String title = urls[url] + rows[url].split(";")[row] + columns[url].split(";")[column];
            series.setTitle(title);

            currentlyDisplayedSeries.put(cur_label, series);

            graph.addSeries(series);
        }
    }

    //database collection
    public void collectAllData(){

        //breaks if we've collected all the data
        if(stats_index >= stat_array.length){
            readCountDatabase++;
            if (readCountDatabase >= 3) {
                getSharedPreferences();
                setUpMatricies();
                updateLayout();
                readCountDatabase = 0;
            }
            return;
        }

        //placeStatInPrefs
        if (hasNextStat){
            SharedPreferences.Editor editor = sharedPref.edit();
            //puts the read value in shared pref
            editor.putString(get_init_input+urls[index], result);
            editor.apply();
            index++;
            if (index == urls.length){
                index = 0;
                stats_index ++;
            }
            hasNextStat = false;
        }

        //retrieves each stat value from database
        if(index < urls.length && stats_index < stat_array.length - 1) {
            getStat(index, stats_index);
        } else {
            //and now we wait
        }
    }

    public void updateData(String data, int url){
        SharedPreferences.Editor editor = sharedPref.edit();
        //puts the read value in shared pref
        if (data != "") {
            editor.putString(get_init_input + urls[url], data);
            editor.apply();
            index++;
        }
    }

    public void updateDate(String value){
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putString("date", value);

        editor.commit();
    }

    public void getStat(final int index, final int stats_index){
        //sets result to the current read data
        // sets a specific statistic for a user
        String refString = "/" + server.replace(".", "") +  mac + "/" + urls[index].replaceAll("[./:]", "") + "/" + stat_array[stats_index];
        DatabaseReference statData = database.getReference(refString);

        //get last read date
        String refString2 = "/" + server.replace(".", "") + mac + "/" + urls[index].replaceAll("[./:]", "")+  "/" + "date";
        DatabaseReference statData2 = database.getReference(refString2);

        statData2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                date = dataSnapshot.getValue(String.class);
                updateDate(date);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        // Attach a listener to read the data at our posts reference
        statData.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String value = dataSnapshot.getValue(String.class);
                result = value;

                SharedPreferences.Editor editor = sharedPref.edit();
                //puts the read value in shared pref
                editor.putString(stripper(urls[index]) + stat_array[stats_index], result);
                editor.apply();

                //calls to collect data after previous data is read
                hasNextStat = true;
                if (index <= urls.length) {
                    collectAllData();
                } else {
                    //updateData(value, index); not sure if this still being used
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
        }
    }
}
