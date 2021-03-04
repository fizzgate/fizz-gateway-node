package com.fizzgate.fizz.examples.dubbo.alibaba.service.impl;

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
        test.setName("call saveComplexBeanTest :" + shoppingCart.getUser().getName());
        return test;
    }

    @Override
    public User saveShoppingCartWithName(ShoppingCart shoppingCart, String name) {
        User test = new User();
        test.setId(shoppingCart.getGoodsIdLists().toString());
        test.setName("call saveComplexBeanTestAndName :" + shoppingCart.getUser().getName() + "-" + name);
        return test;
    }

}
