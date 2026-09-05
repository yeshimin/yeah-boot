package com.yeshimin.yeahboot.app.service;

import com.yeshimin.yeahboot.data.domain.entity.AppUserEntity;
import com.yeshimin.yeahboot.data.repository.AppUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepo appUserRepo;

    public AppUserEntity detail(Long userId) {
        return appUserRepo.findOneById(userId);
    }
}
