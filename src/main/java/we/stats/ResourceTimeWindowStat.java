package we.stats;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Francis Dong
 *
 */
public class ResourceTimeWindowStat {

	/**
	 * Resource ID
	 */
	private String resourceId;

	private List<TimeWindowStat> windows = new ArrayList<>();

	public ResourceTimeWindowStat(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public List<TimeWindowStat> getWindows() {
		return windows;
	}

	public void setWindows(List<TimeWindowStat> windows) {
		this.windows = windows;
	}

}
