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
package com.fizzgate.fizz.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.noear.snack.ONode;

import com.alibaba.fastjson.JSON;
import com.fizzgate.fizz.StepContext;
import com.fizzgate.fizz.component.circle.Circle;
import com.fizzgate.fizz.component.condition.Condition;

import reactor.core.publisher.Mono;

/**
 * Condition component
 * 
 * @author Francis Dong
 *
 */
public class ComponentExecutor {

	/**
	 * Converts step context to ONode
	 * 
	 * @param stepContext context
	 * @return
	 */
	public static ONode toONode(StepContext<String, Object> stepContext) {
		ONode o = null;
		synchronized (stepContext) {
			o = ONode.loadObj(stepContext);
		}
		return o;
	}

	public static List<IComponent> buildComponents(List<Map<String, Object>> componentConfig) {
		List<IComponent> components = new ArrayList<>();

		if (componentConfig != null && componentConfig.size() > 0) {
			for (Map<String, Object> m : componentConfig) {
				// condition
				if (ComponentTypeEnum.CONDITION.getCode().equals(m.get("type"))) {
					Condition c = JSON.parseObject(JSON.toJSONString(m), Condition.class);
					components.add(c);
				}

				// circle
				if (ComponentTypeEnum.CIRCLE.getCode().equals(m.get("type"))) {
					Circle c = JSON.parseObject(JSON.toJSONString(m), Circle.class);
					components.add(c);
				}
			}
		}

		return components;
	}

	/**
	 * 
	 * @param components
	 * @param stepContext
	 * @param f
	 */
	public static Mono<Object> exec(List<IComponent> components, StepContext<String, Object> stepContext,
			StepContextPosition stepCtxPos, BiFunction<StepContext, StepContextPosition, Mono> f) {
		if (components != null && components.size() > 0) {
			// conditions before circle component
			List<Condition> conditions = new ArrayList<>();
			Circle circle = null;
			for (IComponent component : components) {
				if (ComponentTypeEnum.CIRCLE == component.getType()) {
					circle = (Circle) component;
				}
				if (circle == null && ComponentTypeEnum.CONDITION == component.getType()) {
					conditions.add((Condition) component);
				}
			}

			if (conditions != null && conditions.size() > 0) {
				ONode ctxNode = toONode(stepContext);
				for (Condition c : conditions) {
					boolean rs = c.exec(ctxNode);
					stepContext.addConditionResult(stepCtxPos.getStepName(), stepCtxPos.getRequestName(), c.getDesc(),
							rs);
					if (!rs) {
						return Mono.just(new ComponentResult());
					}
				}
			}

			if (circle != null) {
				return circle.exec(stepContext, stepCtxPos, f);
			} else {
				return f.apply(stepContext, stepCtxPos);
			}
		}
		return Mono.just(new ComponentResult());
	}

}
