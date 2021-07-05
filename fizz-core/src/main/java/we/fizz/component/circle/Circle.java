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

package we.fizz.component.circle;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.noear.snack.ONode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Data;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import we.fizz.StepContext;
import we.fizz.component.ComponentHelper;
import we.fizz.component.ComponentTypeEnum;
import we.fizz.component.IComponent;
import we.fizz.component.StepContextPosition;
import we.fizz.component.ValueTypeEnum;
import we.fizz.component.condition.Condition;
import we.fizz.exception.FizzRuntimeException;
import we.fizz.input.PathMapping;
import we.fizz.input.RPCInput;

/**
 * Circle component
 * 
 * @author Francis Dong
 *
 */
public class Circle implements IComponent {

	private static final String type = ComponentTypeEnum.CIRCLE.getCode();

	private String desc;

	private ValueTypeEnum dataSourceType;

	private Object dataSource;

	private List<Condition> execConditions;

	private List<Condition> breakConditions;

	@Override
	public ComponentTypeEnum getType() {
		return ComponentTypeEnum.getEnumByCode(type);
	}

	/**
	 * 
	 * @param desc            [optional] description
	 * @param dataSourceType  [required] type of data source
	 * @param dataSource      [required] data source
	 * @param execConditions  [optional] conditions to execute current circle loop
	 *                        item
	 * @param breakConditions [optional] conditions to break circle
	 */
	public Circle(String desc, ValueTypeEnum dataSourceType, Object dataSource, List<Condition> execConditions,
			List<Condition> breakConditions) {
		this.desc = desc;
		this.dataSourceType = dataSourceType;
		this.dataSource = dataSource;
		this.execConditions = execConditions;
		this.breakConditions = breakConditions;
	}

	/**
	 * Current item
	 */
	private Object currentItem;

	/**
	 * Index of current item
	 */
	private Integer index;

	/**
	 * Fixed value of dataSource
	 */
	private Integer fixedValue;

	/**
	 * Reference value of dataSource
	 */
	private List<Object> refValue;

	private boolean refReadFlag;

	private Integer getFixedValue(ONode ctxNode) {
		if (fixedValue != null) {
			return fixedValue;
		}
		if (dataSource == null) {
			return fixedValue;
		}
		if (dataSource instanceof Integer || dataSource instanceof Long) {
			fixedValue = Integer.valueOf(dataSource.toString());
			if (fixedValue.intValue() < 1) {
				throw new FizzRuntimeException("invalid data source, fixed data source must be a positive integer");
			}
			return fixedValue;
		} else {
			throw new FizzRuntimeException("invalid data source, fixed data source must be a positive integer");
		}
	}

	@SuppressWarnings("unchecked")
	private List<Object> getRefValue(ONode ctxNode) {
		if (refReadFlag) {
			return refValue;
		}
		Object value = PathMapping.getValueByPath(ctxNode, (String) dataSource);
		if (value == null) {
			return null;
		}
		if (value instanceof Collection) {
			refValue = (List<Object>) value;
			return refValue;
		} else {
			throw new FizzRuntimeException("invalid data source, referenced data source must be a array");
		}
	}

	/**
	 * Returns next circle item, returns null if no item left or dataSource is null
	 * 
	 * @return
	 */
	public CircleItem next(ONode ctxNode) {
		if (ValueTypeEnum.FIXED.equals(dataSourceType)) {
			Integer total = this.getFixedValue(ctxNode);
			if (index == null) {
				index = 0;
				currentItem = index;
				return new CircleItem(currentItem, index);
			} else if (index.intValue() < total.intValue() - 1) {
				index = index + 1;
				currentItem = index;
				return new CircleItem(currentItem, index);
			} else {
				return null;
			}
		} else if (ValueTypeEnum.REF.equals(dataSourceType)) {
			List<Object> list = this.getRefValue(ctxNode);
			if (index == null) {
				index = 0;
				currentItem = list.get(index);
				return new CircleItem(currentItem, index);
			} else if (index.intValue() < list.size() - 1) {
				index = index + 1;
				currentItem = list.get(index);
				return new CircleItem(currentItem, index);
			} else {
				return null;
			}
		}
		return null;
	}

	/**
	 * Returns true if execConditions are all true, false otherwise
	 * 
	 * @param ctxNode
	 * @return
	 */
	public boolean canExec(ONode ctxNode) {
		if (this.execConditions != null && this.execConditions.size() > 0) {
			try {
				for (Condition condition : execConditions) {
					if (!condition.exec(ctxNode)) {
						return false;
					}
				}
			} catch (FizzRuntimeException e) {
				throw new FizzRuntimeException(type + " " + e.getMessage(), e.getCause());
			}
		}
		return true;
	}

	/**
	 * Returns true if breakConditions are all true, false otherwise
	 * 
	 * @param ctxNode
	 * @return
	 */
	public boolean breakCircle(ONode ctxNode) {
		if (this.breakConditions != null && this.breakConditions.size() > 0) {
			try {
				for (Condition condition : breakConditions) {
					if (condition.exec(ctxNode)) {
						return true;
					}
				}
			} catch (FizzRuntimeException e) {
				throw new FizzRuntimeException(type + " " + e.getMessage(), e.getCause());
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public Mono<Object> exec(StepContext<String, Object> stepContext, StepContextPosition stepCtxPos,
			BiFunction<StepContext, StepContextPosition, Mono> f) {
		ONode ctxNode = ComponentHelper.toONode(stepContext);
		CircleItem nextItem = this.next(ctxNode);
		if (nextItem != null) {
			return Mono.just(new CircleItemResult(nextItem, null)).expand(circleItemResult -> {
				// put nextItem to step context and ctxNode for further JSON path mapping
				CircleItem cItem = circleItemResult.nextItem;
				if (stepCtxPos.getRequestName() != null) {
					stepContext.setRequestCircleItem(stepCtxPos.getStepName(), stepCtxPos.getRequestName(),
							cItem.getItem(), cItem.getIndex());
				} else {
					stepContext.setStepCircleItem(stepCtxPos.getStepName(), cItem.getItem(), cItem.getIndex());
				}
				PathMapping.setByPath(ctxNode, stepCtxPos.getPath() + ".item", cItem.getItem(), true);
				PathMapping.setByPath(ctxNode, stepCtxPos.getPath() + ".index", cItem.getIndex(), true);

				if (!this.canExec(ctxNode)) {
					return Mono.just(new CircleItemResult(this.next(ctxNode), null));
				}
				if (this.breakCircle(ctxNode)) {
					return Mono.empty();
				}
				return f.apply(stepContext, stepCtxPos).flatMap(r -> {
					CircleItem nextItem2 = this.next(ctxNode);
					if (nextItem2 == null) {
						return Mono.empty();
					}
					return Mono.just(new CircleItemResult(nextItem2, r));
				});
			}).flatMap(circleItemResult -> Flux.just(circleItemResult)).collectList().flatMap(list -> {
				if (list != null && list.size() > 0) {
					Collections.reverse(list);
					for (int i = 0; i < list.size(); i++) {
						if (list.get(i).result != null) {
							return Mono.just(list.get(i).result);
						}
					}
				}
				return Mono.empty();
			});
		} else {
			return Mono.empty();
		}
	}

	@Data
	class CircleItemResult {
		private CircleItem nextItem;
		private Object result;

		public CircleItemResult(CircleItem nextItem, Object result) {
			this.nextItem = nextItem;
			this.result = result;
		}
	}

}
