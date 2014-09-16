package com.hello.pirupea.dfu;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hello.pirupea.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;

/**
 * Created by jchen on 9/16/14.
 */
public class HexListAdapter extends BaseAdapter {
    private final Context mContext;
    private static final int TYPE_TITLE = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_EMPTY = 2;

    private final ArrayList<URI> hexURILocals = new ArrayList<URI>();
    public HexListAdapter(Context context){

        mContext = context;
    }

    @Override
    public int getCount() {
        return hexURILocals.size();
    }

    @Override
    public Object getItem(int position) {
        return hexURILocals.get(position);
    }

    @Override
    public long getItemId(int position) {
        return TYPE_ITEM;
    }
    @Override
    public int getItemViewType(int position){
        return TYPE_ITEM;
    }
    @Override
    public View getView(int position, View oldView, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = oldView;
        final int type = getItemViewType(position);
        switch(type){
            case TYPE_EMPTY:
                if(view == null){
                    view = inflater.inflate(R.layout.device_list_empty,parent,false);
                }
                break;
            case TYPE_TITLE:
                if(view==null){
                    view = inflater.inflate(R.layout.device_list_title, parent,false);
                }
                final TextView title = (TextView) view;
                title.setText((String)getItem(position));
                break;
            default:
            case TYPE_ITEM:
                if(view == null) {
                    view = inflater.inflate(R.layout.hex_list_row, parent, false);
                    final ViewHolder holder = new ViewHolder();
                    holder.name = (TextView)view.findViewById(R.id.name);
                    //holder.name.setText(this.getItem(position).toString());
                    view.setTag(holder);
                }
                final ViewHolder holder = (ViewHolder) view.getTag();
                if(holder != null){
                    holder.name.setText(this.getItem(position).toString());
                }

                break;
        }

        return view;
    }

    public void addURI(URI uri) {
        hexURILocals.add(uri);
        notifyDataSetChanged();
    }

    private class ViewHolder {
        private TextView name;
    }
}
