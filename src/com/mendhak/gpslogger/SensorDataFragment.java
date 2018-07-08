package com.mendhak.gpslogger;

import java.text.DecimalFormat;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment to show the sensor data
 */
public class SensorDataFragment extends Fragment {
    private static final String LOG_TAG = SensorDataFragment.class.getSimpleName();
    
    private SharedPreferences preferences;
    
    private static final DecimalFormat temperatureValueTexviewFormat = new DecimalFormat("0.0;-0.0");
    private TextView temperatureValueTextview;
    private TextView temperatureUnitTextview;
    private Double temperatureInDegreeCelcius = null;
    
    private static final DecimalFormat humidityValueTexviewFormat = new DecimalFormat("0.0;0.0");
    private TextView humidityValueTextview;
    
    private static final DecimalFormat airPressureValueTextViewFormat = new DecimalFormat("0;0");
    private TextView airPressureValueTextview;
    
    private void applyPreferences() {
        setTemperatureUnitBasedOnPreference();
        //setTemperature(temperatureInDegreeCelcius);
    }

    private void setTemperatureUnitBasedOnPreference() {
        String preferredTemperatureUnit = getPreferredTemperatureUnit();
        temperatureUnitTextview.setText(preferredTemperatureUnit);
    }

    private String getPreferredTemperatureUnit() {
        String temperatureUnitPreferenceKey = getResources().getString(R.string.preference_temperature_unit_key);
        String temperatureUnitDefaultValue = getResources().getString(R.string.preference_temperature_unit_default_value);
        return preferences.getString(temperatureUnitPreferenceKey, temperatureUnitDefaultValue);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(LOG_TAG, "onCreate fragment");
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }
    
   /* @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       // View rootView = inflater.inflate(R.layout.fragment_sensordata, container, false);
        
       // temperatureUnitTextview = (TextView) rootView.findViewById(R.id.temperatureUnitText);
       // temperatureValueTextview = (TextView) rootView.findViewById(R.id.temperatureValue);
       // humidityValueTextview = (TextView) rootView.findViewById(R.id.humidityValue);
       // airPressureValueTextview = (TextView) rootView.findViewById(R.id.airPressureValue);
        
       // return rootView;
    } */

    @Override
    public void onStart() {
        super.onStart();
        applyPreferences();
    }
     
    
}
