package com.mendhak.gpslogger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import android.opengl.Matrix;
import android.opengl.Matrix.*;

import com.mendhak.gpslogger.GPSDataContentProvider.DatabaseHelper;
import com.mendhak.gpslogger.common.Session;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.widget.TextView;

public class ZamanServisi extends Service implements SensorEventListener {
	private static final String DEBUG_TAG = "ZamanServisi";

	Timer zamanlayici;
	Handler yardimci;
	DatabaseHelper db;
	public static int zsay;
	public static int ekksatir = 1;
	public static double katyuk;
	public static int zmanmak = 30;
	public static double heightmak = 2.85;
	int zamanmak;
	float pressure_value = 0.0f;
	float height = 0.0f;
	double recentTemperatureReading = 3000;
	double recentHumidityReading = 3000;
	double mesafe;
	double enlemHafizada;
	double boylamHafizada;
	static int countpress;
	public static int floorresult = 500;
	static int girkontrol;
	static double kat;
	double ax, ay, az;
	double bx, by, bz;
	double cx, cy, cz;
	double dx, dy, dz;
	double fx, fy, fz;
	double gx, gy, gz;
	double hx, hy, hz;
	int ekkkontrol = 0;
	double c3[][];
	double kats[][];
	
	double yy = 4 * Math.PI;
	double ass4 = 3 * Math.PI;
	double tt = 2 * Math.PI;
	double ass = 1.5 * Math.PI;
	double zz = 1 * Math.PI;
	
	double zz1 = 1.25 * Math.PI;
	double zz2 = 1.125 * Math.PI;
	double zz3 = 2.5 * Math.PI;
	double zz4 = 2.25 * Math.PI;
	double zz5 = 2.125 * Math.PI;
	
	double zz6 = 3.5 * Math.PI;
	double zz7 = 3.25 * Math.PI;
	double zz8 = 3.125 * Math.PI;
	
	double zz9 = 64 * Math.PI;
	double zz10 = 32 * Math.PI;
	double zz11 = 16 * Math.PI;
	double zz12 = 8 * Math.PI;
	
	
	double zz13 = 0.5 * Math.PI;
	//double zz14 = 0.25 * Math.PI;
	//double zz15 = 0.125 * Math.PI;
	//double zz16 = 0.0625 * Math.PI;
	//double zz17 = 0.03125 * Math.PI;
	//double zz18 = 0.015625 * Math.PI;
	
	
	
	
	double cc1 = 0;
	double cc2 = 0;
	
	double ck1 = 0;
	double ck2 = 0;
	double ck3 = 0;
	double ck4 = 0;
	double ck5 = 0;
	double ck6 = 0;
	double ck7 = 0;
	double ck8 = 0;
	double ck9 = 0;
	double ck10 = 0;
	double ck11 = 0;
	double ck12 = 0;
	double ck13 = 0;
	double ck14 = 0;
	double ck15 = 0;
	double ck16 = 0;
	double ck17 = 0;
	double ck18 = 0;
	double ck19 = 0;
	double ck20 = 0;
	double ck21 = 0;
	double ck22 = 0;
	double ck23 = 0;
	double ck24 = 0;
	double ck25 = 0;
	double ck26 = 0;
	double ck27 = 0;
	double ck28 = 0;
	double ck29 = 0;
	double ck30 = 0;
	double ck31 = 0;
	double ck32 = 0;
	double ck33 = 0;
	double ck34 = 0;
	double ck35 = 0;
	double ck36 = 0;
	double ck37 = 0;
	double ck38 = 0;
	double ck39 = 0;
	double ck40 = 0;
	double ck41 = 0;
	double ck42 = 0;
	double ck43 = 0;
	double ck44 = 0;
	double ck45 = 0;
	double ck46 = 0;
	
	double aylar = 0;
	double Tahmin=0;
	int veritabani;

	private SensorManager sensorManager = null;
	private Sensor sensor = null;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		zamanmak = zmanmak * 1000;

		final int ZAMAN = zamanmak;

		// System.out.println("Süre= " + zamanmak);
		


		db = new GPSDataContentProvider.DatabaseHelper(getApplicationContext());
		

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
		sensorManager.registerListener(this, sensor,
				SensorManager.SENSOR_DELAY_NORMAL);

		sensor = sensorManager
				.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
		sensorManager.registerListener(this, sensor,
				SensorManager.SENSOR_DELAY_NORMAL);

		sensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
		sensorManager.registerListener(this, sensor,
				SensorManager.SENSOR_DELAY_NORMAL);

		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager
				.registerListener(this, sensorManager
						.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
						SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
				SensorManager.SENSOR_DELAY_NORMAL);

		zamanlayici = new Timer();
		yardimci = new Handler(Looper.getMainLooper());

