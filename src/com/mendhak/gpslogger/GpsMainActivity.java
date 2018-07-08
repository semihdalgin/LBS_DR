package com.mendhak.gpslogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;


import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.mendhak.gpslogger.ZamanServisi;
import com.mendhak.gpslogger.GPSDataContentProvider.DatabaseHelper;
import com.mendhak.gpslogger.common.AppSettings;
import com.mendhak.gpslogger.common.IActionListener;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Utilities;

import com.mendhak.gpslogger.senders.FileSenderFactory;
import com.mendhak.gpslogger.senders.IFileSender;

import com.mendhak.gpslogger.senders.email.AutoEmailActivity;
import com.mendhak.gpslogger.senders.ftp.AutoFtpActivity;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import com.mendhak.gpslogger.NfcHelper;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.*;


import com.mendhak.gattsensor.SensorData;


import com.mendhak.gpslogger.SensorDataFragment;


import com.mendhak.gpslogger.BluetoothDeviceInfo;
import com.mendhak.gpslogger.BluetoothLeService;

import com.mendhak.gattsensor.BarometerGatt;
import com.mendhak.gattsensor.GattSensor;
import com.mendhak.gattsensor.HygrometerGatt;
import com.mendhak.gattsensor.ThermometerGatt;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class GpsMainActivity extends SherlockActivity implements OnCheckedChangeListener,
        IGpsLoggerServiceClient, View.OnClickListener, IActionListener
{
	

    /**
     * General all purpose handler used for updating the UI from threads.
     */
    private static Intent serviceIntent;
    private GpsLoggingService loggingService;
    private MenuItem mnuFloor;
    private MenuItem mnuZaman;
    private MenuItem mnuHeight;
    
	
	private static final String TAG = "GPSDataContentProvider";
	
	float pressure_value = 0.0f;
    float height = 0.0f;
    static int semsat=0;
    
	
	double ortpressure;
 	double ortheight;
 	double orttemp;
 	double orthumidty;
 	double mesafe;
 	
 	double yenpressure;
 	double yenheight;
 	double yentemp;
 	double yenhumidty;
 	
 	double zlat;
 	double zlong;
 	double zalt;
	
	double homelat= 39.88728;
	double homelong= 32.84679;
	int    homefloor=0;

	double comlat= 39.89500;
	double comlong= 32.81600;
	int    comfloor=4;	
		
	DatabaseHelper db;
	int say=0;
	int esay=0;

	static String providerName;
	

	public static int floorresult=500;
	
	
	NfcHelper nfcHelper;
	EditText edTextContent;
	TextView txtViewTagId;
	TextView txtViewTagType;
	TextView txtViewTagContentType;
	TextView txtViewTagContentLength;
	
	
	// BLE TI
	
	private static final String LOG_TAG = GpsMainActivity.class.getSimpleName();

    // Requests to other activities
    private static final int REQUEST_TO_ENABLE_BLUETOOTHE_LE = 0;

    private Intent bindIntent;
    
    
    public static double sempressure;
    public static double sematemperature;
    public static double semirtemperature;
    public static double semhum;
    
    public static int dene;

    
	File file=null;
	 
	
    /**
     * Provides a connection to the GPS Logging Service
     */
        
    
    private final ServiceConnection gpsServiceConnection = new ServiceConnection()
    {
    	
    	

        public void onServiceDisconnected(ComponentName name)
        {
            loggingService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service)
        {
            loggingService = ((GpsLoggingService.GpsLoggingBinder) service).getService();
            GpsLoggingService.SetServiceClient(GpsMainActivity.this);
            
        

            Button buttonSinglePoint = (Button) findViewById(R.id.buttonSinglePoint);

            buttonSinglePoint.setOnClickListener(GpsMainActivity.this);

            if (Session.isStarted())
            {
                if (Session.isSinglePointMode())
                {
                    SetMainButtonEnabled(false);
                }
                else
                {
                    SetMainButtonChecked(true);
                    SetSinglePointButtonEnabled(false);
                }

                DisplayLocationInfo(Session.getCurrentLocationInfo());
            }

            // Form setup - toggle button, display existing location info
            ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
            buttonOnOff.setOnCheckedChangeListener(GpsMainActivity.this);

            if (Session.hasDescription())
                OnSetAnnotation();
        }
    };


    /**
     * Event raised when the form is created for the first time
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        Utilities.LogDebug("GpsMainActivity.onCreate");

        super.onCreate(savedInstanceState);
        
        db = new GPSDataContentProvider.DatabaseHelper (getApplicationContext());

        Utilities.LogInfo("GPSLogger started");

        setContentView(R.layout.main);
        
        //setContentView(R.layout.activity_main);
        
        NotificationManager notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	    
	    Intent intentz = new Intent(getApplicationContext(), ZamanServisi.class );
		startService(intentz);	
		
		/* if (savedInstanceState == null) {
            //Use this check to determine whether BLE is supported on the device.
            //Then you can selectively disable BLE-related features.
            if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
                finish();
            }

           getFragmentManager().beginTransaction().add(R.layout.activity_main, new SensorDataFragment()).commit();
        }  */
        


		
        // Moved to onResume to update the list of loggers
        //GetPreferences();

        StartAndBindService();
        ShowWhatsNew();
        
        nfcHelper = new NfcHelper(this);
        
        if (nfcHelper.isNfcIntent(getIntent())) {
			handleNfcIntent(getIntent());
		}
    }

    private void ShowWhatsNew()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int currentVersionNumber = 0;

        int savedVersionNumber = prefs.getInt("SAVED_VERSION", 0);
        String gdocsKey = prefs.getString("GDOCS_ACCOUNT_NAME", "");

        try
        {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersionNumber = pi.versionCode;
        }
        catch (Exception e) {}

        if (currentVersionNumber > savedVersionNumber)
        {
            if(!Utilities.IsNullOrEmpty(gdocsKey))
            {
                Utilities.MsgBox("Google Docs users, please note",
                        "A few weeks ago, Google Docs upload stopped working due to Google breaking an API.\r\n\r\n " +
                        "I've had to rewrite this feature, but you will need to reauthenticate. \r\n\r\n " +
                        "To do this, go to the Google Docs settings, clear your authorization and reauthorize yourself. \r\n\r\n " +
                        "Also note that phones without Google Play can no longer use the Google Docs upload feature.\r\n\r\n" +
                        "Please report on Github if there are any problems with the Google Docs upload.\r\n", this);
            }

            SharedPreferences.Editor editor   = prefs.edit();
            editor.putInt("SAVED_VERSION", currentVersionNumber);
            editor.commit();
        }
    }

    @Override
    protected void onStart()
    {
        Utilities.LogDebug("GpsMainActivity.onStart");
        super.onStart();
        StartAndBindService();
        
    }
    

    
    
    private static IntentFilter createSensorTagUpdateIntentFilter() {
        final IntentFilter fi = new IntentFilter();
        fi.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        fi.addAction(BluetoothLeService.ACTION_DATA_NOTIFY);
        fi.addAction(BluetoothLeService.ACTION_DATA_WRITE);
        fi.addAction(BluetoothLeService.ACTION_DATA_READ);
        return fi;
    }
    
         
    
    @Override
    protected void onResume()
    {
        Utilities.LogDebug("GpsMainactivity.onResume");
        super.onResume();        
        GetPreferences();
        StartAndBindService();
        
        if (nfcHelper.isNfcEnabledDevice()) {
			nfcHelper.enableForegroundDispatch();
		}
        
    }
    


    /**
     * Starts the service and binds the activity to it.
     */
    private void StartAndBindService()
    {
        Utilities.LogDebug("StartAndBindService - binding now");
        serviceIntent = new Intent(this, GpsLoggingService.class);
        Intent intent = new Intent(getApplicationContext(), ZamanServisi.class );
        // Start the service in case it isn't already running
        startService(serviceIntent);
        // Now bind to service
        bindService(serviceIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);
        Session.setBoundToService(true);
    }


    /**
     * Stops the service if it isn't logging. Also unbinds.
     */
    private void StopAndUnbindServiceIfRequired()
    {
        Utilities.LogDebug("GpsMainActivity.StopAndUnbindServiceIfRequired");
        if (Session.isBoundToService())
        {
            unbindService(gpsServiceConnection);
            Session.setBoundToService(false);
        }

        if (!Session.isStarted())
        {
            Utilities.LogDebug("StopServiceIfRequired - Stopping the service");
            //serviceIntent = new Intent(this, GpsLoggingService.class);
            stopService(serviceIntent);
        }

    }

    @Override
    protected void onPause()
    {

        Utilities.LogDebug("GpsMainActivity.onPause");
        StopAndUnbindServiceIfRequired();
        super.onPause();
        
        if (nfcHelper.isNfcEnabledDevice()) {
			nfcHelper.disableForegroundDispatch();
		}
    }

    @Override
    protected void onDestroy()
    {

        Utilities.LogDebug("GpsMainActivity.onDestroy");
        StopAndUnbindServiceIfRequired();
        super.onDestroy();

    }

    /**
     * Called when the toggle button is clicked
     */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        Utilities.LogDebug("GpsMainActivity.onCheckedChanged");

        if (isChecked)
        {
            GetPreferences();
            SetSinglePointButtonEnabled(false);
            loggingService.SetupAutoSendTimers();
            loggingService.StartLogging();
        }
        else
        {
            SetSinglePointButtonEnabled(true);
            loggingService.StopLogging();
        }
    }

    /**
     * Called when the single point button is clicked
     */
    public void onClick(View view)
    {
        Utilities.LogDebug("GpsMainActivity.onClick");

        if (!Session.isStarted())
        {
            SetMainButtonEnabled(false);
            loggingService.StartLogging();
            Session.setSinglePointMode(true);
        }
        else if (Session.isStarted() && Session.isSinglePointMode())
        {
            loggingService.StopLogging();
            SetMainButtonEnabled(true);
            Session.setSinglePointMode(false);
        }
    }


    public void SetSinglePointButtonEnabled(boolean enabled)
    {
        Button buttonSinglePoint = (Button) findViewById(R.id.buttonSinglePoint);
        buttonSinglePoint.setEnabled(enabled);
    }
    
    public void SetFloorButtonMarked(boolean marked)
    {
        if (marked)
        {
            mnuFloor.setIcon(R.drawable.entry);
        }
        else
        {
            mnuFloor.setIcon(R.drawable.entry);
        }
    }
    
    public void SetZamanButtonMarked(boolean marked)
    {
        if (marked)
        {
            mnuZaman.setIcon(R.drawable.mnu_time);
        }
        else
        {
            mnuZaman.setIcon(R.drawable.mnu_time);
        }
    }
    
    public void SetHeightButtonMarked(boolean marked)
    {
        if (marked)
        {
            mnuHeight.setIcon(R.drawable.apart);
        }
        else
        {
            mnuHeight.setIcon(R.drawable.apart);
        }
    }

    public void SetMainButtonEnabled(boolean enabled)
    {
        ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
        buttonOnOff.setEnabled(enabled);
    }

    public void SetMainButtonChecked(boolean checked)
    {
        ToggleButton buttonOnOff = (ToggleButton) findViewById(R.id.buttonOnOff);
        buttonOnOff.setChecked(checked);
    }

    /**
     * Gets preferences chosen by the user
     */
    private void GetPreferences()
    {
        Utilities.PopulateAppSettings(getApplicationContext());
        ShowPreferencesSummary();
    }

    /**
     * Displays a human readable summary of the preferences chosen by the user
     * on the main form
     */
    private void ShowPreferencesSummary()
    {
        Utilities.LogDebug("GpsMainActivity.ShowPreferencesSummary");
        try
        {
            TextView txtLoggingTo = (TextView) findViewById(R.id.txtLoggingTo);
            TextView txtFrequency = (TextView) findViewById(R.id.txtFrequency);
            TextView txtDistance = (TextView) findViewById(R.id.txtDistance);
            TextView txtAutoEmail = (TextView) findViewById(R.id.txtAutoEmail);

            if (AppSettings.getMinimumSeconds() > 0)
            {
                String descriptiveTime = Utilities.GetDescriptiveTimeString(AppSettings.getMinimumSeconds(),
                        getApplicationContext());

                txtFrequency.setText(descriptiveTime);
            }
            else
            {
                txtFrequency.setText(R.string.summary_freq_max);

            }


            if (AppSettings.getMinimumDistanceInMeters() > 0)
            {
                if (AppSettings.shouldUseImperial())
                {
                    int minimumDistanceInFeet = Utilities.MetersToFeet(AppSettings.getMinimumDistanceInMeters());
                    txtDistance.setText(((minimumDistanceInFeet == 1)
                            ? getString(R.string.foot)
                            : String.valueOf(minimumDistanceInFeet) + getString(R.string.feet)));
                }
                else
                {
                    txtDistance.setText(((AppSettings.getMinimumDistanceInMeters() == 1)
                            ? getString(R.string.meter)
                            : String.valueOf(AppSettings.getMinimumDistanceInMeters()) + getString(R.string.meters)));
                }
            }
            else
            {
                txtDistance.setText(R.string.summary_dist_regardless);
            }


            if (AppSettings.isAutoSendEnabled())
            {
                String autoEmailResx;

                if (AppSettings.getAutoSendDelay() == 0)
                {
                    autoEmailResx = "autoemail_frequency_whenistop";
                }
                else
                {

                    autoEmailResx = "autoemail_frequency_"
                            + String.valueOf(AppSettings.getAutoSendDelay()).replace(".", "");
                }

                String autoEmailDesc = getString(getResources().getIdentifier(autoEmailResx, "string", getPackageName()));

                txtAutoEmail.setText(autoEmailDesc);
            }
            else
            {
                TableRow trAutoEmail = (TableRow) findViewById(R.id.trAutoEmail);
                trAutoEmail.setVisibility(View.INVISIBLE);
            }

            onFileName(Session.getCurrentFileName());
        }
        catch (Exception ex)
        {
            Utilities.LogError("ShowPreferencesSummary", ex);
        }


    }

    /**
     * Handles the hardware back-button press
     */
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        Utilities.LogInfo("KeyDown - " + String.valueOf(keyCode));

        if (keyCode == KeyEvent.KEYCODE_BACK && Session.isBoundToService())
        {
            StopAndUnbindServiceIfRequired();
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Called when the menu is created.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        com.actionbarsherlock.view.MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.optionsmenu, menu);
        mnuFloor = menu.findItem(R.id.mnuFloor);
        mnuZaman = menu.findItem(R.id.mnuZaman);
        mnuHeight = menu.findItem(R.id.mnuHeight);

        getSupportActionBar().setDisplayShowTitleEnabled(false);

        return true;
    }


    /**
     * Called when one of the menu items is selected.
     */
    public boolean onOptionsItemSelected(MenuItem item)
    {

        int itemId = item.getItemId();
        Utilities.LogInfo("Option item selected - " + String.valueOf(item.getTitle()));

        switch (itemId)
        {
            case R.id.mnuSettings:
                Intent settingsActivity = new Intent(getApplicationContext(), GpsSettingsActivity.class);
                startActivity(settingsActivity);
                break;
            case R.id.mnuEmail:
                SelectAndEmailFile();
                break;
            case R.id.mnuExport:
            	OnExport();
            	break;
            case R.id.mnuDell:
            	OnDell();
            	break;
            case R.id.mnuFloor:
                OnFloor();
                break;
            case R.id.mnuZaman:
                OnZaman();
                break;
            case R.id.mnuHeight:
                OnHeight();
                break;
            case R.id.mnuEmailnow:
                EmailNow();
                break;
            case R.id.mnuFAQ:
                Intent faqtivity = new Intent(getApplicationContext(), Faqtivity.class);
                startActivity(faqtivity);
                break;
            case R.id.mnuExit:
                loggingService.StopLogging();
                loggingService.stopSelf();
                finish();
                break;
        }
        return false;
    }


    private void EmailNow()
    {
        Utilities.LogDebug("GpsMainActivity.EmailNow");

        if (AppSettings.isAutoSendEnabled())
        {
            loggingService.ForceEmailLogFile();
        }
        else
        {

            Intent pref = new Intent().setClass(this, GpsSettingsActivity.class);
            pref.putExtra("autosend_preferencescreen", true);
            startActivity(pref);

        }

    }



    private void SelectAndEmailFile()
    {
        Utilities.LogDebug("GpsMainActivity.SelectAndEmailFile");

        Intent settingsIntent = new Intent(getApplicationContext(), AutoEmailActivity.class);

        if (!Utilities.IsEmailSetup())
        {

            startActivity(settingsIntent);
        }
        else
        {
            ShowFileListDialog(settingsIntent, FileSenderFactory.GetEmailSender(this));
        }

    }

    private void SendToFtp()
    {
        Utilities.LogDebug("GpsMainActivity.SendToFTP");

        Intent settingsIntent = new Intent(getApplicationContext(), AutoFtpActivity.class);

        if(!Utilities.IsFtpSetup())
        {
            startActivity(settingsIntent);
        }
        else
        {
            IFileSender fs = FileSenderFactory.GetFtpSender(getApplicationContext(), this);
            ShowFileListDialog(settingsIntent, fs);

        }
    }





    private void ShowFileListDialog(final Intent settingsIntent, final IFileSender sender)
    {

        final File gpxFolder = new File(Environment.getExternalStorageDirectory(), "GPSLogger");

        if (gpxFolder.exists())
        {
            File[] enumeratedFiles = gpxFolder.listFiles(sender);

            Arrays.sort(enumeratedFiles, new Comparator<File>()
            {
                public int compare(File f1, File f2)
                {
                    return -1 * Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                }
            });

            List<String> fileList = new ArrayList<String>(enumeratedFiles.length);

            for (File f : enumeratedFiles)
            {
                fileList.add(f.getName());
            }

            final String settingsText = getString(R.string.menu_settings);

            fileList.add(0, settingsText);
            final String[] files = fileList.toArray(new String[fileList.size()]);

            final Dialog dialog = new Dialog(this);
            dialog.setTitle(R.string.osm_pick_file);
            dialog.setContentView(R.layout.filelist);
            ListView displayList = (ListView) dialog.findViewById(R.id.listViewFiles);

            displayList.setAdapter(new ArrayAdapter<String>(getApplicationContext(),
                    android.R.layout.simple_list_item_single_choice, files));

            displayList.setOnItemClickListener(new OnItemClickListener()
            {
                public void onItemClick(AdapterView<?> av, View v, int index, long arg)
                {

                    dialog.dismiss();
                    String chosenFileName = files[index];

                    if (chosenFileName.equalsIgnoreCase(settingsText))
                    {
                        startActivity(settingsIntent);
                    }
                    else
                    {
                        Utilities.ShowProgress(GpsMainActivity.this, getString(R.string.please_wait),
                                getString(R.string.please_wait));
                        List<File> files = new ArrayList<File>();
                        files.add(new File(gpxFolder, chosenFileName));
                        sender.UploadFile(files);
                    }
                }
            });
            dialog.show();
        }
        else
        {
            Utilities.MsgBox(getString(R.string.sorry), getString(R.string.no_files_found), this);
        }
    }


    /**
     * Clears the table, removes all values.
     */
    public void ClearForm()
    {

        Utilities.LogDebug("GpsMainActivity.ClearForm");

        TextView tvLatitude = (TextView) findViewById(R.id.txtLatitude);
        TextView tvLongitude = (TextView) findViewById(R.id.txtLongitude);
        TextView tvPressure = (TextView) findViewById(R.id.txtPressure);
        TextView tvTemperature = (TextView) findViewById(R.id.txtTemperature);
        TextView tvHumidity = (TextView) findViewById(R.id.txtHumidity);

        
        TextView tvDateTime = (TextView) findViewById(R.id.txtDateTimeAndProvider);

        TextView tvAltitude = (TextView) findViewById(R.id.txtAltitude);

        TextView txtSpeed = (TextView) findViewById(R.id.txtSpeed);

        TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
        TextView txtDirection = (TextView) findViewById(R.id.txtDirection);
        TextView txtAccuracy = (TextView) findViewById(R.id.txtAccuracy);
        TextView txtDistance = (TextView) findViewById(R.id.txtDistanceTravelled);

        tvLatitude.setText("");
        tvLongitude.setText("");
        tvPressure.setText("");
        tvTemperature.setText("");
        tvHumidity.setText("");
        
        tvDateTime.setText("");
        tvAltitude.setText("");
        txtSpeed.setText("");
        txtSatellites.setText("");
        txtDirection.setText("");
        txtAccuracy.setText("");
        txtDistance.setText("");
        Session.setPreviousLocationInfo(null);
        Session.setTotalTravelled(0d);
    }

    public void OnStopLogging()
    {
        Utilities.LogDebug("GpsMainActivity.OnStopLogging");
        SetMainButtonChecked(false);
    }

    public void OnSetAnnotation()
    {
        Utilities.LogDebug("GpsMainActivity.OnSetAnnotation");
       
    }

    public void OnClearAnnotation()
    {
        Utilities.LogDebug("GpsMainActivity.OnClearAnnotation");
   
    }


    /**
     * Sets the message in the top status label.
     *
     * @param message The status message
     */
   
    private void SetStatus(String message)
    {
        Utilities.LogDebug("GpsMainActivity.SetStatus: " + message);
        TextView tvStatus = (TextView) findViewById(R.id.textStatus);
        tvStatus.setText(message);
        Utilities.LogInfo(message);
    }

    /**
     * Sets the number of satellites in the satellite row in the table.
     *
     * @param number The number of satellites
     */
    private void SetSatelliteInfo(int number)
    {
        Session.setSatelliteCount(number);
        TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
        txtSatellites.setText(String.valueOf(number));
        semsat = number;
    }

    /**
     * Given a location fix, processes it and displays it in the table on the
     * form.
     *
     * @param loc Location information
     */
    public void DisplayLocationInfo(Location loc)
    {
        Utilities.LogDebug("GpsMainActivity.DisplayLocationInfo");
        
        
        try
        {

            if (loc == null)
            {
                return;
            }

            TextView tvLatitude = (TextView) findViewById(R.id.txtLatitude);
            TextView tvLongitude = (TextView) findViewById(R.id.txtLongitude);
            
            TextView tvDateTime = (TextView) findViewById(R.id.txtDateTimeAndProvider);

            TextView tvAltitude = (TextView) findViewById(R.id.txtAltitude);

            TextView txtSpeed = (TextView) findViewById(R.id.txtSpeed);

            TextView txtSatellites = (TextView) findViewById(R.id.txtSatellites);
            TextView txtDirection = (TextView) findViewById(R.id.txtDirection);
            TextView txtAccuracy = (TextView) findViewById(R.id.txtAccuracy);
            TextView txtTravelled = (TextView) findViewById(R.id.txtDistanceTravelled);
            providerName = loc.getProvider();
            

            if (providerName.equalsIgnoreCase("gps"))
            {
                providerName = getString(R.string.providername_gps);
            }
            else
            {
                providerName = getString(R.string.providername_celltower);
            }

            tvDateTime.setText(new Date(Session.getLatestTimeStamp()).toLocaleString()
                    + getString(R.string.providername_using, providerName));
            
            
            double latok = loc.getLatitude();
            double longok= loc.getLongitude();
            
            int degreelat = (int) latok;
            double minlat1=((latok-degreelat)*60);
            int minlat= (int) minlat1;
            double seclat=(minlat1-minlat)*60;
            
            if (latok>0) {
            	
            	tvLatitude.setText(degreelat+getString(R.string.degree_symbol)+" " + minlat + "' " + new DecimalFormat("##.0").format(seclat)+"'' " + getString(R.string.north_i));
            } else {
            	tvLatitude.setText(degreelat+getString(R.string.degree_symbol)+" " + minlat + "' " + new DecimalFormat("##.0").format(seclat) +"'' " + getString(R.string.south_i));
            }
            	
            int degreelong = (int) longok;
            double minlong1=((longok-degreelong)*60);
            int minlong= (int) minlong1;
            double seclong=(minlong1-minlong)*60;
            
            
            
            if (latok>0) {
            	
            	tvLongitude.setText(degreelong+getString(R.string.degree_symbol)+" " + minlong + "' " + new DecimalFormat("##.0").format(seclong) +"'' " + getString(R.string.east_i));
            } else {
            	tvLongitude.setText(degreelong+getString(R.string.degree_symbol)+" " + minlong + "' " + new DecimalFormat("##.0").format(seclong) +"'' " + getString(R.string.west_i));
            }
            
            
           // double d1=loc.getLatitude()-homelat;
           // double d2=loc.getLongitude()-homelong;

           // if (d1<0) {
            //   d1=d1*-1;
             //  	}
                		
            // if(d2<0){
            //   d2=d2*-1;
            //   	}
                		
            // double d3=loc.getLatitude()-comlat;
            // double d4=loc.getLongitude()-comlong;
                		
            //  if (d3<0) {
            //	d3=d3*-1;
            //	}
                		
            //  if(d4<0){
            //	d4=d4*-1;
            //	}
                		
                		
                //		if (d1<0.0005 && d2<0.0005) {
                //			floorresult=homefloor;
                //		} else if  (d3<0.001 && d4<0.001){
                //			floorresult=comfloor;
                //		} else{
                //			floorresult=500;
                //		} 
                		
                //		if(floorresult==500) {
                //    		
                //			TextView tvFloor = (TextView) findViewById(R.id.txtFloor);
                //            tvFloor.setText(R.string.floor_info); }
                //		else {                			
                			
                //		TextView tvFloor = (TextView) findViewById(R.id.txtFloor);
                //        tvFloor.setText(String.valueOf(floorresult)); } 
            
            if (ZamanServisi.floorresult == 500) {
        		
        		TextView tvFloor = (TextView) findViewById(R.id.txtFloor);
                tvFloor.setText("Kat Bilgisi Verilemiyor"); }
        		else {
        		
        		TextView tvFloor = (TextView) findViewById(R.id.txtFloor);
                tvFloor.setText(String.valueOf(ZamanServisi.floorresult)); 
                }		
    	        
    	        say= db.getDbCount();
    	        
    	        if (say>2) {
    	        
    	        String unit=" mbar";
    	        
    	        double a1=db.getPressure(say);
    	        double a2=db.getPressure(say-1);
    	        
    	        ortpressure= (a1+a2)/2;
    	        
    	        double b1=db.getBHeight(say);
    	        double b2=db.getBHeight(say-1);
    	        
    	        ortheight= (b1+b2)/2;
    	        
    	        double c1=db.getTemp(say);
    	        double c2=db.getTemp(say-1);
    	        
    	        orttemp= (c1+c2)/2;
    	        
    	        double d1=db.getHumidity(say);
    	        double d2=db.getHumidity(say-1);
    	        
    	        orthumidty= (d1+d2)/2;
    	    	
    	        TextView tvPressure = (TextView) findViewById(R.id.txtPressure);
    	        TextView tvOheight = (TextView) findViewById(R.id.txtOheight);
    	        tvPressure.setText(String.valueOf(new DecimalFormat("####.00").format(ortpressure))+ unit); 
    	        tvOheight.setText(String.valueOf(new DecimalFormat("##.0").format(ortheight))+" m");
    	        
    	        TextView tvKatdegis = (TextView) findViewById(R.id.txtKatdegis);
    	        int katint=(int) ZamanServisi.kat;
    	        tvKatdegis.setText(String.valueOf(new DecimalFormat("##").format(katint)));
    	        
    	        
    	        if (orttemp ==3000) {
    	        	TextView tvTemperature = (TextView) findViewById(R.id.txtTemperature);
    				tvTemperature.setText(R.string.temp_info);
    	        
    	        }else {
    				TextView tvTemperature = (TextView) findViewById(R.id.txtTemperature);
    				tvTemperature.setText(String.valueOf(new DecimalFormat("##.0").format(orttemp))+" °C");
    				}
    			
    	        if (orthumidty ==3000) {
    	        	TextView tvHumidity = (TextView) findViewById(R.id.txtHumidity);
    				tvHumidity.setText(R.string.humidty_info);
    	        }else {
    	    		TextView tvHumidity = (TextView) findViewById(R.id.txtHumidity);
    	        	tvHumidity.setText(String.valueOf(new DecimalFormat("##.0").format(orthumidty))+" %");
    	    			}
    	        
    	        }	
            
            
            
            if (ZamanServisi.countpress>500) {
       			
       			
	        	final int NOTIF_ID = 1234;
			 
	        		NotificationManager notifManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	        		Notification note = new Notification(R.drawable.icon, getString(R.string.verigirisiana), System.currentTimeMillis());
			 
	        		Intent intent = new Intent(getApplicationContext(), GpsMainActivity.class);
            	intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			 
            	PendingIntent intent1 = PendingIntent.getActivity(this, 0, intent, 0);
			 
            	startActivity(intent);
			 
            	note.setLatestEventInfo(this, getString(R.string.verigirisiana), getString(R.string.verigirisi), intent1);
			 
            	notifManager.notify(NOTIF_ID, note);
			 
            	ZamanServisi.countpress=0;
	 			}
            
            
       
            
            
            if (loc.hasAltitude())
            {

                double altitude = loc.getAltitude();

                if (AppSettings.shouldUseImperial())
                {
                    tvAltitude.setText(String.valueOf(Utilities.MetersToFeet(altitude))
                            + getString(R.string.feet));
                }
                else
                {
                    tvAltitude.setText(new DecimalFormat("##.0").format(altitude)+ getString(R.string.meters));
                }

            }
            else
            {
                tvAltitude.setText(R.string.not_applicable);
            }
            
            if (loc.hasSpeed())
            {

                float speed = loc.getSpeed();
                String unit;
                if (AppSettings.shouldUseImperial())
                {
                    if (speed > 1.47)
                    {
                        speed = speed * 0.6818f;
                        unit = getString(R.string.miles_per_hour);

                    }
                    else
                    {
                        speed = Utilities.MetersToFeet(speed);
                        unit = getString(R.string.feet_per_second);
                    }
                }
                else
                {
                    if (speed > 0.277)
                    {
                        speed = speed * 3.6f;
                        unit = getString(R.string.kilometers_per_hour);
                    }
                    else
                    {
                        unit = getString(R.string.meters_per_second);
                    }
                }

                txtSpeed.setText(String.valueOf(speed) + unit);

            }
            else
            {
                txtSpeed.setText(R.string.not_applicable);
            }

            if (loc.hasBearing())
            {

                float bearingDegrees = loc.getBearing();
                String direction;

                direction = Utilities.GetBearingDescription(bearingDegrees, getApplicationContext());

                txtDirection.setText(direction + "(" + String.valueOf(Math.round(bearingDegrees))
                        + getString(R.string.degree_symbol) + ")");
            }
            else
            {
                txtDirection.setText(R.string.not_applicable);
            }

            if (!Session.isUsingGps())
            {
                txtSatellites.setText(R.string.not_applicable);
                Session.setSatelliteCount(0);
            }

            if (loc.hasAccuracy())
            {

                float accuracy = loc.getAccuracy();

                if (AppSettings.shouldUseImperial())
                {
                    txtAccuracy.setText(getString(R.string.accuracy_within,
                            String.valueOf(Utilities.MetersToFeet(accuracy)), getString(R.string.feet)));

                }
                else
                {
                    txtAccuracy.setText(getString(R.string.accuracy_within, String.valueOf(accuracy),
                            getString(R.string.meters)));
                }

            }
            else
            {
                txtAccuracy.setText(R.string.not_applicable);
            }


            String distanceUnit;
            double distanceValue = Session.getTotalTravelled();
               
            
            if (AppSettings.shouldUseImperial())
            {
                distanceUnit = getString(R.string.feet);
                distanceValue = Utilities.MetersToFeet(distanceValue);
                // When it passes more than 1 kilometer, convert to miles.
                if (distanceValue > 3281)
                {
                    distanceUnit = getString(R.string.miles);
                    distanceValue = distanceValue / 5280;
                }
            }
            else
            {
                distanceUnit = getString(R.string.meters);
                if (distanceValue > 1000)
                {
                    distanceUnit = getString(R.string.kilometers);
                    distanceValue = distanceValue / 1000;
                }
            }

            txtTravelled.setText(String.valueOf(Math.round(distanceValue)) + " " + distanceUnit +
                    " (" + Session.getNumLegs() + " points)");
        
        }
        catch (Exception ex)
        {
        	SetStatus(getString(R.string.error_displaying, ex.getMessage()));
        }  
     }
   
  

    public void OnLocationUpdate(Location loc)
    {
        Utilities.LogDebug("GpsMainActivity.OnLocationUpdate");
        DisplayLocationInfo(loc);
        ShowPreferencesSummary();
        SetMainButtonChecked(true); 
        Log.e(TAG, loc.toString());
      
        
                     
        if (Session.isSinglePointMode())
        {
            loggingService.StopLogging();
            SetMainButtonEnabled(true);
            Session.setSinglePointMode(false);
        }

    }
       
  

    public void OnSatelliteCount(int count)
    {
        SetSatelliteInfo(count);

    }

    public void onFileName(String newFileName)
    {
        if (newFileName == null || newFileName.length() <= 0)
        {
            return;
        }

        TextView txtFilename = (TextView) findViewById(R.id.txtFileName);

        if (AppSettings.shouldLogToGpx() || AppSettings.shouldLogToKml())
        {


            txtFilename.setText(getString(R.string.summary_current_filename_format,
                    Session.getCurrentFileName()));
        }
        else
        {
            txtFilename.setText("");
        }


    }
    

    public void OnStatusMessage(String message)
    {
        SetStatus(message);
    }

    public void OnFatalMessage(String message)
    {
        Utilities.MsgBox(getString(R.string.sorry), message, this);
    }

    public Activity GetActivity()
    {
        return this;
    }


    @Override
    public void OnComplete()
    {
        Utilities.HideProgress();
    } 
   
    @Override
    public void OnFailure()
    {
        Utilities.HideProgress();
    }  
    
    private static String padding_str(int c) {
		if (c >= 10)
		   return String.valueOf(c);
		else
		   return "0" + String.valueOf(c);
	}
    
	public void OnExport() {

		File sd = Environment.getExternalStorageDirectory();
		File data = Environment.getDataDirectory();
		FileChannel source = null;
		FileChannel destination = null;

		final Calendar dd = Calendar.getInstance();
		int day = dd.get(Calendar.DAY_OF_MONTH);
		int month = dd.get(Calendar.MONTH) + 1;
		int year = dd.get(Calendar.YEAR);

		String currentDBPath = "/data/" + "com.mendhak.gpslogger"
				+ "/databases/" + "semgpsdata.db";
		String backupDBPath = year + "." + month + "." + day + "_"
				+ "basinc_veritabani.db";
		File currentDB = new File(data, currentDBPath);
		File backupDB = new File(sd, backupDBPath);
		try {
			source = new FileInputStream(currentDB).getChannel();
			destination = new FileOutputStream(backupDB).getChannel();
			destination.transferFrom(source, 0, source.size());
			source.close();
			destination.close();
			Toast.makeText(this, "!!! Veritabaný Kaydedildi !!!",
					Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void OnDell() {
		for (long i = 1; i < ZamanServisi.ekksatir; i++) {
			db.deleteDb(i);
		}
		ZamanServisi.ekksatir=1;
	}
    
public void OnFloor() {
    	
    	AlertDialog.Builder alert = new AlertDialog.Builder(GpsMainActivity.this);

        alert.setTitle(R.string.add_description);
        alert.setMessage(R.string.numbers);

        // Set an EditText view to get user input
        final EditText input = new EditText(getApplicationContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        alert.setView(input);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
            	
            	final int desc =  Integer.parseInt(input.getText().toString());
                                    
                ZamanServisi.floorresult=desc;
                ZamanServisi.girkontrol=1;
                ZamanServisi.ekksatir = db.getDbCount() + 1;
                
                TextView tvFloor = (TextView) findViewById(R.id.txtFloor);
                tvFloor.setText(String.valueOf(ZamanServisi.floorresult));
                
            }

        });
        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int whichButton)
            {
                // Cancelled.
            }
        });

        AlertDialog alertDialog = alert.create();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        alertDialog.show();
        
    	  	
    }
