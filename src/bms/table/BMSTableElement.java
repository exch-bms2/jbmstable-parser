package bms.table;

import java.util.*;

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
	
	public BMSTableElement() {
	}
	
	public String getTitle() {
		return (String) values.get(TITLE);
	}

	public void setTitle(String title) {
		values.put(TITLE, title);
	}
	
	public String getURL1() {
		return (String)values.get("url");
	}

	public void setURL1(String url1) {
		values.put("url", url1);
	}

	public String getURL1name() {
		return (String) values.get(ARTIST);
	}

	public void setURL1name(String url1name) {
		values.put(ARTIST, url1name);
	}

	public String getHash() {
		return (String) values.get(MD5);
	}

	public void setHash(String hash) {
		values.put(MD5, hash);
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
