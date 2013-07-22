package com.makina.collect.android.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.makina.collect.android.R;

public class DrawerAdapter extends ArrayAdapter<String>{
	
	private final Context mContext;
	private final String[] mNames;

	public DrawerAdapter(Context context, String[] values) {
	  super(context, R.layout.drawer_list_item, values);
	  this.mContext = context;
	  this.mNames = values;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	  LayoutInflater inflater = (LayoutInflater) mContext
	      .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	  View rowView = inflater.inflate(R.layout.drawer_list_item, parent, false);
	  TextView textView = (TextView) rowView.findViewById(R.id.part_name);
	  ImageView imageView = (ImageView) rowView.findViewById(R.id.action_icon);
	  textView.setText(mNames[position]);
	  switch (position) {
	  	case 0 : 
	  		imageView.setImageResource(R.drawable.ic_menu_add);
	  		break;
	  	case 1 :
	  		imageView.setImageResource(android.R.drawable.ic_menu_edit);
	  		break;
	  	case 2 :
	  		imageView.setImageResource(android.R.drawable.ic_menu_upload);
	  		break;
	  	case 3 :
	  		imageView.setImageResource(R.drawable.stat_sys_download_anim0);
	  		break;
	  	case 4 :
	  		imageView.setImageResource(android.R.drawable.ic_menu_delete);
	  		break;
	  	default :
	  		break;
	  
	  }
	  return rowView;
	}
}
