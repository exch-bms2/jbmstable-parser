package bms.table;

import java.io.Serializable;
import java.util.Map;


/**
 * 難易度表の要素
 *
 * @author exch
 */
public class DifficultyTableElement extends BMSTableElement implements
		Serializable {

	/**
	 * 譜面の状態
	 */
	private int state = 0;
	/**
	 * 譜面の状態:新規追加
	 */
	public static final int STATE_NEW = 1;
	/**
	 * 譜面の状態:難易度更新
	 */
	public static final int STATE_UPDATE = 2;
	/**
	 * 譜面の状態:投票中
	 */
	public static final int STATE_VOTE = 3;
	/**
	 * 譜面の状態:おすすめ
	 */
	public static final int STATE_RECOMMEND = 4;
	/**
	 * 譜面の状態:削除
	 */
	public static final int STATE_DELETE = 5;
	/**
	 * 譜面の状態:復活
	 */
	public static final int STATE_REVIVE = 6;

	/**
	 * 譜面評価
	 */
	private int eval = 0;
	/**
	 * レベル表記
	 */
	private String level = "";
	/**
	 * 差分作者名
	 */
	private String diffname = "";
	/**
	 * コメント
	 */
	private String comment = "";
	/**
	 * 譜面情報
	 */
	private String info = "";

	private String proposer = "";

	public DifficultyTableElement() {
	}

	public DifficultyTableElement(String did, String title, int bmsid,
			String url1, String url2, String comment, String hash,String ipfs) {
		this.setLevel(did);
		this.setTitle(title);
		this.setBMSID(bmsid);
		this.setURL(url1);
		this.setAppendURL(url2);
		this.setComment(comment);
		this.setMD5(hash);
		this.setIPFS(ipfs);
	}

	public int getState() {
		return state;
	}

	public void setState(int id) {
		state = id;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String did) {
		if(did == null) {
			level = "";
		} else {
			level = did;
		}
	}

	public int getEvaluation() {
		return eval;
	}

	public void setEvaluation(int eval) {
		this.eval = eval;
	}

	public String getPackageURL() {
		return (String) getValues().get("url_pack");
	}

	public void setPackageURL(String url1sub) {
		getValues().put("url_pack", url1sub);
	}

	public String getPackageName() {
		return (String) getValues().get("name_pack");
	}

	public void setPackageName(String url1subname) {
		getValues().put("name_pack", url1subname);
	}

	public String getAppendURL() {
		return (String) getValues().get("url_diff");
	}

	public void setAppendURL(String url2) {
		getValues().put("url_diff", url2);
	}

	public String getAppendIPFS() {
		return (String)getValues().get("ipfs_diff");
	}

	public void setAppendIPFS(String ipfs2) {
		getValues().put("ipfs_diff", ipfs2);
	}

	public String getAppendArtist() {
		return diffname;
	}

	public void setAppendArtist(String url2name) {
		diffname = url2name;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment1) {
		comment = comment1;
	}

	public String getInformation() {
		return info;
	}

	public void setInformation(String comment2) {
		info = comment2;
	}

	public String getProposer() {
		return proposer;
	}

	public void setProposer(String proposer) {
		this.proposer = proposer;
	}

	public int getBMSID() {
		int result = 0;
		try {
			result = Integer.parseInt(String.valueOf(getValues().get(
					"lr2_bmsid")));
		} catch (NumberFormatException e) {

		}
		return result;
	}

	public void setBMSID(int bmsid) {
		getValues().put("lr2_bmsid", bmsid);
	}

	@Override
	public void setValues(Map<String, Object> values) {
		super.setValues(values);
		int statevalue = 0;
/*		try {
			statevalue = Integer.parseInt(String.valueOf(values.get("state")));
		} catch (NumberFormatException e) {

		}*/
		state = statevalue;

		int evalvalue = 0;
/*		try {
			evalvalue = Integer.parseInt(String.valueOf(values.get("eval")));
		} catch (NumberFormatException e) {

		}*/
		eval = evalvalue;

		Object level = values.get("level");
		setLevel(level != null ? level.toString() : "");
		Object diffname = values.get("name_diff");
		setAppendArtist(diffname != null ? diffname.toString() : "");
		Object comment = values.get("comment");
		setComment(comment != null ? comment.toString() : "");
		Object info = values.get("tag");
		setInformation(info != null ? info.toString() : "");
		Object proposer = values.get("proposer");
		setProposer(proposer != null ? proposer.toString() : "");
	}

	@Override
	public Map<String, Object> getValues() {
		Map<String, Object> result = super.getValues();
		result.put("level", getLevel());
		result.put("eval", getEvaluation());
		result.put("state", getState());
		result.put("name_diff", getAppendArtist());
		result.put("comment", getComment());
		result.put("tag", getInformation());
		if(getProposer() != null && getProposer().length() > 0) {
			result.put("proposer", getProposer());
		} else {
			result.remove("proposer");
		}
		return result;
	}

	// public String makeTable(String d) {
	// String s = "[" + id + ",\"" + d + did + "\",\n";
	// s += "\"" + this.getTitle() + "\",\n";
	// s += "\"" + bmsid + "\",\n";
	// if (this.getURL1() != null && this.getURL1().length() > 0) {
	// s += "\"<a href='" + this.getURL1() + "'>" + this.getURL1name()
	// + "</a>";
	// } else {
	// s += "\"" + this.getURL1name();
	// }
	// if (url1sub != null && url1sub.length() > 0) {
	// s += "<br />(<a href='" + url1sub + "'>" + url1subname
	// + "</a>)\",\n";
	// } else {
	// s += "\",\n";
	// }
	//
	// if (url2 != null && url2.length() > 0) {
	// s += "\"<a href='" + url2 + "'>" + url2name + "</a>\",\n";
	// } else {
	// s += "\"" + url2name + "\",\n";
	// }
	// s += "\"" + comment1 + "\",\n";
	// s += "],\n";
	//
	// return s;
	// }

}
