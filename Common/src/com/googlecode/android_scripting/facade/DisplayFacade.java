
package com.googlecode.android_scripting.facade;

import java.util.HashMap;

import android.app.Service;
import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.view.Display;

import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcDefault;
import com.googlecode.android_scripting.rpc.RpcParameter;

public class DisplayFacade extends RpcReceiver {

    private final Service mService;
    private final DisplayManager mDisplayManager;
    private HashMap<Integer, Display> mDisplays;

    public DisplayFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mDisplayManager = (DisplayManager) mService.getSystemService(Context.DISPLAY_SERVICE);
        updateDisplays(mDisplayManager.getDisplays());
    }

    private void updateDisplays(Display[] displays) {
        if (mDisplays == null) {
            mDisplays = new HashMap<Integer, Display>();
        }
        mDisplays.clear();
        for(Display d : displays) {
            mDisplays.put(d.getDisplayId(), d);
        }
    }

    @Rpc(description = "Get a list of IDs of the logical displays connected."
                     + "Also updates the cached displays.")
    public Integer[] displayGetDisplays() {
        Display[] displays = mDisplayManager.getDisplays();
        updateDisplays(displays);
        Integer[] results = new Integer[displays.length];
        for(int i = 0; i < displays.length; i++) {
            results[i] = displays[i].getDisplayId();
        }
        return results;
    }

    @Rpc(description = "Get the size of the specified display in pixels.")
    public Point displayGetSize(
            @RpcParameter(name = "displayId")
            @RpcDefault(value = "0")
            Integer displayId) {
        Point outSize = new Point();
        Display d = mDisplays.get(displayId);
        d.getSize(outSize);
        return outSize;
    }

    @Rpc(description = "Get the maximum screen size dimension that will happen.")
    public Integer displayGetMaximumSizeDimension(
            @RpcParameter(name = "displayId")
            @RpcDefault(value = "0")
            Integer displayId) {
        Display d = mDisplays.get(displayId);
        return d.getMaximumSizeDimension();
    }

    @Rpc(description = "Get display metrics based on the real size of this display.")
    public DisplayMetrics displayGetRealMetrics(
            @RpcParameter(name = "displayId")
            @RpcDefault(value = "0")
            Integer displayId) {
        Display d = mDisplays.get(displayId);
        DisplayMetrics outMetrics = new DisplayMetrics();
        d.getRealMetrics(outMetrics);
        return outMetrics;
    }

    @Override
    public void shutdown() {
    }
}
