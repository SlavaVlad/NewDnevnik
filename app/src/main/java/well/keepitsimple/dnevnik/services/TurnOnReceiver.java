package well.keepitsimple.dnevnik.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TurnOnReceiver extends BroadcastReceiver {
    public TurnOnReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Test", "RAN");
        //Intent intent1 = new Intent(context, MyNewIntentService.class);
        //context.startService(intent1);
    }
}
