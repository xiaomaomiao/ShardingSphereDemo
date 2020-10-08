package cn.cf.sharding.service.impl;


import cn.cf.sharding.entity.UserInfo;
import cn.cf.sharding.repository.UserInfoRepository;
import cn.cf.sharding.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class UserInfoServiceImpl implements UserInfoService {
    @Autowired
    private UserInfoRepository userInfoRepository;

    @Override

    public Optional<UserInfo> getUserInfo(long userId) {
        return userInfoRepository.findById(userId);
    }

    @Override
    public UserInfo saveUserInfo(UserInfo userInfo) {
        return userInfoRepository.save(userInfo);
    }
}
