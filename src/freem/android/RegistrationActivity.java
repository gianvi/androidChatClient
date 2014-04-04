package freem.android;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import freem.android.R;

public class RegistrationActivity extends Activity {

	private EditText mServerAddress;

	private EditText mPhoneNumber;

	private Button mRegister;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		String currentSimNumber = telephonyManager.getLine1Number();
		setContentView(R.layout.activity_registration);
		mServerAddress = (EditText) findViewById(R.id.server_address);
		mPhoneNumber = (EditText) findViewById(R.id.phone_number);
		mRegister = (Button) findViewById(R.id.register);
		mServerAddress.setText(Config.SERVER_DEFAULT_BASE_URL);
		if (currentSimNumber != null && currentSimNumber.length() > 0) {
			if (!currentSimNumber.startsWith("+")) {
				currentSimNumber = "+" + Utils.resolveCurrentICC(this);
			}
			mPhoneNumber.setText(currentSimNumber);
		} else {
			mPhoneNumber.setText("+" + Utils.resolveCurrentICC(this));
		}
		mRegister.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				registerAsync();
			}
		});
	}

	private void registerAsync() {
		// Recupera i dati e li valida.
		String serverAddress = mServerAddress.getText().toString().trim();
		if (serverAddress.length() == 0) {
			Toast.makeText(this, getString(R.string.registration_toast_missing_server_address), Toast.LENGTH_SHORT).show();
			return;
		}
		if (serverAddress.charAt(serverAddress.length() - 1) != '/') {
			serverAddress += "/";
		}
		final URL serverBaseUrl;
		try {
			serverBaseUrl = new URL(serverAddress);
		} catch (MalformedURLException e) {
			Toast.makeText(this, getString(R.string.registration_toast_invalid_server_address), Toast.LENGTH_SHORT).show();
			return;
		}
		final String phoneNumber = mPhoneNumber.getText().toString().trim();
		if (phoneNumber.length() == 0) {
			Toast.makeText(this, getString(R.string.registration_toast_missing_phone_number), Toast.LENGTH_SHORT).show();
			return;
		}
		if (!Pattern.matches("^\\+\\d{5,19}$", phoneNumber)) {
			Toast.makeText(this, getString(R.string.registration_toast_invalid_phone_number), Toast.LENGTH_SHORT).show();
			return;
		}
		// Lancia il task asincrono di registrazione.
		AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

			private ProgressDialog dialog;

			@Override
			protected void onPreExecute() {
				dialog = new ProgressDialog(RegistrationActivity.this);
				dialog.setTitle(getString(R.string.registration_dialog_title));
				dialog.setMessage(getString(R.string.registration_dialog_message));
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
			protected Boolean doInBackground(Void... params) {
				return registerRoutine(serverBaseUrl, phoneNumber);
			}

			@Override
			protected void onPostExecute(Boolean result) {
			    try {
			        dialog.dismiss();
			        dialog = null;
			    } catch (Exception e) {
			    }
				if (result == true) {
					// Termina l'activity di registrazione e rilancia quella principale.
					finish();
					startActivity(new Intent(RegistrationActivity.this, ThreadsActivity.class));
				} else {
					// Mostra errore.
					Toast.makeText(RegistrationActivity.this, R.string.registration_toast_error, Toast.LENGTH_SHORT).show();
				}
			}
		};
		task.execute();
	}

	private boolean registerRoutine(URL serverBaseUrl, String phoneNumber) {
		// Recupera le preferenze condivise.
		SharedPreferences prefs = getSharedPreferences("registration", MODE_PRIVATE);
		String deviceId = prefs.getString("gcm_id", null);
		if (deviceId == null) {
			GCMRegistrar.checkDevice(this);
			GCMRegistrar.checkManifest(this);
			GCMRegistrar.register(this, Config.GCM_SENDER_ID);
		}
		// Controlla la registrazione a GCM.
		while (deviceId == null) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				return false;
			}
			deviceId = prefs.getString("gcm_id", null);
		}
		// Esegue la chiamata al server.
		String privateKey;
		try {
			privateKey = ServerProxy.register(serverBaseUrl, phoneNumber, deviceId);
		} catch (IOException e) {
			Log.e("freem", "Errore in fase di registrazione con il server", e);
			return false;
		}
		// Salva sulle preferenze condivise.
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("registered", true);
		editor.putString("server_address", serverBaseUrl.toExternalForm());
		editor.putString("phone_number", phoneNumber);
		editor.putString("private_key", privateKey);
		editor.commit();
		// Restituisce avviso di successo.
		return true;
	}

}
