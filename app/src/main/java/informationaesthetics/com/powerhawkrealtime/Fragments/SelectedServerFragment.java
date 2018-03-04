package informationaesthetics.com.powerhawkrealtime.Fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.icu.util.UniversalTimeScale;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.lzyzsd.circleprogress.ArcProgress;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import informationaesthetics.com.powerhawkrealtime.Data.UnitData;
import informationaesthetics.com.powerhawkrealtime.Data.UserData;
import informationaesthetics.com.powerhawkrealtime.Objects.Meter;
import informationaesthetics.com.powerhawkrealtime.Objects.Unit;
import informationaesthetics.com.powerhawkrealtime.R;
import informationaesthetics.com.powerhawkrealtime.Utilities.FragmentUtilities;
import informationaesthetics.com.powerhawkrealtime.Utilities.Utilities;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class SelectedServerFragment extends Fragment implements View.OnClickListener {


    private static final String TAG = "SelServFrag";
    TableRow selectedRow;
    Unit selectedUnit;
    int clicked;
    TableLayout table;
    TextView noUnitsText;
    int[] colors;
    String[] headers;
    String[] headersreceived;

    public SelectedServerFragment() {
        // Required empty public constructor
    }

    public static SelectedServerFragment newInstance(String param1, String param2) {
        SelectedServerFragment fragment = new SelectedServerFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_selected_server, container, false);

        table = rootView.findViewById(R.id.units_table);
        noUnitsText = rootView.findViewById(R.id.no_units_found_text);
        FrameLayout thisFragment = rootView.findViewById(R.id.selected_server_fragment_id);
        addSwitchButtons(thisFragment);
        setUpTableView();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            colors = new int[]{getContext().getColor(R.color.colorPrimary), getContext().getColor(R.color.colorPrimaryOrange), getContext().getColor(R.color.colorPrimaryBlue)};
        }
        headersreceived = new String[]{getString(R.string.wattsreceived),getString(R.string.varreceived),getString(R.string.vah)};
        headers = new String[]{getString(R.string.watts),getString(R.string.var),getString(R.string.va)};

        return rootView;
    }

    public void setUpTableView(){
        if(UnitData.getInstance().getUnits() == null) {
            noUnitsText.setVisibility(View.VISIBLE);
            return;
        }
        for (Unit unit: UnitData.getInstance().getUnits()){
            unit.setUnitId(View.generateViewId());

            //create table row
            TableRow row = (TableRow) getLayoutInflater().inflate(R.layout.units_table_row, null);
            row.setId(View.generateViewId());
            unit.setRowId(row.getId());

            // make date text and title text
            TextView t = (TextView)row.getChildAt(0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                SpannableString ss1 = getSpannableString(unit, getContext().getColor(R.color.grey_accent_dark));
                t.setText(ss1);
            }else{
                SpannableString ss1 = getSpannableString(unit, Color.BLACK);
                t.setText(ss1);
            }

            //set arc hours data
            ArcProgress ah = (ArcProgress) row.getChildAt(1);
            ah.setMax((int) UnitData.getInstance().getTotalWattsReceived());
            ah.setProgress((int) unit.getUnitTotalWattsReceived());
            ah.setTextSize(Utilities.getInstance().getDp(12f));

            //set arc data
            ArcProgress a = (ArcProgress) row.getChildAt(2);
            a.setMax((int) UnitData.getInstance().getTotalWatts());
            a.setProgress((int) unit.getUnitTotalWatts());
            a.setTextSize(Utilities.getInstance().getDp(12f));

            row.setOnClickListener(this);

            table.addView(row);
  //          count++;
        }
    }

    private void addSwitchButtons(FrameLayout thisFrag){
        LinearLayout buttonLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.watts_var_va_switch, null);
        thisFrag.addView(buttonLayout);
    }

    private SpannableString getSpannableString(Unit unit, int color){

        String date = unit.getLastUpdated();
        String[] dates = date.split("/");
        String formattedDate = String.format("%s/%s/%s - %s:%s:%s", dates[0],dates[1],dates[2],dates[3],dates[4],dates[5]);
        String text = "  " + unit.getKeyName() + "\n    Last updated: " + formattedDate;
        SpannableString ss1=  new SpannableString(text);
        ss1.setSpan(new RelativeSizeSpan(0.5f), unit.getKeyName().length()+3,text.length(), 0); // set size
        ss1.setSpan(new ForegroundColorSpan(color), unit.getKeyName().length()+3,text.length(), 0);// set color
        return ss1;
    }


    @Override
    public void onDetach() {
        FragmentUtilities.getInstance().getCommunicator().fragmentDetached(R.id.selected_server_fragment_id);
        super.onDetach();
    }

    @Override
    public void onClick(View v) {
        for(Unit unit : UnitData.getInstance().getUnits()){
            if(v.getId() == unit.getRowId()){
                onUnitClicked(unit);
                return;
            }
        }
    }

    private void onUnitClicked(Unit unit){
        TableRow row = getActivity().findViewById(unit.getRowId());
        if(selectedRow != row) clicked = 0;
        if(selectedRow != null) {
            selectedRow.setBackgroundColor(80000000);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ((ArcProgress) selectedRow.getChildAt(1)).setFinishedStrokeColor(colors[UnitData.getInstance().getStatType()]);
                ((ArcProgress) selectedRow.getChildAt(2)).setFinishedStrokeColor(colors[UnitData.getInstance().getStatType()]);
                ((TextView) selectedRow.getChildAt(0)).setText(getSpannableString(selectedUnit, getContext().getColor(R.color.grey_accent_dark)));
            }
        }

        row.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorPrimaryLight));
        ((TextView) row.getChildAt(0)).setText(getSpannableString(unit, Color.WHITE));
        ((ArcProgress) row.getChildAt(1)).setFinishedStrokeColor(Color.WHITE);
        ((ArcProgress) row.getChildAt(2)).setFinishedStrokeColor(Color.WHITE);

        selectedRow = row;
        selectedUnit = unit;
        clicked+=1;
        if(clicked > 1){
            goToUnit(unit);
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

    private void goToUnit(Unit unit) {
        UserData.getInstance().setTransitFlag(R.integer.SELECTED_UNIT);
        UserData.getInstance().setSelectedUnit(unit);
        onDetach();
    }

    public void updateTable(){

        ((TextView)getActivity().findViewById(R.id.x_header_sel_server)).setText(headers[UnitData.getInstance().getStatType()]);
        ((TextView)getActivity().findViewById(R.id.hours_header_sel_server)).setText(headersreceived[UnitData.getInstance().getStatType()]);

        for (Unit unit : UnitData.getInstance().getUnits()){
            TableRow row = getActivity().findViewById(unit.getRowId());

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
                    ah.setProgress((int) unit.getUnitTotalVarReceived());

                    a.setMax((int) UnitData.getInstance().getTotalVar());
                    a.setProgress((int) unit.getUnitTotalVar());
                    break;
                case 2:
                    ah.setMax((int) UnitData.getInstance().getTotalVaH());
                    ah.setProgress((int) unit.getUnitTotalVaH());

                    a.setMax((int) UnitData.getInstance().getTotalVa());
                    a.setProgress((int) unit.getUnitTotalVa());
                    break;
                default:
                    ah.setMax((int) UnitData.getInstance().getTotalWattsReceived());
                    ah.setProgress((int) unit.getUnitTotalWattsReceived());

                    a.setMax((int) UnitData.getInstance().getTotalWatts());
                    a.setProgress((int) unit.getUnitTotalWatts());
                    break;
            }
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
