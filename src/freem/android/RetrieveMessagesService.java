package freem.android;

import java.net.URL;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import freem.android.R;

public class RetrieveMessagesService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			int failedAttempts = intent.getIntExtra("failed_attempts", 0);
			RetrieveMessageTask task = new RetrieveMessageTask();
			task.execute(failedAttempts);
		}
		return START_STICKY;
	}

	private void retrieveMessage(int failedAttempts) {
		// Risolve i dati dati necessari per la richiesta al server.
		SharedPreferences prefs = getSharedPreferences("registration", MODE_PRIVATE);
		String serverBaseUrl = prefs.getString("server_address", null);
		String privateKey = prefs.getString("private_key", null);
		String phoneNumber = prefs.getString("phone_number", null);
		// Esegue la chiamata sul server.
		ServerProxy.Message[] messages;
		try {
			messages = ServerProxy.retrieve(new URL(serverBaseUrl), privateKey, phoneNumber);
		} catch (Exception e) {
			Log.e("freem", "errore durante il download dei messaggi", e);
			// Rischedula se ci sono meno di 5 tentativi falliti.
			failedAttempts++;
			if (failedAttempts < 5) {
				Intent intent = new Intent(this, RetrieveMessagesService.class);
				intent.putExtra("failed_attempts", failedAttempts);
				PendingIntent operation = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
				AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
				alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (60000 * failedAttempts), operation);
			}
			// Esce dalla procedura
			return;
		}
		// Salva su database.
		for (ServerProxy.Message message : messages) {
			saveMessage(message);
		}
		// Se il thread risulta chiuso, mostra la notifica sulla barra di Android.
		Intent intent = new Intent(this, ThreadsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setContentTitle(getString(R.string.notification_title));
		builder.setContentText(getString(R.string.notification_text));
		builder.setWhen(System.currentTimeMillis());
		builder.setAutoCancel(true);
		builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		builder.setContentIntent(contentIntent);
		Notification notification = builder.build();
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(1, notification);
	}

	private void saveMessage(ServerProxy.Message message) {
		long now = System.currentTimeMillis();
		ContentResolver r = getContentResolver();
		// Cerca se esiste un thread per l'interlocutore.
		long threadId = -1;
		int unreadCount = 0;
		Cursor c = r.query(MessagesContentProvider.THREADS_URI, new String[] { "_id", "unread" }, "phone_number = ?", new String[] { message.senderPhoneNumber }, null);
		if (c.moveToFirst()) {
			threadId = c.getLong(0);
			unreadCount = c.getInt(1);
		}
		c.close();
		if (threadId == -1) {
			// Se non esiste, lo crea.
			ContentValues values = new ContentValues();
			values.put("phone_number", message.senderPhoneNumber);
			values.put("unread", 1);
			values.put("last_update", now);
			Uri uri = r.insert(MessagesContentProvider.THREADS_URI, values);
			threadId = Long.parseLong(uri.getLastPathSegment());
		} else {
			// Se esiste, lo aggiorna.
			ContentValues values = new ContentValues();
			values.put("last_update", now);
			values.put("unread", unreadCount + 1);
			r.update(MessagesContentProvider.THREADS_URI, values, "phone_number = ?", new String[] { message.senderPhoneNumber });
		}
		// Aggiunge il messaggio.
		ContentValues values = new ContentValues();
		values.put("thread", threadId);
		values.put("direction", 1);
		values.put("message", message.text);
		values.put("ts_sent", message.sentTimestamp);
		r.insert(MessagesContentProvider.MESSAGES_URI, values);
	}

	private class RetrieveMessageTask extends AsyncTask<Integer, Void, Void> {

		private WakeLock mWakeLock;

		@Override
		protected void onPreExecute() {
			// Acquisisce un Wake Lock.
			PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
			mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "freem-retrieve-message");
			mWakeLock.acquire();
		}

		@Override
		protected Void doInBackground(Integer... params) {
			retrieveMessage(params[0]);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// Rilascia il lock.
			mWakeLock.release();
		}

	}

}
