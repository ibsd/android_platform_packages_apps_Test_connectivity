package com.googlecode.android_scripting.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;


public class BluetoothHspFacade extends RpcReceiver {
  static final ParcelUuid[] HSP_UUIDS = {
    BluetoothUuid.HSP,
    BluetoothUuid.Handsfree,
  };

  private final Service mService;
  private final BluetoothAdapter mBluetoothAdapter;

  private boolean mIsHspReady = false;
  private BluetoothHeadset mHspProfile = null;

  public BluetoothHspFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    mBluetoothAdapter.getProfileProxy(mService, new HspServiceListener(), BluetoothProfile.HEADSET);
  }

  class HspServiceListener implements BluetoothProfile.ServiceListener {
    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
      mHspProfile = (BluetoothHeadset) proxy;
      mIsHspReady = true;
    }
    @Override
    public void onServiceDisconnected(int profile) {
      mIsHspReady = false;
    }
  }

  public Boolean hspConnect(BluetoothDevice device) {
    return mHspProfile.connect(device);
  }

  public Boolean hspDisconnect(BluetoothDevice device) {
    if (mHspProfile.getPriority(device) > BluetoothProfile.PRIORITY_ON) {
      mHspProfile.setPriority(device, BluetoothProfile.PRIORITY_ON);
    }
    return mHspProfile.disconnect(device);
  }

  @Rpc(description="Is Hsp profile ready.")
  public Boolean bluetoothHspIsReady() {
    return mIsHspReady;
  }

  @Rpc(description="Connect to HSP device.")
  public Boolean bluetoothHspConnect(
      @RpcParameter(
          name = "device",
          description = "Name or MAC address of a bluetooth device.")
          String device) throws Exception {
    if (mHspProfile == null) return false;
    BluetoothDevice mDevice = BluetoothFacade.getDevice(BluetoothFacade.DiscoveredDevices, device);
    Log.d("Connecting to device " + mDevice.getAliasName());
    return hspConnect(mDevice);
  }

  @Rpc(description="Disconnect an HSP device.")
  public Boolean bluetoothHspDisconnect(
      @RpcParameter(
          name = "device",
          description = "Name or MAC address of a bluetooth device.")
          String device) throws Exception {
    if (mHspProfile == null) return false;
    BluetoothDevice mDevice = BluetoothFacade.getDevice(mHspProfile.getConnectedDevices(), device);
    return hspDisconnect(mDevice);
  }

  @Override
  public void shutdown() {
  }
}
