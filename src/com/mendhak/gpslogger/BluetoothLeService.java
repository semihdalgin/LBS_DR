/**************************************************************************************************
  Filename:       BluetoothLeService.java
  Revised:        $Date: 2013-09-09 16:23:36 +0200 (ma, 09 sep 2013) $
  Revision:       $Revision: 27674 $

  Copyright 2013 Texas Instruments Incorporated. All rights reserved.
 
  IMPORTANT: Your use of this Software is limited to those specific rights
  granted under the terms of a software license agreement between the user
  who downloaded the software, his/her employer (which must be your employer)
  and Texas Instruments Incorporated (the "License").  You may not use this
  Software unless you agree to abide by the terms of the License. 
  The License limits your use, and you acknowledge, that the Software may not be 
  modified, copied or distributed unless used solely and exclusively in conjunction 
  with a Texas Instruments Bluetooth device. Other than for the foregoing purpose, 
  you may not use, reproduce, copy, prepare derivative works of, modify, distribute, 
  perform, display or sell this Software and/or its documentation for any purpose.
 
  YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
  PROVIDED �AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
  INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
  NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
  TEXAS INSTRUMENTS OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT,
  NEGLIGENCE, STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER
  LEGAL EQUITABLE THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES
  INCLUDING BUT NOT LIMITED TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE
  OR CONSEQUENTIAL DAMAGES, LOST PROFITS OR LOST DATA, COST OF PROCUREMENT
  OF SUBSTITUTE GOODS, TECHNOLOGY, SERVICES, OR ANY CLAIMS BY THIRD PARTIES
  (INCLUDING BUT NOT LIMITED TO ANY DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 
  Should you have any questions regarding your right to use this Software,
  contact Texas Instruments Incorporated at www.TI.com

 **************************************************************************************************/
