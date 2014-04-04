package freem.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {

	public GCMIntentService() {
		super(Config.GCM_SENDER_ID);
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		SharedPreferences prefs = getSharedPreferences("registration", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("gcm_id", regId);
		editor.commit();
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		// Recupera il tipo della notifica.
		String type = intent.getStringExtra("type");
		if ("message".equals(type)) {
			// Avvia il servizio per il download dei nuovi messaggi.
			Intent intent2 = new Intent(this, RetrieveMessagesService.class);
			intent2.putExtra("failed_attempts", 0);
			startService(intent2);
		}
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
	}

	@Override
	protected void onError(Context context, String regId) {
	}

}
