package com.googlecode.android_scripting.bluetooth;

import java.util.List;

import android.app.Service;
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcDefault;
import com.googlecode.android_scripting.rpc.RpcParameter;

public class BluetoothHidFacade extends RpcReceiver {
  public final static ParcelUuid[] HID_UUID = { BluetoothUuid.Hid };

  private final Service mService;
  private final BluetoothAdapter mBluetoothAdapter;

  private static boolean sIsHidReady = false;
  private static BluetoothInputDevice sHidProfile = null;

  public BluetoothHidFacade(FacadeManager manager) {
    super(manager);
    mService = manager.getService();
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    mBluetoothAdapter.getProfileProxy(mService, new HidServiceListener(),
        BluetoothProfile.INPUT_DEVICE);
  }

  class HidServiceListener implements BluetoothProfile.ServiceListener {
    @Override
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
      sHidProfile = (BluetoothInputDevice) proxy;
      sIsHidReady = true;
    }

    @Override
    public void onServiceDisconnected(int profile) {
      sIsHidReady = false;
    }
  }

  public Boolean hidConnect(BluetoothDevice device) {
    if (sHidProfile == null) return false;
    return sHidProfile.connect(device);
  }

  public Boolean hidDisconnect(BluetoothDevice device) {
    if (sHidProfile == null) return false;
    return sHidProfile.disconnect(device);
  }

  @Rpc(description = "Is Hid profile ready.")
  public Boolean bluetoothHidIsReady() {
    return sIsHidReady;
  }

  @Rpc(description = "Connect to an HID device.")
  public Boolean bluetoothHidConnect(
      @RpcParameter(name = "device", description = "Name or MAC address of a bluetooth device.")
      String device)
      throws Exception {
    if (sHidProfile == null)
      return false;
    BluetoothDevice mDevice = BluetoothFacade.getDevice(BluetoothFacade.DiscoveredDevices, device);
    Log.d("Connecting to device " + mDevice.getAliasName());
    return hidConnect(mDevice);
  }

  @Rpc(description = "Disconnect an HID device.")
  public Boolean bluetoothHidDisconnect(
      @RpcParameter(name = "device", description = "Name or MAC address of a device.")
      String device)
      throws Exception {
    if (sHidProfile == null)
      return false;
    Log.d("Connected devices: " + sHidProfile.getConnectedDevices());
    BluetoothDevice mDevice = BluetoothFacade.getDevice(sHidProfile.getConnectedDevices(),
                                                        device);
    return hidDisconnect(mDevice);
  }

  @Rpc(description = "Get all the devices connected through HID.")
  public List<BluetoothDevice> bluetoothHidGetConnectedDevices() {
    while (!sIsHidReady);
    return sHidProfile.getConnectedDevices();
  }

  @Rpc(description = "Get the connection status of a device.")
  public Integer bluetoothHidGetConnectionStatus(
          @RpcParameter(name = "deviceID",
                        description = "Name or MAC address of a bluetooth device.")
          String deviceID) {
      if (sHidProfile == null) {
          return BluetoothProfile.STATE_DISCONNECTED;
      }
      List<BluetoothDevice> deviceList = sHidProfile.getConnectedDevices();
      BluetoothDevice device;
      try {
          device = BluetoothFacade.getDevice(deviceList, deviceID);
      } catch (Exception e) {
          return BluetoothProfile.STATE_DISCONNECTED;
      }
      return sHidProfile.getConnectionState(device);
  }

  @Rpc(description = "Set report on a connected HID device.")
  public Boolean bluetoothHidSetReport(
          @RpcParameter(name = "deviceID",
          description = "Name or MAC address of a bluetooth device.")
          String deviceID,
          @RpcParameter(name = "type")
          @RpcDefault(value = "1")
          Byte type,
          @RpcParameter(name = "report")
          String report) throws Exception {
      BluetoothDevice device = BluetoothFacade.getDevice(sHidProfile.getConnectedDevices(),
              deviceID);
      return sHidProfile.setReport(device, type, report);
  }

  @Rpc(description = "Send data to a connected HID device.")
  public Boolean bluetoothHidSendData(
          @RpcParameter(name = "deviceID",
          description = "Name or MAC address of a bluetooth device.")
          String deviceID,
          @RpcParameter(name = "report")
          String report) throws Exception {
      BluetoothDevice device = BluetoothFacade.getDevice(sHidProfile.getConnectedDevices(),
              deviceID);
      return sHidProfile.sendData(device, report);
  }

  @Override
  public void shutdown() {
  }
}
