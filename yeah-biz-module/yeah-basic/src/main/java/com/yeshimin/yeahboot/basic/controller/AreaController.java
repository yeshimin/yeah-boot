package com.yeshimin.yeahboot.basic.controller;

import com.yeshimin.yeahboot.data.domain.vo.AreaVo;
import com.yeshimin.yeahboot.service.AreaService;
import com.yeshimin.yeahboot.common.controller.base.BaseController;
import com.yeshimin.yeahboot.common.domain.base.R;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 地区相关
 */
@RestController
@RequestMapping("/area")
@RequiredArgsConstructor
public class AreaController extends BaseController {

    private final AreaService areaService;

    /**
     * tree
     */
    @PreAuthorize("@pms.hasPermission('admin:area:tree')")
    @GetMapping("/tree")
    public R<List<AreaVo>> tree(
            @RequestParam(value = "maxLevel", required = false, defaultValue = "3") Integer maxLevel) {
        // 暂时限制最大级别为3，即省市区
        if (maxLevel > 3) {
            maxLevel = 3;
        }
        return R.ok(areaService.tree(maxLevel));
    }
}