public void OnZaman() {
	
	AlertDialog.Builder alert = new AlertDialog.Builder(GpsMainActivity.this);

    alert.setTitle(R.string.add_descriptionzaman);
    alert.setMessage(R.string.numberszaman);

    // Set an EditText view to get user input
    final EditText input = new EditText(getApplicationContext());
    input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
    alert.setView(input);

    alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int whichButton)
        {
        	
        	final int desc2 =  Integer.parseInt(input.getText().toString());
                                
            ZamanServisi.zmanmak=desc2;
        }

    });
    alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int whichButton)
        {
            // Cancelled.
        }
    });

    AlertDialog alertDialog = alert.create();
    alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    alertDialog.show();
    
	  	
}

public void OnHeight() {
	
	AlertDialog.Builder alert = new AlertDialog.Builder(GpsMainActivity.this);

    alert.setTitle(R.string.add_descriptionheight);
    alert.setMessage(R.string.numbersheight);

    // Set an EditText view to get user input
    final EditText input = new EditText(getApplicationContext());
    input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
    alert.setView(input);

    alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int whichButton)
        {
        	
        	final double desc3 =  Double.parseDouble(input.getText().toString());
                                
            ZamanServisi.heightmak=desc3;
        }

    });
    alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int whichButton)
        {
            // Cancelled.
        }
    });

    AlertDialog alertDialog = alert.create();
    alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    alertDialog.show();
	  	
}
private String tnfToString(short tnf) {
	switch (tnf) {
	case NdefRecord.TNF_EMPTY:
		return "TNF_EMPTY";
	case NdefRecord.TNF_ABSOLUTE_URI:
		return "TNF_ABSOLUTE_URI";
	case NdefRecord.TNF_EXTERNAL_TYPE:
		return "TNF_EXTERNAL_TYPE";
	case NdefRecord.TNF_MIME_MEDIA:
		return "TNF_MIME_MEDIA";
	case NdefRecord.TNF_UNCHANGED:
		return "TNF_UNCHANGED";
	case NdefRecord.TNF_WELL_KNOWN:
		return "TNF_WELL_KNOWN";
	default:
	case NdefRecord.TNF_UNKNOWN:
		return "TNF_UNKNOWN";
	}
}

