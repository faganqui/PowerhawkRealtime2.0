package informationaesthetics.com.powerhawkrealtime.Fragments;

import android.app.ActionBar;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import informationaesthetics.com.powerhawkrealtime.Data.FirebaseDatabaseData;
import informationaesthetics.com.powerhawkrealtime.Data.UnitData;
import informationaesthetics.com.powerhawkrealtime.Data.UserData;
import informationaesthetics.com.powerhawkrealtime.Interfaces.DataLoded;
import informationaesthetics.com.powerhawkrealtime.MainActivity;
import informationaesthetics.com.powerhawkrealtime.Objects.Server;
import informationaesthetics.com.powerhawkrealtime.R;
import informationaesthetics.com.powerhawkrealtime.Utilities.FragmentUtilities;
import informationaesthetics.com.powerhawkrealtime.Utilities.Utilities;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Created by quinnfagan on 2018-01-31.
 */

public class ServerFragment extends Fragment implements View.OnClickListener{

    private static final String TAG = "Server";
    FragmentUtilities.OnFragmentInteractionListener mListener;
    Utilities utils;
    UserData userData;
    TableLayout table;
    private TextView noServersFoundText;
    private TableRow selectedRow;
    private int clicked = 0;

    @Override
    public void onDetach() {
        FragmentUtilities.getInstance().getCommunicator().fragmentDetached(R.id.server_fragment_id);
        super.onDetach();
    }

    public ServerFragment(){
        //required builder method
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        utils = new Utilities().getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_server, container, false);

        userData = UserData.getInstance();
        table = rootView.findViewById(R.id.servers_table);
        noServersFoundText = rootView.findViewById(R.id.no_servers_found_text);
        FloatingActionButton fab = rootView.findViewById(R.id.fab);
        fab.setOnClickListener(this);

        setUpServers(makeTable());
        return rootView;
    }

    public void setUpServers(Server[] servers){
        if (servers == null){
            onNoServersFound();
            return;
        }
        for(Server server : servers){
            DataLoded loader = server;
            FirebaseDatabaseData.getInstance().getObject(server.getsIP().replace(".",""), loader);
        }
        userData.setServerList(servers);
    }

    private void onNoServersFound(){
        noServersFoundText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentUtilities.OnFragmentInteractionListener) {
            mListener = (FragmentUtilities.OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.fab:
                addServer();
                return;
        }
        for (Server server : userData.getServerList()){
            if(server.getRowId() == v.getId()){
                onServerClicked(server);
            }else if(server.getRemoveId() == v.getId()){
                onRemoveClicked(server);
            }
        }
    }

    private void onRemoveClicked(Server server){
        userData.removeFromServerList(server.getIndex());
        userData.setTransitFlag(R.integer.REMOVE_SERVER);
        FirebaseDatabaseData.getInstance().setObjectValue(getString(R.string.servers_database_key), userData.getTempServerList()==null?null:Arrays.asList(userData.getTempServerList()));
        onDetach();
    }

    private void onServerClicked(Server server){
        TableRow row = getActivity().findViewById(server.getRowId());
        if(selectedRow != row) clicked = 0;
        if(selectedRow != null) selectedRow.setBackgroundColor(80000000);

        row.setBackgroundColor(ContextCompat.getColor(getActivity().getApplicationContext(), R.color.colorPrimaryLight));
        selectedRow = row;
        clicked+=1;
        if(clicked > 1){
            goToServer(server);
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

    private void addServer(){
        userData.setTransitFlag(R.integer.ADD_SERVER);
        onDetach();
    }

    private void goToServer(Server server) {
        userData.setTransitFlag(R.integer.SELECT_SERVER);
        userData.setSelectedServer(server);
        onDetach();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void setServerBlinker(int status, int serverId){
        ImageView blinker = getActivity().findViewById(serverId);
        if(blinker == null) return;
        switch (status){
            case 1:
                blinker.setColorFilter(getContext().getColor(R.color.filterOnline));
                break;
            case 2:
                break;
            case 3:
                break;
            default:
                blinker.setColorFilter(getContext().getColor(R.color.filterNoServer));
                break;
        }
    }

    public Server[] makeTable(){
        if (userData.getUser() == null) return null;
        //add a row for each server
        int count = 0;
        Server[] servers = userData.getTempServerList();
        if (servers == null) return null;
        for(Server server : servers){
            server.setIndex(count);

            //create table row
            TableRow row = buildRow(server);

            table.addView(row);
            count++;
        }
        return servers;
    }

    public TableRow buildRow(Server server){
        TableRow row = new TableRow(getContext());
        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.width = MATCH_PARENT;
        params.height= WRAP_CONTENT;
        params.gravity = Gravity.CENTER_VERTICAL;
        row.setLayoutParams(params);
        row.setGravity(Gravity.CENTER_VERTICAL);

        //add views to row
        ImageView status = new ImageView(getContext());
        TableRow.LayoutParams imageParams = new TableRow.LayoutParams(50, 100);
        imageParams.gravity = Gravity.CENTER_VERTICAL;
        status.setLayoutParams(imageParams);
        Drawable d = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.blinking_server_status);
        status.setImageDrawable(d);
        int statusId = View.generateViewId();
        server.setStatusImageId(statusId);
        status.setId(statusId);
        row.addView(status);

        TextView serverName = new TextView(getContext());
        serverName.setText(server.getsName());
        serverName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        row.addView(serverName);

        ImageView removeServer = new ImageView(getContext());
        d = ContextCompat.getDrawable(getActivity().getApplicationContext(), R.drawable.remove_server_button);
        removeServer.setImageDrawable(d);
        removeServer.setLayoutParams(imageParams);
        removeServer.setOnClickListener(this);
        server.setRemoveId(View.generateViewId());
        removeServer.setId(server.getRemoveId());
        row.addView(removeServer);
        server.setRowId(View.generateViewId());
        row.setId(server.getRowId());

        row.setOnClickListener(this);
        return row;
    }

}
