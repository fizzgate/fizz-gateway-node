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

package com.fizzgate.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fizzgate.util.MapUtil;

/**
 * 
 * @author Francis Dong
 *
 */
public class MapUtilTests {
	
	
	@Test
	public void TestMerge() {
		/**
		 * target = {
			    a: 1,
			    c: 3,
			    b: {
			        b1: "b1",
			        b2: {
			            c1: "c1"
			            c2: "c2"
			        },
			        b3: [1, 2, 3],
			        b4: "b4"
			    }
			};
		 */
		Map<String, Object> target = new HashMap<>();
		target.put("a", 1);
		target.put("c", 3);

		Map<String, Object> b = new HashMap<>();
		target.put("b", b);
		b.put("b1", "b1");

		Map<String, Object> c = new HashMap<>();
		c.put("c1", "c1");
		c.put("c2", "c2");
		b.put("b2", c);

		List<Integer> list1 = new ArrayList<>();
		list1.add(1);
		list1.add(2);
		list1.add(3);
		b.put("b3", list1);
		b.put("b4", "b4");
		
		
		
		/**
		 * src = {
			    b: {
			        b1: "b1-src",
			        b2: {
			            c1: "c1-src"
			        },
			        b3: [1, 4]
			    },
			    d: "d1"
			};
		 */
		Map<String, Object> src = new HashMap<>();
		Map<String, Object> srcb = new HashMap<>();
		srcb.put("b1", "b1-src");

		Map<String, Object> srcc = new HashMap<>();
		srcc.put("c1", "c1-src");
		srcb.put("b2", srcc);

		List<Integer> list2 = new ArrayList<>();
		list2.add(1);
		list2.add(4);
		srcb.put("b3", list2);

		src.put("b", srcb);
		src.put("d", "d1");
		
		
		MapUtil.merge(target, src);
		
		/**
		 * expected output: 
		 * {
			    "a": 1,
			    "b": {
			        "b1": "b1-src",
			        "b2": {
			            "c1": "c1-src",
			            "c2": "c2"
			        },
			        "b3": [1, 4],
			        "b4": "b4"
			    },
			    "c": 3,
			    "d": "d1"
			}
		 */
		assertEquals(1, Integer.valueOf(target.get("a").toString()));
		assertEquals(3, Integer.valueOf(target.get("c").toString()));
		assertEquals("d1", target.get("d"));

		Map<String, Object> mapb = (Map<String, Object>) target.get("b");
		assertEquals("b1-src", mapb.get("b1"));
		assertEquals("b4", mapb.get("b4"));
		List<Integer> list = (List<Integer>) mapb.get("b3");
		assertEquals(2, list.size());
		assertEquals(1, list.get(0));
		assertEquals(4, list.get(1));

		Map<String, Object> mapc = (Map<String, Object>) mapb.get("b2");
		assertEquals("c1-src", mapc.get("c1"));
		assertEquals("c2", mapc.get("c2"));
	}

}
