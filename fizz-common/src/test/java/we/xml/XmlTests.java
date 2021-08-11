/*
 *  Copyright (C) 2020 the original author or authors.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package we.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class XmlTests {

	private String xmlStr = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + "<library>\n"
			+ "    <book id=\"007\">James Bond</book>\n" + "</library>";

	private String jsonStr1 = "{\"library\":{\"book\":{\"#text\":\"James Bond\",\"-id\":\"007\"}}}";
	private String jsonStr2 = "{\n"
			+ "  \"library\": {\n"
			+ "    \"owner\": \"John Doe\",\n"
			+ "    \"book\": [\n"
			+ "      \"James Bond\",\n"
			+ "      \"Book for the dummies\"\n"
			+ "    ]\n"
			+ "  }\n"
			+ "}";
	
	private String jsonStr3 = "{\n"
			+ "  \"library\": {\n"
			+ "    \"book\": [\n"
			+ "      \"James Bond\",\n"
			+ "      \"Book for the dummies\"\n"
			+ "    ]\n"
			+ "  }\n"
			+ "}";
	
	private String jsonStr4 = "{\n"
			+ "  \"library\": {\n"
			+ "    \"owner\": \"John Doe\",\n"
			+ "    \"book\": [\n"
			+ "      {\n"
			+ "        \"-id\": \"007\",\n"
			+ "        \"#text\": \"James Bond\"\n"
			+ "      },\n"
			+ "      \"Book for the dummies\"\n"
			+ "    ]\n"
			+ "  }\n"
			+ "}";
	
	private String jsonStr5 = "{\n"
			+ "  \"library\": {\n"
			+ "    \"owner\": \"John Doe\",\n"
			+ "    \"book\": [\n"
			+ "      \"1\",\n"
			+ "      \"2\"\n"
			+ "    ]\n"
			+ "  }\n"
			+ "}";
	
	@Test
	public void TestXmlToJson() {
		XmlToJson xmlToJson = new XmlToJson.Builder(xmlStr).build();
		String jsonStr = xmlToJson.toString();

		// System.out.println(jsonStr);
		
		JSONObject jsonObj = new JSONObject(jsonStr);
		
		assertEquals("007", jsonObj.getJSONObject("library").getJSONObject("book").getString("-id"));
		assertEquals("James Bond", jsonObj.getJSONObject("library").getJSONObject("book").getString("#text"));
	}
	
	@Test
	public void TestXmlToJsonForceList() {
		XmlToJson xmlToJson = new XmlToJson.Builder(xmlStr).forceList("/library/book").build();
		String jsonStr = xmlToJson.toString();

		JSONObject jsonObj = new JSONObject(jsonStr);
		
		assertEquals("007", jsonObj.getJSONObject("library").getJSONArray("book").getJSONObject(0).getString("-id"));
		assertEquals("James Bond", jsonObj.getJSONObject("library").getJSONArray("book").getJSONObject(0).getString("#text"));
	}
	
	@Test
	public void TestJsonToXml1() {
		JsonToXml jsonToXml = new JsonToXml.Builder(jsonStr1).build();
		
		XmlToJson xmlToJson = new XmlToJson.Builder(jsonToXml.toString()).forceList("/library/book").build();
		String jsonStr = xmlToJson.toString();

		JSONObject jsonObj = new JSONObject(jsonStr);
		
		assertEquals("007", jsonObj.getJSONObject("library").getJSONArray("book").getJSONObject(0).getString("-id"));
		assertEquals("James Bond", jsonObj.getJSONObject("library").getJSONArray("book").getJSONObject(0).getString("#text"));
	}
	
	@Test
	public void TestJsonToXml2() {
		JsonToXml jsonToXml = new JsonToXml.Builder(jsonStr2).build();
		
		XmlToJson xmlToJson = new XmlToJson.Builder(jsonToXml.toString()).build();
		String jsonStr = xmlToJson.toString();

		JSONObject jsonObj = new JSONObject(jsonStr);
		
		// System.out.println(xmlToJson.toFormattedString());
		assertEquals("John Doe", jsonObj.getJSONObject("library").getString("owner"));
		assertEquals("James Bond", jsonObj.getJSONObject("library").getJSONArray("book").get(0).toString());
		assertEquals("Book for the dummies", jsonObj.getJSONObject("library").getJSONArray("book").get(1).toString());
	}
	
	@Test
	public void TestJsonToXml3() {
		JsonToXml jsonToXml = new JsonToXml.Builder(jsonStr3).build();
		
		XmlToJson xmlToJson = new XmlToJson.Builder(jsonToXml.toString()).build();
		String jsonStr = xmlToJson.toString();

		JSONObject jsonObj = new JSONObject(jsonStr);
		
		assertEquals("James Bond", jsonObj.getJSONObject("library").getJSONArray("book").get(0).toString());
		assertEquals("Book for the dummies", jsonObj.getJSONObject("library").getJSONArray("book").get(1).toString());
	}
	
	@Test
	public void TestJsonToXml4() {
		JsonToXml jsonToXml = new JsonToXml.Builder(jsonStr4).build();
		
		XmlToJson xmlToJson = new XmlToJson.Builder(jsonToXml.toString()).build();
		String jsonStr = xmlToJson.toString();

		JSONObject jsonObj = new JSONObject(jsonStr);
		
		// System.out.println(xmlToJson.toFormattedString());
		assertEquals("007", jsonObj.getJSONObject("library").getJSONArray("book").getJSONObject(0).getString("-id"));
		assertEquals("James Bond", jsonObj.getJSONObject("library").getJSONArray("book").getJSONObject(0).getString("#text"));
		assertEquals("Book for the dummies", jsonObj.getJSONObject("library").getJSONArray("book").get(1).toString());
	}
	
	@Test
	public void TestJsonToXml5() {
		JsonToXml jsonToXml = new JsonToXml.Builder(jsonStr5).build();
		
		XmlToJson xmlToJson = new XmlToJson.Builder(jsonToXml.toString()).forceIntegerForPath("/library/book").build();
		String jsonStr = xmlToJson.toString();

		JSONObject jsonObj = new JSONObject(jsonStr);
		
		// System.out.println(xmlToJson.toFormattedString());
		assertEquals("John Doe", jsonObj.getJSONObject("library").getString("owner"));
		Object val = jsonObj.getJSONObject("library").getJSONArray("book").get(0);
		assertTrue(val instanceof Integer);
		assertEquals(1, jsonObj.getJSONObject("library").getJSONArray("book").getInt(0));
		assertEquals(2, jsonObj.getJSONObject("library").getJSONArray("book").getInt(1));
	}

}
