package com.googlecode.android_scripting.bluetooth;

import java.util.concurrent.ConcurrentHashMap;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
      BluetoothUuid.AudioSink,
      BluetoothUuid.AdvAudioDist,
  };
  static final ParcelUuid[] HSP_UUIDS = {
    BluetoothUuid.HSP,
    BluetoothUuid.Handsfree,
  };

  private final Service mService;
  private final BluetoothAdapter mBluetoothAdapter;
  private final BluetoothPairingHelper mPairingHelper;
  private final ConcurrentHashMap<String, BroadcastReceiver> listeningDevices;

  private final EventFacade mEventFacade;
  private BluetoothHspFacade mHspProfile;
  private BluetoothA2dpFacade mA2dpProfile;

  private final IntentFilter mDiscoverConnectFilter;
  private final IntentFilter mPairingFilter;

  public BluetoothConnectionFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    mPairingHelper = new BluetoothPairingHelper();
    listeningDevices = new ConcurrentHashMap<String, BroadcastReceiver>();

    mEventFacade = manager.getReceiver(EventFacade.class);
    mA2dpProfile = manager.getReceiver(BluetoothA2dpFacade.class);
    mHspProfile = manager.getReceiver(BluetoothHspFacade.class);

    mDiscoverConnectFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    mDiscoverConnectFilter.addAction(BluetoothDevice.ACTION_UUID);
    mDiscoverConnectFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

    mPairingFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
  }

  /**
   * Connect to a specific device upon its discovery
   * @author angli
   *
   */
  public class DiscoverConnectReceiver extends BroadcastReceiver {
    private final String mDeviceID;
    private final Boolean mBond;
    private BluetoothDevice mDevice;
    /**
     * Constructor
     * @param deviceID Either the device alias name or mac address.
     * @param bond If true, create bond to the device only.
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
      if (action.equals(BluetoothDevice.ACTION_FOUND)) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if(mDeviceID.equals(device.getAliasName()) || mDeviceID.equals(device.getAddress())) {
          Log.d("Found device " + device.getAliasName() + " for connection.");
          mBluetoothAdapter.cancelDiscovery();
          mDevice = device;
        }
      }else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
        if(mBond) {
          Log.d("Bond with " + mDevice.getAliasName());
          if (!mDevice.createBond()) {
            Log.e("Failed to bond with " + mDevice.getAliasName());
          }
          mService.unregisterReceiver(this);
        }else{
          Log.d("Discovery finished, start fetching UUIDs.");
          mDevice.fetchUuidsWithSdp();
        }
      }else if (action.equals(BluetoothDevice.ACTION_UUID)) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if(mDeviceID.equals(device.getAliasName()) || mDeviceID.equals(device.getAddress())) {
          connectProfile(device, mDeviceID);
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
      Log.d("Status " + status);
      if(status) {
        Log.d("Posting event.");
        Bundle mResults = new Bundle();
        mEventFacade.postEvent("A2dpConnected" + deviceID, mResults);
      }
    }
    if (BluetoothUuid.containsAnyUuid(HSP_UUIDS, deviceUuids)) {
      boolean status = mHspProfile.hspConnect(device);
      if(status) {
        Log.d("Posting event.");
        Bundle mResults = new Bundle();
        mEventFacade.postEvent("HspConnected" + deviceID, mResults);
      }
    }
    mService.unregisterReceiver(listeningDevices.remove(deviceID));
    mService.unregisterReceiver(mPairingHelper);
  }

  @Rpc(description = "Connect to a specified device once it's discovered.",
       returns = "Whether discovery started successfully.")
  public Boolean bluetoothDiscoverAndConnect(
      @RpcParameter(
          name = "device",
          description = "Name or MAC address of a bluetooth device.")
          String device) {
    if (listeningDevices.containsKey(device)) {
      Log.d("This device is already in the process of discovery and connecting.");
      return false;
    }
    DiscoverConnectReceiver receiver = new DiscoverConnectReceiver(device);
    listeningDevices.put(device, receiver);
    mService.registerReceiver(receiver, mDiscoverConnectFilter);
    return mBluetoothAdapter.startDiscovery();
  }

  @Rpc(description = "Bond to a specified device once it's discovered.",
      returns = "Whether discovery started successfully. ")
  public Boolean bluetoothDiscoverAndBond(
      @RpcParameter(
          name = "device",
          description = "Name or MAC address of a bluetooth device.")
          String device) {
    if (listeningDevices.containsKey(device)) {
      Log.d("This device is already in the process of discovery and bonding.");
      return false;
    }
    DiscoverConnectReceiver receiver = new DiscoverConnectReceiver(device, true);
    listeningDevices.put(device, receiver);
    mService.registerReceiver(receiver, mDiscoverConnectFilter);
    return mBluetoothAdapter.startDiscovery();
  }

  @Rpc(description = "Remove bond to a device.",
      returns = "Whether the device was successfully unbonded.")
  public Boolean bluetoothUnbond(
      @RpcParameter(
          name = "device",
          description = "Name or MAC address of a bluetooth device.")
          String device) throws Exception {
    BluetoothDevice mDevice =
        BluetoothFacade.getDevice(mBluetoothAdapter.getBondedDevices(), device);
    return mDevice.removeBond();
  }

  @Rpc(description = "Connect to a device that is already bonded.")
  public void bluetoothConnectBonded(
      @RpcParameter(
          name = "device",
          description = "Name or MAC address of a bluetooth device.")
          String device) throws Exception {
    BluetoothDevice mDevice =
        BluetoothFacade.getDevice(mBluetoothAdapter.getBondedDevices(), device);
    connectProfile(mDevice, device);
  }

  @Override
  public void shutdown() {
  }
}
