package com.googlecode.android_scripting.facade.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.googlecode.android_scripting.Log;

public class BluetoothPairingHelper extends BroadcastReceiver {
  public BluetoothPairingHelper() {
    super();
  }
  /**
   * Blindly confirm passkey
   */
  @Override
  public void onReceive(Context c, Intent intent) {
    String action = intent.getAction();
    int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
    Log.d("Bluetooth pairing intent received: " + action + " with type " + type);
    BluetoothDevice mBtDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    if(action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
      Log.d("Processing Action Paring Request.");
      if(type == BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION ||
         type == BluetoothDevice.PAIRING_VARIANT_CONSENT) {
        mBtDevice.setPairingConfirmation(true);
        Log.d("Connection confirmed");
        abortBroadcast(); // Abort the broadcast so Settings app doesn't get it.
      }
    }
  }
}