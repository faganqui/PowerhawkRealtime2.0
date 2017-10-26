package informationaesthetics.powerhawkrealtime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
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
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.min;

public class MainActivity extends AppCompatActivity {

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
    GraphView graph;

    //variables for data
    String[] rows; // holds titles of each row for each URL
    String[] columns; // holds titles for each column
    String[] urls; // holds the addresses of each URL
    String[] titles; // holds the titles for each meter
    String[] data_arrays; // holds all the data temporarily
    MatrixArray[] matrix_array; // holds all the data in an easier-to-acess kind of way
    Map<Integer, Series> currentlyDisplayedSeries = new HashMap<>();
    Integer labelCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //load the data into preferences
        getSharedPreferences();

        //set up matrix
        setUpMatricies();

        //build the layout
        buildLayout();
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

    protected void buildLayout(){
        //build the layout
        LinearLayout main = (LinearLayout) findViewById(R.id.main_layout);

        setHeader();

        for (int i = 0; i < urls.length; i += URL_PAGES_BEING_READ){ //counting by URL_PAGES_BEING_READ since each URL has multiple pages
            // make a linear layout for each URL
            LinearLayout unit_layout = new LinearLayout(this);
            unit_layout.setPadding(70,70,0,0);
            unit_layout.setOrientation(LinearLayout.VERTICAL);
            RelativeLayout.LayoutParams unit_lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            unit_layout.setBackgroundResource(R.drawable.border);
            unit_layout.setLayoutParams(unit_lp);
            unit_layout.setId(100 + i);

            LinearLayout headerLayout = makeHeaderLayout(i);

            LinearLayout spinnerLayout = makeSpinnerLayout(i);

            LinearLayout extraSpace = new LinearLayout(this);
            extraSpace.setOrientation(LinearLayout.VERTICAL);
            extraSpace.setId(520+i);

            unit_layout.addView(headerLayout);
            unit_layout.addView(extraSpace);
            unit_layout.addView(spinnerLayout);
            main.addView(unit_layout);
        }

        buildGraph();

        LinearLayout g_layout = (LinearLayout) findViewById(R.id.main_layout_graph);
        g_layout.addView(graph);
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

            title.setText(row_text);
            switch (row_text){
                case "Address: ":
                    content.setText(urls[i].split("/")[2]);  //may need to update this for now hides active/reactive label
                    break;
                case "Unit: ":
                    content.setText(titles[i]);
                    break;
                case "Watts: ":
                    int watt_index = Arrays.asList(columns[i].split(";")).indexOf(" Watts ");

                    if (watt_index == -1){
                        watt_index = Arrays.asList(columns[i].split(";")).indexOf(" W ");
                    }

                    int total_watts = 0;
                    for (int j = 0; j < matrix_array[i].getHeight(); j++) {
                        total_watts += matrix_array[i].get_recent_item(j, watt_index);
                    }

                    content.setText(String.valueOf(total_watts));
                    break;
                case "kwh delivered: ":
                    int kwh_index = Arrays.asList(columns[i].split(";")).indexOf(" kWh delivered ");
                    if (kwh_index != -1 && kwh_index < matrix_array[i].getWidth()) {
                        int total_kwh = 0;
                        for (int j = 0; j < matrix_array[i].getHeight(); j++) {
                            total_kwh += matrix_array[i].get_recent_item(j, kwh_index);
                        }
                        content.setText(String.valueOf(total_kwh));
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

        rowsSpinner.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item,rows[i].split(";")));
        columnsSpinner.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item,columns[i].split(";")));

        Button confirmButton = new Button(this);
        confirmButton.setText("+");
        confirmButton.setId(420+i);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int url = v.getId() - 420;
                int row = rowsSpinner.getSelectedItemPosition();
                int column = columnsSpinner.getSelectedItemPosition();
                addSeriesToGraph(url,row,column);

                LinearLayout observedItem = new LinearLayout(getApplicationContext());
                observedItem.setOrientation(LinearLayout.HORIZONTAL);
                TextView itemText = new TextView(getApplicationContext());
                itemText.setText(rows[url].split(";")[row] + ", " + columns[url].split(";")[column] + ": " + matrix_array[url].get_recent_item(row, column));
                itemText.setTextColor(getResources().getColor(COLORS_ARRAY[(url+row+column)%COLORS_ARRAY.length]));

                Button removeButton = new Button(getApplicationContext());
                removeButton.setText("-");
                removeButton.setId(720 + labelCount);
                removeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeSeriesFromGraph(v.getId() - 720);
                        ((ViewGroup)v.getParent().getParent()).removeView((ViewGroup)v.getParent());
                    }
                });

                observedItem.addView(removeButton);
                observedItem.addView(itemText);

                LinearLayout extraStuff = (LinearLayout) findViewById(520+url);
                extraStuff.addView(observedItem);
                labelCount++;
            }
        });

        spinnerLayout.addView(rowsSpinner);
        spinnerLayout.addView(columnsSpinner);
        spinnerLayout.addView(confirmButton);
        return spinnerLayout;
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
    }

    private void getSharedPreferences() {
        SharedPreferences sharedPref = this.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE);
        urls = sharedPref.getString("urls","").split("URLSPLIT");

        rows = new String[urls.length];
        columns = new String[urls.length];
        titles = new String[urls.length];
        matrix_array = new MatrixArray[urls.length];
        data_arrays = new String[urls.length];

        for (int i = 0; i < urls.length; i++) {
            rows[i] = sharedPref.getString(get_rows + urls[i], "");
            columns[i] = sharedPref.getString(get_columns + urls[i], "");
            titles[i] = sharedPref.getString(get_title + urls[i], "");
            data_arrays[i] = sharedPref.getString(get_init_input + urls[i], "");
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

    public void buildGraph(){
        GraphView graphView = new GraphView(this);
        graphView.setPadding(0,0,0,0);
        graphView.getViewport().setMaxX(0);
        graphView.getViewport().setMinX(-120);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setScrollable(true);
        graphView.getViewport().setScalable(true);
        graph = graphView;
    }

    public void addSeriesToGraph(int url, int row, int column){
        LineGraphSeries series = make_series(matrix_array[url].get_all_items(row,column));
        series.setColor(getResources().getColor(COLORS_ARRAY[(url+row+column)%10]));
        String title = urls[url] + rows[url].split(";")[row] + columns[url].split(";")[column];
        series.setTitle(title);
        currentlyDisplayedSeries.put(labelCount, series);
        graph.addSeries(series);
    }

    public void removeSeriesFromGraph(Integer seriesLabel){
        graph.removeSeries(currentlyDisplayedSeries.get(seriesLabel));
        currentlyDisplayedSeries.remove(seriesLabel);
    }

}
