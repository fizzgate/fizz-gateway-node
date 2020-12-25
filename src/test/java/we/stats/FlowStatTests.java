package we.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import we.stats.FlowStat;
import we.stats.TimeWindowStat;
import we.util.JacksonUtils;

/**
 * 
 * @author Francis Dong
 *
 */
public class FlowStatTests {

	private FlowStat stat = new FlowStat();

	@Test
	public void testIncr() throws Throwable {
		long t = stat.currentTimeSlotId();
		long slotId = t + 1000;
		String resourceId = "a";

		stat.incrRequestToTimeSlot(resourceId, t, 100l, true);
		TimeWindowStat tws = stat.getPreviousSecondStat(resourceId, slotId);
		assertEquals(1, tws.getTotal());

		stat.incrRequestToTimeSlot(resourceId, t, 300l, false);
		tws = stat.getPreviousSecondStat(resourceId, slotId);
		assertEquals(2, tws.getTotal());
		assertEquals(200, tws.getAvgRt());
		assertEquals(100, tws.getMin());
		assertEquals(300, tws.getMax());
		assertEquals(2, tws.getRps().intValue());
		assertEquals(1, tws.getErrors());

		// System.out.println(JacksonUtils.writeValueAsString(stat.resourceStats));
	}

	@Test
	public void testStat() throws Throwable {
		// requests per slot per resource
		int requests = 100;
		int threads = 10;
		int resources = 10;
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
			TimeWindowStat tws = stat.getTimeWindowStat(FlowStat.ALL_RESOURCES, start, end);

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

			assertEquals(totalRequests, tws.getTotal());
			assertEquals((rt * rtBase1 + rt * rtBase3) / 2, tws.getAvgRt());
			assertEquals(rt * rtBase1, tws.getMin());
			assertEquals(rt * rtBase3, tws.getMax());
			assertEquals(totalRequests / nsecs, tws.getRps().intValue());
			assertEquals(totalRequests / 10, tws.getErrors().intValue());
			System.out.println("RPS of all resources: " + tws.getRps().intValue());

			// performance of getTimeWindowStat
			for (int n = 0; n < 10; n++) {
				long t3 = System.currentTimeMillis();
				int times = 100000;
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

			List<ResourceTimeWindowStat> list = stat.getResourceTimeWindowStats("resource-" + 1, start, end, 10);
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
						stat.incrConcurrentRequest("resource-" + j);
						// 10% error
						boolean isSuccess = i % 10 == 1 ? false : true;
						// rt will be triple while even
						stat.incrRequestToTimeSlot("resource-" + j, startSlotId + (m * FlowStat.INTERVAL),
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

}
