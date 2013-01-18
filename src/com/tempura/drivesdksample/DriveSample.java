package com.tempura.drivesdksample;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

public class DriveSample extends Activity {
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 0;
    static final int REQUEST_ACCOUNT_PICKER = 1;
    static final int REQUEST_AUTHORIZATION = 2;  
  
  protected static final String TAG = "DriveSample";
  
  private final static String PREFS_KEY = "DrivePrefs";
  
  private static final String PREF_ACCOUNT_NAME = "accountName";
  
  private final static String DRIVE_ACCESS_KEY_NAME = "DRIVE_LINK"; // A boolean key indicating whether live account is linked
  private final static String DRIVE_USERNAME = "DRIVE_USERNAME";
  private final static String DRIVE_USERNAME_DEFAULT = "test@gmail.com";
  private final static String DRIVE_QUOTA_TOTAL = "DRIVE_QUOTA_TOTAL";  
  private final static long DRIVE_QUOTA_TOTAL_DEFAULT = 0L;
  private final static String DRIVE_QUOTA_USED = "DRIVE_QUOTA_USED";
  private final static long DRIVE_QUOTA_FREE_DEFAULT = 0L;   

  private static Uri fileUri;
  static Drive service;
  private GoogleAccountCredential credential;
  private String mAccountName;
  
  private TextView textViewHello;
  private TextView textViewQuota;
  private Button mBtnLinkDrive;
  
  private boolean mDriveLoggedIn;
  private CheckBox mCbShowDrive;
  
  int numAsyncTasks;
  public String mUsername;
  public Long mTotalQuota;
  public Long mUsedQuota;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    setContentView(R.layout.activity_main);
    
    textViewHello = (TextView)findViewById(R.id.textView_hello);
    textViewQuota = (TextView)findViewById(R.id.textView_quota);
    mBtnLinkDrive = (Button)findViewById(R.id.button_auth);    
    
    // Set listener
    mBtnLinkDrive.setOnClickListener(new OnClickListener() {
        public void onClick(View v) {
            // This logs you out if you're logged in, or vice versa
            if (mDriveLoggedIn) {
                driveLogOut();
            } else {
                try {
                    chooseAccount();
                } catch (Exception e) {
                    Log.e(TAG, "Can't start account picker");
                }
            }
        }
    });      

    // instantiate credential
    credential = GoogleAccountCredential.usingOAuth2(this, DriveScopes.DRIVE_METADATA_READONLY);
    SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
    mAccountName = settings.getString(PREF_ACCOUNT_NAME, null);
    credential.setSelectedAccountName(mAccountName);
    // Drive client
    service = getDriveService(credential);   
        
    Spinner accountSpinner = (Spinner) findViewById(R.id.spinner_account_name);
    ArrayAdapter accountAdapter = new ArrayAdapter(this,
         android.R.layout.simple_spinner_item, getAccountNames());
    accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    accountSpinner.setAdapter(accountAdapter);
    accountSpinner.setSelection(0); 
    
		// add layout dynamically
    String[] accounts = getAccountNames();
    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    LinearLayout mainlayout = (LinearLayout)findViewById(R.id.linearLayout_gdriveBlock);
    View section = getLayoutInflater().inflate(R.layout.section, null);
    section.setId(R.id.gdrive_section);
    section.setTag("GDrive_Section");      
    mainlayout.addView(section); 
    
    for (String name : accounts) {
        View account = getLayoutInflater().inflate(R.layout.account_item, null);
        account.setId(R.id.gdrive_account); // this doesn't work in relativeLayout
        account.setTag("GDrive_account_" + name);         
        ((CheckBox)account.findViewById(R.id.checkbox_show_account)).setText("Show storage for: " + name);
        mainlayout.addView(account);    	
    }    
    
    /*    RelativeLayout mainlayout = (RelativeLayout)findViewById(R.id.relativeLayoutMain);
    //TableLayout table = (TableLayout)findViewById(R.id.tablelayout_section);
    //inflater.inflate(R.layout.section, table);    
    View section = getLayoutInflater().inflate(R.layout.section, null);    
    
    RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
    p.addRule(RelativeLayout.BELOW, R.id.spinner_account_name);
   // p.addRule(RelativeLayout.ALIGN_BOTTOM, RelativeLayout.TRUE);
    section.setId(R.id.gdrive_section);
    section.setTag("GDrive_Section");      
    mainlayout.addView(section, p);  
    
    int belowId = section.getId();
    for (String name : accounts) {
        RelativeLayout.LayoutParams accountLP = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);  
        accountLP.addRule(RelativeLayout.BELOW, belowId);
        View account = getLayoutInflater().inflate(R.layout.account_item, null);
        account.setLayoutParams(accountLP);
        account.setId(R.id.gdrive_account); // this doesn't work in relativeLayout
        account.setTag("GDrive_account_" + name);         
        ((CheckBox)account.findViewById(R.id.checkbox_show_account)).setText("Show storage for: " + name);
        mainlayout.addView(account, accountLP);    	
        belowId = account.getId();
    }  */  
    
    
  }  
  
  @Override
  protected void onResume() {
    super.onResume();
    if (checkGooglePlayServicesAvailable()) {
      haveGooglePlayServices();
    }
  }
  
  /** Check that Google Play services APK is installed and up to date. */
  private boolean checkGooglePlayServicesAvailable() {
    final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
      showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
      return false;
    }
    return true;
  }

  private void haveGooglePlayServices() {
    // check if there is already an account selected
    if (credential.getSelectedAccountName() == null) {
      // ask user to choose account
      chooseAccount();
    } else {
      // load quota
        AsyncDriveQuotaTask.run(this);
    }
  }  
  
  private void chooseAccount() {
      startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }  
  
  private void driveLogOut() {            
      // TODO: Remove credentials from the session
      // credential.getSession().unlink();

      // Clear our stored keys
      clearKeys(this);
      // Change UI state to display logged out version
      setDriveLoggedIn(false);
  }
  
  private void clearKeys(Context context) {
      SharedPreferences prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
      Editor edit = prefs.edit();
      edit.remove(DRIVE_ACCESS_KEY_NAME);       
      // Wipe clean, clear quota info as well.
      edit.remove(DRIVE_USERNAME);
      edit.remove(DRIVE_QUOTA_TOTAL);
      edit.remove(DRIVE_QUOTA_USED);
      edit.commit();
  }

