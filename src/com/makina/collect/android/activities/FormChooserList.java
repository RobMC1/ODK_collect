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
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.actionbarsherlock.app.SherlockListFragment;
import com.makina.collect.android.R;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.listeners.DiskSyncListener;
import com.makina.collect.android.provider.FormsProviderAPI.FormsColumns;
import com.makina.collect.android.tasks.DiskSyncTask;
import com.makina.collect.android.utilities.VersionHidingCursorAdapter;

/**
 * Responsible for displaying all the valid forms in the forms directory. Stores the path to
 * selected form for use by {@link MainMenuActivity}.
 * 
 * @author Yaw Anokwa (yanokwa@gmail.com)
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class FormChooserList extends SherlockListFragment implements DiskSyncListener {

    private static final String t = "FormChooserList";
    private static final boolean EXIT = true;
    private static final String syncMsgKey = "syncmsgkey";

    private DiskSyncTask mDiskSyncTask;

    private AlertDialog mAlertDialog;
    
    private String statusText;
    
    
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
        inflater.inflate(R.layout.chooser_list_layout, container, false);
        getActivity().setTitle(getString(R.string.enter_data));
        
        setRetainInstance(true);
        
        
        
        /*Experimental section
        
        mMenuEntries = getResources().getStringArray(R.array.planets_array);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mMenuEntries));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        
        End of experimental section*/
        
        
        
        //home button leads back to home
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        String sortOrder = FormsColumns.DISPLAY_NAME + " ASC, " + FormsColumns.JR_VERSION + " DESC";
        Cursor c = getActivity().managedQuery(FormsColumns.CONTENT_URI, null, null, null, sortOrder);

        String[] data = new String[] {
                FormsColumns.DISPLAY_NAME, FormsColumns.DISPLAY_SUBTEXT, FormsColumns.JR_VERSION
        };
        int[] view = new int[] {
                R.id.text1, R.id.text2, R.id.text3
        };

        // render total instance view
        SimpleCursorAdapter instances =
            new VersionHidingCursorAdapter(FormsColumns.JR_VERSION, getActivity(), R.layout.two_item, c, data, view);
        setListAdapter(instances);

        if (savedInstanceState != null && savedInstanceState.containsKey(syncMsgKey)) {
            statusText = savedInstanceState.getString(syncMsgKey);
        }

        // DiskSyncTask checks the disk for any forms not already in the content provider
        // that is, put here by dragging and dropping onto the SDCard
        //mDiskSyncTask = (DiskSyncTask) getActivity().getLastNonConfigurationInstance();
        if (mDiskSyncTask == null) {
            Log.i(t, "Starting new disk sync task");
            mDiskSyncTask = new DiskSyncTask();
            mDiskSyncTask.setDiskSyncListener(this);
            mDiskSyncTask.execute((Void[]) null);
        }
        
        return super.onCreateView(inflater, container, savedInstanceState);
    }
    
    

	@Override
	public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(syncMsgKey, statusText);
    }


    /**
     * Stores the path of selected form and finishes.
     */
    @Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
        // get uri to form
    	long idFormsTable = ((SimpleCursorAdapter) getListAdapter()).getItemId(position);
        Uri formUri = ContentUris.withAppendedId(FormsColumns.CONTENT_URI, idFormsTable);

		Collect.getInstance().getActivityLogger().logAction(this, "onListItemClick", formUri.toString());

        String action = getActivity().getIntent().getAction();
        if (Intent.ACTION_PICK.equals(action)) {
            // caller is waiting on a picked form
            getActivity().setResult(getActivity().RESULT_OK, new Intent().setData(formUri));
        } else {
            // caller wants to view/edit a form, so launch formentryactivity
        	Intent i = new Intent(Intent.ACTION_EDIT, formUri);
        	i.putExtra("newForm", true);
            startActivity(i);
        }
        
        //TODO  
        //getActivity().finish();
    }
    
    //TODO Back to previous task
    /*public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
	        case android.R.id.home:
	            // This is called when the Home (Up) button is pressed
	            // in the Action Bar.
	            Intent parentActivityIntent = new Intent(this, MainMenuActivity.class);
	            parentActivityIntent.addFlags(
	                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                    Intent.FLAG_ACTIVITY_NEW_TASK);
	            startActivity(parentActivityIntent);
	            finish();
	            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/

    @Override
	public void onResume() {
        mDiskSyncTask.setDiskSyncListener(this);
        super.onResume();

        if (mDiskSyncTask.getStatus() == AsyncTask.Status.FINISHED) {
        	SyncComplete(mDiskSyncTask.getStatusMessage());
        }
    }


    @Override
	public void onPause() {
        mDiskSyncTask.setDiskSyncListener(null);
        super.onPause();
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
     * Called by DiskSyncTask when the task is finished
     */
    @Override
    public void SyncComplete(String result) {
        Log.i(t, "disk sync task complete");
        statusText = result;
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