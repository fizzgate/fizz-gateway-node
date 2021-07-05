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
package we.fizz.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.noear.snack.ONode;

import com.alibaba.fastjson.JSON;

import reactor.core.publisher.Mono;
import we.fizz.StepContext;
import we.fizz.component.circle.Circle;
import we.fizz.component.condition.Condition;

/**
 * Condition component
 * 
 * @author Francis Dong
 *
 */
public class ComponentHelper {

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
	public static Mono<Object> run(List<IComponent> components, StepContext<String, Object> stepContext,
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
					if (!c.exec(ctxNode)) {
						return null;
					}
				}
			}

			if (circle != null) {
				return circle.exec(stepContext, stepCtxPos, f);
			}
//			// conditions before circle component
//			List<Condition> conditions1 = new ArrayList<>();
//			// conditions after circle component
//			List<Condition> conditions2 = new ArrayList<>();
//			Circle circle = null;
//			for (IComponent component : components) {
//				if (ComponentTypeEnum.CIRCLE == component.getType()) {
//					circle = (Circle) component;
//				}
//				if (circle == null && ComponentTypeEnum.CONDITION == component.getType()) {
//					conditions1.add((Condition) component);
//				}
//				if (circle != null && ComponentTypeEnum.CONDITION == component.getType()) {
//					conditions2.add((Condition) component);
//				}
//			}
//
//			if (conditions1 != null && conditions1.size() > 0) {
//				ONode ctxNode = toONode(stepContext);
//				for (Condition c : conditions1) {
//					if (!c.exec(ctxNode)) {
//						return null;
//					}
//				}
//			}
//
//			if (circle != null) {
//				return circle.exec(stepContext, (ctx) -> {
//					boolean canRun = true;
//					if (conditions2 != null && conditions2.size() > 0) {
//						ONode ctxNode = toONode(ctx);
//						for (Condition c : conditions2) {
//							if (!c.exec(ctxNode)) {
//								canRun = false;
//							}
//						}
//					}
//					if (canRun) {
//						return f.apply(ctx);
//					} else {
//						return Mono.empty();
//					}
//
//				});
//			}
		}
		return Mono.empty();
	}

}
