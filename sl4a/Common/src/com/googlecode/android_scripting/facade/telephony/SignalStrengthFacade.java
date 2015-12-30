/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.googlecode.android_scripting.facade.telephony;

import android.app.Service;
import android.content.Context;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcMinSdk;
import com.googlecode.android_scripting.rpc.RpcStartEvent;
import com.googlecode.android_scripting.rpc.RpcStopEvent;

import java.util.concurrent.Callable;

/**
 * Exposes SignalStrength functionality.
 *
 * @author Joerg Zieren (joerg.zieren@gmail.com)
 */
@RpcMinSdk(7)
public class SignalStrengthFacade extends RpcReceiver {
  private final Service mService;
  private final TelephonyManager mTelephonyManager;
  private final EventFacade mEventFacade;
  private final PhoneStateListener mPhoneStateListener;
  private Bundle mSignalStrengths;
  private final int INVALID_VALUE = Integer.MIN_VALUE;

  public SignalStrengthFacade(FacadeManager manager) {
    super(manager);
    mSignalStrengths = new Bundle();
    mService = manager.getService();
    mEventFacade = manager.getReceiver(EventFacade.class);
    mTelephonyManager =
        (TelephonyManager) manager.getService().getSystemService(Context.TELEPHONY_SERVICE);
    mPhoneStateListener = MainThread.run(mService, new Callable<PhoneStateListener>() {
      @Override
      public PhoneStateListener call() throws Exception {
        return new PhoneStateListener() {
          @Override
          public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            mSignalStrengths.putInt("GsmSignalStrength", signalStrength.getGsmSignalStrength());
            mSignalStrengths.putInt("GsmBitErrorRate", signalStrength.getGsmBitErrorRate());
            mSignalStrengths.putInt("CdmaEcio", signalStrength.getCdmaEcio());
            mSignalStrengths.putInt("EvdoDbm", signalStrength.getEvdoDbm());
            mSignalStrengths.putInt("EvdoEcio", signalStrength.getEvdoEcio());
            mSignalStrengths.putInt("LteSignalStrength", signalStrength.getLteSignalStrength());
            mSignalStrengths.putInt("LteDbm", signalStrength.getLteDbm());
            mSignalStrengths.putInt("LteLevel", signalStrength.getLteLevel());
            mSignalStrengths.putInt("LteAsuLevel", signalStrength.getLteAsuLevel());
            mSignalStrengths.putInt("Level", signalStrength.getLevel());
            mSignalStrengths.putInt("AsuLevel", signalStrength.getAsuLevel());
            mSignalStrengths.putInt("Dbm", signalStrength.getDbm());
            mSignalStrengths.putInt("GsmDbm", signalStrength.getGsmDbm());
            mSignalStrengths.putInt("GsmLevel", signalStrength.getGsmLevel());
            mSignalStrengths.putInt("GsmAsuLevel", signalStrength.getGsmAsuLevel());
            mSignalStrengths.putInt("CdmaLevel", signalStrength.getCdmaLevel());
            mSignalStrengths.putInt("CdmaAsuLevel", signalStrength.getCdmaAsuLevel());
            mSignalStrengths.putInt("CdmaDbm", signalStrength.getCdmaDbm());
            mEventFacade.postEvent("signal_strengths", mSignalStrengths.clone());
          }
        };
      }
    });
  }

  @Rpc(description = "Starts tracking signal strengths.")
  @RpcStartEvent("signal_strengths")
  public void startTrackingSignalStrengths() {
    mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
  }

  @Rpc(description = "Returns the current signal strengths.", returns = "A map of \"gsm_signal_strength\"")
  public Bundle phoneGetSignalStrengthInfo() {
    return mSignalStrengths;
  }

  @Rpc(description = "Returns current signal strength in dBm.")
  public Integer phoneGetSignalStrengthDbm() {
    Integer result = mSignalStrengths.getInt("Dbm", INVALID_VALUE);
    return (result.equals(INVALID_VALUE)?null:result);
  }

  @Rpc(description = "Returns current signal strength in AsuLevel.")
  public Integer phoneGetSignalStrengthAsu() {
    Integer result = mSignalStrengths.getInt("AsuLevel", INVALID_VALUE);
    return (result.equals(INVALID_VALUE)?null:result);
  }

  @Rpc(description = "Returns current signal strength in Level.")
  public Integer phoneGetSignalStrengthLevel() {
    Integer result = mSignalStrengths.getInt("Level", INVALID_VALUE);
    return (result.equals(INVALID_VALUE)?null:result);
  }

  @Rpc(description = "Stops tracking signal strength.")
  @RpcStopEvent("signal_strengths")
  public void stopTrackingSignalStrengths() {
    mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
  }

  @Override
  public void shutdown() {
    stopTrackingSignalStrengths();
  }
}
