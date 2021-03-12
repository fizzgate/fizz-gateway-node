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

package we.constants;

/**
 * @author unknown
 */
public class CommonConstants {
	
	/**
	 * traceId for log
	 */
	public static final String TRACE_ID = "traceId";
	
	/**
	 * Header key to transfer traceId
	 */
	public static final String HEADER_TRACE_ID = "X-TRACE-ID";
	
	
	/**
	 * Prefix of traceId
	 */
	public static final String TRACE_ID_PREFIX = "fizz-";
	
	
	/**
	 * Star WildCard for PathMapping
	 */
	public static final String WILDCARD_STAR = "*";
	
	
	/**
	 * Tilde WildCard for PathMapping
	 */
	public static final String WILDCARD_TILDE = "~";
	
	
	/**
	 * Stop the underlying processes and response immediately, using in scripts
	 */
	public static final String STOP_AND_RESPONSE_KEY = "_stopAndResponse";
	
	/**
	 * Stop the underlying processes and redirect to the specified URL immediately, work with STOP_AND_RESPONSE_KEY using in scripts
	 */
	public static final String REDIRECT_URL_KEY = "_redirectUrl";
	

}
