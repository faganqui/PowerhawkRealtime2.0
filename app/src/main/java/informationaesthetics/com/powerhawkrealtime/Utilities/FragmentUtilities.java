package informationaesthetics.com.powerhawkrealtime.Utilities;

import android.net.Uri;

/**
 * Created by quinnfagan on 2018-01-30.
 */

public class FragmentUtilities {

    private static volatile FragmentUtilities mInstance;
    FragmentCommunicator communicator;

    private FragmentUtilities(){

    }

    public FragmentCommunicator getCommunicator(){
        return communicator;
    }

    public interface FragmentCommunicator {
        public void fragmentDetached(int frag_id);
    }

    public void setCommunicator(FragmentCommunicator communicator) {
        this.communicator = communicator;
    }

    public static void init(){
        mInstance = new FragmentUtilities();
    }

    public static FragmentUtilities getInstance(){
        if(mInstance == null){
            init();
        }
        return mInstance;
    }


    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
