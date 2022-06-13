/*
 * Copyright (c) 2010-2015, Semih Dalgin. All Rights Reserved.
 *
 * Licensed under the ITU-TUBITAK
 *
 */

package com.mendhak.gpslogger;

import android.net.Uri;
import android.provider.BaseColumns;

public final class GPSData {
    public static final String AUTHORITY = "com.mendhak.gpslogger";

    // This class cannot be instantiated
    private GPSData() {}
    
    /**
     * GPS data table
     */
    public static final class GPSPoint implements BaseColumns {
        // This class cannot be instantiated
        private GPSPoint() {}

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/gpspoint");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a track (list of points).
         */
        public static final String CONTENT_TYPE = "mime/text";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single point.
         */
        public static final String CONTENT_ITEM_TYPE = "";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "modified DESC";

        public static final String KEY_ID = "Id";
        public static final String SRG_ID = "SId";
        public static final String TIME = "Time";
        public static final String GUN = "Gun";
        public static final String LONGITUDE = "Longitude";
        public static final String LATITUDE = "Latitude";
        public static final String ALTITUDE = "Altitude";
        public static final String PRESSURE = "Pressure";
        public static final String TPRESSURE = "TahPressure";
        public static final String TEMPERATURE = "Temperature";
        public static final String HUMIDITY = "Humidity";
        public static final String FLOOR = "Floor";
        public static final String KATDEGIS = "Katdegis";
        public static final String BHEIGHT = "Bheight";
        public static final String PROVIDER = "Provider";
        public static final String SAT = "Satellite";
        public static final String SPEED = "Speed";
        public static final String ACCURACY = "Accuracy";
        public static final String USER = "User";
        public static final String PHONE = "Phone";
        public static final String ESAY = "Esay";
        public static final String SAY = "Say";
        public static final String KNTRL = "Kntrl";
        public static final String GIRKNTRL = "Girkntrl";
        public static final String DEGKNTRL = "Degkntrl";
        public static final String INTERVAL = "Interval";
        public static final String GX = "Gx";
        public static final String GY = "Gy";
        public static final String GZ = "Gz";
        public static final String MSF = "Mesafe";
        public static final String C1 = "C1";
        public static final String C2 = "C2";
        public static final String K1 = "K1";
        public static final String K2 = "K2";
        public static final String K3 = "K3";
        public static final String K4 = "K4";
        public static final String K5 = "K5";
        public static final String K6 = "K6";
        public static final String K7 = "K7";
        public static final String K8 = "K8";
        public static final String K9 = "K9";
        public static final String K10 = "K10";
        public static final String K11 = "K11";
        public static final String K12 = "K12";
        public static final String K13 = "K13";
        public static final String K14 = "K14";
        public static final String K15 = "K15";
        public static final String K16 = "K16";
        public static final String K17 = "K17";
        public static final String K18 = "K18";
        public static final String K19 = "K19";
        public static final String K20 = "K20";
        public static final String K21 = "K21";
        public static final String K22 = "K22";
        public static final String K23 = "K23";
        public static final String K24 = "K24";
        public static final String K25 = "K25";
        public static final String K26 = "K26";
        public static final String K27 = "K27";
        public static final String K28 = "K28";
        public static final String K29 = "K29";
        public static final String K30 = "K30";
        public static final String K31 = "K31";
        public static final String K32 = "K32";
        public static final String K33 = "K33";
        public static final String K34 = "K34";
        public static final String K35 = "K35";
        public static final String K36 = "K36";
        public static final String K37 = "K37";
        public static final String K38 = "K38";
        public static final String K39 = "K39";
        public static final String K40 = "K40";
        public static final String K41 = "K41";
        public static final String K42 = "K42";
        public static final String K43 = "K43";
        public static final String K44 = "K44";
        public static final String K45 = "K45";
        public static final String K46 = "K46";
        public static final String KatYuk = "KatYuk";
        
    }
    
    
}
