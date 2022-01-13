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

package we.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import we.util.JacksonUtils;

/**
 * 
 * @author Francis Dong
 *
 */
public class FlowStatTests {

	private FlowStat stat = new FlowStat();

	class FlowRuleCase {
		public int threads = 3;
		public int requests = 1000;
		public int totalReqs = threads * requests;
		public List<ResourceConfig> resourceConfigs = new ArrayList<>();
		public List<ResourceExpect> resourceExpects = new ArrayList<>();
		public IncrRequestResult expectResult;
	}

	class ResourceExpect {
		public long concurrents;
		public long QPS;
		public long total;
		public long blockedReqs;

		public ResourceExpect(long concurrents, long QPS, long total, long blockedReqs) {
			this.concurrents = concurrents;
			this.QPS = QPS;
			this.total = total;
			this.blockedReqs = blockedReqs;
		}

		public ResourceExpect() {
		}
	}

	public List<FlowRuleCase> createFlowRuleCase() {
		List<FlowRuleCase> cases = new ArrayList<>();
		// blocked by service concurrent request
		FlowRuleCase c1 = new FlowRuleCase();
		c1.resourceConfigs.add(new ResourceConfig("_global1", 100, 200));
		c1.resourceConfigs.add(new ResourceConfig("service1", 10, 200));
		c1.resourceExpects.add(new ResourceExpect(10, 10, 10, 0));
		c1.resourceExpects.add(new ResourceExpect(10, 10, 10, c1.totalReqs - 10));
		c1.expectResult = IncrRequestResult.block("service1", BlockType.CONCURRENT_REQUEST);
		cases.add(c1);

		// Note: use different resource ID to avoid being affected by previous test data
		FlowRuleCase c2 = new FlowRuleCase();
		c2.resourceConfigs.add(new ResourceConfig("_global2", 10, 200));
		c2.resourceConfigs.add(new ResourceConfig("service2", 200, 200));
		c2.resourceExpects.add(new ResourceExpect(10, 10, 10, c2.totalReqs - 10));
		c2.resourceExpects.add(new ResourceExpect(10, 10, 10, 0));
		c2.expectResult = IncrRequestResult.block("_global2", BlockType.CONCURRENT_REQUEST);
		cases.add(c2);

		// Note: use different resource ID to avoid being affected by previous test data
		FlowRuleCase c3 = new FlowRuleCase();
		c3.resourceConfigs.add(new ResourceConfig("_global3", 200, 10));
		c3.resourceConfigs.add(new ResourceConfig("service3", 200, 100));
		c3.resourceExpects.add(new ResourceExpect(10, 10, 10, c3.totalReqs - 10));
		c3.resourceExpects.add(new ResourceExpect(10, 10, 10, 0));
		c3.expectResult = IncrRequestResult.block("_global3", BlockType.QPS);
		cases.add(c3);

		// Note: use different resource ID to avoid being affected by previous test data
		FlowRuleCase c4 = new FlowRuleCase();
		c4.resourceConfigs.add(new ResourceConfig("_global4", 200, 100));
		c4.resourceConfigs.add(new ResourceConfig("service4", 200, 10));
		c4.resourceExpects.add(new ResourceExpect(10, 10, 10, 0));
		c4.resourceExpects.add(new ResourceExpect(10, 10, 10, c4.totalReqs - 10));
		c4.expectResult = IncrRequestResult.block("service4", BlockType.QPS);
		cases.add(c4);

		// Note: use different resource ID to avoid being affected by previous test data
		FlowRuleCase c5 = new FlowRuleCase();
		c5.resourceConfigs.add(new ResourceConfig("_global5", 0, 0));
		c5.resourceConfigs.add(new ResourceConfig("service5", 0, 0));
		c5.resourceExpects.add(new ResourceExpect(c5.totalReqs, c5.totalReqs, c5.totalReqs, 0));
		c5.resourceExpects.add(new ResourceExpect(c5.totalReqs, c5.totalReqs, c5.totalReqs, 0));
		c5.expectResult = IncrRequestResult.success();
		cases.add(c5);

		// Note: use different resource ID to avoid being affected by previous test data
		FlowRuleCase c6 = new FlowRuleCase();
		c6.resourceConfigs.add(new ResourceConfig("_global6", 20, 0));
		c6.resourceConfigs.add(new ResourceConfig("service6", 20, 0));
		c6.resourceExpects.add(new ResourceExpect(20, 20, 20, c6.totalReqs - 20));
		c6.resourceExpects.add(new ResourceExpect(20, 20, 20, 0));
		c6.expectResult = IncrRequestResult.block("_global6", BlockType.CONCURRENT_REQUEST);
		cases.add(c6);

		// Note: use different resource ID to avoid being affected by previous test data
		FlowRuleCase c7 = new FlowRuleCase();
		c7.resourceConfigs.add(new ResourceConfig("_global7", 0, 0));
		c7.resourceConfigs.add(new ResourceConfig("service7", 0, 20));
		c7.resourceExpects.add(new ResourceExpect(20, 20, 20, 0));
		c7.resourceExpects.add(new ResourceExpect(20, 20, 20, c7.totalReqs - 20));
		c7.expectResult = IncrRequestResult.block("service7", BlockType.QPS);
		cases.add(c7);

		return cases;
	}