package com.mendhak.gpslogger;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private static final String TAG = BluetoothLeService.class.getSimpleName();
    private static BluetoothLeService mThis = null;

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    
    public final static String ACTION_GATT_CONNECTED = "ti.android.ble.common.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ti.android.ble.common.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ti.android.ble.common.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_READ = "ti.android.ble.common.ACTION_DATA_READ";
    public final static String ACTION_DATA_NOTIFY = "ti.android.ble.common.ACTION_DATA_NOTIFY";
    public final static String ACTION_DATA_WRITE = "ti.android.ble.common.ACTION_DATA_WRITE";
    public final static String EXTRA_DATA = "ti.android.ble.common.EXTRA_DATA";
    public final static String EXTRA_UUID = "ti.android.ble.common.EXTRA_UUID";
    public final static String EXTRA_STATUS = "ti.android.ble.common.EXTRA_STATUS";
    public final static String EXTRA_ADDRESS = "ti.android.ble.common.EXTRA_ADDRESS";

    // BLE
    private BluetoothManager bluetoothManager = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothGatt bluetoothGatt = null;
    private String bluetoothDeviceAddress;

    private CountDownLatch waitForResponseLatch = null;
    
    /**
     * GATT client callbacks
     */
    private BluetoothGattCallback mGattCallbacks = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (bluetoothGatt == null) {
                Log.e(TAG, "mBluetoothGatt not created!");
                return;
            }

            BluetoothDevice device = gatt.getDevice();
            String address = device.getAddress();
            Log.d(TAG, "onConnectionStateChange (" + address + ") " + newState + " status: " + status);

            switch (newState) {
            case BluetoothProfile.STATE_CONNECTED:
                broadcastUpdate(ACTION_GATT_CONNECTED, address, status);
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                broadcastUpdate(ACTION_GATT_DISCONNECTED, address, status);
                break;
            default:
                Log.e(TAG, "New state not processed: " + newState);
                break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothDevice device = gatt.getDevice();
            broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, device.getAddress(), status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_NOTIFY, characteristic, BluetoothGatt.GATT_SUCCESS);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            waitForResponseLatch.countDown();
            broadcastUpdate(ACTION_DATA_READ, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            waitForResponseLatch.countDown();
            broadcastUpdate(ACTION_DATA_WRITE, characteristic, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorRead");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            waitForResponseLatch.countDown();
            Log.d(TAG, "onDescriptorWrite");
        }
    };

    private void broadcastUpdate(final String action, final String address, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ADDRESS, address);
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic, final int status) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID, characteristic.getUuid().toString());
        intent.putExtra(EXTRA_DATA, characteristic.getValue());
        intent.putExtra(EXTRA_STATUS, status);
        sendBroadcast(intent);
    }

    /**
     * Manage the BLE service
     */
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that
        // BluetoothGatt.close() is called
        // such that resources are cleaned up properly. In this particular
        // example, close() is invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder binder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     * 
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        boolean sucessfullyInitialized = true;
        
        Log.d(TAG, "initialize");
        // For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
        mThis = this;
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                sucessfullyInitialized = false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            sucessfullyInitialized = false;
        }
        return sucessfullyInitialized;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     * 
     * @param characteristic
     *            The characteristic to read from.
     */
    public void initiateReadCharacteristic(BluetoothGattCharacteristic characteristic) {      
        waitForResponseLatch = new CountDownLatch(1);
        if (bluetoothGatt.readCharacteristic(characteristic)) {
            waitForLatch();            
        } else {
            Log.e(TAG, "readCharacteristic initiation failed");
        }
    }

    public boolean initiateWriteCharacteristic(BluetoothGattCharacteristic characteristic, byte b) {
        byte[] value = new byte[1];
        value[0] = b;
        characteristic.setValue(value);

        waitForResponseLatch = new CountDownLatch(1);
        if (bluetoothGatt.writeCharacteristic(characteristic)) {
            waitForLatch();            
        } else {
            Log.e(TAG, "writeCharacteristic initiation failed");
        }
        return true;
    }
    
    /**
     * Enables or disables notification on a given characteristic.
     * 
     * @param characteristic
     *            Characteristic to act on.
     * @param enabled
     *            If true, enable notification. False otherwise.
     */
    public boolean initiateNotificationCharacteristic(BluetoothGattCharacteristic characteristic, boolean enable) {
        
        // Enabling notifications is a two-part job, enabling it locally, and enabling it remotely.
        
        if (!bluetoothGatt.setCharacteristicNotification(characteristic, enable)) { //Enable locally...
            Log.w(TAG, "setCharacteristicNotification failed");
            return false;
        } else {            
            BluetoothGattDescriptor clientConfig = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
            if (clientConfig == null) {
                return false;
            } else {
                // Enable remotely ...
                if (enable) {
                    clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                } else {
                    clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                }

                waitForResponseLatch = new CountDownLatch(1);
                if (bluetoothGatt.writeDescriptor(clientConfig)) {
                    waitForLatch();
                } else {
                    Log.e(TAG, "writeDescriptor initiation failed");
                }
            }
        }
        return true;
    }

    private void waitForLatch() {
        try {
            waitForResponseLatch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Timed out waiting for the operatotion to finish");
        }
    }
    
    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     * 
     * @param address
     *            The device address of the destination device.
     * 
     * @return Return true if the connection is initiated successfully. The
     *         connection result is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        int connectionState = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

        if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
            // Previously connected device. Try to reconnect.
            if (bluetoothDeviceAddress != null && address.equals(bluetoothDeviceAddress) && bluetoothGatt != null) {
                Log.d(TAG, "Re-use GATT connection");
                if (bluetoothGatt.connect()) {
                    return true;
                } else {
                    Log.w(TAG, "GATT re-connect failed.");
                    return false;
                }
            }

            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                return false;
            }
            // We want to directly connect to the device, so we are setting the
            // autoConnect parameter to false.
            Log.d(TAG, "Create a new GATT connection.");
            bluetoothGatt = device.connectGatt(this, false, mGattCallbacks);
            bluetoothDeviceAddress = address;
        } else {
            Log.w(TAG, "Attempt to connect in state: " + connectionState);
            return false;
        }
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect(String address) {
        if (bluetoothAdapter == null) {
            Log.w(TAG, "disconnect: BluetoothAdapter not initialized");
        } else {
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            int connectionState = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
            
            if (bluetoothGatt != null) {
                Log.i(TAG, "disconnect");
                if (connectionState != BluetoothProfile.STATE_DISCONNECTED) {
                    bluetoothGatt.disconnect();
                } else {
                    Log.w(TAG, "Attempt to disconnect in state: " + connectionState);
                }
            }
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    public void close() {
        if (bluetoothGatt != null) {
            Log.i(TAG, "close");
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    public static BluetoothGatt getBluetoothGatt() {
        return mThis.bluetoothGatt;
    }

    public static BluetoothManager getBtManager() {
        return mThis.bluetoothManager;
    }

    public static BluetoothLeService getInstance() {
        return mThis;
    }
}
