package bms.table;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Logger;

/**
 * 難易度表
 * 
 * @author exch
 */
public class DifficultyTable extends BMSTable<DifficultyTableElement> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2757817491532398378L;
	/**
	 * レベル表記
	 */
	public static final String LEVEL_ORDER = "level_order";
	/**
	 * コース定義
	 */
	private Course[][] course = new Course[0][0];

	public DifficultyTable() {
		super();
	}

	public DifficultyTable(String sourceURL) {
		super();
		this.setSourceURL(sourceURL);
	}

	public DifficultyTableElement[] getElements() {
		DifficultyTableElement[] dte = this.getModels().toArray(new DifficultyTableElement[0]);
		Comparator asc = new Comparator() {
			public int compare(Object o1, Object o2) {
				DifficultyTableElement dte1 = (DifficultyTableElement) o1;
				DifficultyTableElement dte2 = (DifficultyTableElement) o2;
				int c = indexOf(dte1.getLevel()) - indexOf(dte2.getLevel());
				if (c == 0) {
					return dte1.getTitle().compareToIgnoreCase(dte2.getTitle());
				}
				return c;
			}
		};
		//Arrays.sort(dte, asc);
		return dte;
	}

	private int indexOf(String level) {
		for (int i = 0; i < getLevelDescription().length; i++) {
			if (getLevelDescription()[i].equals(level)) {
				return i;
			}
		}
		return -1;
	}

	public String[] getLevelDescription() {
		List l = (List) this.getValues().get(LEVEL_ORDER);
		if (l != null) {
			String[] levels = new String[l.size()];
			for (int i = 0; i < levels.length; i++) {
				levels[i] = l.get(i).toString();
			}
			return levels;
		}
		return new String[] {};
	}

	public void setLevelDescription(String[] levelDescription) {
		this.getValues().put(LEVEL_ORDER, Arrays.asList(levelDescription));
	}

	public Course[][] getCourse() {
		return course;
	}

	public void setCourse(Course[][] course) {
		this.course = course;
	}
}
