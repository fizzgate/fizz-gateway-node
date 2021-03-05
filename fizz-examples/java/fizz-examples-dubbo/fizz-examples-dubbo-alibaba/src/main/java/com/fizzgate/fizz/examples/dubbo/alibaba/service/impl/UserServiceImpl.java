package com.fizzgate.fizz.examples.dubbo.alibaba.service.impl;

import java.util.Random;
import com.fizzgate.fizz.examples.dubbo.common.entity.User;
import com.fizzgate.fizz.examples.dubbo.common.service.UserService;
import org.springframework.stereotype.Service;

/**
 * UserServiceImpl.
 *
 * @author linwaiwai
 */
@Service("userService")
public class UserServiceImpl implements UserService {

    @Override
    public User findById(final String id) {
        User user = new User();
        user.setId(id);
        user.setName("call findById");
        return user;
    }

    @Override
    public User findAll() {
        User user = new User();
        user.setName("call findAll");
        user.setId(String.valueOf(new Random().nextInt()));
        return user;
    }

    @Override
    public User insert(final User user) {
        user.setName("call insert and vo name is " + user.getName());
        return user;
    }
}
