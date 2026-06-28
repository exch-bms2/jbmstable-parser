package bms.table;

import java.io.Serializable;
import java.util.*;

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
		super(sourceURL);
	}

	public DifficultyTableElement[] getElements() {
		DifficultyTableElement[] dte = this.getModels().toArray(new DifficultyTableElement[0]);
		Arrays.sort(dte, (dte1, dte2) -> {
			int c = indexOf(dte1.getLevel()) - indexOf(dte2.getLevel());
			if (c == 0) {
				return dte1.getTitle().compareToIgnoreCase(dte2.getTitle());
			}
			return c;
		});
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
		Object value = this.getValues().get(LEVEL_ORDER);
		if (value instanceof List<?> l) {
			String[] levels = new String[l.size()];
			for (int i = 0; i < levels.length; i++) {
				levels[i] = l.get(i).toString();
			}
			return levels;
		}
		return new String[0];
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
