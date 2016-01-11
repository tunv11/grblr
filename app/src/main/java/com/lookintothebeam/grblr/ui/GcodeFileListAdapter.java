package com.lookintothebeam.grblr.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lookintothebeam.grblr.R;
import com.lookintothebeam.grblr.cnc.GcodeCommand;

public class GcodeFileListAdapter extends ArrayAdapter<GcodeCommand> {

    public GcodeFileListAdapter(Context context, GcodeCommand[] values) {
        super(context, R.layout.gcode_file_row_layout, values);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.gcode_file_row_layout, parent, false);
        GcodeCommand command = getItem(position);

        // Text
        TextView gcodeLineTextView = (TextView) view.findViewById(R.id.gcodeLine);
        gcodeLineTextView.setText(command.getCommand());

        // Status
        ImageView gcodeStatusImage = (ImageView) view.findViewById(R.id.gcodeLineStatusImageView);
        switch(command.getStatus()) {
            case QUEUED:
                gcodeStatusImage.setImageDrawable(null);
                break;
            case RUNNING:
                gcodeStatusImage.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_directions_run_white_48dp));
                gcodeStatusImage.setColorFilter(getContext().getResources().getColor(R.color.colorBlue));
                break;
            case COMPLETE:
                gcodeStatusImage.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_check_white_24dp));
                gcodeStatusImage.setColorFilter(getContext().getResources().getColor(R.color.colorGo));
                gcodeLineTextView.setAlpha(0.5f);
                break;
        }

        return view;
    }
}