private void setDriveLoggedIn(boolean loggedIn) {        
      mDriveLoggedIn = loggedIn;
      if (loggedIn) {
          mBtnLinkDrive.setText("Unlink");
          this.mCbShowDrive.setEnabled(true); // Enable the checkbox if linked
      } else {
          mBtnLinkDrive.setText("Link");
          this.mCbShowDrive.setEnabled(false); // Disable the checkbox if linked
          this.mCbShowDrive.setChecked(false); // Uncheck the checkbox if linked
      }
  }   

private String[] getAccountNames() {
    AccountManager mAccountManager = AccountManager.get(this);
    Account[] accounts = mAccountManager.getAccountsByType(
            GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
    String[] names = new String[accounts.length];
    for (int i = 0; i < names.length; i++) {
        names[i] = accounts[i].name;
    }
    return names;
}

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
      switch (requestCode) {
      case REQUEST_GOOGLE_PLAY_SERVICES:
        if (resultCode == Activity.RESULT_OK) {
          haveGooglePlayServices();
        } else {
          checkGooglePlayServicesAvailable();
        }
        break;
      case REQUEST_AUTHORIZATION:
        if (resultCode == Activity.RESULT_OK) {
            AsyncDriveQuotaTask.run(this);
        } else {
          chooseAccount();
        }
        break;
      case REQUEST_ACCOUNT_PICKER:
        if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
          String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
          if (accountName != null) {
            credential.setSelectedAccountName(accountName);
            SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(PREF_ACCOUNT_NAME, accountName);
            editor.commit();
            mAccountName = accountName;
            AsyncDriveQuotaTask.run(this);
          }
        }
        break;
    }
  }

  private Drive getDriveService(GoogleAccountCredential credential) {
    return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential)
        .build();
  }
  
  //Example of how to use AsyncTask to call blocking code on a background thread.
  void getAndUseAuthTokenInAsyncTask() {
      AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

		@Override
		protected Void doInBackground(Void... params) {
			getAndUseAuthTokenBlocking();
			return null;
		}

      };
      task.execute((Void)null);
  }
  
  //Example of how to use the GoogleAuthUtil in a blocking, non-main thread context
  void getAndUseAuthTokenBlocking() {
      try {
         // Retrieve a token for the given account and scope. It will always return either
         // a non-empty String or throw an exception.
    	  Log.v(TAG, "Getting token for " + mAccountName);
         final String token = GoogleAuthUtil.getToken(this, this.mAccountName, DriveScopes.DRIVE_METADATA_READONLY);
         // Do work with token.         
         return;
      } catch (GooglePlayServicesAvailabilityException playEx) {
          Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                  playEx.getConnectionStatusCode(),
                  DriveSample.this,
                  REQUEST_AUTHORIZATION);
      } catch (UserRecoverableAuthException userAuthEx) {
         // Start the user recoverable action using the intent returned by
         Intent recoveryIntent = userAuthEx.getIntent();
         // Use the intent in a custom dialog or just startActivityForResult.
         this.startActivityForResult(recoveryIntent, REQUEST_AUTHORIZATION);
         return;
      } catch (IOException ioEx) {
         // network or server error, the call is expected to succeed if you try again later.
         // Don't attempt to call again immediately - the request is likely to
         // fail, you'll hit quotas or back-off.
    	  Log.i(TAG, "transient error encountered: " + ioEx.getMessage());    	  
         return;
      } catch (GoogleAuthException authEx) {
         // Failure. The call is not expected to ever succeed so it should not be
         // retried.
    	  Log.e(TAG, "Unrecoverable authentication exception: " + authEx.getMessage(), authEx);
         return;
      }
  }  
  
// https://developer.android.com/reference/com/google/android/gms/auth/GoogleAuthUtil.html#getToken%28android.content.Context,%20java.lang.String,%20java.lang.String%29
  public void showToast(final String toast) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
      }
    });
  }
  
  void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
      runOnUiThread(new Runnable() {
        public void run() {
          Dialog dialog =
              GooglePlayServicesUtil.getErrorDialog(connectionStatusCode, DriveSample.this,
                  REQUEST_GOOGLE_PLAY_SERVICES);
          dialog.show();
        }
      });
    }

    void refreshView() {
        if (mAccountName != null)
            textViewHello.setText("Hello " + mAccountName + "!!!");
        textViewQuota.setText(mUsername + " (" + mUsedQuota + "/" + mTotalQuota + ")");
    }  
}