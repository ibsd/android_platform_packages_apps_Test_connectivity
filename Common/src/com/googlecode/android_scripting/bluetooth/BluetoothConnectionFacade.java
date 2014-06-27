
package com.googlecode.android_scripting.bluetooth;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothUuid;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelUuid;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

public class BluetoothConnectionFacade extends RpcReceiver {
    static final ParcelUuid[] SINK_UUIDS = {
            BluetoothUuid.AudioSink, BluetoothUuid.AdvAudioDist,
    };
    static final ParcelUuid[] HSP_UUIDS = {
            BluetoothUuid.HSP, BluetoothUuid.Handsfree,
    };

    private final Service mService;
    private final BluetoothAdapter mBluetoothAdapter;
    private final BluetoothPairingHelper mPairingHelper;
    private final Map<String, BroadcastReceiver> listeningDevices;

    private final EventFacade mEventFacade;
    private BluetoothHspFacade mHspProfile;
    private BluetoothA2dpFacade mA2dpProfile;

    private final IntentFilter mDiscoverConnectFilter;
    private final IntentFilter mPairingFilter;
    private final IntentFilter mBondStateChangeFilter;
    private final IntentFilter mA2dpStateChangeFilter;
    private final IntentFilter mHspStateChangeFilter;

    public BluetoothConnectionFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mPairingHelper = new BluetoothPairingHelper();
        // Use a synchronized map to avoid running problems
        listeningDevices = Collections.synchronizedMap(new HashMap<String, BroadcastReceiver>());

        mEventFacade = manager.getReceiver(EventFacade.class);
        mA2dpProfile = manager.getReceiver(BluetoothA2dpFacade.class);
        mHspProfile = manager.getReceiver(BluetoothHspFacade.class);

        mDiscoverConnectFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mDiscoverConnectFilter.addAction(BluetoothDevice.ACTION_UUID);
        mDiscoverConnectFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        mPairingFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);

        mBondStateChangeFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        mA2dpStateChangeFilter = new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        mHspStateChangeFilter = new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
    }

    /**
     * Connect to a specific device upon its discovery
     */
    public class DiscoverConnectReceiver extends BroadcastReceiver {
        private final String mDeviceID;
        private final Boolean mBond;
        private BluetoothDevice mDevice;

        /**
         * Constructor
         * 
         * @param deviceID Either the device alias name or mac address.
         * @param bond If true, bond the device only.
         */
        public DiscoverConnectReceiver(String deviceID, Boolean bond) {
            super();
            mDeviceID = deviceID;
            mBond = bond;
        }

        public DiscoverConnectReceiver(String deviceID) {
            this(deviceID, false);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // The specified device is found.
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (BluetoothFacade.deviceMatch(device, mDeviceID)) {
                    Log.d("Found device " + device.getAliasName() + " for connection.");
                    mBluetoothAdapter.cancelDiscovery();
                    mDevice = device;
                }
                // After discovery stops.
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                if (mDevice == null) {
                    Log.d("Device " + mDeviceID + " was not discovered.");
                    return;
                }
                // Attempt to initiate bonding if this is a bond request.
                if (mBond) {
                    Log.d("Bond with " + mDevice.getAliasName());
                    StateChangeReceiver receiver = new StateChangeReceiver(mDeviceID);
                    listeningDevices.put("Bonding" + mDeviceID, receiver);
                    mService.registerReceiver(receiver, mBondStateChangeFilter);
                    if (mDevice.createBond()) {
                        Log.d("Bonding started.");
                    } else {
                        Log.e("Failed to bond with " + mDevice.getAliasName());
                        mService.unregisterReceiver(listeningDevices.remove("Bonding" + mDeviceID));
                    }
                    mService.unregisterReceiver(listeningDevices.remove("Bond" + mDeviceID));
                    // Otherwise fetch the device's UUID.
                } else {
                    Log.d("Discovery finished, start fetching UUIDs.");
                    boolean status = mDevice.fetchUuidsWithSdp();
                    Log.d("Initiated ACL connection: " + status);
                }
                // Initiate connection based on the UUIDs retrieved.
            } else if (action.equals(BluetoothDevice.ACTION_UUID)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (BluetoothFacade.deviceMatch(device, mDeviceID)) {
                    Log.d("Initiating connections.");
                    connectProfile(device, mDeviceID);
                    mService.unregisterReceiver(listeningDevices.remove(mDeviceID));
                }
            }
        }
    }

    public class StateChangeReceiver extends BroadcastReceiver {
        private final String mDeviceID;

        public StateChangeReceiver(String deviceID) {
            mDeviceID = deviceID;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                if (state == BluetoothDevice.BOND_BONDED) {
                    mEventFacade.postEvent("Bonded" + mDeviceID, new Bundle());
                    mService.unregisterReceiver(listeningDevices.remove("Bonding" + mDeviceID));
                }
            } else if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1);
                if (state == BluetoothA2dp.STATE_CONNECTED) {
                    mEventFacade.postEvent("A2dpConnected" + mDeviceID, new Bundle());
                    mService.unregisterReceiver(listeningDevices.remove("A2dpConnecting"
                            + mDeviceID));
                }
            } else if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
                if (state == BluetoothHeadset.STATE_CONNECTED) {
                    mEventFacade.postEvent("HspConnected" + mDeviceID, new Bundle());
                    mService.unregisterReceiver(listeningDevices
                            .remove("HspConnecting" + mDeviceID));
                }
            }
        }
    }

    private void connectProfile(BluetoothDevice device, String deviceID) {
        ParcelUuid[] deviceUuids = device.getUuids();
        Log.d("Device uuid is " + deviceUuids);
        mService.registerReceiver(mPairingHelper, mPairingFilter);
        if (BluetoothUuid.containsAnyUuid(SINK_UUIDS, deviceUuids)) {
            Log.d("Connecting to " + device.getAliasName());
            boolean status = mA2dpProfile.a2dpConnect(device);
            if (status) {
                Log.d("Connecting A2dp...");
                StateChangeReceiver receiver = new StateChangeReceiver(deviceID);
                mService.registerReceiver(receiver, mA2dpStateChangeFilter);
                listeningDevices.put("A2dpConnecting" + deviceID, receiver);
            } else {
                Log.d("Failed starting A2dp connection.");
            }
        }
        if (BluetoothUuid.containsAnyUuid(HSP_UUIDS, deviceUuids)) {
            boolean status = mHspProfile.hspConnect(device);
            if (status) {
                Log.d("Posting event.");
                StateChangeReceiver receiver = new StateChangeReceiver(deviceID);
                mService.registerReceiver(receiver, mHspStateChangeFilter);
                listeningDevices.put("HspConnecting" + deviceID, receiver);
            } else {
                Log.d("Failed starting Hsp connection.");
            }
        }
        mService.unregisterReceiver(mPairingHelper);
    }

    private Set<BluetoothDevice> getConnectedDevices() {
        Set<BluetoothDevice> a2dp = new HashSet<BluetoothDevice>(
                mA2dpProfile.bluetoothA2dpGetConnectedDevices());
        Set<BluetoothDevice> hsp = new HashSet<BluetoothDevice>(
                mHspProfile.bluetoothHspGetConnectedDevices());
        a2dp.addAll(hsp);
        return a2dp;
    }

    @Rpc(description = "Connect to a specified device once it's discovered.",
         returns = "Whether discovery started successfully.")
    public Boolean bluetoothDiscoverAndConnect(
            @RpcParameter(name = "deviceID",
                          description = "Name or MAC address of a bluetooth device.")
            String deviceID) {
        mBluetoothAdapter.cancelDiscovery();
        if (listeningDevices.containsKey(deviceID)) {
            Log.d("This device is already in the process of discovery and connecting.");
            return false;
        }
        if (BluetoothFacade.deviceExists(getConnectedDevices(), deviceID)) {
            Log.d("Device " + deviceID + " is already connected through A2DP.");
            return false;
        }
        DiscoverConnectReceiver receiver = new DiscoverConnectReceiver(deviceID);
        listeningDevices.put(deviceID, receiver);
        mService.registerReceiver(receiver, mDiscoverConnectFilter);
        return mBluetoothAdapter.startDiscovery();
    }

    @Rpc(description = "Bond to a specified device once it's discovered.",
         returns = "Whether discovery started successfully. ")
    public Boolean bluetoothDiscoverAndBond(
            @RpcParameter(name = "device",
                          description = "Name or MAC address of a bluetooth device.")
            String deviceID) {
        mBluetoothAdapter.cancelDiscovery();
        if (listeningDevices.containsKey(deviceID)) {
            Log.d("This device is already in the process of discovery and bonding.");
            return false;
        }
        if (BluetoothFacade.deviceExists(mBluetoothAdapter.getBondedDevices(), deviceID)) {
            Log.d("Device " + deviceID + " is already bonded.");
            return false;
        }
        DiscoverConnectReceiver receiver = new DiscoverConnectReceiver(deviceID, true);
        listeningDevices.put("Bond" + deviceID, receiver);
        mService.registerReceiver(receiver, mDiscoverConnectFilter);
        return mBluetoothAdapter.startDiscovery();
    }

    @Rpc(description = "Remove bond to a device.",
         returns = "Whether the device was successfully unbonded.")
    public Boolean bluetoothUnbond(
            @RpcParameter(name = "device",
                          description = "Name or MAC address of a bluetooth device.")
            String device) throws Exception {
        BluetoothDevice mDevice = BluetoothFacade.getDevice(mBluetoothAdapter.getBondedDevices(),
                device);
        return mDevice.removeBond();
    }

    @Rpc(description = "Connect to a device that is already bonded.")
    public void bluetoothConnectBonded(
            @RpcParameter(name = "device",
                          description = "Name or MAC address of a bluetooth device.")
            String device) throws Exception {
        BluetoothDevice mDevice = BluetoothFacade.getDevice(mBluetoothAdapter.getBondedDevices(),
                device);
        connectProfile(mDevice, device);
    }

    @Override
    public void shutdown() {
    }
}
