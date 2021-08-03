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

/**
 * @author Francis Dong
 */
public class ResourceConfig {
    /**
     * Resouce ID
     */
    private String resourceId;

    /**
     * Maximum concurrent request, zero or negative for no limit
     */
    private long maxCon;

    /**
     * Maximum QPS, zero or negative for no limit
     */
    private long maxQPS;

    public ResourceConfig(String resourceId, long maxCon, long maxQPS) {
        this.resourceId = resourceId;
        this.maxCon = maxCon;
        this.maxQPS = maxQPS;
    }

    public ResourceConfig() {
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public long getMaxCon() {
        return maxCon;
    }

    public void setMaxCon(long maxCon) {
        this.maxCon = maxCon;
    }

    public long getMaxQPS() {
        return maxQPS;
    }

    public void setMaxQPS(long maxQPS) {
        this.maxQPS = maxQPS;
    }

}
