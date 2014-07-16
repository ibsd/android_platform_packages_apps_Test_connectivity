
package com.googlecode.android_scripting.facade;

import java.util.concurrent.Callable;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.media.session.MediaSession.Callback;
import android.view.KeyEvent;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.MainThread;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;

public class MediaSessionFacade extends RpcReceiver {

    private final Service mService;
    private final EventFacade mEventFacade;
    private MediaSessionManager mMediaManager;
    private MediaSession mMedia;
    private Callback mCallback;

    public MediaSessionFacade(FacadeManager manager) {
        super(manager);
        mService = manager.getService();
        mEventFacade = manager.getReceiver(EventFacade.class);
        Log.d("Creating MediaSession.");
        mMediaManager = (MediaSessionManager) mService.getSystemService(Context.MEDIA_SESSION_SERVICE);
        mMedia = mMediaManager.createSession("SL4A");
        mMedia.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
        mMedia.setActive(true);
        mCallback = new ButtonCallback(mEventFacade);
    }

    public static class ButtonCallback extends MediaSession.Callback {
        private final EventFacade mEventFacade;
        public ButtonCallback(EventFacade eventFacade) {
            this.mEventFacade = eventFacade;
        }
        @Override
        public void onMediaButtonEvent(Intent mediaButtonIntent) {
            String action = mediaButtonIntent.getAction();
            Log.d("Received intent with action " + action);
            if (action.equals(Intent.ACTION_MEDIA_BUTTON)) {
                KeyEvent event = (KeyEvent) mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                int keyAction = event.getAction();
                Log.d("Received KeyEvent with action " + keyAction);
                if (keyAction == KeyEvent.ACTION_DOWN) {
                    int keyCode = event.getKeyCode();
                    Log.d("Received ACTION_DOWN with keycode " + keyCode);
                    if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                        mEventFacade.postEvent("mediaKeyOnPlay", null);
                    } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                        mEventFacade.postEvent("mediaOnPause", null);
                    } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
                        mEventFacade.postEvent("mediaOnStop", null);
                    } else if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                        mEventFacade.postEvent("mediaOnNext", null);
                    } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                        mEventFacade.postEvent("mediaOnPrevious", null);
                    } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                        mEventFacade.postEvent("mediaOnPlayPause", null);
                    }
                }
            }
        }
    }

    @Rpc(description = "Checks whether any music is active.")
    public void mediaSessionAddCallback() {
        MainThread.run(mService, new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                Log.d("Adding callback.");
                mMedia.addCallback(mCallback);
                PlaybackState.Builder bob = new PlaybackState.Builder();
                bob.setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_STOP);
                bob.setState(PlaybackState.STATE_PLAYING, 0, 1);
                mMedia.setPlaybackState(bob.build());
                return null;
            }
        });
    }

    @Rpc(description = "Whether current media session is active.")
    public Boolean mediaSessionIsActive() {
        return mMedia.isActive();
    }

    @Override
    public void shutdown() {
        mMedia.removeCallback(mCallback);
        mMedia.release();
    }
}
