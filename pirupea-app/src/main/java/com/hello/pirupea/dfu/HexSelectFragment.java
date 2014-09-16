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

import com.hello.pirupea.R;

import java.io.File;

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

        //onclick listener
        return dialog;
    }
    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        //mListener.onDialogCacneled();
    }

}
