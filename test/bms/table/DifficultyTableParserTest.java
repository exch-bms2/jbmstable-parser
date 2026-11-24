package bms.table;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DifficultyTableParserTest {
	@TempDir
	Path directory;

	@Test
	void malformedMetaAndDataSourceDoNotStopLoading() throws Exception {
		write("page.html", "<meta name=\"bmstable\" content=broken>\n"
				+ "<meta name='bmstable' content='header.json'>\n");
		write("header.json", "{\"name\":\"Table\",\"symbol\":\"T\","
				+ "\"data_url\":[\"missing.json\",\"null.json\",\"data.json\"],\"mode\":3,"
				+ "\"data_rule\":[{\"1\":3}],\"level_order\":[null,\"1\"],"
				+ "\"course\":[{\"name\":\"C\",\"md5\":[\"abc\"],"
				+ "\"trophy\":[{\"missrate\":1,\"scorerate\":95}]}]}");
		write("data.json", "[{\"level\":\"1\",\"md5\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"},"
				+ "{\"level\":\"1\",\"md5\":\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\"},"
				+ "{\"level\":\"1\",\"md5\":3}]");
		write("null.json", "null");

		DifficultyTableParser parser = new DifficultyTableParser();
		String page = directory.resolve("page.html").toUri().toString();
		assertTrue(parser.containsHeader(page));
		DifficultyTable table = new DifficultyTable(page);
		parser.decode(true, table);

		assertEquals(2, table.getElements().length);
		assertArrayEquals(new String[] { "", "1" }, table.getLevelDescription());
		assertNull(table.getMode());
		assertEquals(1, table.getCourse().length);
		assertArrayEquals(new String[0], table.getCourse()[0][0].getConstraint());
		assertEquals(95.0, table.getCourse()[0][0].getTrophy()[0].getScorerate());
	}

	@Test
	void headerUrlWithoutSourcePageDecodesIntegerTrophyRates() throws Exception {
		Path header = write("header.json", "{\"name\":\"Table\",\"symbol\":\"T\",\"data_url\":\"data.json\","
				+ "\"course\":[[{\"name\":\"C\",\"md5\":[\"abc\"],"
				+ "\"trophy\":[{\"name\":\"silver\",\"missrate\":5,\"scorerate\":70}]}]]}");
		write("data.json", "[{\"level\":\"1\",\"md5\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}]");
		DifficultyTable table = new DifficultyTable();
		table.setHeadURL(header.toUri().toString());

		new DifficultyTableParser().decode(true, table);

		assertEquals(1, table.getElements().length);
		Course.Trophy trophy = table.getCourse()[0][0].getTrophy()[0];
		assertEquals(5.0, trophy.getMissrate());
		assertEquals(70.0, trophy.getScorerate());
	}

	@Test
	void emptyCourseAndMissingDataRetainPreviousElements() throws Exception {
		Path header = write("header.json", "{\"name\":\"Table\",\"symbol\":\"T\","
				+ "\"data_url\":\"missing.json\",\"course\":[]}");
		DifficultyTable table = new DifficultyTable();
		table.addElement(new DifficultyTableElement());

		new DifficultyTableParser().decodeJSONTable(table, header.toUri().toURL(), true);

		assertEquals(1, table.getElements().length);
		assertEquals(0, table.getCourse().length);
	}

	@Test
	void missingRequiredHeaderFieldsRemainAnIOException() throws Exception {
		Path header = write("header.json", "{\"course\":[]}");
		assertThrows(IOException.class,
				() -> new DifficultyTableParser().decodeJSONTableHeader(new DifficultyTable(), header.toFile()));
	}

	private Path write(String filename, String content) throws IOException {
		return Files.writeString(directory.resolve(filename), content, StandardCharsets.UTF_8);
	}
}
