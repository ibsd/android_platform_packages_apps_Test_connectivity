
package com.googlecode.android_scripting.facade.tele;

import java.util.HashMap;

import android.telecom.Call;
import android.telecom.InCallService;
import android.telecom.Phone;

import com.googlecode.android_scripting.Log;

public class InCallServiceImpl extends InCallService {

    public static Phone mPhone;
    public static HashMap<String, Call> mCalls = new HashMap<String, Call>();

    private Phone.Listener mPhoneListener = new Phone.Listener() {

        @Override
        public void onCallAdded(Phone phone, Call call) {
            Log.d("onCallAdded: " + call.toString());
            String id = TelecomManagerFacade.getCallId(call);
            Log.d("Adding " + id);
            mCalls.put(id, call);
        }

        @Override
        public void onCallRemoved(Phone phone, Call call) {
            Log.d("onCallRemoved: " + call.toString());
            String id = TelecomManagerFacade.getCallId(call);
            Log.d("Removing " + id);
            mCalls.remove(id);
        }
    };

    @Override
    public void onPhoneCreated(Phone phone) {
        Log.d("onPhoneCreated");
        mPhone = phone;
        mPhone.addListener(mPhoneListener);
    }

    @Override
    public void onPhoneDestroyed(Phone phone) {
        Log.d("onPhoneDestroyed");
        mPhone.removeListener(mPhoneListener);
        mPhone = null;
    }
}
