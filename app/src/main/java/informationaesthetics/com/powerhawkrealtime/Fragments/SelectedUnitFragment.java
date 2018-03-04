package informationaesthetics.com.powerhawkrealtime.Fragments;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.ArcProgress;

import java.util.concurrent.TimeUnit;

import informationaesthetics.com.powerhawkrealtime.Data.UnitData;
import informationaesthetics.com.powerhawkrealtime.Data.UserData;
import informationaesthetics.com.powerhawkrealtime.Objects.Meter;
import informationaesthetics.com.powerhawkrealtime.Objects.Unit;
import informationaesthetics.com.powerhawkrealtime.R;
import informationaesthetics.com.powerhawkrealtime.Utilities.FragmentUtilities;
import informationaesthetics.com.powerhawkrealtime.Utilities.Utilities;

public class SelectedUnitFragment extends Fragment implements View.OnClickListener {
    private final String TAG = "SelUnFrag";
    TextView noMetersText;
    TableRow selectedRow;
    Meter selectedMeter;
    TableLayout table;
    int[] colors;
    int clicked;
    Meter[] meters;
    String[] headers;
    String[] headersreceived;

    public SelectedUnitFragment() {
        // Required empty public constructor
    }

    public static SelectedUnitFragment newInstance(String param1, String param2) {
        SelectedUnitFragment fragment = new SelectedUnitFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_selected_unit, container, false);

        table = rootView.findViewById(R.id.meters_table);
        meters = UserData.getInstance().getSelectedUnit().getMeters();
        noMetersText = rootView.findViewById(R.id.no_meters_found_text);
        FrameLayout thisFragment = rootView.findViewById(R.id.selected_unit_fragment_id);
        addSwitchButtons(thisFragment);
        setUpTableView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            colors = new int[]{getContext().getColor(R.color.colorPrimary), getContext().getColor(R.color.colorPrimaryOrange), getContext().getColor(R.color.colorPrimaryBlue)};
        }

        headersreceived = new String[]{getString(R.string.wattsreceived),getString(R.string.varreceived),getString(R.string.vah)};
        headers = new String[]{getString(R.string.watts),getString(R.string.var),getString(R.string.va)};

        return rootView;

    }


    public void updateTable(){

        ((TextView)getActivity().findViewById(R.id.x_header_sel_unit)).setText(headers[UnitData.getInstance().getStatType()]);
        ((TextView)getActivity().findViewById(R.id.hours_header_sel_unit)).setText(headersreceived[UnitData.getInstance().getStatType()]);

        for (Meter meter : UserData.getInstance().getSelectedUnit().getMeters()){
            TableRow row = getActivity().findViewById(meter.getRowId());

            //set arc hours data
            ArcProgress ah = (ArcProgress) row.getChildAt(1);
            ah.setTextSize(Utilities.getInstance().getDp(12f));
            if(selectedRow == null || ah != selectedRow.getChildAt(1)) {
                ah.setFinishedStrokeColor(colors[UnitData.getInstance().getStatType()]);
            }

            //set arc data
            ArcProgress a = (ArcProgress) row.getChildAt(2);
            a.setTextSize(Utilities.getInstance().getDp(12f));
            if(selectedRow == null || a != selectedRow.getChildAt(2)) {
                a.setFinishedStrokeColor(colors[UnitData.getInstance().getStatType()]);
            }

            switch (UnitData.getInstance().getStatType()) {
                case 1:
                    ah.setMax((int) UnitData.getInstance().getTotalVarReceived());
                    ah.setProgress((int) meter.getMeterTotalVarReceived());

                    a.setMax((int) UnitData.getInstance().getTotalVar());
                    a.setProgress((int) meter.getMeterTotalVar());
                    break;
                case 2:
                    ah.setMax((int) UnitData.getInstance().getTotalVaH());
                    ah.setProgress((int) meter.getMeterTotalVaH());

                    a.setMax((int) UnitData.getInstance().getTotalVa());
                    a.setProgress((int) meter.getMeterTotalVa());
                    break;
                default:
                    ah.setMax((int) UnitData.getInstance().getTotalWattsReceived());
                    ah.setProgress((int) meter.getMeterTotalWattsReceived());

                    a.setMax((int) UnitData.getInstance().getTotalWatts());
                    a.setProgress((int) meter.getMeterTotalWatts());
                    break;
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        FragmentUtilities.getInstance().getCommunicator().fragmentDetached(R.id.selected_unit_fragment_id);
        super.onDetach();
    }

    public void setUpTableView(){
        if(meters == null) {
            noMetersText.setVisibility(View.VISIBLE);
            return;
        }
        for (Meter meter: UserData.getInstance().getSelectedUnit().getMeters()){

            //create table row
            TableRow row = (TableRow) getLayoutInflater().inflate(R.layout.units_table_row, null);
            row.setId(View.generateViewId());
            meter. setRowId(row.getId());

            // make date text and title text
            TextView t = (TextView)row.getChildAt(0);
            t.setText(String.format("  %s", meter.getName()));

            //set arc hours data
            ArcProgress ah = (ArcProgress) row.getChildAt(1);
            ah.setMax((int) UserData.getInstance().getSelectedUnit().getUnitTotalWattsReceived());
            ah.setProgress((int) meter.getMeterTotalWattsReceived());
            ah.setTextSize(Utilities.getInstance().getDp(12f));

            //set arc data
            ArcProgress a = (ArcProgress) row.getChildAt(2);
            a.setMax((int) UserData.getInstance().getSelectedUnit().getUnitTotalWatts());
            a.setProgress((int) meter.getMeterTotalWatts());
            a.setTextSize(Utilities.getInstance().getDp(12f));

            row.setOnClickListener(this);

            table.addView(row);
        }
    }

    private void addSwitchButtons(FrameLayout thisFrag){
        LinearLayout buttonLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.watts_var_va_switch, null);
        thisFrag.addView(buttonLayout);
    }


    @Override
    public void onClick(View v) {
        for(Meter meter : meters) {
            if (v.getId() == meter.getRowId()) {
                onMeterClicked(meter);
                return;
            }
        }
    }

    private void onMeterClicked(Meter meter){
        TableRow row = getActivity().findViewById(meter.getRowId());
        if(selectedRow != row) clicked = 0;
        if(selectedMeter != null) {
            selectedRow.setBackgroundColor(80000000);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ((ArcProgress) selectedRow.getChildAt(1)).setFinishedStrokeColor(colors[UnitData.getInstance().getStatType()]);
                ((ArcProgress) selectedRow.getChildAt(2)).setFinishedStrokeColor(colors[UnitData.getInstance().getStatType()]);
                ((TextView) selectedRow.getChildAt(0)).setText(String.format("  %s", meter.getName()));
            }
        }

        row.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorPrimaryLight));
        ((TextView) row.getChildAt(0)).setText(String.format("  %s", meter.getName()));
        ((ArcProgress) row.getChildAt(1)).setFinishedStrokeColor(Color.WHITE);
        ((ArcProgress) row.getChildAt(2)).setFinishedStrokeColor(Color.WHITE);

        selectedRow = row;
        selectedMeter = meter;
        clicked+=1;
        if(clicked > 1){
            goToMeter(meter);
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    TimeUnit.MILLISECONDS.sleep(250);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Interrupt in restart");
                    e.printStackTrace();
                }
                clicked = 0;
            }
        });
        t.start();
    }

    private void goToMeter(Meter meter) {
        UserData.getInstance().setTransitFlag(R.integer.SELECTED_METER);
        UserData.getInstance().setSelectedMeter(meter);
        onDetach();
    }

}
