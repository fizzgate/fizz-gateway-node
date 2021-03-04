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

package com.fizzgate.fizz.examples.dubbo.apache.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.fizzgate.fizz.examples.dubbo.common.entity.ShoppingCart;
import com.fizzgate.fizz.examples.dubbo.common.entity.User;
import com.fizzgate.fizz.examples.dubbo.common.service.ShoppingCartService;
import org.springframework.stereotype.Service;


@Service("shoppingCartService")
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Override
    public User findByIdsAndName(List<Integer> ids, String name) {
        User test = new User();
        test.setId(ids.toString());
        test.setName("call findByIdsAndName ：" + name);
        return test;
    }

    @Override
    public User findByArrayIdsAndName(Integer[] ids, String name) {
        User test = new User();
        test.setId(Arrays.toString(ids));
        test.setName("call findByArrayIdsAndName ：" + name);
        return test;
    }

    @Override
    public User findByStringArray(String[] ids) {
        User test = new User();
        test.setId(Arrays.toString(ids));
        test.setName("call findByStringArray");
        return test;
    }

    @Override
    public User findByListId(List<String> ids) {
        User test = new User();
        test.setId(ids.toString());
        test.setName("call findByListId");
        return test;
    }

    @Override
    public User batchSave(List<User> userList) {
        User test = new User();
        test.setId(userList.stream().map(User::getId).collect(Collectors.joining("-")));
        test.setName("call batchSave :" + userList.stream().map(User::getName).collect(Collectors.joining("-")));
        return test;
    }

    @Override
    public User batchSaveAndNameAndId(List<User> userList, String id, String name) {
        User test = new User();
        test.setId(id);
        test.setName("call batchSaveAndNameAndId :" + name + ":" + userList.stream().map(User::getName).collect(Collectors.joining("-")));
        return test;
    }

    @Override
    public User saveShoppingCart(ShoppingCart shoppingCart) {
        User test = new User();
        test.setId(shoppingCart.getGoodsIdLists().toString());
        test.setName("call saveShoppingCart :" + shoppingCart.getUser().getName());
        return test;
    }

    @Override
    public User saveShoppingCartWithName(ShoppingCart shoppingCart, String name) {
        User test = new User();
        test.setId(shoppingCart.getGoodsIdLists().toString());
        test.setName("call saveShoppingCartWithName :" + shoppingCart.getUser().getName() + "-" + name);
        return test;
    }

}
