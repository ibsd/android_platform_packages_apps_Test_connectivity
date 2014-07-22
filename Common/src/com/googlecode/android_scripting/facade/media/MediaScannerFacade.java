package com.googlecode.android_scripting.facade.media;

import android.app.Service;
import android.content.Intent;
import android.media.MediaScanner;
import android.net.Uri;
import android.os.Environment;

import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

/**
 * Expose functionalities of MediaScanner related APIs.
 */
public class MediaScannerFacade extends RpcReceiver {

    private final Service mService;
    private final MediaScanner mScanService;

    public MediaScannerFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mScanService = new MediaScanner(mService);
    }

    @Rpc(description = "Scan external storage for media files.")
    public void mediaScanForFiles() {
        mService.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                               Uri.parse("file://" + Environment.getExternalStorageDirectory())));
    }

    @Rpc(description = "Scan for a media file.")
    public void mediaScanForOneFile(@RpcParameter(name = "path") String path) {
        mService.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(path)));
    }

    @Override
    public void shutdown() {
    }
}
