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

package we.fizz.input;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author linwaiwai
 *
 */

public class InputType {

    private final String type;
    static private Map<String,InputType > inputs = new HashMap<String,InputType >();
    public InputType(String aType) {
         this.type = aType;
        inputs.put(aType, this);
    }

    public static InputType valueOf(String string) {
        return inputs.get(string);
    }
    public String toString(){
        return type;
    }

}