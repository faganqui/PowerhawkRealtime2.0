package informationaesthetics.powerhawkrealtime;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //static variables
    public static int[] COLORS_ARRAY = {R.color.colorOne, R.color.colorTwo, R.color.colorThree, R.color.colorFour, R.color.colorTen , R.color.colorFive, R.color.colorSix,R.color.colorNine, R.color.colorSeven, R.color.colorEight};
    public static int TIMER_UPDATE_RATE = 5;


    //gets screen size for making dynamic interface
    DisplayMetrics displaymetrics = new DisplayMetrics();
    int screen_height;
    int screen_width;

    // key names for getting information in shared prefs and DB
    private static final String SHARED_PREFS = "POWERHAWK_URL_SAVED_PREFS";
    private static final String get_title = "POWERHAWK_TITLE";
    private static final String get_init_input = "POWERHAWK_OUTPUT";
    private static final String get_rows = "POWERHAWK_ROWS";
    private static final String get_columns = "POWERHAWK_COLUMNS";

    //variables for data
    String[] rows; // holds titles of each row for each URL
    String[] columns; // holds titles for each column
    String[] urls; // holds the addresses of each URL
    String[] titles; // holds the titles for each meter
    String[] data_arrays; // holds all the data temporarily
    MatrixArray[] matrix_array; // holds all the data in an easier-to-acess kind of way

    //todo fill this for graph
    ArrayList<String> selected_items = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //load the data into preferences
        getSharedPreferences();

        //set up matrices
        for (int i = 0; i < data_arrays.length; i ++){
            matrix_array[i] = new MatrixArray(data_arrays[i]);
        }

        selected_items.add("1;1");

        GraphView graph = (GraphView) findViewById(R.id.graph);
        graph.setPadding(0,0,0,0);
        graph.setBackgroundColor(getResources().getColor(R.color.colorBackGroundNew));
        graph.getViewport().setMaxX(0);
        graph.getViewport().setMinX(-120);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalable(true);

        updateGraph(graph, 0);

        TextView view = new TextView(this);
        LinearLayout main = (LinearLayout) findViewById(R.id.main_layout);
        view.setText("herro");
        main.addView(view);
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

    public void updateGraph(GraphView graph, int index){

        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(screen_width,0,1.0f);
        graph.setLayoutParams(scrollParams);

        graph.removeAllSeries();

        GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
        //gridLabel.setVerticalAxisTitle(sectionHeaders.get(set_stat));
        gridLabel.setHorizontalAxisTitle("Seconds");


        for (String dataPoint : selected_items) {
            //gets cur data point
            int row = Integer.valueOf(dataPoint.split(";")[0]);
            int col = Integer.valueOf(dataPoint.split(";")[1]);

            LineGraphSeries series;
            series = make_series(matrix_array[index].get_all_items(row, col));
            series.setTitle(urls[index] + ", " + rows[index].split(";")[row] + ", " + columns[index].split(";")[col]);
            series.setColor(getResources().getColor(COLORS_ARRAY[(row+col)%10]));
            graph.addSeries(series);
        }
    }

}
