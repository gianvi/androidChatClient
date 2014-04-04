package freem.android;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import freem.android.R;

public class MessagesActivity extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {

	private long mThreadId;

	private String mPhoneNumber;

	private String mDisplayName;

	private ListView mListView;

	private CursorAdapter mCursorAdapter;

	@Override
	@TargetApi(11)
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Recupera l'ID del thread da mostrare.
		Intent intent = getIntent();
		mThreadId = intent.getLongExtra("thread_id", 0);
		// Carica il thread dal database.
		Cursor c = getContentResolver().query(MessagesContentProvider.THREADS_URI, new String[] { "_id", "phone_number" }, "_id = ?", new String[] { String.valueOf(mThreadId) }, null);
		if (!c.moveToFirst()) {
			// thread non trovato!
			finish();
			return;
		}
		mPhoneNumber = c.getString(1);
		c.close();
		// Risolve, se possibile, il numero dell'interlocutore in rubrica.
		mDisplayName = Utils.resolveContactDisplayName(this, mPhoneNumber);
		// Imposta il titolo e la action bar dove disponibile.
		if (Build.VERSION.SDK_INT >= 11) {
			ActionBar actionBar = getActionBar();
			actionBar.setDisplayHomeAsUpEnabled(true);
			if (mDisplayName != null) {
				actionBar.setTitle(mDisplayName);
				actionBar.setSubtitle(mPhoneNumber);
			} else {
				actionBar.setTitle(mPhoneNumber);
				actionBar.setSubtitle(null);
			}
		} else {
			if (mDisplayName != null) {
				setTitle(mDisplayName + "( " + mPhoneNumber + ")");
			} else {
				setTitle(mPhoneNumber);
			}
		}
		// Crea l'adapter per la lista..
		mCursorAdapter = new MessagesCursorAdapter(this);
		// Carica il layout.
		setContentView(R.layout.activity_messages);
		mListView = (ListView) findViewById(R.id.listview);
		mListView.setAdapter(mCursorAdapter);
		// Carica i messaggi con un loader asincrono.
		getSupportLoaderManager().initLoader(1, null, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_messages, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			finish();
			return true;
		}
		if (id == R.id.compose) {
			Intent intent = new Intent(this, ComposeActivity.class);
			intent.putExtra("phone_number", mPhoneNumber);
			startActivity(intent);
			return true;
		}
		return false;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(this, MessagesContentProvider.MESSAGES_URI, new String[] { "_id", "direction", "message", "ts_sent" }, "thread = ?", new String[] { String.valueOf(mThreadId) }, "ts_sent ASC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mCursorAdapter.swapCursor(cursor);
		// Esegue una update che imposta a zero i messaggi non letti per il thread.
		ContentValues values = new ContentValues();
		values.put("unread", 0);
		getContentResolver().update(MessagesContentProvider.THREADS_URI, values, "_id = ?", new String[] { String.valueOf(mThreadId) });
		// Scrolla la lista fino in fondo.
		if (mListView.getCount() > 0) {
			mListView.setSelection(mListView.getCount() - 1);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mCursorAdapter.swapCursor(null);
	}

	private class MessagesCursorAdapter extends CursorAdapter {

		public MessagesCursorAdapter(Context context) {
			super(context, null, FLAG_REGISTER_CONTENT_OBSERVER);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return new FrameLayout(context);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// Estre i dati dal cursore.
			boolean sent = cursor.getInt(1) == 0;
			String message = cursor.getString(2);
			long sentTimestamp = cursor.getLong(3);
			// Impagina i dati nella view
			int res = sent ? R.layout.list_element_message_sent : R.layout.list_element_message_received;
			View messageView = getLayoutInflater().inflate(res, null);
			TextView tvMessage = (TextView) messageView.findViewById(R.id.message);
			TextView tvTimestamp = (TextView) messageView.findViewById(R.id.timestamp);
			tvMessage.setText(message);
			tvTimestamp.setText(DateFormat.format("yyyy/MM/dd k:mm", sentTimestamp));
			FrameLayout frame = (FrameLayout) view;
			frame.removeAllViews();
			frame.addView(messageView);
		}
	}

}
