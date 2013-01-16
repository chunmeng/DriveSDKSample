package com.tempura.drivesdksample;

import java.io.IOException;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;

public class GDriveQuotaChecker extends AsyncTask<Void, Void, Boolean> {
	private Context mContext;	
	private int mId;
	private Drive mService;
	
	private String mErrorMsg;
	
	private String mUsername; //!< account name
	private long mTotalQuota; //!< total in B
	private long mUsedQuota;  //!< used in B	
	
	private TextView textView;
	
    public GDriveQuotaChecker(Context context, Drive service, TextView textViewQuota) {
        // We set the context this way so we don't accidentally leak activities
        mContext = context.getApplicationContext();
        mService = service;
        textView = textViewQuota;
    }
    
	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			About about = mService.about().get().execute();

		    /*System.out.println("Current user name: " + about.getName());
		      System.out.println("Root folder ID: " + about.getRootFolderId());
		      System.out.println("Total quota (bytes): " + about.getQuotaBytesTotal());
		      System.out.println("Used quota (bytes): " + about.getQuotaBytesUsed());*/
		      			
		    mUsername = about.getName();
		    mTotalQuota = about.getQuotaBytesTotal();
		    mUsedQuota = about.getQuotaBytesUsed();
		      
		} catch (IOException e) {
			mErrorMsg = "GDrive update failed: " + e;
		}

		return false;
	}

    @Override
    protected void onPostExecute(Boolean result) {
        if (result) {
        	textView.setText(mUsername + " " + mUsedQuota + "/" + mTotalQuota);            	
        } else {
            // Couldn't download it, so show an error
        	textView.setText(mErrorMsg);
            showToast(mErrorMsg);
        }
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(mContext, msg, Toast.LENGTH_LONG);
        error.show();
    }	    
}
