package informationaesthetics.com.powerhawkrealtime.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import informationaesthetics.com.powerhawkrealtime.Data.FirebaseDatabaseData;
import informationaesthetics.com.powerhawkrealtime.Data.UserData;
import informationaesthetics.com.powerhawkrealtime.MainActivity;
import informationaesthetics.com.powerhawkrealtime.Objects.Server;
import informationaesthetics.com.powerhawkrealtime.R;
import informationaesthetics.com.powerhawkrealtime.Utilities.FragmentUtilities;
import informationaesthetics.com.powerhawkrealtime.Utilities.FragmentUtilities.*;
import informationaesthetics.com.powerhawkrealtime.Utilities.Utilities;

public class AddServerFragment extends Fragment implements OnFragmentInteractionListener, View.OnClickListener {

    private OnFragmentInteractionListener mListener;

    public AddServerFragment() {
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
        View rootView = inflater.inflate(R.layout.fragment_add_server, container, false);

        rootView.findViewById(R.id.add_server_button).setOnClickListener(this);

        return rootView;
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
    public void onDetach() {
        FragmentUtilities.getInstance().getCommunicator().fragmentDetached(R.id.add_server_fragment_id);
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.add_server_button:
                addServer();
                break;
        }
    }

    private void addServer(){
        // Get user inputs
        EditText nameInput = getActivity().findViewById(R.id.server_name);
        EditText ipInput = getActivity().findViewById(R.id.server_ip);
        String name = nameInput.getText().toString();
        String ip = ipInput.getText().toString();

        // Check if empty
        if (name == null || name == "" || ip == null || ip == ""){
            Utilities.getInstance().makeToast((MainActivity) getActivity(), getString(R.string.invalid_input_error));
            return;
        }

        // Create the server (Only name and ip needed to save to database since it re-initilizes in Server Fragment)
        Server server = new Server();
        server.setsName(name);
        server.setsIP(ip);

        // Get the current server array and add the new server
        Server[] servers = UserData.getInstance().getTempServerList();
        Server[] serversWnew = new Server[(servers==null?0:servers.length)+1];
        try {for(int i = 0; i < servers.length; i++){ serversWnew[i] = servers[i]; }} catch (Exception e){}
        serversWnew[serversWnew.length-1]=server;

        // Put our new server array in the database
        FirebaseDatabaseData.getInstance().setObjectValue(getString(R.string.servers_database_key), Arrays.asList(serversWnew));

        // add to the the temp server list which is used in Server Fragment
        UserData.getInstance().addToServerList(nameInput.getText().toString(), ipInput.getText().toString());

        // go back to server fragment
        onDetach();
        UserData.getInstance().setTransitFlag(R.integer.SUCCESS);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
