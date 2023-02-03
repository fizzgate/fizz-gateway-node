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

package com.fizzgate.fizz.input;

import org.springframework.context.ConfigurableApplicationContext;

import com.fizzgate.fizz.Step;
import com.fizzgate.fizz.StepContext;
import com.fizzgate.fizz.StepResponse;

import reactor.core.publisher.Mono;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.Map;

public interface IInput {
    public static final InputType TYPE = null;
    public static Class inputConfigClass() {
        return null;
    }
    public String getName() ;
    public boolean needRun(StepContext<String, Object> stepContext);
    public void beforeRun(InputContext context);
    public Mono<Map> run();

    public StepResponse getStepResponse() ;
    public void setStepResponse(StepResponse stepResponse);
    public SoftReference<Step> getWeakStep();
    public void setWeakStep(SoftReference<Step> weakStep);

    public ConfigurableApplicationContext getCurrentApplicationContext();

}
