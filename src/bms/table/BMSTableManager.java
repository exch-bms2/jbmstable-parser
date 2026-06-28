package bms.table;

import java.util.*;

/**
 * 表管理用クラス
 * 
 * @author exch
 */
public class BMSTableManager {
	/**
	 * 表リスト
	 */
	private List<BMSTable<?>> tableList = new ArrayList<>();

	private List<BMSTableManagerListener> listener = new ArrayList<>();

	private Map<String, List<DifficultyTableElement>> userList = new HashMap<>();

	private Map<String, String> memoMap = new HashMap<>();

	public BMSTableManager() {
	}

	public void addListener(
			BMSTableManagerListener l) {
		listener.add(l);
	}

	public void fireModelChanged() {
		listener.forEach(BMSTableManagerListener::modelChanged);
	}

	/**
	 * 難易度表を追加する
	 * 
	 * @param dt
	 *            追加する難易度表
	 */
	public void addBMSTable(BMSTable<?> dt) {
		tableList.add(dt);
		this.fireModelChanged();
	}

	/**
	 * 難易度表を削除する
	 * 
	 * @param dt
	 *            削除する難易度表
	 */
	public void removeBMSTable(BMSTable<?> dt) {
		tableList.remove(dt);
		this.fireModelChanged();
	}

	/**
	 * 難易度表リストを取得する
	 * 
	 * @return 難易度表リスト
	 */
	public BMSTable<?>[] getBMSTables() {
		return tableList.toArray(new BMSTable<?>[0]);
	}

	public List<BMSTable<?>> getTableList() {
		return tableList;
	}

	public Map<String, List<DifficultyTableElement>> getUserList() {
		return userList;
	}
	
	public void setUserList(Map<String, List<DifficultyTableElement>> userList) {
		this.userList = userList;
	}
	
	public Map<String, String> getMemoMap() {
		return memoMap;
	}
	
	public void setMemoMap(Map<String, String> memoMap) {
		this.memoMap = memoMap;
	}
	
	public List<DifficultyTableElement> getUserDifficultyTableElements(String name) {
		return userList.computeIfAbsent(name, key -> new ArrayList<>());
	}

	public void setTableList(List<BMSTable<?>> tableList) {
		this.tableList = tableList;
	}

	public void clearAllTableElements() {
		for (BMSTable<?> table : tableList) {
			table.removeAllElements();
		}
	}
}
