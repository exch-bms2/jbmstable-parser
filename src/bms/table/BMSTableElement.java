package bms.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表の要素
 *
 * @author exch
 */
public abstract class BMSTableElement {

	private Map<String, Object> values = new HashMap<String, Object>();

	/**
	 * タイトル
	 */
	public static final String TITLE = "title";
	/**
	 * アーティスト
	 */
	public static final String ARTIST = "artist";
	/**
	 * MD5
	 */
	public static final String MD5 = "md5";
	/**
	 * SHA256
	 */
	public static final String SHA256 = "sha256";
	/**
	 * モード
	 */
	public static final String MODE = "mode";

	public BMSTableElement() {
	}

	public String getTitle() {
		return (String) values.get(TITLE);
	}

	public void setTitle(String title) {
		values.put(TITLE, title);
	}

	public String getURL() {
		return (String)values.get("url");
	}

	public void setURL(String url1) {
		values.put("url", url1);
	}

	public String getIPFS() {
		return (String)values.get("ipfs");
	}

	public void setIPFS(String ipfs1) {
		values.put("ipfs", ipfs1);
	}

	public String getArtist() {
		return (String) values.get(ARTIST);
	}

	public void setArtist(String url1name) {
		values.put(ARTIST, url1name);
	}

	public String getMD5() {
		return (String) values.get(MD5);
	}

	public void setMD5(String hash) {
		values.put(MD5, hash);
	}

	public String getSHA256() {
		return (String) values.get(SHA256);
	}

	public void setSHA256(String hash) {
		values.put(SHA256, hash);
	}

	public String getMode() {
		return (String) values.get(MODE);
	}

	public void setMode(String mode) {
		values.put(MODE, mode);
	}

	public List<String> getParentHash() {
		Object o = values.get("org_md5");
		if(o instanceof String) {
			List<String> result = new ArrayList<String>();
			result.add((String) o);
			return result;
		}
		if(o instanceof List) {
			return (List<String>) o;
		}
		return null;
	}

	public void setParentHash(List<String> hashes) {
		if(hashes == null || hashes.size() == 0) {
			values.remove("org_md5");
		} else {
			values.put("org_md5", hashes);
		}
	}

	public Map<String, Object> getValues() {
		return values;
	}

	public void setValues(Map<String, Object> values) {
		this.values.clear();
		this.values.putAll(values);
	}
}
