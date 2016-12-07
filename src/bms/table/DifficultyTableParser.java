package bms.table;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import bms.table.Course.Trophy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * 難易度表パーサ
 * 
 * @author exch
 */
public class DifficultyTableParser {

	// TODO bug:HTTP RequestPropertyを指定しないと403を返すサイトへの対応(JSONは一度byteデータで読む必要あり)

	/**
	 * 難易度表データ
	 */
	private Map<String, String[]> data = new HashMap<String, String[]>();

	public DifficultyTableParser() {
	}

	/**
	 * 難易度表ヘッダを含んでいるかどうかを判定する
	 * 
	 * @return 難易度表ヘッダを含んでいればtrue
	 */
	public boolean containsHeader(String urlname) {
		return getMetaTag(urlname, "bmstable") != null;
	}

	/**
	 * 難易度表ヘッダを含んでいるかどうかを判定する
	 * 
	 * @return 難易度表ヘッダを含んでいればtrue
	 */
	public String getAlternateBMSTableURL(String urlname) {
		return getMetaTag(urlname, "bmstable-alt");
	}

	private String[] readAllLines(String urlname) {
		String[] result = null;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(urlname).openStream()))) {
			List<String> l = new ArrayList();
			String line = null;
			while ((line = br.readLine()) != null) {
				l.add(line);
			}
			result = l.toArray(new String[l.size()]);
		} catch (IOException e) {
			Logger.getGlobal().severe("難易度表サイト解析中の例外:" + e.getMessage());
		}
		return result;
	}

	private String getMetaTag(String urlname, String name) {
		if (data.get(urlname) == null) {
			data.put(urlname, readAllLines(urlname));
		}
		if (data.get(urlname) == null) {
			return null;
		}
		for (String line : data.get(urlname)) {
			// 難易度表ヘッダ
			if (line.toLowerCase().contains("<meta name=\"" + name + "\"")) {
				Pattern p = Pattern.compile("\"");
				return p.split(line)[3];
			}
		}
		return null;
	}

	/**
	 * 難易度表ページをデコードし、反映する
	 * 
	 * @param b
	 *            譜面データも取り込むかどうか。設定項目のみを取り出したい場合はfalseとする
	 * @param diff
	 *            難易度表の情報(名称、記号、タグ)
	 * @throws IOException
	 */
	public void decode(boolean b, DifficultyTable diff) throws IOException {
		String urlname = diff.getSourceURL();
		String tableurl = null;
		String enc = null;
		if (urlname == null || urlname.length() == 0) {
			tableurl = diff.getHeadURL();
		} else {
			if (data.get(urlname) == null) {
				data.put(urlname, readAllLines(urlname));
			}
			if (data.get(urlname) == null) {
				throw new IOException();
			}
			Pattern p = Pattern.compile("\"");
			for (String line : data.get(urlname)) {
				// 文字エンコード
				if (line.toLowerCase().contains("<meta http-equiv=\"content-type\"")) {
					String str = p.split(line)[3];
					enc = str.substring(str.indexOf("charset=") + 8);
				}
				// 難易度表ヘッダ
				if (line.toLowerCase().contains("<meta name=\"bmstable\"")) {
					tableurl = p.split(line)[3];
				}
			}
		}
		// 難易度表ヘッダ(JSON)がある場合
		if (tableurl != null) {
			this.decodeJSONTable(diff, new URL(this.getAbsoluteURL(urlname, tableurl)), b);
			diff.setSourceURL(urlname);
		} else {
			// 難易度表ヘッダ(JSON)がない場合は、IRmemo用難易度表パーサに移行
			// ただしcontainsHeaderでヘッダの存在を確認してからdecodeを実行するため、ここには到達しない
			// エンコード不明の場合はreturn
			if (enc != null) {
				// エンコード表記の統一
				if (enc.toUpperCase().equals("UTF-8")) {
					enc = enc.toUpperCase();
				}
				if (enc.toUpperCase().equals("SHIFT_JIS")) {
					enc = "Shift_JIS";
				}
				// this.parseDifficultyTable(diff, diff.getID(),
				// new InputStreamReader(new
				// ByteArrayInputStream(data.get(urlname)), enc), b);
			}
		}
	}

	private String getAbsoluteURL(String source, String path) {
		// DataURL相対パス対応
		String urldir = source.substring(0, source.lastIndexOf('/') + 1);
		if (!path.startsWith("http") && !path.startsWith(urldir)) {
			if (path.startsWith("./")) {
				path = path.substring(2);
			}
			return urldir + path;
		}
		return path;
	}

	/**
	 * 難易度表JSONページをデコードし、反映する
	 * 
	 * @param jsonheader
	 *            難易度表JSONヘッダURL
	 * @param saveElements
	 *            譜面データも取り込むかどうか。設定項目のみを取り出したい場合はfalseとする
	 */
	public void decodeJSONTable(DifficultyTable dt, URL jsonheader, boolean saveElements) throws IOException {
		// 難易度表ヘッダ(JSON)読み込み
		this.decodeJSONTableHeader(dt, jsonheader);
		String[] urls = dt.getDataURL();
		if (saveElements) {
			dt.removeAllElements();
			List<DifficultyTableElement> elements = new ArrayList();
			List<String> levels = new ArrayList();
			for (String url : urls) {
				Map<String, String> conf = dt.getMergeConfigurations().get(url);
				if (conf == null) {
					conf = new HashMap();
				}
				DifficultyTable table = new DifficultyTable();

				this.decodeJSONTableData(
						table,
						new URL(this.getAbsoluteURL(
								(dt.getSourceURL() == null || dt.getSourceURL().length() == 0) ? dt.getHeadURL() : this
										.getAbsoluteURL(dt.getSourceURL(), dt.getHeadURL()), url)));
				levels.addAll(Arrays.asList(table.getLevelDescription()));
				// 重複BMSの処理
				for (DifficultyTableElement dte : table.getElements()) {
					if (conf.get(dte.getDifficultyID()) == null || conf.get(dte.getDifficultyID()).length() > 0) {
						boolean contains = false;
						for (DifficultyTableElement dte2 : elements) {
							if ((dte.getMD5() != null && dte.getMD5().length() > 10
									&& dte.getMD5().equals(dte2.getMD5()) || (dte.getSHA256() != null
									&& dte.getSHA256().length() > 10 && dte.getSHA256().equals(dte2.getSHA256())))) {
								contains = true;
								break;
							}
						}
						if (!contains) {
							if (conf.get(dte.getDifficultyID()) != null) {
								dte.setDifficultyID(conf.get(dte.getDifficultyID()));
							}
							elements.add(dte);
						}
					}
				}
			}
			if (dt.getLevelDescription().length == 0) {
				dt.setLevelDescription(levels.toArray(new String[0]));
			}
			dt.setModels(elements);
		}
	}

	/**
	 * JSONヘッダ部をデコードして指定のDifficultyTableオブジェクトに反映する
	 * 
	 * @param dt
	 *            反映するDifficultyTableオブジェクト
	 * @param jsonheader
	 *            JSONヘッダ部ファイル
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public void decodeJSONTableHeader(DifficultyTable dt, File jsonheader) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		this.decodeJSONTableHeader(dt, mapper.readValue(jsonheader, Map.class));
	}

	/**
	 * JSONヘッダ部をデコードして指定のDifficultyTableオブジェクトに反映する
	 * 
	 * @param dt
	 *            反映するDifficultyTableオブジェクト
	 * @param jsonheader
	 *            JSONヘッダ部URL
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public void decodeJSONTableHeader(DifficultyTable dt, URL jsonheader) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		this.decodeJSONTableHeader(dt, mapper.readValue(jsonheader, Map.class));
		dt.setHeadURL(jsonheader.toExternalForm());
	}

	private DifficultyTable decodeJSONTableHeader(DifficultyTable dt, Map<String, Object> result) throws IOException {
		dt.setValues(result);
		// level_order処理
		Object dataurl = result.get("data_url");
		if (dataurl instanceof String) {
			dt.setDataURL(new String[] { (String) dataurl });
		}
		if (dataurl instanceof List) {
			dt.setDataURL((String[]) ((List) dataurl).toArray(new String[0]));
		}
		Map<String, Map<String, String>> mergerule = new HashMap();
		List<Map<String, String>> merge = (List<Map<String, String>>) result.get("data_rule");
		if (merge == null) {
			merge = new ArrayList();
		}
		for (int i = 0; i < dt.getDataURL().length; i++) {
			if (i == merge.size()) {
				break;
			}
			mergerule.put(dt.getDataURL()[i], merge.get(i));
		}
		dt.setMergeConfigurations(mergerule);
		List<Course[]> courses = new ArrayList();
		if (result.get("course") != null) {
			List<List<Map<String, Object>>> courselist = new ArrayList<List<Map<String, Object>>>();
			if (((List) result.get("course")).get(0) instanceof List) {
				courselist = (List<List<Map<String, Object>>>) result.get("course");
			}
			if (((List) result.get("course")).get(0) instanceof Map) {
				courselist.add((List<Map<String, Object>>) result.get("course"));
			}
			for (List<Map<String, Object>> course : courselist) {
				List<Course> l = new ArrayList<Course>();
				for (Map<String, Object> grade : course) {
					Course gr = new Course();
					gr.setName((String) grade.get("name"));
					gr.setHash(((List<String>) grade.get("md5")).toArray(new String[0]));
					gr.setStyle((String) grade.get("style"));
					gr.setConstraint(((List<String>) grade.get("constraint")).toArray(new String[0]));
					if (grade.get("trophy") != null) {
						List<Trophy> trophy = new ArrayList();
						for (Map<String, Object> tr : (List<Map<String, Object>>) grade.get("trophy")) {
							Trophy t = new Trophy();
							t.setName((String) tr.get("name"));
							t.setMissrate((double) tr.get("missrate"));
							t.setScorerate((double) tr.get("scorerate"));
							t.setStyle((String) tr.get("style"));
							trophy.add(t);
						}
						gr.setTrophy(trophy.toArray(new Trophy[0]));
					}
					l.add(gr);
				}
				courses.add(l.toArray(new Course[0]));
			}
		} else if (result.get("grade") != null) {
			List<Course> l = new ArrayList<Course>();
			for (Map<String, Object> grade : (List<Map<String, Object>>) result.get("grade")) {
				Course gr = new Course();
				gr.setName((String) grade.get("name"));
				gr.setHash(((List<String>) grade.get("md5")).toArray(new String[0]));
				gr.setStyle((String) grade.get("style"));
				gr.setConstraint(new String[] { "grade_mirror" });
				l.add(gr);
			}
			courses.add(l.toArray(new Course[0]));
		}
		dt.setCourse(courses.toArray(new Course[0][]));
		// 必須項目が定義されていない場合は例外をスロー
		if (result.get("name") == null || result.get("symbol") == null) {
			throw new IOException("ヘッダ部の情報が不足しています", null);
		}
		return dt;
	}

	/**
	 * 難易度表JSONデータをデコードし、指定の難易度表に反映する
	 * 
	 * @param dt
	 *            難易度表
	 * @param jsondata
	 *            難易度表JSONデータファイル
	 */
	public void decodeJSONTableData(DifficultyTable dt, File jsondata) throws IOException {
		// JSON読み込み
		ObjectMapper mapper = new ObjectMapper();
		this.decodeJSONTableData(dt, mapper.readValue(jsondata, List.class), true);
	}

	/**
	 * 難易度表JSONデータをデコードし、指定の難易度表に反映する
	 * 
	 * @param dt
	 *            難易度表
	 * @param jsondata
	 *            難易度表JSONデータURL
	 */
	public void decodeJSONTableData(DifficultyTable dt, URL jsondata) throws IOException {
		Logger.getGlobal().info("難易度表データ読み込み - " + jsondata.toExternalForm());
		// JSON読み込み
		ObjectMapper mapper = new ObjectMapper();
		// 難易度表に変換
		this.decodeJSONTableData(dt, mapper.readValue(jsondata, List.class), false);
	}

	private void decodeJSONTableData(DifficultyTable dt, List<Map<String, Object>> result, boolean accept) {
		dt.removeAllElements();
		List<String> levelorder = new ArrayList<String>();
		for (int i = 0; i < result.size(); i++) {
			Map<String, Object> m = result.get(i);
			// levelとmd5(sha256)が定義されていない要素は弾く
			if (accept
					|| (m.get("level") != null && ((m.get("md5") != null && m.get("md5").toString().length() > 24) || (m
							.get("sha256") != null && m.get("sha256").toString().length() > 24)))) {
				DifficultyTableElement dte = new DifficultyTableElement();
				dte.setValues(m);

				String level = String.valueOf(m.get("level"));
				boolean b = true;
				for (int j = 0; j < levelorder.size(); j++) {
					if (levelorder.get(j).equals(level)) {
						b = false;
					}
				}
				if (b) {
					levelorder.add(level);
				}
				dt.addElement(dte);
			} else {
				Logger.getGlobal().info(
						m.get("title") + "の譜面定義に不備があります - level:" + m.get("level") + "  md5:" + m.get("md5"));
			}
		}

		if (dt.getLevelDescription().length == 0) {
			dt.setLevelDescription(levelorder.toArray(new String[0]));
		}
	}

	/**
	 * 難易度表モデルをJSONヘッダ部にエンコードし、指定のファイルに保存する
	 * 
	 * @param dt
	 *            エンコードする難易度表モデル
	 * @param jsonheader
	 *            JSONヘッダ部ファイル
	 */
	public void encodeJSONTableHeader(DifficultyTable dt, File jsonheader) {
		try {
			// ヘッダ部のエクスポート
			Map<String, Object> header = new HashMap<String, Object>();
			header.put("name", dt.getName());
			header.put("symbol", dt.getID());
			header.put("tag", dt.getTag());
			header.put("level_order", dt.getLevelDescription());
			if (dt.getDataURL().length > 1) {
				header.put("data_url", dt.getDataURL());
			} else if (dt.getDataURL().length == 1) {
				header.put("data_url", dt.getDataURL()[0]);
			}
			if (dt.getAttrmap().keySet().size() > 0) {
				header.put("attr", dt.getAttrmap());
			}

			// TODO 後でcourseの仕様に合わせる
			List<Map<String, Object>> grade = new ArrayList<Map<String, Object>>();
			for (Course g : dt.getCourse()[0]) {
				Map<String, Object> m = new HashMap<String, Object>();
				m.put("name", g.getName());
				m.put("md5", g.getHash());
				m.put("style", g.getStyle());
				grade.add(m);
			}
			header.put("course", grade);

			ObjectMapper objectMapper = new ObjectMapper();
			String json = objectMapper.writeValueAsString(header);
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(jsonheader), "UTF-8");
			osw.write(json);
			osw.flush();
			osw.close();
		} catch (Exception e) {
			// controller.showErrorMessage("難易度表の保存に失敗しました");
			Logger.getGlobal().severe("難易度表の保存中の例外:" + e.getMessage());
		}
	}

	/**
	 * 難易度表モデルをJSONヘッダ部/データ部にエンコードし、指定のファイルに保存する
	 * 
	 * @param dt
	 *            エンコードする難易度表モデル
	 * @param jsonheader
	 *            JSONヘッダ部ファイル
	 * @param jsondata
	 *            JSONデータ部ファイル
	 */
	public void encodeJSONTableData(DifficultyTable dt, File jsonheader, File jsondata) {
		try {
			dt.setDataURL(new String[] { jsondata.getName() });
			// ヘッダ部のエクスポート
			this.encodeJSONTableHeader(dt, jsonheader);
			// データ部のエクスポート
			List<Map<String, Object>> datas = new ArrayList<Map<String, Object>>();
			for (DifficultyTableElement te : dt.getElements()) {
				datas.add(te.getValues());
			}
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			String json = objectMapper.writeValueAsString(datas);
			OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(jsondata), "UTF-8");
			osw.write(json);
			osw.flush();
			osw.close();
		} catch (Exception e) {
			// controller.showErrorMessage("難易度表の保存に失敗しました");
			Logger.getGlobal().severe("難易度表の保存中の例外:" + e.getMessage());
		}
	}

	/**
	 * 旧難易度表フォーマットを解析する。現在は使用していない
	 * 
	 * @param dt
	 * @param mark
	 *            難易度表マーク
	 * @param isr
	 *            難易度表ストリームリーダー
	 * @param saveElement
	 *            要素を保存するかどうか
	 * @return 難易度表データ
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	private DifficultyTable parseDifficultyTable(DifficultyTable dt, String mark, InputStreamReader isr,
			boolean saveElement) throws NumberFormatException, IOException {
		// System.out.println("難易度表チェック...");
		String line = null;
		BufferedReader br = new BufferedReader(isr);

		boolean diff = false;
		int state = -1;
		List<DifficultyTableElement> result = new ArrayList<DifficultyTableElement>();
		DifficultyTableElement dte = null;
		Pattern p = Pattern.compile("\"");
		dt.removeAllElements();
		Pattern first = Pattern.compile("\\s*\\[\\s*\\d+,\\s*\"" + mark + ".+\"\\s*,.*");
		while ((line = br.readLine()) != null) {
			if (line.contains("var mname = [")) {
				// System.out.println("難易度表を検出しました");
				diff = true;
			}
			if (line.contains("</script>")) {
				diff = false;
			}
			if (diff && state == -1 && first.matcher(line).matches()) {
				dte = new DifficultyTableElement();
				String did = p.split(line)[1].substring(mark.length());
				// dte.setID(p.split(line)[0].replaceAll("[\\[\\s,]", ""));
				dte.setDifficultyID(did);
				state = 0;
			}

			if (state >= 0) {
				switch (state) {
				case 0:
					state++;
					break;
				case 1:
					// 曲名
					dte.setTitle(p.split(line)[1]);
					state++;
					break;
				case 2:
					// bmsid
					dte.setBMSID(Integer.parseInt(p.split(line)[1].replaceAll("[\\s]", "")));
					state++;
					break;
				case 3:
					// URL1
					String[] split = p.split(line)[1].split("'");
					if (split.length > 2) {
						dte.setURL1(split[1]);
					}
					split = p.split(line)[1].split("<[bB][rR]\\s*/*>");
					dte.setURL1name(split[0].replaceFirst("<[aA]\\s[hH][rR][eE][fF]=.+'>|</[aA]>", "").replaceFirst(
							"</[aA]>", ""));
					// URL1サブ
					if (split.length > 1) {
						String[] split2 = split[1].split("'");
						if (split2.length > 2) {
							dte.setURL1sub(split2[1]);
						}
						dte.setURL1subname(split[1].replaceFirst("<[aA]\\s[hH][rR][eE][fF]=.+'>|</[aA]>", "")
								.replaceFirst("</[aA]>", ""));
					}
					state++;
					break;
				case 4:
					// URL2
					String[] split3 = p.split(line)[1].split("'");
					if (split3.length > 2) {
						dte.setURL2(split3[1]);
					}
					dte.setURL2name(p.split(line)[1].replaceFirst("<[aA]\\s[hH][rR][eE][fF]=.+'>|</[aA]>", "")
							.replaceFirst("</[aA]>", ""));
					state++;
					break;
				case 5:
					// コメント
					dte.setComment1(p.split(line)[1].replaceFirst("Avg:.*JUDGE:[A-Z]+\\s*", ""));
					result.add(dte);
					dte = null;
					state = -1;
					break;
				}
			}
		}
		if (saveElement) {
			for (int i = 0; i < result.size(); i++) {
				dt.addElement(result.get(i));
			}
		}
		if (dt.getLevelDescription().length == 0) {
			List<String> l = new ArrayList<String>();
			for (int i = 0; i < result.size(); i++) {
				boolean b = true;
				for (int j = 0; j < l.size(); j++) {
					if (l.get(j).equals(result.get(i).getDifficultyID())) {
						b = false;
					}
				}
				if (b) {
					l.add(result.get(i).getDifficultyID());
				}
			}
			dt.setLevelDescription(l.toArray(new String[0]));
		}

		// System.out.println("難易度表リスト抽出完了 リスト数:" + dt.getElements().length);
		return dt;
	}
}