	class ConcurrentJob1 implements Runnable {
		public ConcurrentJob1(int requests, long curTimeSlotId, List<ResourceConfig> resourceConfigs,
				IncrRequestResult expectResult) {
			this.requests = requests;
			this.resourceConfigs = resourceConfigs;
			this.curTimeSlotId = curTimeSlotId;
			this.expectResult = expectResult;
		}

		private int requests = 0;
		private List<ResourceConfig> resourceConfigs;
		private long curTimeSlotId = 0;
		private IncrRequestResult expectResult;

		@Override
		public void run() {
			for (int i = 0; i < requests; i++) {
				IncrRequestResult result = stat.incrRequest(resourceConfigs, curTimeSlotId);
				if (result != null && !result.isSuccess()) {
					assertEquals(expectResult.getBlockedResourceId(), result.getBlockedResourceId());
					assertEquals(expectResult.getBlockType(), result.getBlockType());
				}
			}
		}
	}
	
	@Test
	public void testIncrRequestResultByResourceChain() throws Throwable {
		// concurrent
		FlowRuleCase c1 = new FlowRuleCase();
		c1.resourceConfigs.add(new ResourceConfig("testIncrRequestResultByResourceChain_global1", 100, 200));
		c1.resourceConfigs.add(new ResourceConfig("testIncrRequestResultByResourceChain_service1", 10, 200));
		
		long startTimeSlotId = stat.currentTimeSlotId();
		long endTimeSlotId = startTimeSlotId + 1000;
		for (int i = 0; i < 10; i++) {
			stat.incrRequest(c1.resourceConfigs, startTimeSlotId);
		}
		
		IncrRequestResult result = stat.incrRequest(c1.resourceConfigs, startTimeSlotId);
		assertTrue(!result.isSuccess());
		assertEquals("testIncrRequestResultByResourceChain_service1", result.getBlockedResourceId());
		assertEquals(BlockType.CONCURRENT_REQUEST, result.getBlockType());

		stat.addRequestRT(c1.resourceConfigs, startTimeSlotId, 1, true);
		
		result = stat.incrRequest(c1.resourceConfigs, startTimeSlotId);
		assertTrue(result.isSuccess());
		
		// QPS
		FlowRuleCase c2 = new FlowRuleCase();
		c2.resourceConfigs.add(new ResourceConfig("testIncrRequestResultByResourceChain_global2", 100, 200));
		c2.resourceConfigs.add(new ResourceConfig("testIncrRequestResultByResourceChain_service2", 100, 10));
		
		for (int i = 0; i < 10; i++) {
			stat.incrRequest(c2.resourceConfigs, startTimeSlotId);
		}
		
		result = stat.incrRequest(c2.resourceConfigs, startTimeSlotId);
		assertTrue(!result.isSuccess());
		assertEquals("testIncrRequestResultByResourceChain_service2", result.getBlockedResourceId());
		assertEquals(BlockType.QPS, result.getBlockType());

		stat.addRequestRT(c2.resourceConfigs, startTimeSlotId, 1, true);
		
		result = stat.incrRequest(c2.resourceConfigs, startTimeSlotId);
		assertTrue(!result.isSuccess());
		assertEquals("testIncrRequestResultByResourceChain_service2", result.getBlockedResourceId());
		assertEquals(BlockType.QPS, result.getBlockType());
		
	}

