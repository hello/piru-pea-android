package com.hello.pirupea.dfu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.hello.pirupea.R;

/**
 * Created by jchen on 9/16/14.
 */
public class HexListAdapter extends BaseAdapter {
    private final Context mContext;

    public HexListAdapter(Context context){
        mContext = context;
    }
    @Override
    public int getCount() {
        return 1;
    }

    @Override
    public Object getItem(int position) {
        return "1";
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View oldView, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = oldView;
        if(view == null) {
            view = inflater.inflate(R.layout.device_list_row, parent, false);
            final ViewHolder holder = new ViewHolder();
            holder.name = (TextView)view.findViewById(R.id.address);
            view.setTag(holder);
            holder.name.setText((String)this.getItem(0));
        }
        return view;
    }
    private class ViewHolder {
        private TextView name;
    }
}
