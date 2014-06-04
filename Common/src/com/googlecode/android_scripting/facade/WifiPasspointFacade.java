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

package com.googlecode.android_scripting.facade;

import android.app.Service;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.passpoint.WifiPasspointManager;
import android.net.wifi.passpoint.WifiPasspointManager.Channel;
import android.net.wifi.passpoint.WifiPasspointManager.ChannelListener;
import android.os.Bundle;
import android.os.Looper;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.facade.EventFacade;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;
import com.googlecode.android_scripting.rpc.RpcStartEvent;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Exposes WifiPasspointManger functionality.
 */
public class WifiPasspointFacade extends RpcReceiver {

  private final static String mEventType = "ANQPInfo";
  private final Service mService;
  private final EventFacade mEventFacade;
  private final WifiPasspointManager mWifiPasspointMgr;
  private static int mWifiPasspointChannelALCnt;
  private Hashtable<Integer, WifiPasspointChannelActionListener> mWifiPasspointChannelAlList;
  private List<ScanResult> mScanResults;

  public WifiPasspointFacade(FacadeManager facadeManager){
    super(facadeManager);
    mService = facadeManager.getService();
    mEventFacade = facadeManager.getReceiver(EventFacade.class);
    mWifiPasspointMgr = (WifiPasspointManager)mService.getSystemService(Context.WIFI_PASSPOINT_SERVICE);
    mWifiPasspointChannelAlList = new Hashtable<Integer, WifiPasspointChannelActionListener>();
    mScanResults = new ArrayList<ScanResult>();

  }

  public class WifiPasspointChannelActionListener implements WifiPasspointManager.ActionListener{

    private ChannelListener mChannelListener;
    private Channel mChannel;
    private int mIndex;
    private final Bundle mStatus;

    public WifiPasspointChannelActionListener(){
      mChannelListener = new WifiPasspointManager.ChannelListener() {
        @Override
        public void onChannelDisconnected() {
          Log.e("Channel Disconnected with WifiPasspoint Framwork");
        }
      };
      mChannel  = mWifiPasspointMgr.initialize(mService.getApplicationContext(), Looper.getMainLooper() , mChannelListener);
      mIndex = ++mWifiPasspointChannelALCnt;
      mStatus = new Bundle();
    }

    @Override
    public void onSuccess() {
      Log.d("onSuccess " + mEventType + " " + mIndex);
      mStatus.putString("Type", "onSuccess");
      mEventFacade.postEvent(mEventType + mIndex, mStatus.clone());
      mStatus.clear();
    }

    @Override
    public void onFailure(int reason){
      Log.d("onFailure " + mEventType + " " + mIndex);
      mStatus.putString("Type", "onFailure");
      mStatus.putInt("Reason", reason);
      mEventFacade.postEvent(mEventType + mIndex, mStatus.clone());
      mStatus.clear();
    }

  }

  /**
   * Constructs a WifiPasspointChannelListener object and initialize it
   * @return WifiPasspointChannelListener
   */
  private WifiPasspointChannelActionListener genWifiPasspointChannelAL() {
    WifiPasspointChannelActionListener mWifiPpChannelAL =
        MainThread.run(mService, new Callable<WifiPasspointChannelActionListener>() {
      @Override
      public WifiPasspointChannelActionListener call() throws Exception {
        return new WifiPasspointChannelActionListener();
      }
    });
    mWifiPasspointChannelAlList.put(mWifiPpChannelAL.mIndex, mWifiPpChannelAL);
    return mWifiPpChannelAL;
  }

  /**
   * Shuts down all activities associated with Passpoint
   */
  @Rpc(description = "Shuts down all Passpoint activities")
  public void wifiPasspointShutdown() {
    this.shutdown();
  }

  /** RPC Method Section */

  /**
   * Request ANQP Info of Passpoints
   * @param mask
   * @return the id of the Passpoint channel listener associated with this
   */
  @Rpc(description = "Request ANQP info.")
  @RpcStartEvent("ANQPInfo")
  public Integer requestAnqpInfoOfPasspoints(@RpcParameter(name = "scanIndex") Integer scanIndex,
      @RpcParameter(name = "mask") Integer mask) {
    ScanFacade.getWifiScanResult(scanIndex, mScanResults);
    if(mScanResults != null && mScanResults.size() >= 0) {
      WifiPasspointChannelActionListener mWifiPpChannelAL = genWifiPasspointChannelAL();
      mWifiPasspointMgr.requestAnqpInfo(mWifiPpChannelAL.mChannel,  mScanResults, mask, mWifiPpChannelAL);
      return mWifiPpChannelAL.mIndex;
    }
    return -1;
  }
  /*
   * Release all resource before closing down
   */
  @Override
  public void shutdown() {
    mWifiPasspointChannelAlList.clear();
    mScanResults.clear();
  }

}
