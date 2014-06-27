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
  static final ParcelUuid[] SINK_UUIDS = { BluetoothUuid.AudioSink,
      BluetoothUuid.AdvAudioDist, };

  private final Service mService;
  private final BluetoothAdapter mBluetoothAdapter;

  private static boolean sIsA2dpReady = false;
  private static BluetoothA2dp sA2dpProfile = null;

  public BluetoothA2dpFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    mBluetoothAdapter.getProfileProxy(mService, new A2dpServiceListener(),
        BluetoothProfile.A2DP);
  }

  class A2dpServiceListener implements BluetoothProfile.ServiceListener {
    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
      sA2dpProfile = (BluetoothA2dp) proxy;
      sIsA2dpReady = true;
    }

    @Override
    public void onServiceDisconnected(int profile) {
      sIsA2dpReady = false;
    }
  }

  public Boolean a2dpConnect(BluetoothDevice device) {
    List<BluetoothDevice> sinks = sA2dpProfile.getConnectedDevices();
    if (sinks != null) {
      for (BluetoothDevice sink : sinks) {
        sA2dpProfile.disconnect(sink);
      }
    }
    return sA2dpProfile.connect(device);
  }

  public Boolean a2dpDisconnect(BluetoothDevice device) {
    if (sA2dpProfile.getPriority(device) > BluetoothProfile.PRIORITY_ON) {
      sA2dpProfile.setPriority(device, BluetoothProfile.PRIORITY_ON);
    }
    return sA2dpProfile.disconnect(device);
  }

  @Rpc(description = "Is A2dp profile ready.")
  public Boolean bluetoothA2dpIsReady() {
    return sIsA2dpReady;
  }

  @Rpc(description = "Connect to an A2DP device.")
  public Boolean bluetoothA2dpConnect(
      @RpcParameter(name = "device", description = "Name or MAC address of a bluetooth device.") String device)
      throws Exception {
    if (sA2dpProfile == null)
      return false;
    BluetoothDevice mDevice = BluetoothFacade.getDevice(
        BluetoothFacade.DiscoveredDevices, device);
    Log.d("Connecting to device " + mDevice.getAliasName());
    return a2dpConnect(mDevice);
  }

  @Rpc(description = "Disconnect an A2DP device.")
  public Boolean bluetoothA2dpDisconnect(
      @RpcParameter(name = "device", description = "Name or MAC address of a device.") String device)
      throws Exception {
    if (sA2dpProfile == null)
      return false;
    Log.d("Connected devices: " + sA2dpProfile.getConnectedDevices());
    BluetoothDevice mDevice = BluetoothFacade.getDevice(
        sA2dpProfile.getConnectedDevices(), device);
    return a2dpDisconnect(mDevice);
  }

  @Rpc(description = "Get all the devices connected through A2DP.")
  public List<BluetoothDevice> bluetoothA2dpGetConnectedDevices() {
    while (!sIsA2dpReady)
      ;
    return sA2dpProfile.getConnectedDevices();
  }

  @Override
  public void shutdown() {
  }
}
