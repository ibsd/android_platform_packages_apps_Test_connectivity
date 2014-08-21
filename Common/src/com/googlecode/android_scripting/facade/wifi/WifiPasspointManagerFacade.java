/*
 * Copyright (C) 2014 Google Inc.
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

package com.googlecode.android_scripting.facade.wifi;

import android.app.Service;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.passpoint.WifiPasspointManager;
import android.net.wifi.passpoint.WifiPasspointManager.Channel;
import android.os.Bundle;
import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * Exposes WifiPasspointManger functionality.
 */
public class WifiPasspointManagerFacade extends RpcReceiver {

  private final static String mEventType = "WifiPasspointManager";
  private final Service mService;
  private final EventFacade mEventFacade;
  private final WifiPasspointManager mWifiPasspointMgr;
  private Hashtable<Integer, PasspointActionListener> mPasspointActionListeners;
  private Hashtable<Integer, PasspointChannelListener> mPasspointChannelListeners;
  private Hashtable<Integer, Channel> mChannels;
  private List<ScanResult> mScanResults;

  public WifiPasspointManagerFacade(FacadeManager facadeManager){
    super(facadeManager);
    mService = facadeManager.getService();
    mEventFacade = facadeManager.getReceiver(EventFacade.class);
    mWifiPasspointMgr = (WifiPasspointManager)mService.getSystemService(Context.WIFI_PASSPOINT_SERVICE);
    mPasspointActionListeners = new Hashtable<Integer, PasspointActionListener>();
    mPasspointChannelListeners = new Hashtable<Integer, PasspointChannelListener>();
    mChannels = new Hashtable<Integer, Channel>();
    mScanResults = new ArrayList<ScanResult>();

  }

  public static class PasspointActionListener implements WifiPasspointManager.ActionListener{
    private static final String TAG = "PasspointAction";
    private static int sIndex;
    private int mIndex;
    private final Bundle msg = new Bundle();
    private final EventFacade mEventFacade;

    public PasspointActionListener(EventFacade ef){
      sIndex += 1;
      mIndex = sIndex;
      mEventFacade = ef;
    }

    @Override
    public void onSuccess() {
      mEventFacade.postEvent(TAG + mIndex + "onSuccess", null);
    }

    @Override
    public void onFailure(int reason){
      Log.d("onFailure " + mEventType + " " + mIndex);
      msg.putInt("Reason", reason);
      mEventFacade.postEvent(TAG + mIndex + "onFailure", msg);
      msg.clear();
    }
  }

  private static class PasspointChannelListener implements WifiPasspointManager.ChannelListener {
    private static int sIndex;
    private static EventFacade mEventFacade;
    private static final String TAG = "PasspointChannel";
    public final int mIndex;

    public PasspointChannelListener(EventFacade ef) {
        sIndex += 1;
        mIndex = sIndex;
        mEventFacade = ef;
    }
    @Override
    public void onChannelDisconnected() {
        Log.d("Wifi Passpoint channel disconnected.");
        mEventFacade.postEvent(TAG + mIndex + "onChannelDisconnected", null);
    }
  }

  private PasspointChannelListener genChangeListener() {
      PasspointChannelListener l = new PasspointChannelListener(mEventFacade);
      mPasspointChannelListeners.put(l.mIndex, l);
      return l;
  }

  /**
   * Constructs a WifiPasspointChannelListener object and initialize it
   * @return WifiPasspointChannelListener
   */
  private PasspointActionListener genPasspointActionListener() {
    PasspointActionListener listener = new PasspointActionListener(mEventFacade);
    mPasspointActionListeners.put(listener.mIndex, listener);
    return listener;
  }

  /**
   * Shuts down all activities associated with Passpoint
   */
  @Rpc(description = "Shuts down all Passpoint activities")
  public void wifiPasspointShutdown() {
    this.shutdown();
  }

  /** RPC Method Section */

  @Rpc(description = "Initialize wifi passpoint.",
       returns = "The index of the listener and channel associated with the passpoint.")
  public Integer wifiPasspointInitialize() {
      PasspointChannelListener listener = genChangeListener();
      Channel c = mWifiPasspointMgr.initialize(mService, mService.getMainLooper(), listener);
      mChannels.put(listener.mIndex, c);
      return listener.mIndex;
  }

  /**
   * Request ANQP Info of Passpoints
   * @param mask
   * @return the id of the Passpoint channel listener associated with this
   */
  @Rpc(description = "Request ANQP info.")
  public Integer requestAnqpInfoOfPasspoints(
      @RpcParameter(name = "scanIndex") Integer scanIndex,
      @RpcParameter(name = "channelIndex") Integer channelIndex,
      @RpcParameter(name = "mask") Integer mask) {
    List<ScanResult> sr = WifiScannerFacade.getWifiScanResult(scanIndex);
    if(sr != null && sr.size() >= 0) {
      PasspointActionListener listener = genPasspointActionListener();
      Channel chl = mChannels.get(channelIndex);
      mWifiPasspointMgr.requestAnqpInfo(chl, sr, mask, listener);
      return listener.mIndex;
    }
    return -1;
  }
  /*
   * Release all resource before closing down
   */
  @Override
  public void shutdown() {
    mPasspointActionListeners.clear();
    mPasspointChannelListeners.clear();
    mScanResults.clear();
  }

}
