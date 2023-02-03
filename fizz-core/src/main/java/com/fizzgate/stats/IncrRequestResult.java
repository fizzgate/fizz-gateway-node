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

package com.fizzgate.stats;

/**
 * 
 * @author Francis Dong
 *
 */
public class IncrRequestResult {

	/**
	 * true if success, otherwise false
	 */
	private boolean success;

	/**
	 * Resource ID that causes block
	 */
	private String blockedResourceId;

	/**
	 * block type
	 */
	private BlockType blockType;

	public static IncrRequestResult success() {
		return new IncrRequestResult(true, null, null);
	}
	
	public static IncrRequestResult block(String resourceId, BlockType blockType) {
		return new IncrRequestResult(false, resourceId, blockType);
	}

	public IncrRequestResult(boolean success, String resourceId, BlockType blockType) {
		this.success = success;
		this.blockedResourceId = resourceId;
		this.blockType = blockType;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public String getBlockedResourceId() {
		return blockedResourceId;
	}

	public void setBlockedResourceId(String blockedResourceId) {
		this.blockedResourceId = blockedResourceId;
	}

	public BlockType getBlockType() {
		return blockType;
	}

	public void setBlockType(BlockType blockType) {
		this.blockType = blockType;
	}

}
