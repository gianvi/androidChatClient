package freem.android;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.util.Log;

import com.google.gson.Gson;

public class ServerProxy {

	static {
		TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					public void checkClientTrusted(X509Certificate[] certs, String authType) {
					}

					public void checkServerTrusted(X509Certificate[] certs, String authType) {
					}
				}
		};
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Throwable t) {
		}
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}

	private static <T> T call(URL serverBaseUrl, String endpoint, Map<String, String> params, Class<T> responseClass) throws IOException {
		T response = null;
		URL url = new URL(serverBaseUrl, endpoint);
		BufferedWriter writer = null;
		BufferedReader reader = null;
		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);
			// Invia i parametri attraverso il writer.
			writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
			int paramCounter = 0;
			for (Map.Entry<String, String> entry : params.entrySet()) {
				if (paramCounter > 0) {
					writer.write('&');
				}
				writer.write(URLEncoder.encode(entry.getKey(), "UTF-8"));
				writer.write('=');
				writer.write(URLEncoder.encode(entry.getValue(), "UTF-8"));
				paramCounter++;
			}
			writer.flush();
			writer.close();
			writer = null;
			// Legge la risposta del server.
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			Gson gson = new Gson();
			response = gson.fromJson(reader, responseClass);
			reader.close();
			reader = null;
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Throwable t) {
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (Throwable t) {
				}
			}
		}
		return response;
	}

	public static String register(URL serverBaseUrl, String phoneNumber, String deviceId) throws IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("phone_number", phoneNumber);
		params.put("device_type", "Android");
		params.put("device_id", deviceId);
		RegisterResponse response = call(serverBaseUrl, "register.php", params, RegisterResponse.class);
		if (response.code != 0) {
			throw new IOException("errore server: " + response.code + " " + response.description);
		}
		if (response.result == null || response.result.private_key == null) {
			throw new IOException("errore server: risposta non valida");
		}
		return response.result.private_key;
	}

	public static void send(URL serverBaseUrl, String privateKey, String senderPhoneNumber, String recipientPhoneNumber, String message) throws IOException, UnknownRecipientException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("private_key", privateKey);
		params.put("phone_number", senderPhoneNumber);
		params.put("recipient", recipientPhoneNumber);
		params.put("message", message);
		SendResponse response = call(serverBaseUrl, "send.php", params, SendResponse.class);
		if (response.code == 4) {
			throw new UnknownRecipientException();
		}
		if (response.code != 0) {
			throw new IOException("errore server: " + response.code + " " + response.description);
		}
	}

	public static Message[] retrieve(URL serverBaseUrl, String privateKey, String phoneNumber) throws IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("private_key", privateKey);
		params.put("phone_number", phoneNumber);
		RetrieveResponse response = call(serverBaseUrl, "retrieve.php", params, RetrieveResponse.class);
		if (response.code != 0) {
			throw new IOException("errore server: " + response.code + " " + response.description);
		}
		if (response.result == null || response.result.messages == null) {
			return new Message[0];
		}
		int size = response.result.messages.length;
		Message[] ret = new Message[size];
		for (int i = 0; i < size; i++) {
			ret[i] = new Message();
			ret[i].id = response.result.messages[i].id;
			ret[i].senderPhoneNumber = response.result.messages[i].sender;
			ret[i].recipientPhoneNumber = response.result.messages[i].recipient;
			ret[i].text = response.result.messages[i].message;
			try {
				ret[i].sentTimestamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z", Locale.US).parse(response.result.messages[i].ts_sent).getTime();
			} catch (Exception e) {
				Log.w("freem", "formato timestamp non valido in " + response.result.messages[i].ts_sent, e);
				ret[i].sentTimestamp = System.currentTimeMillis();
			}
		}
		return ret;
	}

	private static abstract class Response {
		public int code = -1;
		public String description = null;
	}

	private static class RegisterResult {
		public String private_key = null;
	}

	private static class RegisterResponse extends Response {
		public RegisterResult result;
	}

	private static class SendResponse extends Response {
	}

	private static class RetrieveResult_Message {
		public String id;
		public String sender;
		public String recipient;
		public String message;
		public String ts_sent;
	}

	private static class RetrieveResult {
		public RetrieveResult_Message[] messages;
	}

	private static class RetrieveResponse extends Response {
		public RetrieveResult result;
	}

	public static class UnknownRecipientException extends Exception {

		private static final long serialVersionUID = 1L;

	}

	public static class Message {
		public String id;
		public String senderPhoneNumber;
		public String recipientPhoneNumber;
		public String text;
		public long sentTimestamp;
	}

}
