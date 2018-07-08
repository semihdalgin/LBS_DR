/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.mendhak.gpslogger;

import android.app.Activity;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;

interface IGpsLoggerServiceClient
{

    public void OnStatusMessage(String message);

 
    public void OnFatalMessage(String message);

    public void OnLocationUpdate(Location loc);
    

    public void OnSatelliteCount(int count);


    public void ClearForm();


    public void OnStopLogging();


    public void OnSetAnnotation();


    public void OnClearAnnotation();


    public Activity GetActivity();

    public void onFileName(String newFileName);


}