private String rtdToString(byte[] rdt) {
	if (Arrays.equals(rdt, NdefRecord.RTD_ALTERNATIVE_CARRIER)) {
		return "RTD_ALTERNATIVE_CARRIER";
	} else if (Arrays.equals(rdt, NdefRecord.RTD_HANDOVER_CARRIER)) {
		return "RTD_HANDOVER_CARRIER";
	} else if (Arrays.equals(rdt, NdefRecord.RTD_HANDOVER_REQUEST)) {
		return "RTD_HANDOVER_REQUEST";
	} else if (Arrays.equals(rdt, NdefRecord.RTD_HANDOVER_SELECT)) {
		return "RTD_HANDOVER_SELECT";
	} else if (Arrays.equals(rdt, NdefRecord.RTD_SMART_POSTER)) {
		return "RTD_SMART_POSTER";
	} else if (Arrays.equals(rdt, NdefRecord.RTD_TEXT)) {
		return "RTD_TEXT";
	} else if (Arrays.equals(rdt, NdefRecord.RTD_URI)) {
		return "RTD_URI";
	} else {
		return "RTD_UNKNOWN";
	}
}

void handleNfcIntent(Intent intent) {
	Tag tag = nfcHelper.getTagFromIntent(intent);
	NdefMessage ndefMessage = nfcHelper.getNdefMessageFromIntent(intent);
	if (ndefMessage != null) {
		NdefRecord ndefRecord = nfcHelper.getFirstNdefRecord(ndefMessage);
		if (ndefRecord != null) {
			String content = nfcHelper.getTextFromNdefRecord(ndefRecord);
			String tnf = tnfToString(ndefRecord.getTnf());
			String rtd = rtdToString(ndefRecord.getType());
			int contents = Integer.parseInt(content);
			ZamanServisi.floorresult=contents;
            ZamanServisi.girkontrol=7;
            ZamanServisi.ekksatir = db.getDbCount() + 1;
		}
	}
}

@Override
protected void onNewIntent(Intent intent) {
	if (nfcHelper.isNfcIntent(intent)) {
		handleNfcIntent(intent);
	}
}
        
}