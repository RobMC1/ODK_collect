/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.makina.collect.android.activities;



import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.actionbarsherlock.app.SherlockListFragment;
import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.provider.InstanceProviderAPI;
import com.makina.collect.android.provider.InstanceProviderAPI.InstanceColumns;

/**
 * Responsible for displaying all the valid instances in the instance directory.
 * 
 * @author Yaw Anokwa (yanokwa@gmail.com)
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class InstanceChooserList extends SherlockListFragment{

    private static final boolean EXIT = true;
    private static final boolean DO_NOT_EXIT = false;
    private AlertDialog mAlertDialog;
    
    
    
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        
    	inflater.inflate(R.layout.chooser_list_layout, container, false);
    	
        getActivity().setTitle(getString(R.string.review_data));
        /*TextView tv = (TextView) findViewById(R.id.status_text);
        tv.setVisibility(View.GONE);*/
        
        String selection = InstanceColumns.STATUS + " != ?";
        String[] selectionArgs = {InstanceProviderAPI.STATUS_SUBMITTED};
        String sortOrder = InstanceColumns.STATUS + " DESC, " + InstanceColumns.DISPLAY_NAME + " ASC";
        Cursor c = getActivity().managedQuery(InstanceColumns.CONTENT_URI, null, selection, selectionArgs, sortOrder);

        String[] data = new String[] {
                InstanceColumns.DISPLAY_NAME, InstanceColumns.DISPLAY_SUBTEXT
        };
        int[] view = new int[] {
                R.id.text1, R.id.text2
        };

        // render total instance view
        SimpleCursorAdapter instances =
            new SimpleCursorAdapter(getActivity(), R.layout.two_item, c, data, view);
        setListAdapter(instances);
        
        return super.onCreateView(inflater, container, savedInstanceState);
    }
    

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
    }


    /**
     * Stores the path of selected instance in the parent class and finishes.
     */
    @Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
        Cursor c = (Cursor) getListAdapter().getItem(position);
        getActivity().startManagingCursor(c);
        Uri instanceUri =
            ContentUris.withAppendedId(InstanceColumns.CONTENT_URI,
                c.getLong(c.getColumnIndex(BaseColumns._ID)));

        Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick", instanceUri.toString());

        String action = getActivity().getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action)) {
            // caller is waiting on a picked form
            getActivity().setResult(getActivity().RESULT_OK, new Intent().setData(instanceUri));
        } else {
            // the form can be edited if it is incomplete or if, when it was
            // marked as complete, it was determined that it could be edited
            // later.
            String status = c.getString(c.getColumnIndex(InstanceColumns.STATUS));
            String strCanEditWhenComplete = 
                c.getString(c.getColumnIndex(InstanceColumns.CAN_EDIT_WHEN_COMPLETE));

            boolean canEdit = status.equals(InstanceProviderAPI.STATUS_INCOMPLETE)
                	           || Boolean.parseBoolean(strCanEditWhenComplete);
            if (!canEdit) {
            	createErrorDialog(getString(R.string.cannot_edit_completed_form),
                    	          DO_NOT_EXIT);
            	return;
            }
            System.out.println("URI : "+instanceUri);
            // caller wants to view/edit a form, so launch formentryactivity
            startActivity(new Intent(Intent.ACTION_EDIT, instanceUri));
        }
        //TODO 
        //getActivity().finish();
    }
	
    @Override
	public void onStart() {
    	super.onStart();
		Collect.getInstance().getActivityLogger().logOnStart(getActivity()); 
    }
    
    @Override
	public void onStop() {
		Collect.getInstance().getActivityLogger().logOnStop(getActivity()); 
    	super.onStop();
    }

    
    /**
     * Creates a dialog with the given message. Will exit the activity when the user preses "ok" if
     * shouldExit is set to true.
     * 
     * @param errorMsg
     * @param shouldExit
     */
	private void createErrorDialog(String errorMsg, final boolean shouldExit) {
        Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", "show");

        mAlertDialog = new AlertDialog.Builder(getActivity()).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1:
                        Collect.getInstance().getActivityLogger().logAction(this, "createErrorDialog", 
                        		shouldExit ? "exitApplication" : "OK");
                        if (shouldExit) {
                            getActivity().finish();
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mAlertDialog.show();
    }  


}
