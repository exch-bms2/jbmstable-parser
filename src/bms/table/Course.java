package bms.table;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Course {

	/**
	 * コース名
	 */
	private String name = "新規段位";
	/**
	 * 段位を構成する譜面のハッシュ値
	 */
	private String[] hashes = new String[0];
	/**
	 * 段位名のスタイル
	 */
	private String style = "";
	/**
	 * 制限
	 */
	private String[] constraint = {};
	/**
	 * トロフィー
	 */
	private Trophy[] trophy = {};
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String[] getHash() {
		return hashes;
	}

	public void setHash(String[] hashes) {
		this.hashes = hashes;
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	public String[] getConstraint() {
		return constraint;
	}

	public void setConstraint(String[] constraint) {
		this.constraint = constraint;
	}

	public Trophy[] getTrophy() {
		return trophy;
	}

	public void setTrophy(Trophy[] trophy) {
		this.trophy = trophy;
	}

	public static class Trophy {
		
		private String name = "新規トロフィー";
		/**
		 * トロフィーのスタイル
		 */
		private String style = "";
		
		private double scorerate = 0;
		
		private double missrate = 100;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getStyle() {
			return style;
		}

		public void setStyle(String style) {
			this.style = style;
		}

		public double getScorerate() {
			return scorerate;
		}

		public void setScorerate(double scorerate) {
			this.scorerate = scorerate;
		}

		public double getMissrate() {
			return missrate;
		}

		public void setMissrate(double missrate) {
			this.missrate = missrate;
		}
		
	}
}