	@Test
	public void testIncrRequestByResourceChain() throws Throwable {
		// create data
		List<FlowRuleCase> cases = createFlowRuleCase();
		long startTimeSlotId = stat.currentTimeSlotId();
		long endTimeSlotId = startTimeSlotId + 1000;
		for (FlowRuleCase c : cases) {
			ExecutorService pool = Executors.newFixedThreadPool(c.threads);
			long t1 = System.currentTimeMillis();
			for (int i = 0; i < c.threads; i++) {
				pool.submit(new ConcurrentJob1(c.requests, startTimeSlotId, c.resourceConfigs, c.expectResult));
			}
			pool.shutdown();
			if (pool.awaitTermination(5, TimeUnit.SECONDS)) {
				long t2 = System.currentTimeMillis();
				System.out.println("testIncrRequestByResourceChain elapsed time: " + (t2 - t1) + "ms for " + c.totalReqs
						+ " requests");
				for (int i = 0; i < c.resourceConfigs.size(); i++) {
					ResourceConfig cfg = c.resourceConfigs.get(i);
					ResourceExpect expect = c.resourceExpects.get(i);

					TimeWindowStat tws = stat.getTimeWindowStat(cfg.getResourceId(), startTimeSlotId, endTimeSlotId);
					assertEquals(expect.concurrents, tws.getPeakConcurrentReqeusts());
					assertEquals(expect.QPS, tws.getTotal());
					assertEquals(expect.total, tws.getTotal());
					assertEquals(expect.blockedReqs, tws.getBlockRequests());
				}
			} else {
				System.out.println("testIncrRequestByResourceChain timeout");
			}
			startTimeSlotId = startTimeSlotId + 1000;
			endTimeSlotId = endTimeSlotId + 1000;
		}
	}

	// @Test
	public void testPeakConcurrentJob() throws Throwable {
		long curTimeSlotId = stat.currentTimeSlotId();
		long nextSlotId = curTimeSlotId + 1000;
		String resourceId = "PeakConcurrentJob";
		stat.incrRequest(resourceId, curTimeSlotId, null, null);
		Thread.sleep(1200);
		TimeWindowStat tws = stat.getPreviousSecondStat(resourceId, nextSlotId + 1000);
		assertEquals(1, tws.getPeakConcurrentReqeusts());
	}

	@Test
	public void testIncr() throws Throwable {
		long curTimeSlotId = stat.currentTimeSlotId();
		long slotId = curTimeSlotId + 1000;
		String resourceId = "a";

		stat.incrRequest(resourceId, curTimeSlotId, null, null);
		TimeWindowStat tws = stat.getPreviousSecondStat(resourceId, slotId);
		assertEquals(1, tws.getTotal());

		stat.incrRequest(resourceId, curTimeSlotId, null, null);
		stat.addRequestRT(resourceId, curTimeSlotId, 100, false);
		stat.addRequestRT(resourceId, curTimeSlotId, 300, true);

		tws = stat.getPreviousSecondStat(resourceId, slotId);
		assertEquals(2, tws.getTotal());
		assertEquals(200, tws.getAvgRt());
		assertEquals(100, tws.getMin());
		assertEquals(300, tws.getMax());
		assertEquals(2, tws.getRps().intValue());
		assertEquals(1, tws.getErrors());

		stat.decrConcurrentRequest(resourceId, curTimeSlotId);
		Long con = stat.getConcurrentRequests(resourceId);
		assertEquals(1, con);

		// System.out.println(JacksonUtils.writeValueAsString(stat.resourceStats));
	}

	@Test
	public void testIncrRequest() throws Throwable {
		long curTimeSlotId = stat.currentTimeSlotId();
		long nextSlotId = curTimeSlotId + 1000;
		String resourceId = "b";
		Long maxCon = 10l;
		Long maxRPS = 20l;

		stat.incrRequest(resourceId, curTimeSlotId, maxCon, maxRPS);

		TimeWindowStat tws = stat.getTimeWindowStat(resourceId, curTimeSlotId, nextSlotId);
		long peakCon = tws.getPeakConcurrentReqeusts();
		assertEquals(1l, peakCon);
	}

