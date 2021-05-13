/*
 *  Copyright (C) 2021 the original author or authors.
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

package we.fizz.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import we.fizz.input.extension.request.RequestInput;

/**
 * 
 * @author Francis Dong
 *
 */
public class RequestInputTests {

	@Test
	public void testParseBody() {

		// test json and text
		String[] contentTypes = new String[] { "application/json; charset=UTF-8", "application/json;",
				"application/json", "text/plain" };

		String[] respBodys = new String[] { "{\"a\":\"b\"}", "[{\"a\":\"b\"}]" };

		RequestInput requestInput = new RequestInput();
		for (int n = 0; n < respBodys.length; n++) {
			String respBody = respBodys[n];
			for (int i = 0; i < contentTypes.length; i++) {
				String ct = contentTypes[i];
				Object body = requestInput.parseBody(ct, respBody);
				assertEquals(respBody, body.toString());
			}
		}

		// test invalid text content
		contentTypes = new String[] { "text/plain" };
		respBodys = new String[] { "{\"a\":\"b\"", "{\"a\":\"b\"}]" };
		for (int n = 0; n < respBodys.length; n++) {
			String respBody = respBodys[n];
			for (int i = 0; i < contentTypes.length; i++) {
				String ct = contentTypes[i];
				Object body = requestInput.parseBody(ct, respBody);
				assertEquals(respBody, body.toString());
			}
		}

		// test html js
		contentTypes = new String[] {"application/javascript",  "text/html"};
		respBodys = new String[] { "{\"a\":\"b\"", "{\"a\":\"b\"}]", "var a=1;" , "<person a=\"b\"></person>" };
		for (int n = 0; n < respBodys.length; n++) {
			String respBody = respBodys[n];
			for (int i = 0; i < contentTypes.length; i++) {
				String ct = contentTypes[i];
				Object body = requestInput.parseBody(ct, respBody);
				assertEquals(respBody, body.toString());
			}
		}
		
		// test xml
		String xmlBody = "<person a=\"b\">123</person>";
		Map<String, Map<String, String>> root = (Map<String, Map<String, String>>) requestInput
				.parseBody("application/xml", xmlBody);
		Map<String, String> m = root.get("person");
		assertEquals("b", m.get("-a"));
		assertEquals("123", m.get("#text"));
	}

}
