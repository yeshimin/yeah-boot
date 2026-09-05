package com.yeshimin.yeahboot.admin.auth;

import com.yeshimin.yeahboot.auth.domain.model.UserDetail;
import com.yeshimin.yeahboot.auth.service.UserDetailService;
import com.yeshimin.yeahboot.common.common.enums.AuthSubjectEnum;
import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.upms.domain.vo.UserRolesAndResourcesVo;
import com.yeshimin.yeahboot.upms.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminUserDetailService implements UserDetailService {

    private final SysUserService sysUserService;

    @Override
    public UserDetail getUserDetail(String userId) {
        UserRolesAndResourcesVo resultVo = sysUserService.queryUserRolesAndResources(Long.valueOf(userId));
        if (resultVo == null) {
            throw new BaseException(ErrorCodeEnum.FAIL);
        }

        AdminUserDetail userDetail = new AdminUserDetail();
        userDetail.setUsername(resultVo.getUser().getUsername());
        userDetail.setRoles(resultVo.getRoles());
        userDetail.setResources(resultVo.getResources());
        return userDetail;
    }

    @Override
    public String getSubject() {
        return AuthSubjectEnum.ADMIN.getValue();
    }
}
