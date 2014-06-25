package com.googlecode.android_scripting.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;


public class BluetoothHidFacade extends RpcReceiver {
  static final ParcelUuid[] HSP_UUIDS = {
    BluetoothUuid.HSP,
    BluetoothUuid.Handsfree,
  };

  private final Service mService;
  private final BluetoothAdapter mBluetoothAdapter;

  private boolean mIsHidReady = false;
  private BluetoothInputDevice mHidProfile = null;

  public BluetoothHidFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    mBluetoothAdapter.getProfileProxy(mService, new HidServiceListener(), BluetoothProfile.INPUT_DEVICE);
  }

  class HidServiceListener implements BluetoothProfile.ServiceListener {
    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
      mHidProfile = (BluetoothInputDevice) proxy;
      mIsHidReady = true;
    }
    @Override
    public void onServiceDisconnected(int profile) {
      mIsHidReady = false;
    }
  }

  public Boolean HIDConnect(BluetoothDevice device) {
    return mHidProfile.connect(device);
  }

  public Boolean HIDDisconnect(BluetoothDevice device) {
    if (mHidProfile.getPriority(device) > BluetoothProfile.PRIORITY_ON) {
      mHidProfile.setPriority(device, BluetoothProfile.PRIORITY_ON);
    }
    return mHidProfile.disconnect(device);
  }

  @Rpc(description="Is Hid profile ready.")
  public Boolean bluetoothHidIsReady() {
    return mIsHidReady;
  }

  @Rpc(description="Connect to HSP device.")
  public Boolean bluetoothHidConnect(
      @RpcParameter(
          name = "device",
          description = "Name or MAC address of a bluetooth device.")
          String device) throws Exception {
    if (mHidProfile == null) return false;
    BluetoothDevice mDevice = BluetoothFacade.getDevice(BluetoothFacade.DiscoveredDevices, device);
    Log.d("Connecting to device " + mDevice.getAliasName());
    return HIDConnect(mDevice);
  }

  @Rpc(description="Disconnect an HSP device.")
  public Boolean bluetoothHidDisconnect(
      @RpcParameter(
          name = "device",
          description = "Name or MAC address of a bluetooth device.")
          String device) throws Exception {
    if (mHidProfile == null) return false;
    BluetoothDevice mDevice = BluetoothFacade.getDevice(mHidProfile.getConnectedDevices(), device);
    return HIDDisconnect(mDevice);
  }

  @Override
  public void shutdown() {
  }
}
