package bms.table;

import java.util.*;

/**
 * 表
 * 
 * @author exch
 */
public abstract class BMSTable<T> {
	
	private Map<String, Object> values = new HashMap<String, Object>();

	/**
	 * 表の名称
	 */
	public static final String NAME = "name";
	/**
	 * 難易度表のマーク
	 */	
	public static final String SYMBOL = "symbol";
	/**
	 * 難易度表のタグ付加時のマーク
	 */	
	public static final String TAG = "tag";
	/**
	 * 難易度表のデータURL
	 */
	public static final String DATA_URL = "data_url";
	/**
	 * 表固有の属性値-名称のマップ
	 */
	public static final String ATTR = "attr";
	/**
	 * モード
	 */
	public static final String MODE = "mode";
	
	/**
	 * 難易度表のソースURL
	 */
	private String sourceURL = "";
	/**
	 * 難易度表のヘッダURL
	 */
	private String headURL = "";
	/**
	 * 難易度表のデータURL
	 */
	private String[] dataURL = new String[0];
	
	private boolean autoUpdate = true;
	
	/**
	 * 統合時のレベルマッピング.。key:元のレベル表記-value:統合時のレベル表記に変換する。
	 * value=""の場合、そのレベルは統合時に除外する。nullの場合は元のレベル表記=統合時のレベル表記とする。
	 */
	Map<String, Map<String, String>> mergeConfigurations = new HashMap<String, Map<String, String>>();

	/**
	 * 最終更新時間(ms)。終了時に保存しない
	 */
	private long lastupdate = 0;
	/**
	 * 表の要素
	 */
	private List<T> models = new ArrayList<T>();
	
	private boolean editable = false;
	/**
	 * アクセス回数
	 */
	private int accessCount = 0;
	
	public String getName() {
		return (String)values.get(NAME);
	}

	public void setName(String name) {
		values.put(NAME, name);
	}

	public String getID() {
		return (String)values.get(SYMBOL);
	}

	public void setID(String id) {
		values.put(SYMBOL, id);
	}

	public String[] getDataURL() {
		return dataURL;
	}

	public void setDataURL(String[] datas) {
		this.dataURL = datas;
	}
	
	public Map<String, Map<String, String>> getMergeConfigurations() {
		return mergeConfigurations;
	}

	public void setMergeConfigurations(
			Map<String, Map<String, String>> mergeConfigurations) {
		this.mergeConfigurations = mergeConfigurations;
	}


	public String getTag() {
		if(values.containsKey(TAG)) {
			return (String)values.get(TAG);
		}
		return getID();
	}

	public void setTag(String tag) {
		values.put(TAG, tag);
	}

	public String getSourceURL() {
		return sourceURL;
	}

	public void setSourceURL(String sourceURL) {
		this.sourceURL = sourceURL;
	}
	
	public List<T> getModels() {
		return models;
	}

	public void setModels(List<T> models) {
		this.models.clear();
		this.models.addAll(models);
	}

	public void addElement(T dte) {
		models.add(dte);
		lastupdate = System.currentTimeMillis();
	}

	public void removeElement(T dte) {
		models.remove(dte);
	}

	public void removeAllElements() {
		models.clear();
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public Map<String, String> getAttrmap() {
		if(values.containsKey(ATTR)) {
			return (Map<String, String>) values.get(ATTR);
		}
		return new HashMap<String, String>();
	}

	public void setAttrmap(Map<String, String> attrmap) {
		values.put(ATTR, attrmap);
	}

	public String getHeadURL() {
		return headURL;
	}

	public void setHeadURL(String headURL) {
		this.headURL = headURL;
	}

	public long getLastupdate() {
		return lastupdate;
	}

	public int getAccessCount() {
		return accessCount;
	}

	public void setAccessCount(int accessCount) {
		this.accessCount = accessCount;
	}

	public boolean isAutoUpdate() {
		return autoUpdate;
	}

	public void setAutoUpdate(boolean autoupdate) {
		this.autoUpdate = autoupdate;
	}
	
	public Map<String, Object> getValues() {
		return values;
	}

	public void setValues(Map<String, Object> values) {
		this.values.clear();
		this.values.putAll(values);
	}

	public String getMode() {
		return (String)values.get(MODE);
	}

	public void setMode(String mode) {
		values.put(MODE, mode);
	}
}