	@Test
	public void testBlockedByMaxCon() throws Throwable {
		long curTimeSlotId = stat.currentTimeSlotId();
		long nextSlotId = curTimeSlotId + 1000;
		Long maxCon = 10l;
		Long maxRPS = 20l;
		int threads = 3;
		int requests = 1000;
		int totalRequests = threads * requests;
		String resourceId = "c";

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		long t1 = System.currentTimeMillis();
		for (int i = 0; i < threads; i++) {
			pool.submit(new ConcurrentJob(requests, curTimeSlotId, resourceId, maxCon, maxRPS));
		}
		pool.shutdown();
		if (pool.awaitTermination(20, TimeUnit.SECONDS)) {
			long t2 = System.currentTimeMillis();
			TimeWindowStat tws = stat.getTimeWindowStat(resourceId, curTimeSlotId, nextSlotId);
			assertEquals(maxCon, tws.getPeakConcurrentReqeusts());
			assertEquals(totalRequests - maxCon, tws.getBlockRequests());
			System.out.println("testBlockedByMaxCon total elapsed time for " + threads * requests + " requests："
					+ (t2 - t1) + "ms");
		} else {
			System.out.println("testIncrConcurrentRequest timeout");
		}
	}

	@Test
	public void testBlockedByMaxRPS() throws Throwable {
		long curTimeSlotId = stat.currentTimeSlotId();
		long nextSlotId = curTimeSlotId + 1000;
		Long maxCon = Long.MAX_VALUE;
		Long maxRPS = 20l;
		int threads = 3;
		int requests = 1000;
		int totalRequests = threads * requests;
		String resourceId = "c";

		for (int i = 0; i < maxRPS; i++) {
			stat.incrRequest(resourceId, curTimeSlotId, maxCon, maxRPS);
		}

		ExecutorService pool = Executors.newFixedThreadPool(threads);
		long t1 = System.currentTimeMillis();
		for (int i = 0; i < threads; i++) {
			pool.submit(new ConcurrentJob(requests, curTimeSlotId, resourceId, maxCon, maxRPS));
		}
		pool.shutdown();
		if (pool.awaitTermination(20, TimeUnit.SECONDS)) {
			long t2 = System.currentTimeMillis();
			TimeWindowStat tws = stat.getTimeWindowStat(resourceId, curTimeSlotId, nextSlotId);
			assertEquals(maxRPS, tws.getRps().intValue());
			assertEquals(totalRequests, tws.getBlockRequests());
			System.out.println("testIncrConcurrentRequest total elapsed time for " + threads * requests + " requests："
					+ (t2 - t1) + "ms");
		} else {
			System.out.println("testIncrConcurrentRequest timeout");
		}
	}

