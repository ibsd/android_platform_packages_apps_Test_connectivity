package com.googlecode.android_scripting.bluetooth;

import java.util.List;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;


public class BluetoothA2dpFacade extends RpcReceiver {
  static final ParcelUuid[] SINK_UUIDS = {
      BluetoothUuid.AudioSink,
      BluetoothUuid.AdvAudioDist,
  };

  private final Service mService;
  private final BluetoothAdapter mBluetoothAdapter;

  private boolean mIsA2dpReady = false;
  private BluetoothA2dp mA2dpProfile = null;

  public BluetoothA2dpFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    mBluetoothAdapter.getProfileProxy(mService, new A2dpServiceListener(), BluetoothProfile.A2DP);
  }

  class A2dpServiceListener implements BluetoothProfile.ServiceListener {
    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
      mA2dpProfile = (BluetoothA2dp) proxy;
      mIsA2dpReady = true;
    }
    @Override
    public void onServiceDisconnected(int profile) {
      mIsA2dpReady = false;
    }
  }

  public Boolean a2dpConnect(BluetoothDevice device) {
    List<BluetoothDevice> sinks = mA2dpProfile.getConnectedDevices();
    if (sinks != null) {
        for (BluetoothDevice sink : sinks) {
          mA2dpProfile.disconnect(sink);
        }
    }
    return mA2dpProfile.connect(device);
  }

  public Boolean a2dpDisconnect(BluetoothDevice device) {
    if (mA2dpProfile.getPriority(device) > BluetoothProfile.PRIORITY_ON){
      mA2dpProfile.setPriority(device, BluetoothProfile.PRIORITY_ON);
    }
    return mA2dpProfile.disconnect(device);
  }

  @Rpc(description="Is A2dp profile ready.")
  public Boolean bluetoothA2dpIsReady() {
    return mIsA2dpReady;
  }
  @Rpc(description="Connect to an A2DP device.")
  public Boolean bluetoothA2dpConnect(
      @RpcParameter(
          name = "device",
          description = "Name or MAC address of a bluetooth device.")
          String device) throws Exception {
    if (mA2dpProfile == null) return false;
    BluetoothDevice mDevice = BluetoothFacade.getDevice(BluetoothFacade.DiscoveredDevices, device);
    Log.d("Connecting to device " + mDevice.getAliasName());
    return a2dpConnect(mDevice);
  }

  @Rpc(description="Disconnect an A2DP device.")
  public Boolean bluetoothA2dpDisconnect(
      @RpcParameter(
          name = "device",
          description = "Name or MAC address of a device.")
          String device) throws Exception {
    if (mA2dpProfile == null) return false;
    BluetoothDevice mDevice = BluetoothFacade.getDevice(mA2dpProfile.getConnectedDevices(), device);
    return a2dpDisconnect(mDevice);
  }

  @Rpc(description="Get all the devices connected through A2DP.")
  public List<BluetoothDevice> bluetoothA2dpGetConnectedDevices() {
    return mA2dpProfile.getConnectedDevices();
  }

  @Override
  public void shutdown() {
  }
}
