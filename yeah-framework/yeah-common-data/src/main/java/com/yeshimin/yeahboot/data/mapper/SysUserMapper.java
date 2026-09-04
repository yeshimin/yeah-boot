package com.yeshimin.yeahboot.data.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yeshimin.yeahboot.data.domain.vo.SysUserAuthResourceVo;
import com.yeshimin.yeahboot.data.domain.dto.SysUserQueryDto;
import com.yeshimin.yeahboot.data.domain.entity.SysUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUserEntity> {

    /**
     * 查询用户列表
     */
    IPage<SysUserEntity> query(Page<SysUserEntity> page, @Param("query") SysUserQueryDto dto);

    /**
     * 查询用户运行时鉴权所需的有效角色编码和权限标识
     */
    List<SysUserAuthResourceVo> queryAuthResources(@Param("userId") Long userId);
}
