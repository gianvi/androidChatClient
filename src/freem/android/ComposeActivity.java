package freem.android;

import java.net.URL;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import freem.android.R;

public class ComposeActivity extends Activity {

	private static final int REQUEST_PICK_CONTACT = 10;

	private EditText mPhoneNumber;
	private EditText mMessage;

	@Override
	@TargetApi(11)
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Numero del destinatario preimpostato da intent?
		String phoneNumber = getIntent().getStringExtra("phone_number");
		// Usa la action bar dove disponibile.
		if (Build.VERSION.SDK_INT >= 11) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		// Carica il layout ed associa i valori.
		setContentView(R.layout.activity_compose);
		mPhoneNumber = (EditText) findViewById(R.id.phone_number);
		mMessage = (EditText) findViewById(R.id.message);
		if (phoneNumber != null) {
			mPhoneNumber.setText(phoneNumber);
			mMessage.requestFocus();
		} else {
			mPhoneNumber.requestFocus();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_compose, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			finish();
			return true;
		}
		if (id == R.id.contacts) {
			pickRecipientFromContacts();
			return true;
		}
		if (id == R.id.send) {
			sendMessage();
			return true;
		}
		return false;
	}

	private void pickRecipientFromContacts() {
		Intent intent = new Intent(Intent.ACTION_PICK, Phone.CONTENT_URI);
		startActivityForResult(intent, REQUEST_PICK_CONTACT);
	}

	@TargetApi(16)
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		if (reqCode == REQUEST_PICK_CONTACT && resultCode == Activity.RESULT_OK) {
			// Contatto selezionato, ne estrae l'identificativo.
			String phoneNumber = null;
			if (data != null) {
				Uri uri = data.getData();
				Log.i("freem", "URI: " + uri);
				if (uri != null) {
					String[] selection;
					if (Build.VERSION.SDK_INT >= 16) {
						selection = new String[] { Phone.NORMALIZED_NUMBER };
					} else {
						selection = new String[] { Phone.NUMBER };
					}
					Cursor c = getContentResolver().query(uri, selection, null, null, null);
					if (c.moveToFirst()) {
						phoneNumber = c.getString(0);
					}
					c.close();
				}
			}
			if (phoneNumber != null) {
				mPhoneNumber.setText(phoneNumber);
			}
		}
	}

	private void sendMessage() {
		// Recupera e valida i campi del modulo.
		String recipientPhoneNumber = mPhoneNumber.getText().toString().trim();
		if (recipientPhoneNumber.length() == 0) {
			Toast.makeText(this, getString(R.string.missing_phone_number), Toast.LENGTH_SHORT).show();
			return;
		}
		String message = mMessage.getText().toString().trim();
		if (message.length() == 0) {
			Toast.makeText(this, getString(R.string.missing_message), Toast.LENGTH_SHORT).show();
			return;
		}
		// Aggiunge il prefisso al numero, se necessario.
		if (!recipientPhoneNumber.startsWith("+")) {
			recipientPhoneNumber = "+" + Utils.resolveCurrentICC(this) + recipientPhoneNumber;
		}
		// Risolve gli ulteriori dati necessari per l'invio.
		SharedPreferences prefs = getSharedPreferences("registration", MODE_PRIVATE);
		String serverBaseUrl = prefs.getString("server_address", null);
		String privateKey = prefs.getString("private_key", null);
		String senderPhoneNumber = prefs.getString("phone_number", null);
		// Avvia un task parallelo per la chiamata al server.
		AsyncTask<String, Void, Exception> task = new AsyncTask<String, Void, Exception>() {

			private ProgressDialog dialog;

			@Override
			protected void onPreExecute() {
				dialog = new ProgressDialog(ComposeActivity.this);
				dialog.setTitle(getString(R.string.send_dialog_title));
				dialog.setMessage(getString(R.string.send_dialog_message));
				dialog.setIndeterminate(true);
				dialog.setCancelable(true);
				dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						cancel(true);
					}
				});
				dialog.show();
			}

			@Override
			protected Exception doInBackground(String... params) {
				Exception error = null;
				try {
					ServerProxy.send(new URL(params[0]), params[1], params[2], params[3], params[4]);
				} catch (Exception e) {
					Log.e("freem", "impossibile inviare il messaggio", e);
					error = e;
				}
				if (error == null) {
					// Salva su database.
					saveSentMessage(params[3], params[4]);
				}
				return error;
			}

			@Override
			protected void onPostExecute(Exception error) {
			    try {
			        dialog.dismiss();
			        dialog = null;
			    } catch (Exception e) {
			    }
				if (error == null) {
					// Termina il composer.
					finish();
					// Mostra avviso.
					Toast.makeText(ComposeActivity.this, R.string.send_toast_success, Toast.LENGTH_SHORT).show();
				} else {
					// Mostra errore.
					if (error instanceof ServerProxy.UnknownRecipientException) {
						Toast.makeText(ComposeActivity.this, R.string.send_toast_unknown_recipient, Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(ComposeActivity.this, R.string.send_toast_error, Toast.LENGTH_SHORT).show();
					}
				}
			}

		};
		task.execute(serverBaseUrl, privateKey, senderPhoneNumber, recipientPhoneNumber, message);
	}

	private void saveSentMessage(String phoneNumber, String message) {
		long now = System.currentTimeMillis();
		ContentResolver r = getContentResolver();
		// Cerca se esiste un thread per l'interlocutore.
		long threadId = -1;
		Cursor c = r.query(MessagesContentProvider.THREADS_URI, new String[] { "_id" }, "phone_number = ?", new String[] { phoneNumber }, null);
		if (c.moveToFirst()) {
			threadId = c.getLong(0);
		}
		c.close();
		if (threadId == -1) {
			// Se non esiste, lo crea.
			ContentValues values = new ContentValues();
			values.put("phone_number", phoneNumber);
			values.put("unread", 0);
			values.put("last_update", now);
			Uri uri = r.insert(MessagesContentProvider.THREADS_URI, values);
			threadId = Long.parseLong(uri.getLastPathSegment());
		} else {
			// Se esiste, lo aggiorna.
			ContentValues values = new ContentValues();
			values.put("last_update", now);
			r.update(MessagesContentProvider.THREADS_URI, values, "phone_number = ?", new String[] { phoneNumber });
		}
		// Aggiunge il messaggio.
		ContentValues values = new ContentValues();
		values.put("thread", threadId);
		values.put("direction", 0);
		values.put("message", message);
		values.put("ts_sent", now);
		r.insert(MessagesContentProvider.MESSAGES_URI, values);
	}

}
