package freem.android;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.TelephonyManager;

public class Utils {

	private static final Map<String, String> map = new HashMap<String, String>();

	static {
		map.put("af", "93");
		map.put("al", "355");
		map.put("dz", "213");
		map.put("ad", "376");
		map.put("ao", "244");
		map.put("aq", "672");
		map.put("ar", "54");
		map.put("am", "374");
		map.put("aw", "297");
		map.put("au", "61");
		map.put("at", "43");
		map.put("az", "994");
		map.put("bh", "973");
		map.put("bd", "880");
		map.put("by", "375");
		map.put("be", "32");
		map.put("bz", "501");
		map.put("bj", "229");
		map.put("bt", "975");
		map.put("bo", "591");
		map.put("ba", "387");
		map.put("bw", "267");
		map.put("br", "55");
		map.put("bn", "673");
		map.put("bg", "359");
		map.put("bf", "226");
		map.put("mm", "95");
		map.put("bi", "257");
		map.put("kh", "855");
		map.put("cm", "237");
		map.put("ca", "1");
		map.put("cv", "238");
		map.put("cf", "236");
		map.put("td", "235");
		map.put("cl", "56");
		map.put("cn", "86");
		map.put("cx", "61");
		map.put("cc", "61");
		map.put("co", "57");
		map.put("km", "269");
		map.put("cg", "242");
		map.put("cd", "243");
		map.put("ck", "682");
		map.put("cr", "506");
		map.put("hr", "385");
		map.put("cu", "53");
		map.put("cy", "357");
		map.put("cz", "420");
		map.put("dk", "45");
		map.put("dj", "253");
		map.put("tl", "670");
		map.put("ec", "593");
		map.put("eg", "20");
		map.put("sv", "503");
		map.put("gq", "240");
		map.put("er", "291");
		map.put("ee", "372");
		map.put("et", "251");
		map.put("fk", "500");
		map.put("fo", "298");
		map.put("fj", "679");
		map.put("fi", "358");
		map.put("fr", "33");
		map.put("pf", "689");
		map.put("ga", "241");
		map.put("gm", "220");
		map.put("ge", "995");
		map.put("de", "49");
		map.put("gh", "233");
		map.put("gi", "350");
		map.put("gr", "30");
		map.put("gl", "299");
		map.put("gt", "502");
		map.put("gn", "224");
		map.put("gw", "245");
		map.put("gy", "592");
		map.put("ht", "509");
		map.put("hn", "504");
		map.put("hk", "852");
		map.put("hu", "36");
		map.put("in", "91");
		map.put("id", "62");
		map.put("ir", "98");
		map.put("iq", "964");
		map.put("ie", "353");
		map.put("im", "44");
		map.put("il", "972");
		map.put("it", "39");
		map.put("ci", "225");
		map.put("jp", "81");
		map.put("jo", "962");
		map.put("kz", "7");
		map.put("ke", "254");
		map.put("ki", "686");
		map.put("kw", "965");
		map.put("kg", "996");
		map.put("la", "856");
		map.put("lv", "371");
		map.put("lb", "961");
		map.put("ls", "266");
		map.put("lr", "231");
		map.put("ly", "218");
		map.put("li", "423");
		map.put("lt", "370");
		map.put("lu", "352");
		map.put("mo", "853");
		map.put("mk", "389");
		map.put("mg", "261");
		map.put("mw", "265");
		map.put("my", "60");
		map.put("mv", "960");
		map.put("ml", "223");
		map.put("mt", "356");
		map.put("mh", "692");
		map.put("mr", "222");
		map.put("mu", "230");
		map.put("yt", "262");
		map.put("mx", "52");
		map.put("fm", "691");
		map.put("md", "373");
		map.put("mc", "377");
		map.put("mn", "976");
		map.put("me", "382");
		map.put("ma", "212");
		map.put("mz", "258");
		map.put("na", "264");
		map.put("nr", "674");
		map.put("np", "977");
		map.put("nl", "31");
		map.put("an", "599");
		map.put("nc", "687");
		map.put("nz", "64");
		map.put("ni", "505");
		map.put("ne", "227");
		map.put("ng", "234");
		map.put("nu", "683");
		map.put("kp", "850");
		map.put("no", "47");
		map.put("om", "968");
		map.put("pk", "92");
		map.put("pw", "680");
		map.put("pa", "507");
		map.put("pg", "675");
		map.put("py", "595");
		map.put("pe", "51");
		map.put("ph", "63");
		map.put("pn", "870");
		map.put("pl", "48");
		map.put("pt", "351");
		map.put("pr", "1");
		map.put("qa", "974");
		map.put("ro", "40");
		map.put("ru", "7");
		map.put("rw", "250");
		map.put("bl", "590");
		map.put("ws", "685");
		map.put("sm", "378");
		map.put("st", "239");
		map.put("sa", "966");
		map.put("sn", "221");
		map.put("rs", "381");
		map.put("sc", "248");
		map.put("sl", "232");
		map.put("sg", "65");
		map.put("sk", "421");
		map.put("si", "386");
		map.put("sb", "677");
		map.put("so", "252");
		map.put("za", "27");
		map.put("kr", "82");
		map.put("es", "34");
		map.put("lk", "94");
		map.put("sh", "290");
		map.put("pm", "508");
		map.put("sd", "249");
		map.put("sr", "597");
		map.put("sz", "268");
		map.put("se", "46");
		map.put("ch", "41");
		map.put("sy", "963");
		map.put("tw", "886");
		map.put("tj", "992");
		map.put("tz", "255");
		map.put("th", "66");
		map.put("tg", "228");
		map.put("tk", "690");
		map.put("to", "676");
		map.put("tn", "216");
		map.put("tr", "90");
		map.put("tm", "993");
		map.put("tv", "688");
		map.put("ae", "971");
		map.put("ug", "256");
		map.put("gb", "44");
		map.put("ua", "380");
		map.put("uy", "598");
		map.put("us", "1");
		map.put("uz", "998");
		map.put("vu", "678");
		map.put("va", "39");
		map.put("ve", "58");
		map.put("vn", "84");
		map.put("wf", "681");
		map.put("ye", "967");
		map.put("zm", "260");
		map.put("zw", "263");
	}

	public static String resolveCurrentICC(Context context) {
		TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String simCountryIso = manager.getSimCountryIso();
		String networkCountryIso = manager.getNetworkCountryIso();
		String phoneCountryIso = context.getResources().getConfiguration().locale.getCountry();
		String icc = null;
		if (simCountryIso != null && simCountryIso.length() > 0) {
			icc = map.get(simCountryIso.toLowerCase(Locale.getDefault()));
		} else if (networkCountryIso != null && networkCountryIso.length() > 0) {
			icc = map.get(networkCountryIso.toLowerCase(Locale.getDefault()));
		} else if (phoneCountryIso != null && phoneCountryIso.length() > 0) {
			icc = map.get(phoneCountryIso.toLowerCase(Locale.getDefault()));
		}
		if (icc != null) {
			return icc;
		} else {
			return "";
		}
	}

	public static String resolveContactDisplayName(Context context, String phoneNumber) {
		Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		Cursor c = context.getContentResolver().query(uri, new String[] { Contacts.DISPLAY_NAME }, null, null, null);
		String displayName;
		if (c.moveToFirst()) {
			displayName = c.getString(0);
		} else {
			displayName = null;
		}
		c.close();
		return displayName;
	}

}