package cn.cf.sharding.service.impl;

import cn.cf.sharding.entity.UserInfo;
import cn.cf.sharding.service.UserInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class UserInfoServiceImplTest {
@Autowired
private UserInfoService userInfoService;
    @Test
    public void saveUserInfo() {
        UserInfo userInfo= new UserInfo();
        userInfo.setUserName("张三");
        userInfo.setUserId(14);
        userInfoService.saveUserInfo(userInfo);
    }
}