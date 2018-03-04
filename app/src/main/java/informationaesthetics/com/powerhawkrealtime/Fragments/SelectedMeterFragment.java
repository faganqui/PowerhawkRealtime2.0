package informationaesthetics.com.powerhawkrealtime.Fragments;

import android.content.Context;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.LoginFilter;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.ArcProgress;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;

import informationaesthetics.com.powerhawkrealtime.Data.UserData;
import informationaesthetics.com.powerhawkrealtime.Objects.Meter;
import informationaesthetics.com.powerhawkrealtime.Objects.Unit;
import informationaesthetics.com.powerhawkrealtime.R;
import informationaesthetics.com.powerhawkrealtime.Utilities.Utilities;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static java.lang.String.format;


public class SelectedMeterFragment extends Fragment implements View.OnClickListener {

    HashMap<Integer, String> rowIds = new HashMap<>();
    TableLayout table;
    View rootView;
    GraphView graph = null;
    int[] colors;
    double timeDiff = 5;
    /*
      This array contains hashmaps of the meterpoints data - each hash map is for a single meter point
      the first item in the array is always the totals of all meterpoints      */

    private String selectedRow = "var element1";
    private TableRow actualSelectedRow;

    public SelectedMeterFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_selected_meter, container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            colors = new int[]{getContext().getColor(R.color.colorPrimary), getContext().getColor(R.color.colorPrimaryOrange), getContext().getColor(R.color.colorPrimaryBlue)};
        }

        this.rootView = rootView;

        buildTableView(rootView);
        buildGraphView();

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void buildTableView(View rootView){
        table = rootView.findViewById(R.id.meter_points_table);
        ArrayList<HashMap<String,String>> meterPoints = UserData.getInstance().getSelectedMeter().getMeterPoints();
        if(meterPoints.size() > 1){
            // make headers
            TableRow row_one = buildRow(rootView, "");
            row_one.addView(buildTextView(rootView,""));
            for(int i = 1; i < meterPoints.size()+1; i++) {
                if (i == meterPoints.size()){
                    row_one.addView(buildTextView(rootView, "total"));
                } else {
                    TextView t = buildTextView(rootView, "element " + String.valueOf(i));
                    t.setTextColor(colors[i]);
                    row_one.addView(t);
                }
            }
            table.addView(row_one);
            // make each other row
            for (Object key : (Object[])meterPoints.get(1).keySet().toArray()) {
                TableRow row = buildRow(rootView, (String)key);
                row.addView(buildTextView(rootView, format(" %s  ", key.toString().split(" ")[0].replaceAll("\\d", ""))));
                for (int j = 1; j < meterPoints.size() + 1; j++) {
                    if (j == meterPoints.size()) {
                        row.addView(buildTextView(rootView, meterPoints.get(0).get(key.toString().split(" ")[0].replaceAll("\\d", ""))));
                    } else {
                        row.addView(buildTextView(rootView, meterPoints.get(j).get(key.toString().split("\\d")[0] + j)));
                    }
                }
                table.addView(row);
            }
        }
    }

    public void buildGraphView() {
        if (graph == null) { graph = rootView.findViewById(R.id.graph_view);}
        graph.removeAllSeries();
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalable(true);
        graph.addSeries(new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(0 ,0)
        }));
        for (int i = 1 ; i < UserData.getInstance().getSelectedMeter().getMeterPoints().size(); i++){
            graph.addSeries(buildGraphSeries(i));
        }
    }

    public LineGraphSeries buildGraphSeries(int index) {
        ArrayList<DataPoint> dpArray = new ArrayList<>();
        int count = 0;
        for (Meter[] oldMeters : UserData.getInstance().getSelectedUnit().getOldMeters()){
            Meter oldMeter = oldMeters[UserData.getInstance().getSelectedMeter().getIndex()];
            ArrayList<HashMap<String, String>> meterPoints = oldMeter.getMeterPoints();
            String meterData = meterPoints.get(index).get(selectedRow.split("\\d")[0] + index);
            dpArray.add(new DataPoint(timeDiff * count, Double.valueOf(meterData.replaceAll("[^0-9.]", ""))));
            count++;
        }
        DataPoint[] seriesPoints = new DataPoint[dpArray.size()];
        LineGraphSeries returnSeries = new LineGraphSeries<>(dpArray.toArray(seriesPoints));
        returnSeries.setColor(colors[index]);
        return returnSeries;
    }

    public TableRow buildRow(View rootView, String key){
        TableRow row = new TableRow(rootView.getContext());
        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.width = MATCH_PARENT;
        params.height= MATCH_PARENT;
        params.gravity = Gravity.CENTER_VERTICAL;
        row.setLayoutParams(params);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setId(View.generateViewId());
        rowIds.put(row.getId(), key);
        row.setOnClickListener(this);
        return row;
    }

    public TextView buildTextView(View rootView, String text){
        TextView textView = new TextView(rootView.getContext());
        textView.setText(String.format("%s%s", " ", text));
        textView.setHeight(Utilities.getInstance().getDp(25f));
        textView.setAllCaps(true);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        return textView;
    }

    @Override
    public void onClick(View v) {
        String rowKey = rowIds.get(v.getId());
        if(rowKey != "" && rowKey != null){
            selectedRow = rowKey;
            buildGraphView();
            if (actualSelectedRow != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    actualSelectedRow.setBackgroundColor(getContext().getColor(R.color.stealthBomber));
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                v.setBackgroundColor(getContext().getColor(R.color.colorPrimaryLight));
            }
            actualSelectedRow = (TableRow) v;
        }
    }
}