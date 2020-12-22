package we.stats;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Francis Dong
 *
 */
public class RouteTimeWindowStat {

	/**
	 * Route ID
	 */
	private String routeId;

	private List<TimeWindowStat> windows = new ArrayList<>();

	public RouteTimeWindowStat(String routeId) {
		this.routeId = routeId;
	}

	public String getRouteId() {
		return routeId;
	}

	public void setRouteId(String routeId) {
		this.routeId = routeId;
	}

	public List<TimeWindowStat> getWindows() {
		return windows;
	}

	public void setWindows(List<TimeWindowStat> windows) {
		this.windows = windows;
	}

}
