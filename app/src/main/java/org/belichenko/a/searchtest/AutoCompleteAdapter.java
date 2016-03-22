package org.belichenko.a.searchtest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.belichenko.a.searchtest.data_structure.Results;

import java.util.ArrayList;

/**
 *
 */
public class AutoCompleteAdapter<T extends Results> extends ArrayAdapter
        implements Constant {

    public static final String TAG = "updateCityFromSite()";
    private final Context mContext;
    private ArrayList<Results> mResults;

    public AutoCompleteAdapter(Context context, int resource, ArrayList<Results> objects) {
        super(context, resource, objects);
        mContext = context;
        mResults = objects;
    }

    @Override
    public int getCount() {
        return mResults.size();
    }

    @Override
    public Results getItem(int index) {
        return mResults.get(index);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.simple_dropdown_item_2line, parent, false);
        }
        Results searchResult = getItem(position);
        ((TextView) convertView.findViewById(R.id.text1)).setText(searchResult.name);
        ((TextView) convertView.findViewById(R.id.text2)).setText(searchResult.vicinity);

        return convertView;
    }
}
