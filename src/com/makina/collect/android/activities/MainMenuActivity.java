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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.makina.collect.android.R;
import com.makina.collect.android.adapters.DrawerAdapter;
import com.makina.collect.android.application.Collect;
import com.makina.collect.android.preferences.AdminPreferencesActivity;
import com.makina.collect.android.preferences.PreferencesActivity;

/**
 * Responsible for displaying buttons to launch the major activities. Launches
 * some activities based on returns of others.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 * @author Yaw Anokwa (yanokwa@gmail.com)
 */
public class MainMenuActivity extends SherlockFragmentActivity {
	private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mPartsTitles;

    private AlertDialog mAlertDialog;
	private SharedPreferences mAdminPreferences;

	private int mCompletedCount;
	private int mSavedCount;
	private int mCurrentDrawerPosition;

	private Cursor mFinalizedCursor;
	private Cursor mSavedCursor;

	private IncomingHandler mHandler = new IncomingHandler(this);
	private MyContentObserver mContentObserver = new MyContentObserver();

	private static boolean EXIT = true;

	// private static boolean DO_NOT_EXIT = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);
        
        mTitle = mDrawerTitle = "MakinaCollect";
        mPartsTitles = getResources().getStringArray(R.array.parts_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new DrawerAdapter(this, mPartsTitles));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        
        
        if (getIntent().hasExtra("drawerSelection")){
        	selectItem(getIntent().getIntExtra("drawerSelection", 0));
        }else{
            selectItem(0);
            mDrawerLayout.openDrawer(mDrawerList);
        }
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        MenuInflater inflater = getSupportMenuInflater();
        menu.clear();
        inflater.inflate(R.menu.main, menu);
        menu.findItem(R.id.general_preferences).setVisible(drawerOpen);
        
        switch (mCurrentDrawerPosition) {
        default:
        	break;
        case 2:
        	if (!mDrawerLayout.isDrawerOpen(mDrawerList)) {
        		menu.clear();
        		getSupportMenuInflater().inflate(R.menu.menu_instance_uploader, menu);
        	}
        	break;
        case 3:
        	if (!mDrawerLayout.isDrawerOpen(mDrawerList)) {
        		menu.clear();
        		getSupportMenuInflater().inflate(R.menu.menu_form_download, menu);
        	}
        	break;
        case 4:
        	if (!mDrawerLayout.isDrawerOpen(mDrawerList)) {
        		menu.clear();
        		getSupportMenuInflater().inflate(R.menu.menu_form_manager, menu);
        	}
        	break;
        }
        
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        // Handle action buttons for all fragments
    	Fragment fragment;
        switch(item.getItemId()) {
        case android.R.id.home:
            if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
                mDrawerLayout.closeDrawer(mDrawerList);
            } else {
                mDrawerLayout.openDrawer(mDrawerList);
            }
            return true;
        case R.id.general_preferences:
        case R.id.preferences_delete:
        case R.id.preferences_download:
        case R.id.preferences_uploader:
        	Collect.getInstance().getActivityLogger().logAction(this, "onMenuItemSelected", "MENU_PREFERENCES");
            Intent i = new Intent(this, PreferencesActivity.class);
            startActivity(i);
            return true;
        case R.id.upload_instance:
        	fragment = getSupportFragmentManager().findFragmentByTag("upload");
        	if (fragment != null){
        		((InstanceUploaderList)fragment).uploadInstancesOption();
        	}
        	return true;
        case R.id.select_all_instance:
        	fragment = getSupportFragmentManager().findFragmentByTag("upload");
        	if (fragment != null){
        		((InstanceUploaderList)fragment).selectAllOption();
        	}
        	return true;
        case R.id.delete_upload_instance:
        	fragment = getSupportFragmentManager().findFragmentByTag("upload");
        	if (fragment != null){
        		((InstanceUploaderList)fragment).delete();
        	}
        	return true;
        case R.id.select_all_download:
        	fragment = getSupportFragmentManager().findFragmentByTag("download");
        	if (fragment != null){
        		((FormDownloadList)fragment).selectAllOption();
        	}
        	return true;
        case R.id.forms_get:
        	fragment = getSupportFragmentManager().findFragmentByTag("download");
        	if (fragment != null){
        		((FormDownloadList)fragment).getFormsOption();
        	}
        	return true;
        case R.id.refresh_forms:
        	fragment = getSupportFragmentManager().findFragmentByTag("download");
        	if (fragment != null){
        		((FormDownloadList)fragment).refreshFormsOption();
        	}
        	return true;
        case R.id.delete_forms:
        	fragment = getSupportFragmentManager().findFragmentByTag("delete");
        	if (fragment != null){
        		((FormManagerList)fragment).delete();
        	}
        	return true;
        case R.id.select_all_forms:
        	fragment = getSupportFragmentManager().findFragmentByTag("delete");
        	if (fragment != null){
        		((FormManagerList)fragment).selectAll();
        	}
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
    	
    	Fragment fragment;
    	
    	Log.e(getClass().getName(), "position : "+position);
    	String tag;
    	switch (position){
    	default:
    	case 0:
    		fragment = new FormChooserList();
    		tag = "fill";
    		break;
    	case 1:
    		fragment = new InstanceChooserList();
    		tag = "edit";
    		break;
    	case 2:
    		fragment = new InstanceUploaderList();
    		tag = "upload";
    		break;
    	case 3:
    		fragment = new FormDownloadList();
    		tag = "download";
    		break;
    	case 4:
    		fragment = new FormManagerList();
    		tag = "delete";
    		break;
    	}
    	mCurrentDrawerPosition = position;

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, tag ).commit();

        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mPartsTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
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
	
	private void updateButtons() {
		mFinalizedCursor.requery();
		
		mSavedCursor.requery();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		//ugly way to display the dialog since there's no onCreateDialog method for Fragments
		Fragment f = getSupportFragmentManager().findFragmentById(R.id.content_frame);
		if (f instanceof FormDownloadList){
			return ((FormDownloadList) f).createDialog(id);
		}else{
			return null;
		}
	}
}