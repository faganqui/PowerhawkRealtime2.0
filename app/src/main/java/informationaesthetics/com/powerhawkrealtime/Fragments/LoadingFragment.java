package informationaesthetics.com.powerhawkrealtime.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import informationaesthetics.com.powerhawkrealtime.Data.FirebaseDatabaseData;
import informationaesthetics.com.powerhawkrealtime.Data.UserData;
import informationaesthetics.com.powerhawkrealtime.R;
import informationaesthetics.com.powerhawkrealtime.Utilities.FragmentUtilities;
import informationaesthetics.com.powerhawkrealtime.Interfaces.DataLoded;


public class LoadingFragment extends Fragment implements DataLoded{

    DataLoded loader;
    public Object data;

    public LoadingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loader = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView =  inflater.inflate(R.layout.fragment_loading, container, false);

        switch (UserData.getInstance().getTransitFlag()){
            case R.integer.GO_TO_SERVER:
                String key = UserData.getInstance().getUser().getUid()+getString(R.string.servers_database_key);
                FirebaseDatabaseData.getInstance().getObject(key, loader);
                break;
            case R.integer.GO_TO_SELECTED_SERVER:
                FirebaseDatabaseData.getInstance().getObject(UserData.getInstance().getSelectedServer().getsIP().replace(".",""), loader);
                break;
        }

        return rootView;
    }

    @Override
    public void onDetach() {
        FragmentUtilities.getInstance().getCommunicator().fragmentDetached(R.id.loading_fragment_id);
        super.onDetach();

    }

    @Override
    public void dataLoaded(Object lodedData) {
        this.lodedData = lodedData;
        onDetach();
    }

    private Object lodedData;

    public Object getLodedData(){
        return lodedData;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
