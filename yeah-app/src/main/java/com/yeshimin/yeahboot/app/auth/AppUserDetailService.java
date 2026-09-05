package com.yeshimin.yeahboot.app.auth;

import com.yeshimin.yeahboot.auth.domain.model.UserDetail;
import com.yeshimin.yeahboot.auth.service.UserDetailService;
import com.yeshimin.yeahboot.common.common.enums.AuthSubjectEnum;
import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.data.domain.entity.AppUserEntity;
import com.yeshimin.yeahboot.data.repository.AppUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailService implements UserDetailService {

    private final AppUserRepo appUserRepo;

    @Override
    public UserDetail getUserDetail(String userId) {
        AppUserEntity user = appUserRepo.findOneById(Long.valueOf(userId));
        if (user == null) {
            throw new BaseException(ErrorCodeEnum.FAIL);
        }

        AppUserDetail userDetail = new AppUserDetail();
        userDetail.setUsername(user.getMobile());
        return userDetail;
    }

    @Override
    public String getSubject() {
        return AuthSubjectEnum.APP.getValue();
    }
}
