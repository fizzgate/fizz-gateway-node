/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fizzgate.fizz.examples.dubbo.common.service;

import java.util.List;

import com.fizzgate.fizz.examples.dubbo.common.entity.User;
import com.fizzgate.fizz.examples.dubbo.common.entity.ShoppingCart;

/**
 * The interface shopping cart service with multi params.
 */
public interface ShoppingCartService {

    /**
     * Find by ids and name.
     * body: {"ids":["1232","456"],"name":"hello world"}
     *
     * @param ids  the ids
     * @param name the name
     * @return the user
     */
    User findByIdsAndName(List<Integer> ids, String name);

    /**
     * Find by array ids and name.
     * <p>
     * body :{"ids":[123,4561],"name":"hello world"}
     *
     * @param ids  the ids
     * @param name the name
     * @return the user
     */
    User findByArrayIdsAndName(Integer[] ids, String name);

    /**
     * Find by string array.
     * body :{"ids":["1232","456"]}
     *
     * @param ids the ids
     * @return the user
     */
    User findByStringArray(String[] ids);

    /**
     * Find by list id.
     * body :{"ids":["1232","456"]}
     *
     * @param ids the ids
     * @return the user
     */
    User findByListId(List<String> ids);

    /**
     * Batch save.
     * body :{"userList":[{"id":"123","name":"linwaiwai"},{"id":"456","name":"myth"}]}
     *
     * @param userList the user list
     * @return the user
     */
    User batchSave(List<User> userList);

    /**
     * Batch save and name and id dubbo test.
     * <p>
     * body: {"userList":[{"id":"123","name":"linwaiwai"},{"id":"456","name":"myth"}],"id":"789","name":"ttt"}
     *
     * @param userList the user list
     * @param id            the id
     * @param name          the name
     * @return the user
     */
    User batchSaveAndNameAndId(List<User> userList, String id, String name);


    /**
     * Save shoppingCart bean.
     * body : {"user":{"id":"123","name":"linwaiwai"},"goodsIdList":["456","789"],"detailGoodsMaps":{"id2":"2","id1":"1"}}
     *
     * @param shoppingCart
     * @return the user
     */
    User saveShoppingCart(ShoppingCart shoppingCart);


    /**
     * Save shopping cart bean with name.
     * body : {"shoppingCart":{"dubboTest":{"id":"123","name":"linwaiwai"},"goodsIdList":["456","789"],"detailGoodsMaps":{"id2":"2","id1":"1"}},"name":"linwaiwai"}
     * @param shoppingCart
     * @param name
     * @return the user
     */
    User saveShoppingCartWithName(ShoppingCart shoppingCart, String name);
}
