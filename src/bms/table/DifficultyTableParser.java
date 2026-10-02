package bms.table;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bms.table.Course.Trophy;

import com.fasterxml.jackson.core.type.TypeReference;
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
	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};
	private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {
	};

	private Map<String, String[]> data = new HashMap<>();

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
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(urlname).openStream()))) {
			List<String> lines = new ArrayList<>();
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
			return lines.toArray(String[]::new);
		} catch (IOException e) {
			Logger.getGlobal().warning("難易度表サイト解析中の例外:" + e);
		}
		return null;
	}

	private String metaContent(String line, String name, String attribute) {
		if (!line.toLowerCase(Locale.ROOT).contains("<meta")) {
			return null;
		}
		String value = attributeValue(line, attribute);
		return name.equalsIgnoreCase(value) ? attributeValue(line, "content") : null;
	}

	private String attributeValue(String line, String attribute) {
		Matcher matcher = Pattern.compile("(?:^|\\s)" + attribute + "\\s*=\\s*([\"'])(.*?)\\1", Pattern.CASE_INSENSITIVE)
				.matcher(line);
		return matcher.find() ? matcher.group(2) : null;
	}

	private String getMetaTag(String urlname, String name) {
		if (data.get(urlname) == null) {
			data.put(urlname, readAllLines(urlname));
		}
		if (data.get(urlname) == null) {
			return null;
		}
		for (String line : data.get(urlname)) {
			String content = metaContent(line, name, "name");
			if (content != null) {
				return content;
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
			for (String line : data.get(urlname)) {
				String contentType = metaContent(line, "content-type", "http-equiv");
				if (contentType != null && contentType.contains("charset=")) {
					enc = contentType.substring(contentType.indexOf("charset=") + 8);
				}
				String header = metaContent(line, "bmstable", "name");
				if (header != null) {
					tableurl = header;
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

	private String getAbsoluteURL(String source, String path) throws MalformedURLException {
		return new URL(new URL(source), path).toExternalForm();
	}

	private <T> T readValue(ObjectMapper mapper, URL url, TypeReference<T> type) throws IOException {
		try (InputStream inputStream = url.openStream()) {
			return mapper.readValue(inputStream, type);
		}
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
			List<DifficultyTableElement> elements = new ArrayList<>();
			List<String> levels = new ArrayList<>();
			int loaded = 0;
			for (String url : urls) {
				if (url == null || url.isBlank()) {
					Logger.getGlobal().warning("空の難易度表データURLをスキップします");
					continue;
				}
				Map<String, String> conf = dt.getMergeConfigurations().get(url);
				if (conf == null) {
					conf = new HashMap<>();
				}
				DifficultyTable table = new DifficultyTable();

				try {
					this.decodeJSONTableData(
							table,
							new URL(this.getAbsoluteURL(
									(dt.getSourceURL() == null || dt.getSourceURL().length() == 0) ? dt.getHeadURL() : this
											.getAbsoluteURL(dt.getSourceURL(), dt.getHeadURL()), url)));
				} catch (IOException e) {
					Logger.getGlobal().warning("難易度表データをスキップします: " + url + " - " + e);
					continue;
				}
				loaded++;
				levels.addAll(Arrays.asList(table.getLevelDescription()));
				// 重複BMSの処理
				for (DifficultyTableElement dte : table.getElements()) {
					if (conf.get(dte.getLevel()) == null || conf.get(dte.getLevel()).length() > 0) {
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
							if (conf.get(dte.getLevel()) != null) {
								dte.setLevel(conf.get(dte.getLevel()));
							}
							elements.add(dte);
						}
					}
				}
			}
			if (loaded == 0) {
				return;
			}
			if (dt.getLevelDescription().length == 0) {
				dt.setLevelDescription(levels.toArray(new String[levels.size()]));
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
		Map<String, Object> result = mapper.readValue(jsonheader, MAP_TYPE);
		this.decodeJSONTableHeader(dt, result);
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
		Map<String, Object> result = readValue(mapper, jsonheader, MAP_TYPE);
		this.decodeJSONTableHeader(dt, result);
		dt.setHeadURL(jsonheader.toExternalForm());
	}

	private DifficultyTable decodeJSONTableHeader(DifficultyTable dt, Map<String, Object> result) throws IOException {
		if (result == null || !(result.get("name") instanceof String) || !(result.get("symbol") instanceof String)) {
			throw new IOException("ヘッダ部の情報が不足しています");
		}
		Map<String, Object> values = new HashMap<>(result);
		if (!(values.get("mode") instanceof String)) {
			values.remove("mode");
		}
		dt.setValues(values);
		// level_order処理
		Object dataurl = result.get("data_url");
		dt.setDataURL(new String[0]);
		if (dataurl instanceof String url) {
			dt.setDataURL(new String[] { url });
		}
		if (dataurl instanceof List<?> list) {
			dt.setDataURL(list.stream().filter(String.class::isInstance).map(String.class::cast).toArray(String[]::new));
		}
		Map<String, Map<String, String>> mergerule = new HashMap<>();
		if (result.get("data_rule") instanceof List<?> merge) {
			for (int i = 0; i < Math.min(dt.getDataURL().length, merge.size()); i++) {
				if (merge.get(i) instanceof Map<?, ?> rule) {
					Map<String, String> levels = new HashMap<>();
					for (Map.Entry<?, ?> entry : rule.entrySet()) {
						if (entry.getKey() instanceof String key && entry.getValue() instanceof String value) {
							levels.put(key, value);
						}
					}
					mergerule.put(dt.getDataURL()[i], levels);
				}
			}
		}
		dt.setMergeConfigurations(mergerule);
		List<Course[]> courses = new ArrayList<>();
		if (result.get("course") instanceof List<?> courseList && !courseList.isEmpty()) {
			if (courseList.get(0) instanceof List<?>) {
				for (Object group : courseList) {
					if (group instanceof List<?> grades) {
						courses.add(parseCourses(grades, false));
					}
				}
			} else {
				courses.add(parseCourses(courseList, false));
			}
		} else if (result.get("grade") instanceof List<?> grades) {
			courses.add(parseCourses(grades, true));
		}
		dt.setCourse(courses.toArray(new Course[courses.size()][]));
		return dt;
	}

	private Course[] parseCourses(List<?> grades, boolean legacy) {
		List<Course> courses = new ArrayList<>();
		for (Object item : grades) {
			if (!(item instanceof Map<?, ?> grade)) {
				continue;
			}
			Course course = new Course();
			if (grade.get("name") instanceof String name) course.setName(name);
			if (grade.get("style") instanceof String style) course.setStyle(style);
			List<BMSTableElement> charts = new ArrayList<>();
			if (!legacy && grade.get("charts") instanceof List<?> chartList) {
				for (Object chart : chartList) {
					if (chart instanceof Map<?, ?> values) {
						DifficultyTableElement element = new DifficultyTableElement();
						element.setValues(stringKeyMap(values));
						charts.add(element);
					}
				}
			} else if (grade.get("md5") instanceof List<?> hashes) {
				for (Object hash : hashes) {
					if (hash instanceof String md5) {
						DifficultyTableElement element = new DifficultyTableElement();
						element.setMD5(md5);
						charts.add(element);
					}
				}
			}
			course.setCharts(charts.toArray(BMSTableElement[]::new));
			if (legacy) {
				course.setConstraint(new String[] { "grade_mirror", "gauge_lr2" });
			} else {
				if (grade.get("constraint") instanceof List<?> constraints) {
					course.setConstraint(constraints.stream().filter(String.class::isInstance)
							.map(String.class::cast).toArray(String[]::new));
				}
				if (grade.get("trophy") instanceof List<?> trophies) {
					List<Trophy> parsed = new ArrayList<>();
					for (Object itemTrophy : trophies) {
						if (itemTrophy instanceof Map<?, ?> values) {
							Trophy trophy = new Trophy();
							if (values.get("name") instanceof String name) trophy.setName(name);
							if (values.get("style") instanceof String style) trophy.setStyle(style);
							if (values.get("missrate") instanceof Number rate) trophy.setMissrate(rate.doubleValue());
							if (values.get("scorerate") instanceof Number rate) trophy.setScorerate(rate.doubleValue());
							parsed.add(trophy);
						}
					}
					course.setTrophy(parsed.toArray(Trophy[]::new));
				}
			}
			courses.add(course);
		}
		return courses.toArray(Course[]::new);
	}

	private Map<String, Object> stringKeyMap(Map<?, ?> values) {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<?, ?> entry : values.entrySet()) {
			if (entry.getKey() instanceof String key) result.put(key, entry.getValue());
		}
		return result;
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
		this.decodeJSONTableData(dt, mapper.readValue(jsondata, LIST_MAP_TYPE), true);
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
		this.decodeJSONTableData(dt, readValue(mapper, jsondata, LIST_MAP_TYPE), false);
	}

	private void decodeJSONTableData(DifficultyTable dt, List<Map<String, Object>> result, boolean accept) throws IOException {
		if (result == null) {
			throw new IOException("難易度表データが配列ではありません");
		}
		List<String> levelorder = new ArrayList<>();
		dt.removeAllElements();
		for (Map<String, Object> m : result) {
			if (m == null || !stringOrNull(m.get("title")) || !stringOrNull(m.get("mode"))
					|| !stringOrNull(m.get("md5")) || !stringOrNull(m.get("sha256"))) {
				Logger.getGlobal().warning("不正な難易度表データ行をスキップします");
				continue;
			}
			// levelとmd5(sha256)が定義されていない要素は弾く
			if (accept
					|| (m.get("level") != null && ((m.get("md5") != null && m.get("md5").toString().length() > 24) || (m
							.get("sha256") != null && m.get("sha256").toString().length() > 24)))) {
				DifficultyTableElement dte = new DifficultyTableElement();
				dte.setValues(m);
				if(dte.getMode() == null) {
					dte.setMode(dt.getMode());
				}

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
			dt.setLevelDescription(levelorder.toArray(new String[levelorder.size()]));
		}
	}

	private boolean stringOrNull(Object value) {
		return value == null || value instanceof String;
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
			Map<String, Object> header = new HashMap<>();
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
			List<Map<String, Object>> grade = new ArrayList<>();
			for (Course g : dt.getCourse()[0]) {
				Map<String, Object> m = new HashMap<>();
				m.put("name", g.getName());
//				m.put("md5", g.getHash());
				m.put("style", g.getStyle());
				grade.add(m);
			}
			header.put("course", grade);

			ObjectMapper objectMapper = new ObjectMapper();
			String json = objectMapper.writeValueAsString(header);
			try (BufferedWriter writer = Files.newBufferedWriter(jsonheader.toPath(), StandardCharsets.UTF_8)) {
				writer.write(json);
			}
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
			List<Map<String, Object>> datas = new ArrayList<>();
			for (DifficultyTableElement te : dt.getElements()) {
				datas.add(te.getValues());
			}
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
			String json = objectMapper.writeValueAsString(datas);
			try (BufferedWriter writer = Files.newBufferedWriter(jsondata.toPath(), StandardCharsets.UTF_8)) {
				writer.write(json);
			}
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
		List<DifficultyTableElement> result = new ArrayList<>();
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
				dte.setLevel(did);
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
						dte.setURL(split[1]);
					}
					split = p.split(line)[1].split("<[bB][rR]\\s*/*>");
					dte.setArtist(split[0].replaceFirst("<[aA]\\s[hH][rR][eE][fF]=.+'>|</[aA]>", "").replaceFirst(
							"</[aA]>", ""));
					// URL1サブ
					if (split.length > 1) {
						String[] split2 = split[1].split("'");
						if (split2.length > 2) {
							dte.setPackageURL(split2[1]);
						}
						dte.setPackageName(split[1].replaceFirst("<[aA]\\s[hH][rR][eE][fF]=.+'>|</[aA]>", "")
								.replaceFirst("</[aA]>", ""));
					}
					state++;
					break;
				case 4:
					// URL2
					String[] split3 = p.split(line)[1].split("'");
					if (split3.length > 2) {
						dte.setAppendURL(split3[1]);
					}
					dte.setAppendArtist(p.split(line)[1].replaceFirst("<[aA]\\s[hH][rR][eE][fF]=.+'>|</[aA]>", "")
							.replaceFirst("</[aA]>", ""));
					state++;
					break;
				case 5:
					// コメント
					dte.setComment(p.split(line)[1].replaceFirst("Avg:.*JUDGE:[A-Z]+\\s*", ""));
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
			List<String> l = new ArrayList<>();
			for (int i = 0; i < result.size(); i++) {
				boolean b = true;
				for (int j = 0; j < l.size(); j++) {
					if (l.get(j).equals(result.get(i).getLevel())) {
						b = false;
					}
				}
				if (b) {
					l.add(result.get(i).getLevel());
				}
			}
			dt.setLevelDescription(l.toArray(new String[0]));
		}

		// System.out.println("難易度表リスト抽出完了 リスト数:" + dt.getElements().length);
		return dt;
	}
}
