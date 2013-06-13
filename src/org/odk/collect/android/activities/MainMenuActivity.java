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

package org.odk.collect.android.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Map.Entry;

import org.odk.collect.android.R;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.AdminPreferencesActivity;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.provider.InstanceProviderAPI;
import org.odk.collect.android.provider.InstanceProviderAPI.InstanceColumns;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

/**
 * Responsible for displaying buttons to launch the major activities. Launches
 * some activities based on returns of others.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class MainMenuActivity extends SherlockActivity {
	private static final String t = "MainMenuActivity";

	private static final int PASSWORD_DIALOG = 1;

	// menu options
	private static final int MENU_PREFERENCES = Menu.FIRST;

	// buttons
	private Button mEnterDataButton;
	private Button mManageFilesButton;
	private Button mSendDataButton;
	private Button mReviewDataButton;
	private Button mGetFormsButton;

	private View mReviewSpacer;
	private View mGetFormsSpacer;

	private AlertDialog mAlertDialog;
	private SharedPreferences mAdminPreferences;

	private int mCompletedCount;
	private int mSavedCount;

	private Cursor mFinalizedCursor;
	private Cursor mSavedCursor;

	private IncomingHandler mHandler = new IncomingHandler(this);
	private MyContentObserver mContentObserver = new MyContentObserver();

	private static boolean EXIT = true;
	
	private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mPlanetTitles;

    
	// private static boolean DO_NOT_EXIT = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// must be at the beginning of any activity that can be called from an
		// external intent
		Log.i(t, "Starting up, creating directories");
		try {
			Collect.createODKDirs();
		} catch (RuntimeException e) {
			createErrorDialog(e.getMessage(), EXIT);
			return;
		}

		setContentView(R.layout.main_menu);
		ActionBar bar = getSupportActionBar();
		bar.setDisplayShowHomeEnabled(true);
		
		
		
		/*Experimental section*/
		
		mPlanetTitles = getResources().getStringArray(R.array.planets_array);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, mPlanetTitles));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        
        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
		
		/*End of experimentalsection*/
		
		

		{
			// dynamically construct the "'Application name' vA.B" string
			TextView mainMenuMessageLabel = (TextView) findViewById(R.id.main_menu_header);
			mainMenuMessageLabel.setText(Collect.getInstance()
					.getVersionedAppName());
		}

		setTitle(getString(R.string.app_name));
		
		File f = new File(Collect.ODK_ROOT + "/collect.settings");
		if (f.exists()) {
			boolean success = loadSharedPreferencesFromFile(f);
			if (success) {
				Toast.makeText(this,
						"Settings successfully loaded from file",
						Toast.LENGTH_LONG).show();
				f.delete();
			} else {
				Toast.makeText(
						this,
						"Sorry, settings file is corrupt and should be deleted or replaced",
						Toast.LENGTH_LONG).show();
			}
		}

		mReviewSpacer = findViewById(R.id.review_spacer);
		mGetFormsSpacer = findViewById(R.id.get_forms_spacer);

		mAdminPreferences = this.getSharedPreferences(
				AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

		// enter data button. expects a result.
		mEnterDataButton = (Button) findViewById(R.id.enter_data);
		mEnterDataButton.setText(getString(R.string.enter_data_button));
		mEnterDataButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logAction(this, "fillBlankForm", "click");
				Intent i = new Intent(getApplicationContext(),
						FormChooserList.class);
				startActivity(i);
			}
		});

		// review data button. expects a result.
		mReviewDataButton = (Button) findViewById(R.id.review_data);
		mReviewDataButton.setText(getString(R.string.review_data_button));
		mReviewDataButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logAction(this, "editSavedForm", "click");
				Intent i = new Intent(getApplicationContext(),
						InstanceChooserList.class);
				startActivity(i);
			}
		});

		// send data button. expects a result.
		mSendDataButton = (Button) findViewById(R.id.send_data);
		mSendDataButton.setText(getString(R.string.send_data_button));
		mSendDataButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logAction(this, "uploadForms", "click");
				Intent i = new Intent(getApplicationContext(),
						InstanceUploaderList.class);
				startActivity(i);
			}
		});

		// manage forms button. no result expected.
		mGetFormsButton = (Button) findViewById(R.id.get_forms);
		mGetFormsButton.setText(getString(R.string.get_forms));
		mGetFormsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logAction(this, "downloadBlankForms", "click");
				Intent i = new Intent(getApplicationContext(),
						FormDownloadList.class);
				startActivity(i);

			}
		});

		// manage forms button. no result expected.
		mManageFilesButton = (Button) findViewById(R.id.manage_forms);
		mManageFilesButton.setText(getString(R.string.manage_files));
		mManageFilesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Collect.getInstance().getActivityLogger()
						.logAction(this, "deleteSavedForms", "click");
				Intent i = new Intent(getApplicationContext(),
						FileManagerTabs.class);
				startActivity(i);
			}
		});

		// count for finalized instances
		String selection = InstanceColumns.STATUS + "=? or "
				+ InstanceColumns.STATUS + "=?";
		String selectionArgs[] = { InstanceProviderAPI.STATUS_COMPLETE,
				InstanceProviderAPI.STATUS_SUBMISSION_FAILED };

		mFinalizedCursor = managedQuery(InstanceColumns.CONTENT_URI, null,
				selection, selectionArgs, null);
		startManagingCursor(mFinalizedCursor);
		mCompletedCount = mFinalizedCursor.getCount();
		mFinalizedCursor.registerContentObserver(mContentObserver);

		// count for finalized instances
		String selectionSaved = InstanceColumns.STATUS + "=?";
		String selectionArgsSaved[] = { InstanceProviderAPI.STATUS_INCOMPLETE };

		mSavedCursor = managedQuery(InstanceColumns.CONTENT_URI, null,
				selectionSaved, selectionArgsSaved, null);
		startManagingCursor(mSavedCursor);
		mSavedCount = mFinalizedCursor.getCount();
		// don't need to set a content observer because it can't change in the
		// background
		
		updateButtons();
	}
	
	
	
	/*Experimental section*/
	
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
	    @Override
	    public void onItemClick(AdapterView parent, View view, int position, long id) {
	        selectItem(position);
	    }
	}

	/** Swaps fragments in the main content view */
	private void selectItem(int position) {
	    // Create a new fragment and specify the planet to show based on position
		Fragment fragment;
		switch(position){
	    case 0:
	    case 1:
	    case 2:
	    case 3:
	    case 4:
	    default:
	    	Log.i(getClass().getName(), "position : "+position);
	    	fragment = new FormChooserList();
	    	break;
	    	
	    }

	    // Insert the fragment by replacing any existing fragment
	    FragmentManager fragmentManager = (FragmentManager) getSupportFragmentManager();
	    fragmentManager.beginTransaction()
	                   .replace(R.id.content_frame, fragment)
	                   .commit();

	    // Highlight the selected item, update the title, and close the drawer
	    mDrawer.setItemChecked(position, true);
	    setTitle(mPlanetTitles[position]);
	    mDrawerLayout.closeDrawer(mDrawer);
	}

	@Override
	public void setTitle(CharSequence title) {
	    mTitle = title;
	    getSupportActionBar().setTitle(mTitle);
	}
	
	/* End of experimental section*/

	
	
	@Override
	protected void onResume() {
		super.onResume();
		SharedPreferences sharedPreferences = this.getSharedPreferences(
				AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

		boolean edit = sharedPreferences.getBoolean(
				AdminPreferencesActivity.KEY_EDIT_SAVED, true);
		if (!edit) {
			mReviewDataButton.setVisibility(View.GONE);
			mReviewSpacer.setVisibility(View.GONE);
		} else {
			mReviewDataButton.setVisibility(View.VISIBLE);
			mReviewSpacer.setVisibility(View.VISIBLE);
		}

		boolean send = sharedPreferences.getBoolean(
				AdminPreferencesActivity.KEY_SEND_FINALIZED, true);
		if (!send) {
			mSendDataButton.setVisibility(View.GONE);
		} else {
			mSendDataButton.setVisibility(View.VISIBLE);
		}

		boolean get_blank = sharedPreferences.getBoolean(
				AdminPreferencesActivity.KEY_GET_BLANK, true);
		if (!get_blank) {
			mGetFormsButton.setVisibility(View.GONE);
			mGetFormsSpacer.setVisibility(View.GONE);
		} else {
			mGetFormsButton.setVisibility(View.VISIBLE);
			mGetFormsSpacer.setVisibility(View.VISIBLE);
		}

		boolean delete_saved = sharedPreferences.getBoolean(
				AdminPreferencesActivity.KEY_DELETE_SAVED, true);
		if (!delete_saved) {
			mManageFilesButton.setVisibility(View.GONE);
		} else {
			mManageFilesButton.setVisibility(View.VISIBLE);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mAlertDialog != null && mAlertDialog.isShowing()) {
			mAlertDialog.dismiss();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		Collect.getInstance().getActivityLogger().logOnStart(this);
	}

	@Override
	protected void onStop() {
		Collect.getInstance().getActivityLogger().logOnStop(this);
		super.onStop();
	}

	/*
	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		Collect.getInstance().getActivityLogger()
		.logAction(this, "onCreateOptionsMenu", "show");
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_PREFERENCES, 0,
				getString(R.string.general_preferences)).setIcon(
				android.R.drawable.ic_menu_preferences).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return true;
	}*/

	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
		Collect.getInstance().getActivityLogger()
		.logAction(this, "onCreateOptionsMenu", "show");
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_PREFERENCES, 0,
				getString(R.string.general_preferences)).setIcon(
				android.R.drawable.ic_menu_preferences).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onPrepareOptionsMenu(menu);
    }
	
	@Override
	public boolean onOptionsItemSelected(
			com.actionbarsherlock.view.MenuItem item) {
		switch (item.getItemId()) {
		case MENU_PREFERENCES:
			Collect.getInstance()
					.getActivityLogger()
					.logAction(this, "onOptionsItemSelected",
							"MENU_PREFERENCES");
			Intent ig = new Intent(this, PreferencesActivity.class);
			startActivity(ig);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Displays an error message and if necessary terminates application
	 * 
	 * @param errorMsg
	 * @param shouldExit
	 * 
	 */
	private void createErrorDialog(String errorMsg, final boolean shouldExit) {
		Collect.getInstance().getActivityLogger()
				.logAction(this, "createErrorDialog", "show");
		mAlertDialog = new AlertDialog.Builder(this).create();
		mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
		mAlertDialog.setMessage(errorMsg);
		DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int i) {
				switch (i) {
				case DialogInterface.BUTTON1:
					Collect.getInstance()
							.getActivityLogger()
							.logAction(this, "createErrorDialog",
									shouldExit ? "exitApplication" : "OK");
					if (shouldExit) {
						finish();
					}
					break;
				}
			}
		};
		mAlertDialog.setCancelable(false);
		mAlertDialog.setButton(getString(R.string.ok), errorListener);
		mAlertDialog.show();
	}
	
	/*
	 * I have not the slightest idea of what is this.
	 */
	private void updateButtons() {
		mFinalizedCursor.requery();
		mCompletedCount = mFinalizedCursor.getCount();
		if (mCompletedCount > 0) {
			mSendDataButton.setText(getString(R.string.send_data_button,
					mCompletedCount));
		} else {
			mSendDataButton.setText(getString(R.string.send_data));
		}

		mSavedCursor.requery();
		mSavedCount = mSavedCursor.getCount();
		if (mSavedCount > 0) {
			mReviewDataButton.setText(getString(R.string.review_data_button,
					mSavedCount));
		} else {
			mReviewDataButton.setText(getString(R.string.review_data));
		}

	}

	/**
	 * notifies us that something changed
	 * 
	 */
	private class MyContentObserver extends ContentObserver {

		public MyContentObserver() {
			super(null);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			mHandler.sendEmptyMessage(0);
		}
	}

	/*
	 * Used to prevent memory leaks
	 */
	static class IncomingHandler extends Handler {
		private final WeakReference<MainMenuActivity> mTarget;

		IncomingHandler(MainMenuActivity target) {
			mTarget = new WeakReference<MainMenuActivity>(target);
		}

		@Override
		public void handleMessage(Message msg) {
			MainMenuActivity target = mTarget.get();
			if (target != null) {
				target.updateButtons();
			}
		}
	}
	
	/**
	 * Loads the preferences for the application
	 * 
	 * @param src
	 * @return
	 */
	private boolean loadSharedPreferencesFromFile(File src) {
		// this should probably be in a thread if it ever gets big
		boolean res = false;
		ObjectInputStream input = null;
		try {
			input = new ObjectInputStream(new FileInputStream(src));
			Editor prefEdit = PreferenceManager.getDefaultSharedPreferences(
					this).edit();
			prefEdit.clear();
			// first object is preferences
			Map<String, ?> entries = (Map<String, ?>) input.readObject();
			for (Entry<String, ?> entry : entries.entrySet()) {
				Object v = entry.getValue();
				String key = entry.getKey();

				if (v instanceof Boolean)
					prefEdit.putBoolean(key, ((Boolean) v).booleanValue());
				else if (v instanceof Float)
					prefEdit.putFloat(key, ((Float) v).floatValue());
				else if (v instanceof Integer)
					prefEdit.putInt(key, ((Integer) v).intValue());
				else if (v instanceof Long)
					prefEdit.putLong(key, ((Long) v).longValue());
				else if (v instanceof String)
					prefEdit.putString(key, ((String) v));
			}
			prefEdit.commit();
			
			// second object is admin options
			Editor adminEdit = getSharedPreferences(AdminPreferencesActivity.ADMIN_PREFERENCES, 0).edit();
			adminEdit.clear();
			// first object is preferences
			Map<String, ?> adminEntries = (Map<String, ?>) input.readObject();
			for (Entry<String, ?> entry : adminEntries.entrySet()) {
				Object v = entry.getValue();
				String key = entry.getKey();

				if (v instanceof Boolean)
					adminEdit.putBoolean(key, ((Boolean) v).booleanValue());
				else if (v instanceof Float)
					adminEdit.putFloat(key, ((Float) v).floatValue());
				else if (v instanceof Integer)
					adminEdit.putInt(key, ((Integer) v).intValue());
				else if (v instanceof Long)
					adminEdit.putLong(key, ((Long) v).longValue());
				else if (v instanceof String)
					adminEdit.putString(key, ((String) v));
			}
			adminEdit.commit();
			
			res = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				if (input != null) {
					input.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return res;
	}

}
