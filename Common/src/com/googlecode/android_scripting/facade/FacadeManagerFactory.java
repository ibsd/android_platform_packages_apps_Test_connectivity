package com.googlecode.android_scripting.facade;

import android.app.Service;
import android.content.Intent;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.jsonrpc.RpcReceiverManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiverManagerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FacadeManagerFactory implements RpcReceiverManagerFactory {

  private final int mSdkLevel;
  private final Service mService;
  private final Intent mIntent;
  private final Collection<Class<? extends RpcReceiver>> mClassList;
  private static final Map<Integer, RpcReceiverManager> mFacadeManagers =
      new ConcurrentHashMap<Integer, RpcReceiverManager>();

  public FacadeManagerFactory(int sdkLevel, Service service, Intent intent,
      Collection<Class<? extends RpcReceiver>> classList) {
    mSdkLevel = sdkLevel;
    mService = service;
    mIntent = intent;
    mClassList = classList;
  }

  @Override
  public Map<Integer, RpcReceiverManager> getRpcReceiverManagers() {
    return mFacadeManagers;
  }

  @Override
  public RpcReceiverManager getRpcReceiverManager(Integer UID) {
    if (mFacadeManagers.containsKey(UID)) {
      Log.d("Returning existing session for UID: " + UID);
      return mFacadeManagers.get(UID);
    } else {
      Log.d("Creating new session for UID: " + UID);
      FacadeManager facadeManager = new FacadeManager(mSdkLevel, mService, mIntent, mClassList);
      mFacadeManagers.put(UID, facadeManager);
      return facadeManager;
    }
  }

  @Override
  public boolean remove(Integer UID) {
    if (mFacadeManagers.containsKey(UID)) {
      mFacadeManagers.remove(UID);
      return true;
    }
    return false;
  }
}
