package cn.cf.sharding.service;

import cn.cf.sharding.entity.UserInfo;

import java.util.Optional;

public interface UserInfoService {
    Optional<UserInfo> getUserInfo(long userId);


    UserInfo saveUserInfo(UserInfo userInfo);
}
