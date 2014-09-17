package com.hello.pirupea.dfu;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hello.pirupea.R;
import com.hello.pirupea.settings.LocalSettings;
import com.hello.suripu.android.SuripuClient;
import com.hello.suripu.core.db.models.FirmwareUpdate;
import com.hello.suripu.core.oauth.AccessToken;

import java.io.File;
import java.io.IOException;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by jchen on 9/16/14.
 */
public class HexSelectFragment extends DialogFragment {
    private static final String TAG = "HexFragment";
    private OnHexSelectedListener mListener;
    private HexListAdapter mAdapter;

    /**
     * Interface required to be implemented by activity.
     */
    public static interface OnHexSelectedListener {

        public void onHexSelected(final Uri item);

        public void onHexCanceled();
    }
    public static HexSelectFragment getInstance(final Context context){
        final HexSelectFragment fragment  = new HexSelectFragment();
        fragment.setArguments(new Bundle());
        return fragment;
    }
    @Override
    public void onAttach(final Activity activity){
        super.onAttach(activity);
        try{
            this.mListener = (OnHexSelectedListener) activity;
        }catch(final ClassCastException e){
            throw new ClassCastException(activity.toString() + " must implement listner");
        }
    }
    @Override
    public void onCreate(final Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        //get arguments
    }
    @Override
    public void onDestroyView() {
        //decon here
        super.onDestroyView();
    }
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_hex_selection,null);
        final ListView listView = (ListView) dialogView.findViewById(android.R.id.list);

        builder.setTitle(R.string.hex_title);
        listView.setEmptyView(dialogView.findViewById(android.R.id.empty));
        listView.setAdapter(mAdapter = new HexListAdapter(getActivity()));

        final AlertDialog dialog = builder.setView(dialogView).create();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();
                mListener.onHexSelected((Uri)mAdapter.getItem(position));
            }
        });


        {
            File folder = new File(Environment.getExternalStorageDirectory(),"pirupea");
            if(!folder.exists()){
                folder.mkdir();
            }
            File f = new File(folder,"pill_default.hex");
            if(f.exists()){
                mAdapter.addURI(Uri.fromFile(f));
            }
        }
        //fetch endpoint json

        final SuripuClient suripuClient = new SuripuClient();
        final String accessTokenJSONString = LocalSettings.getOAuthToken();

        final ObjectMapper mapper = new ObjectMapper();

        try {
            final AccessToken accessToken = mapper.readValue(accessTokenJSONString, AccessToken.class);
            suripuClient.getPillFirmwareUpdate(accessToken, new Callback<List<FirmwareUpdate>>() {
                @Override
                public void success(final List<FirmwareUpdate> firmwareUpdates, final Response response) {
                    for(final FirmwareUpdate update:firmwareUpdates){
                        final Uri uri = Uri.parse(update.url);
                        mAdapter.addURI(uri);
                    }
                }

                @Override
                public void failure(final RetrofitError retrofitError) {
                    if(retrofitError.isNetworkError()){
                        // No network
                    }else{
                        int responseCode = retrofitError.getResponse().getStatus();
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }


        //json->array
        //for element url in array
        //mAdapter.addURI(url->Uri)
        //onclick listener
        return dialog;
    }
    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        //mListener.onDialogCacneled();
    }

}