	@Test
	public void testStat() throws Throwable {
		// requests per slot per resource
		int requests = 30;
		int threads = 3;
		int resources = 3;
		int slots = 100;
		long rt = 100;
		long t1 = System.currentTimeMillis();
		long start = (t1 / FlowStat.INTERVAL) * FlowStat.INTERVAL;

		int totalRequests = requests * threads * resources * slots;

		ExecutorService pool = Executors.newFixedThreadPool(10);
		for (int i = 0; i < threads; i++) {
			pool.submit(new Job(requests, resources, slots, start, rt));
		}
		pool.shutdown();
		if (pool.awaitTermination(20, TimeUnit.SECONDS)) {
			long t2 = System.currentTimeMillis();

			long end = start + slots * FlowStat.INTERVAL;
			long nsecs = (end - start) / 1000;

			System.out.println("total requests：" + totalRequests);
			System.out.println("total elapsed time：" + (t2 - t1) + "ms");
			System.out.println("Testing Time Window：" + (end - start) + "ms");

			int resource1 = 1;
			int resource2 = 2;
			int rtBase1 = 1;
			int rtBase3 = 3;
			TimeWindowStat tws1 = stat.getTimeWindowStat("resource-" + resource1, start, end);
			TimeWindowStat tws2 = stat.getTimeWindowStat("resource-" + resource2, start, end);

			assertEquals(totalRequests / resources, tws1.getTotal());
			assertEquals(rt * rtBase1, tws1.getAvgRt());
			assertEquals(rt * rtBase1, tws1.getMin());
			assertEquals(rt * rtBase1, tws1.getMax());
			assertEquals(totalRequests / resources / nsecs, tws1.getRps().intValue());
			assertEquals(totalRequests / resources / 10, tws1.getErrors().intValue());
			System.out.println("RPS of resource1: " + tws1.getRps().intValue());

			assertEquals(totalRequests / resources, tws2.getTotal());
			assertEquals(rt * rtBase3, tws2.getAvgRt());
			assertEquals(rt * rtBase3, tws2.getMin());
			assertEquals(rt * rtBase3, tws2.getMax());
			assertEquals(totalRequests / resources / nsecs, tws2.getRps().intValue());
			assertEquals(totalRequests / resources / 10, tws2.getErrors().intValue());
			System.out.println("RPS of resource2: " + tws2.getRps().intValue());

			// performance of getTimeWindowStat
			for (int n = 0; n < 10; n++) {
				long t3 = System.currentTimeMillis();
//				int times = 100000;
				int times = 1000;
				for (int i = 0; i < times; i++) {
					stat.getTimeWindowStat("resource-" + resource1, start, end);
				}
				long t4 = System.currentTimeMillis();
				System.out.println("performance of getTimeWindowStat: " + (t4 - t3) + "ms " + times + " times");
				try {
					Thread.sleep(10);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// System.out.println(JacksonUtils.writeValueAsString(stat.resourceStats));

			List<ResourceTimeWindowStat> list = stat.getResourceTimeWindowStats("resource-" + 1, start, end + 3 * 1000,
					10);
			assertEquals(nsecs / 10, list.get(0).getWindows().size());
			System.out.println(JacksonUtils.writeValueAsString(list));
		} else {
			System.out.println("timeout");
		}

	}

	class Job implements Runnable {

		public Job(int requests, int resources, int slots, long startSlotId, long rt) {
			this.requests = requests;
			this.resources = resources;
			this.slots = slots;
			this.startSlotId = startSlotId;
			this.rt = rt;
		}

		private int requests = 0;
		private int resources = 0;
		private int slots = 0;
		private long startSlotId = 0;
		private long rt = 0;

		@Override
		public void run() {
			for (int m = 0; m < slots; m++) {
				for (int i = 0; i < requests; i++) {
					for (int j = 0; j < resources; j++) {
						stat.incrRequest("resource-" + j, startSlotId + (m * FlowStat.INTERVAL), null, null);
						// 10% error
						boolean isSuccess = i % 10 == 1 ? false : true;
						// rt will be triple while even
						stat.addRequestRT("resource-" + j, startSlotId + (m * FlowStat.INTERVAL),
								rt * (j % 2 == 0 ? 3 : 1), isSuccess);
					}
					try {
						// Thread.sleep(1);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	class ConcurrentJob implements Runnable {

		public ConcurrentJob(int requests, long curTimeSlotId, String resourceId, Long maxCon, Long maxRPS) {
			this.requests = requests;
			this.resourceId = resourceId;
			this.maxRPS = maxRPS;
			this.maxCon = maxCon;
			this.curTimeSlotId = curTimeSlotId;
		}

		private int requests = 0;
		private String resourceId;
		private Long maxCon = 0l;
		private Long maxRPS = 0l;
		private long curTimeSlotId = 0;

		@Override
		public void run() {
			for (int i = 0; i < requests; i++) {
				stat.incrRequest(resourceId, curTimeSlotId, maxCon, maxRPS);
			}
		}
	}

	@Test
	public void testGetResourceStat() throws Throwable {
		int threads = 3;
		int requests = 100;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		long t1 = System.currentTimeMillis();
		for (int i = 0; i < threads; i++) {
			pool.submit(new GetResourceStatJob(requests));
		}
		pool.shutdown();
		if (pool.awaitTermination(5, TimeUnit.SECONDS)) {
			System.out.println("testGetResourceStat done");
		} else {
			System.out.println("testGetResourceStat timeout");
		}
	}

	class GetResourceStatJob implements Runnable {

		public GetResourceStatJob(int requests) {
			this.requests = requests;
		}

		private int requests = 0;

		@Override
		public void run() {
			for (int i = 0; i < requests; i++) {
				try {
					ResourceStat rs = stat.getResourceStat("" + i);
					assertNotNull(rs);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
