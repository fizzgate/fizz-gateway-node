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
package we.fizz.input.extension.mysql;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import reactor.core.publisher.Mono;
import we.fizz.input.IInput;
import we.fizz.input.Input;
import we.fizz.input.InputContext;
import we.fizz.input.InputType;

import java.util.HashMap;
import java.util.Map;


/**
 * 
 * @author linwaiwai
 *
 */


public class MySQLInput extends Input implements IInput {
    static public InputType TYPE = new InputType("MYSQL");
    protected Map<String, Object> request = new HashMap<>();
    protected Map<String, Object> response = new HashMap<>();
    public static Class inputConfigClass (){
        return MySQLInputConfig.class;
    }
    public void beforeRun(InputContext context){

    }

    public Mono<Map> run(){
        MySQLInputConfig inputConfig = (MySQLInputConfig) this.getConfig();
        ConnectionFactory connectionFactory = ConnectionFactories
                .get(inputConfig.getURL());
        Map<String, Object> binds= inputConfig.getBinds();

        Mono<Map> resultMap = Mono.from(connectionFactory.create())
                .flatMapMany(connection -> {
                    Statement statement = connection
                            .createStatement(inputConfig.getSql());
                    for (String bindIndex: binds.keySet()){
                        statement.bind(bindIndex, binds.get(bindIndex));
                    }
                    return statement.execute();
                }).flatMap(it -> {
//                    import dev.miku.r2dbc.mysql.MySqlResult
//                    import io.r2dbc.spi.RowMetadata
                    return it.map((row,rowMeta) -> {
                        Map<String ,Object> newRow = new HashMap<String, Object>();
                        for (String key : rowMeta.getColumnNames()){
                            newRow.put(key, row.get(key));
                        }
                        return newRow;
                    });
                }).collectList().flatMap(items -> {
                    Map<String, Object> result = new HashMap<String, Object>();
                    result.put("data", items);
                    result.put("request", this);

                    // 把请求信息放入stepContext
                    Map<String, Object> group = new HashMap<>();
                    group.put("request", request);
                    group.put("response", response);
                    this.stepResponse.getRequests().put(name, group);
                    response.put("body", items);

                    return Mono.just(result);
                });
        return resultMap;
    }


}
