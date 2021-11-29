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
package we.dedicatedline.proxy.server;

import lombok.Data;
import we.dedicatedline.proxy.client.ProxyClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Francis Dong
 *
 */
@Data
public class ChannelManager {

	private Map<String, ProxyClient> channelMap;

	public ChannelManager() {
		channelMap = new HashMap<>();
	}

}