		zamanlayici.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {

				SensorEventLogger();
			}
		}, 0, ZAMAN);

		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	// Sýfýr Matris

	public static double[][] zeros(int rows, int columns) {

		double csi[][] = new double[rows][columns];

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				csi[i][j] = 0;
			}
		}
		return csi;
	}

	// Birler
	public static double[][] ones(int rows, int columns) {

		double csi1[][] = new double[rows][columns];

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				csi1[i][j] = 0;
			}
		}
		return csi1;
	}

	// Matrisleri Gör
	public void printMatrix(double[][] m) {
		try {
			int rows = m.length;
			int columns = m[0].length;
			String str = "|\t";
			for (int k = 0; k < rows; k++) {
				for (int j = 0; j < columns; j++) {
					System.out.print(m[k][j] + " ");
					// str += m[k][j]+"\t";
				}
				// System.out.println(str+"|");
				// str = "|\t";
				System.out.println("\n");
			}
		} catch (Exception e) {
			System.out.println("Matris is boþ");
		}
	}

	// Matrisleri Carp

	public static double[][] multiply(double[][] a, double[][] b) {

		int rowsa = a.length;
		int columnsa = a[0].length;
		int rowsb = b.length;
		int columnsb = b[0].length;
		double c[][] = new double[rowsa][columnsb];

		try {

			if (columnsa == rowsb) {

				for (int ii = 0; ii < rowsa; ii++) {
					for (int jj = 0; jj < columnsb; jj++) {
						c[ii][jj] = 0;
						for (int kk = 0; kk < rowsb; kk++) {
							c[ii][jj] += a[ii][kk] * b[kk][jj];
						}
					}
				}

			} else {

			}
		} catch (Exception e) {
			System.out.println("Matris is boþ");
		}

		return c;
	}

	// Matris Transpose
	public static double[][] transpose(double[][] m) {
		double[][] temp = new double[m[0].length][m.length];
		for (int i = 0; i < m.length; i++)
			for (int j = 0; j < m[0].length; j++)
				temp[j][i] = m[i][j];
		return temp;
	}

	// Matris Tersini Al
	public static double[][] inverse(double a[][]) {
		int n = a.length;
		double x[][] = new double[n][n];
		double b[][] = new double[n][n];
		int index[] = new int[n];
		for (int i = 0; i < n; ++i)
			b[i][i] = 1;

		// Transform the matrix into an upper triangle
		gaussian(a, index);

		// Update the matrix b[i][j] with the ratios stored
		for (int i = 0; i < n - 1; ++i)
			for (int j = i + 1; j < n; ++j)
				for (int k = 0; k < n; ++k)
					b[index[j]][k] -= a[index[j]][i] * b[index[i]][k];

		// Perform backward substitutions
		for (int i = 0; i < n; ++i) {
			x[n - 1][i] = b[index[n - 1]][i] / a[index[n - 1]][n - 1];
			for (int j = n - 2; j >= 0; --j) {
				x[j][i] = b[index[j]][i];
				for (int k = j + 1; k < n; ++k) {
					x[j][i] -= a[index[j]][k] * x[k][i];
				}
				x[j][i] /= a[index[j]][j];
			}
		}
		return x;
	}

	public static void gaussian(double a[][], int index[]) {
		int n = index.length;
		double c[] = new double[n];

		// Initialize the index
		for (int i = 0; i < n; ++i)
			index[i] = i;

		// Find the rescaling factors, one from each row
		for (int i = 0; i < n; ++i) {
			double c1 = 0;
			for (int j = 0; j < n; ++j) {
				double c0 = Math.abs(a[i][j]);
				if (c0 > c1)
					c1 = c0;
			}
			c[i] = c1;
		}

		// Search the pivoting element from each column
		int k = 0;
		for (int j = 0; j < n - 1; ++j) {
			double pi1 = 0;
			for (int i = j; i < n; ++i) {
				double pi0 = Math.abs(a[index[i]][j]);
				pi0 /= c[index[i]];
				if (pi0 > pi1) {
					pi1 = pi0;
					k = i;
				}
			}

			// Interchange rows according to the pivoting order
			int itmp = index[j];
			index[j] = index[k];
			index[k] = itmp;
			for (int i = j + 1; i < n; ++i) {
				double pj = a[index[i]][j] / a[index[j]][j];

				// Record pivoting ratios below the diagonal
				a[index[i]][j] = pj;

				// Modify other elements accordingly
				for (int l = j + 1; l < n; ++l)
					a[index[i]][l] -= pj * a[index[j]][l];
			}
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (Sensor.TYPE_PRESSURE == event.sensor.getType()) {
			pressure_value = event.values[0];
			height = SensorManager.getAltitude(
					SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure_value);

		} else if (Sensor.TYPE_AMBIENT_TEMPERATURE == event.sensor.getType()) {
			recentTemperatureReading = event.values[0];
		} else if (Sensor.TYPE_RELATIVE_HUMIDITY == event.sensor.getType()) {
			recentHumidityReading = event.values[0];

		}

		else if (Sensor.TYPE_GRAVITY == event.sensor.getType()) {
			gx = event.values[0];
			gy = event.values[1];
			gz = event.values[2];
		}

	}

	public void SensorEventLogger() {

		ContentValues values = new ContentValues();
		

		/*System.out.println(zz1);
		System.out.println(zz2);

		System.out.println(zz3);

		System.out.println(zz4);

		System.out.println(zz5);

		System.out.println(zz6);

		System.out.println(zz7);

		System.out.println(zz8);

		System.out.println(zz9);

		System.out.println(zz10);

		System.out.println(zz11);

		System.out.println(zz12);

		System.out.println(zz13);

		System.out.println(zz14);

		System.out.println(zz15);

		System.out.println(zz16);

		System.out.println(zz17);

		System.out.println(zz18); */

		Date date = new Date();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/Istanbul"));
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		String model = android.os.Build.MODEL;
		String manufacturer = Build.MANUFACTURER;
		String addressString = "Adres Bulunamadý";

		final Calendar dds = Calendar.getInstance();
		double days = dds.get(Calendar.DAY_OF_MONTH);
		// System.out.println("Gun="+days);
		double months = dds.get(Calendar.MONTH);
		// System.out.println("Ay="+months);
		double saat = dds.get(Calendar.HOUR_OF_DAY);
		// System.out.println("Hour="+saat);
		double dakika = dds.get(Calendar.MINUTE);
		// System.out.println("Dakika="+dakika);
		double saniye = dds.get(Calendar.SECOND);
		// System.out.println("Saniye="+saniye);

		double ggs1 = months * 30;
		double ggs2 = days;
		double ggs3 = saniye / 3600;
		double ggs4 = dakika / 60;
		double ggs5 = saat + ggs4 + ggs3;
		double ggs6 = ggs5 / 24;

		/*
		 * System.out.println("GUN 1="+ ggs1); System.out.println("GUN 1="+
		 * ggs2); System.out.println("GUN 1="+ ggs3);
		 * System.out.println("GUN 1="+ ggs4); System.out.println("GUN 5="+
		 * ggs5); System.out.println("GUN 1="+ ggs6);
		 */

		aylar = ggs1 + ggs2 + ggs6;

		double Lat = Session.getCurrentLatitude();
		double Long = Session.getCurrentLongitude();
		double Altitude = Session.getCurrentAltitude();

		/*
		 * System.out.println("GX= " + gx); System.out.println("GY= " + gy);
		 * System.out.println("GZ= " + gz);
		 */

		// double LatPre=Session.getPreviousLatitude();
		// double LongPre=Session.getPreviousLongitude();
		double LatPre = enlemHafizada;
		double LongPre = boylamHafizada;

		List<Address> addresses = null;

		Geocoder gc = new Geocoder(this, Locale.getDefault());

		try {

			addresses = gc.getFromLocation(Lat, Long, 1);

			StringBuilder sb = new StringBuilder();

			if (addresses.size() > 0) {

				Address addresss = addresses.get(0);

				for (int i = 0; i < addresss.getMaxAddressLineIndex(); i++)
				sb.append(addresss.getAddressLine(i)).append(",");
				sb.append(addresss.getLocality()).append(",");
				sb.append(addresss.getPostalCode()).append(",");
				sb.append(addresss.getCountryName());
			}
			addressString = sb.toString();

		} catch (IOException e) {
		}

		zsay = db.getDbCount();

		if (zsay > 4) {

			double RR = 6371 * 1000;

			/*
			 * System.out.println("Süre= " + zamanmak);
			 * System.out.println("Süre= " + zmanmak);
			 * 
			 * System.out.println("Lat Pre=" + LatPre);
			 * System.out.println("Lat =" + Lat); System.out.println("Long Pre="
			 * + LongPre); System.out.println("Long =" + Long);
			 * System.out.println("Alt =" + Altitude);
			 */
			// System.out.println("1="+enlemHafizada);
			// System.out.println("2="+boylamHafizada);

			double dLat = Math.toRadians(LatPre - Lat);
			// System.out.println(dLat);
			double dLon = Math.toRadians(LongPre - Long);
			// System.out.println(dLon);
			double lat11 = Math.toRadians(LatPre);
			// System.out.println(lat11);
			double lat12 = Math.toRadians(Lat);
			// System.out.println(lat12);

			double a = (Math.sin(dLat / 2) * Math.sin(dLat / 2))
					+ (Math.sin(dLon / 2) * Math.sin(dLon / 2)
							* Math.cos(lat11) * Math.cos(lat12));
			// System.out.println(a);
			double c = 2 * Math.asin(Math.sqrt(a));
			// System.out.println(c);
			mesafe = RR * c;
			// System.out.println("mesafe=" + mesafe);

			int saykalman = db.getDbCount();

			System.out.println("JJsaykalman==" + saykalman);
			System.out.println("*********************JJekksatir ****************\n" + ekksatir);

			int matris = saykalman-3-ekksatir;

			System.out.println("JJmatris==" + matris);
			// int ekkint=db.getEKK(zsay);
			if (matris > 38) {
				double t[][] = new double[matris][1];
				double p[][] = new double[matris][1];
				double dp[][] = new double[matris][1];
				double NNN[][] = new double[matris][2];

				int j = 0;
				// matrisleri oluþtur.
				for (int i = ekksatir; i < saykalman-3; i++) {
					t[j][0] = db.getTime(i);
					p[j][0] = db.getPressure(i);
					dp[j][0] = 1E-55;
					NNN[j][0] = 1;
					NNN[j][1] = db.getTime(i);
					j = j + 1;
				}
				System.out.println("JJ==" + j);
				System.out.println("Gun");
				printMatrix(t);
				System.out.println("Basýnç");
				printMatrix(p);
				System.out.println("Prezisyon");
				printMatrix(dp);

				// EKK parametreler

				double A[][] = zeros(matris, 36);

				for (int i = 0; i < matris; i++) {

					double yys = Math.sin(yy * t[i][0]); // 4pi
					double yyc = Math.cos(yy * t[i][0]);

					double ass4s = Math.sin(ass4 * t[i][0]); // 3pi
					double ass4c = Math.cos(ass4 * t[i][0]);

					double tts = Math.sin(tt * t[i][0]); // 2pi
					double ttc = Math.cos(tt * t[i][0]);

					double asss = Math.sin(ass * t[i][0]); // 1,5pi
					double assc = Math.cos(ass * t[i][0]);

					double zzs = Math.sin(zz * t[i][0]); // pi
					double zzc = Math.cos(zz * t[i][0]);
					
					
					double zz1s = Math.sin(zz1 * t[i][0]); // 
					double zz1c = Math.cos(zz1 * t[i][0]);
					
					double zz2s = Math.sin(zz2 * t[i][0]); // 
					double zz2c = Math.cos(zz2 * t[i][0]);
					
					double zz3s = Math.sin(zz3 * t[i][0]); // 
					double zz3c = Math.cos(zz3 * t[i][0]);
					
					double zz4s = Math.sin(zz4 * t[i][0]); // 
					double zz4c = Math.cos(zz4 * t[i][0]);

					double zz5s = Math.sin(zz5 * t[i][0]); // 
					double zz5c = Math.cos(zz5 * t[i][0]);
					
					double zz6s = Math.sin(zz6 * t[i][0]); // 
					double zz6c = Math.cos(zz6 * t[i][0]);
					
					double zz7s = Math.sin(zz7 * t[i][0]); // 
					double zz7c = Math.cos(zz7 * t[i][0]);
					
					double zz8s = Math.sin(zz8 * t[i][0]); // 
					double zz8c = Math.cos(zz8 * t[i][0]);
					
					double zz9s = Math.sin(zz9 * t[i][0]); // 
					double zz9c = Math.cos(zz9 * t[i][0]);
					
					double zz10s = Math.sin(zz10 * t[i][0]); // 
					double zz10c = Math.cos(zz10 * t[i][0]);
					
					double zz11s = Math.sin(zz11 * t[i][0]); // 
					double zz11c = Math.cos(zz11 * t[i][0]);
					
					double zz12s = Math.sin(zz12 * t[i][0]); // 
					double zz12c = Math.cos(zz12 * t[i][0]);
					
					double zz13s = Math.sin(zz13 * t[i][0]); // 
					double zz13c = Math.cos(zz13 * t[i][0]);
					
					//double zz14s = Math.sin(zz14 * t[i][0]); // 
					//double zz14c = Math.cos(zz14 * t[i][0]);
					
					//double zz15s = Math.sin(zz15 * t[i][0]); // 
					//double zz15c = Math.cos(zz15 * t[i][0]);
					
					//double zz16s = Math.sin(zz16 * t[i][0]); // 
					//double zz16c = Math.cos(zz16 * t[i][0]);
					
					//double zz17s = Math.sin(zz17 * t[i][0]); // 
					//double zz17c = Math.cos(zz17 * t[i][0]);
					
					//double zz18s = Math.sin(zz18 * t[i][0]); // 
					//double zz18c = Math.cos(zz18 * t[i][0]);
					
					A[i][0] = yyc;
					A[i][1] = yys;

					A[i][2] = ass4c;
					A[i][3] = ass4s;

					A[i][4] = ttc;
					A[i][5] = tts;

					A[i][6] = assc;
					A[i][7] = asss;

					A[i][8] = zzc;
					A[i][9] = zzs;
					
					A[i][10] = zz1c;
					A[i][11] = zz1s;
					
					A[i][12] = zz2c;
					A[i][13] = zz2s;
					
					A[i][14] = zz3c;
					A[i][15] = zz3s;
					
					A[i][16] = zz4c;
					A[i][17] = zz4s;
					
					A[i][18] = zz5c;
					A[i][19] = zz5s;
					
					A[i][20] = zz6c;
					A[i][21] = zz6s;
					
					A[i][22] = zz7c;
					A[i][23] = zz7s;
					
					A[i][24] = zz8c;
					A[i][25] = zz8s;
					
					A[i][26] = zz9c;
					A[i][27] = zz9s;
					
					A[i][28] = zz10c;
					A[i][29] = zz10s;
					
					A[i][30] = zz11c;
					A[i][31] = zz11s;
					
					A[i][32] = zz12c;
					A[i][33] = zz12s;
					
					A[i][34] = zz13c;
					A[i][35] = zz13s;
					
					//A[i][36] = zz14c;
					//A[i][37] = zz14s;
					
					//A[i][38] = zz15c;
					//A[i][39] = zz15s;
					
					//A[i][40] = zz16c;
					//A[i][41] = zz16s;
					
					//A[i][42] = zz17c;
					//A[i][43] = zz17s;
					
					//A[i][44] = zz18c;
					//A[i][45] = zz18s; 
										
				}

				System.out.println("AAAAAAAAAAAAAA");
				printMatrix(A);

				double PP[][] = zeros(matris, matris);

				// System.out.println("PPPPPPPPPPPPPPPPPPP");
				// printMatrix(PP);

				for (int i = 0; i < matris; i++) {
					double xdp = dp[i][0];
					PP[i][i] = 1 / Math.pow(xdp, 2);
				}

				double NNN1[][] = transpose(NNN);

				double cNNN[][] = multiply(NNN1, NNN);

				double c1NNN[][] = inverse(cNNN);

				double c2NNN[][] = multiply(c1NNN, NNN1);

				double c3[][] = multiply(c2NNN, p);

				cc1 = c3[0][0];
				cc2 = c3[1][0];

				System.out.println("NNN Transpozu");
				printMatrix(NNN1);
				System.out.println("NNN Transpozu ile NNN çarpýmý");
				printMatrix(cNNN);
				System.out.println("Çarpýmýn inversi");
				printMatrix(c1NNN);
				System.out.println("Tekrar çarpým");
				printMatrix(c2NNN);
				System.out.println("coef=");
				System.out.println("coef1=" + cc1);
				System.out.println("coef2=" + cc2);
				printMatrix(c3);

				// Lineer bileþen

				double lin[][] = new double[matris][1];
				int ikk = 0;

				for (int ij = ekksatir; ij < saykalman-3; ij++) {
					lin[ikk][0] = c3[0][0] + c3[1][0] * t[ikk][0];
					ikk = ikk + 1;
				}

				System.out.println("Lineer Bileþen=");
				printMatrix(lin);

				// ll
				double ll[][] = new double[matris][1];
				int ikk2 = 0;
				for (int ij = ekksatir; ij < saykalman-3; ij++) {

					ll[ikk2][0] = p[ikk2][0] - lin[ikk2][0];
					ikk2 = ikk2 + 1;
				}

				System.out.println("llllllllllllll matrisi=");
				printMatrix(ll);

				// Mevsimsel - Dönemsel Bileþen

				double ATP[][] = transpose(A);
				double n1[][] = multiply(ATP, PP);
				double N1[][] = multiply(n1, A);
				double n2[][] = multiply(n1, ll);
				double N11[][] = inverse(N1);
				double kats[][] = multiply(N11, n2);

				ck1 = kats[0][0];
				ck2 = kats[1][0];
				ck3 = kats[2][0];
				ck4 = kats[3][0];
				ck5 = kats[4][0];
				ck6 = kats[5][0];
				ck7 = kats[6][0];
				ck8 = kats[7][0];
				ck9 = kats[8][0];
				ck10 = kats[9][0];
				
				ck11 = kats[10][0];
				ck12 = kats[11][0];
				ck13 = kats[12][0];
				ck14 = kats[13][0];
				ck15 = kats[14][0];
				ck16 = kats[15][0];
				ck17 = kats[16][0];
				ck18 = kats[17][0];
				
				ck19 = kats[18][0];
				ck20 = kats[19][0];
				ck21 = kats[20][0];
				ck22 = kats[21][0];
				ck23 = kats[22][0];
				ck24 = kats[23][0];
				ck25 = kats[24][0];
				ck26 = kats[25][0];
				ck27 = kats[26][0];
				ck28 = kats[27][0];
				ck29 = kats[28][0];
				ck30 = kats[29][0];
				ck31 = kats[30][0];
				ck32 = kats[31][0];
				ck33 = kats[32][0];
				ck34 = kats[33][0];
				
				ck35 = kats[34][0];
				ck36 = kats[35][0];
				//ck37 = kats[36][0];
				//ck38 = kats[37][0];
				//ck39 = kats[38][0];
				//ck40 = kats[39][0];
				//ck41 = kats[40][0];
				//ck42 = kats[41][0];
				//ck43 = kats[42][0];
				//ck44 = kats[43][0];
				//ck45 = kats[44][0];
				//ck46 = kats[45][0];
				
				
				
				/* System.out.println("ck1:::"+ck1);
				System.out.println("ck2:::"+ck2);
				System.out.println("ck3:::"+ck3);
				System.out.println("ck4:::"+ck4);
				System.out.println("ck5:::"+ck5);
				System.out.println("ck6:::"+ck6);
				System.out.println("ck7:::"+ck7);
				System.out.println("ck8:::"+ck8);
				System.out.println("ck9:::"+ck9);
				System.out.println("ck10:::"+ck10); */

				System.out.println("Katsayýlar=");
				printMatrix(kats);

				double fa = aylar;
				System.out.println("AYLARRRRRR"+aylar);
				
				double da1=Math.cos(yy*fa);
				double da2=Math.sin(yy*fa);
				double da3=Math.cos(ass4*fa);
				double da4=Math.sin(ass4 *fa);
				double da5=Math.cos(tt*fa);
				double da6=Math.sin(tt*fa);
				double da7=Math.cos(ass*fa);
				double da8=Math.sin(ass*fa);
				double da9=Math.cos(zz*fa);
				double da10=Math.sin(zz*fa);
				
				double da11=Math.cos(zz1*fa);
				double da12=Math.sin(zz1*fa);
				
				double da13=Math.cos(zz2*fa);
				double da14=Math.sin(zz2*fa);
				double da15=Math.cos(zz3*fa);
				double da16=Math.sin(zz3*fa);
				double da17=Math.cos(zz4*fa);
				double da18=Math.sin(zz4*fa);
				double da19=Math.cos(zz5*fa);
				double da20=Math.sin(zz5*fa);
				double da21=Math.cos(zz6*fa);
				double da22=Math.sin(zz6*fa);
				
				double da23=Math.cos(zz7*fa);
				double da24=Math.sin(zz7*fa);
				double da25=Math.cos(zz8*fa);
				double da26=Math.sin(zz8*fa);
				double da27=Math.cos(zz9*fa);
				double da28=Math.sin(zz9*fa);
				double da29=Math.cos(zz10*fa);
				double da30=Math.sin(zz10*fa);
				double da31=Math.cos(zz11*fa);
				double da32=Math.sin(zz11*fa);
				double da33=Math.cos(zz12*fa);
				double da34=Math.sin(zz12*fa);
				
				double da35=Math.cos(zz13*fa);
				double da36=Math.sin(zz13*fa);
				//double da37=Math.cos(zz14*fa);
				//double da38=Math.sin(zz14*fa);
				//double da39=Math.cos(zz15*fa);
				//double da40=Math.sin(zz15*fa);
				//double da41=Math.cos(zz16*fa);
				//double da42=Math.sin(zz16*fa);
				//double da43=Math.cos(zz17*fa);
				//double da44=Math.sin(zz17*fa);
				//double da45=Math.cos(zz18*fa);
				//double da46=Math.sin(zz18*fa);
								
				/*System.out.println("da1:::"+da1);
				System.out.println("da2:::"+da2);
				System.out.println("da3:::"+da3);
				System.out.println("da4:::"+da4);
				System.out.println("da5:::"+da5);
				System.out.println("da6:::"+da6);
				System.out.println("da7:::"+da7);
				System.out.println("da8:::"+da8);
				System.out.println("da9:::"+da9);
				System.out.println("da10:::"+da10); */

				Tahmin = (cc1) + (cc2*fa) 
						+ (ck1*da1)  + (ck2*da2) 
						+ (ck3*da3)  + (ck4*da4) 
						+ (ck5*da5)  + (ck6*da6)
						+ (ck7*da7)  + (ck8*da8)
						+ (ck9*da9)  + (ck10*da10)
						+ (ck11*da11)+ (ck12*da12)
						+ (ck13*da13)+ (ck14*da14)
						+ (ck15*da15)+ (ck16*da16)
				        + (ck17*da17)+ (ck18*da18)
                        + (ck19*da19)+ (ck20*da20)
                        + (ck21*da21)+ (ck22*da22)
                        + (ck23*da23)+ (ck24*da24)						
						+ (ck25*da25)+ (ck26*da26)
						+ (ck27*da27)+ (ck28*da28)
						+ (ck29*da29)+ (ck30*da30)
						+ (ck31*da31)+ (ck32*da32)
						+ (ck33*da33)+ (ck34*da34)
						+ (ck35*da35)+ (ck36*da36);
						//+ (ck37*da37)+ (ck38*da38);
						//+ (ck39*da39)+ (ck40*da40);
						//+ (ck41*da41)+ (ck42*da42);
						//+ (ck43*da43)+ (ck44*da44);
						//+ (ck45*da45)+ (ck46*da46); 
						
						
						
		    System.out.println("**********************************Tahmin Basýnc Anaaa****************** \n " + Tahmin);
			 

			// System.out.println("zaman say= "+ zsay);
			double fb = pressure_value;

			//System.out.println("Tahmin Basýnc= " + Tahmin);
			// System.out.println("fb= "+fb);

			double getgz = db.getGravity(zsay);

			double gzfark = getgz - gz;
			// System.out.println("getzfark= " + gzfark);

			if (gzfark > 0.1 || gzfark < -0.1) {

				if (mesafe > 250) {

					floorresult = 500;
				} // mesafe 150 den fazla mý?

				else if (Tahmin > 0) {

					double ffark = fb - Tahmin;

					System.out.println("fark= " + ffark);

					double deltah = ffark * 10;
					double kat1 = deltah / heightmak;
					kat = deltah / heightmak;

					if (kat > 0.9 || kat < -0.9) {

						if (kat > 0) { // aþaðý

							// double getfloor=ffark/0.20;

							// System.out.println("kat= " + kat1);

							long iPart = (long) kat1;

							// System.out.println("kat tam= " + iPart);

							floorresult = (int) (floorresult - iPart);

							// System.out.println("kat bilgisi= " +
							// floorresult);
							countpress = 0;
							ekkkontrol = 1;
							ekksatir = db.getDbCount() + 1;

							if (floorresult < -100 || floorresult > 200) {

								floorresult = 500;
								girkontrol = 5;
								ekkkontrol = 0;
							}
							girkontrol = 3;

						} else { // yukarý katlar

							// double getfloor=ffark/0.22;

							// System.out.println(" kat= " + kat);

							long iPart = (long) kat;

							// System.out.println("kat tam= " + iPart);

							floorresult = (int) (floorresult - iPart);

							// System.out.println("kat bilgisi= " +
							// floorresult);
							countpress = 0;
							ekkkontrol = 1;
							ekksatir = db.getDbCount() + 1;

							if (floorresult < -100 || floorresult > 200) {

								floorresult = 500;
								girkontrol = 6;
								ekkkontrol = 0;
							}

							girkontrol = 4;
						}

					} else {

						countpress = countpress + 1;
						ekkkontrol = 0;
						// System.out.println("Sabitlik= " +countpress);

					} // sabit mi
				  }
				} // mesafe onayý
			} // hareket onayý
		} // veritabaný onayý

		veritabani=db.getDbCount()+1;
		values.put(GPSData.GPSPoint.PRESSURE, pressure_value);
		values.put(GPSData.GPSPoint.SRG_ID, veritabani);
		values.put(GPSData.GPSPoint.TPRESSURE, Tahmin);
		values.put(GPSData.GPSPoint.BHEIGHT, height);
		values.put(GPSData.GPSPoint.TEMPERATURE, recentTemperatureReading);
		values.put(GPSData.GPSPoint.HUMIDITY, recentHumidityReading);
		enlemHafizada = Lat;
		boylamHafizada = Long;
		values.put(GPSData.GPSPoint.LONGITUDE, Long);
		values.put(GPSData.GPSPoint.LATITUDE, Lat);
		values.put(GPSData.GPSPoint.ALTITUDE, Altitude);
		values.put(GPSData.GPSPoint.PROVIDER, GpsMainActivity.providerName);
		values.put(GPSData.GPSPoint.FLOOR, floorresult);
		
		values.put(GPSData.GPSPoint.KATDEGIS, kat);
		// values.put(GPSData.GPSPoint.ADDRESS, addressString);
		values.put(GPSData.GPSPoint.SAT, GpsMainActivity.semsat);
		values.put(GPSData.GPSPoint.USER, telephonyManager.getDeviceId());
		values.put(GPSData.GPSPoint.PHONE, manufacturer + "," + model);
		values.put(GPSData.GPSPoint.GIRKNTRL, girkontrol);
		values.put(GPSData.GPSPoint.DEGKNTRL, ekkkontrol);
		values.put(GPSData.GPSPoint.INTERVAL, zamanmak);
		values.put(GPSData.GPSPoint.MSF, mesafe);

		values.put(GPSData.GPSPoint.GX, gx);
		values.put(GPSData.GPSPoint.GY, gy);
		values.put(GPSData.GPSPoint.GZ, gz);

		// Katsayýlar

		values.put(GPSData.GPSPoint.C1, cc1);
		values.put(GPSData.GPSPoint.C2, cc2);

		values.put(GPSData.GPSPoint.K1, ck1);
		values.put(GPSData.GPSPoint.K2, ck2);
		values.put(GPSData.GPSPoint.K3, ck3);
		values.put(GPSData.GPSPoint.K4, ck4);
		values.put(GPSData.GPSPoint.K5, ck5);
		values.put(GPSData.GPSPoint.K6, ck6);
		values.put(GPSData.GPSPoint.K7, ck7);
		values.put(GPSData.GPSPoint.K8, ck8);
		values.put(GPSData.GPSPoint.K9, ck9);
		values.put(GPSData.GPSPoint.K10, ck10);
		
		values.put(GPSData.GPSPoint.K11, ck11);
		values.put(GPSData.GPSPoint.K12, ck12);
		values.put(GPSData.GPSPoint.K13, ck13);
		values.put(GPSData.GPSPoint.K14, ck14);
		values.put(GPSData.GPSPoint.K15, ck15);
		values.put(GPSData.GPSPoint.K16, ck16);
		values.put(GPSData.GPSPoint.K17, ck17);
		values.put(GPSData.GPSPoint.K18, ck18);
		values.put(GPSData.GPSPoint.K19, ck19);
		values.put(GPSData.GPSPoint.K20, ck20);
		
		values.put(GPSData.GPSPoint.K21, ck21);
		values.put(GPSData.GPSPoint.K22, ck22);
		
		values.put(GPSData.GPSPoint.K23, ck23);
		values.put(GPSData.GPSPoint.K24, ck24);
		values.put(GPSData.GPSPoint.K25, ck25);
		values.put(GPSData.GPSPoint.K26, ck26);
		values.put(GPSData.GPSPoint.K27, ck27);
		values.put(GPSData.GPSPoint.K28, ck28);
		values.put(GPSData.GPSPoint.K29, ck29);
		values.put(GPSData.GPSPoint.K30, ck30);
		
		values.put(GPSData.GPSPoint.K31, ck31);
		values.put(GPSData.GPSPoint.K32, ck32);
		values.put(GPSData.GPSPoint.K33, ck33);
		values.put(GPSData.GPSPoint.K34, ck34);
		
		values.put(GPSData.GPSPoint.K35, ck35);
		values.put(GPSData.GPSPoint.K36, ck36);
		values.put(GPSData.GPSPoint.K37, ck37);
		values.put(GPSData.GPSPoint.K38, ck38);
		values.put(GPSData.GPSPoint.K39, ck39);
		values.put(GPSData.GPSPoint.K40, ck40);
		
		values.put(GPSData.GPSPoint.K41, ck41);
		values.put(GPSData.GPSPoint.K42, ck42);
		values.put(GPSData.GPSPoint.K43, ck43);
		values.put(GPSData.GPSPoint.K44, ck44);
		values.put(GPSData.GPSPoint.K45, ck45);
		values.put(GPSData.GPSPoint.K46, ck46);
		
		
		values.put(GPSData.GPSPoint.KatYuk, heightmak);

		values.put(GPSData.GPSPoint.SAY, zsay + 1);

		values.put(GPSData.GPSPoint.TIME, String.valueOf(sdf.format(date)));

		values.put(GPSData.GPSPoint.GUN, aylar);

		getContentResolver().insert(GPSDataContentProvider.CONTENT_URI, values);
		ekkkontrol = 0;
		
		cc1=0;
		cc2=0;
		ck1=0;
		ck2=0;
		ck3=0;
		ck4=0;
		ck5=0;
		ck6=0;
		ck7=0;
		ck8=0;
		ck9=0;
		ck10=0;
		ck11=0;
		ck12=0;
		ck13=0;
		ck14=0;
		ck15=0;
		ck16=0;
		ck17=0;
		ck18=0;
		ck19=0;
		ck20=0;
		ck21=0;
		ck22=0;
		ck23=0;
		ck24=0;
		ck25=0;
		ck26=0;
		ck27=0;
		ck28=0;
		ck29=0;
		ck30=0;
		ck31=0;
		ck32=0;
		ck33=0;
		ck34=0;
		ck35=0;
		ck36=0;
		ck37=0;
		ck38=0;
		ck39=0;
		ck40=0;
		ck41=0;
		ck42=0;
		ck43=0;
		ck44=0;
		ck45=0;
		ck46=0;
		Tahmin=0;
	
	}

}