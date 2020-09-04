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

package we.exception;

/**
 * @author unknown
 */
public class StopAndResponseException extends RuntimeException {
	
	private String data;

	public StopAndResponseException(String message, String data) {
		super(message);
		this.data = data;
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
	
}
