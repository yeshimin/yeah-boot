package com.yeshimin.yeahboot.service;

import com.yeshimin.yeahboot.common.domain.base.BaseEntity;
import com.yeshimin.yeahboot.data.domain.entity.*;
import com.yeshimin.yeahboot.data.domain.vo.AreaVo;
import com.yeshimin.yeahboot.data.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaProvinceRepo areaProvinceRepo;
    private final AreaCityRepo areaCityRepo;
    private final AreaDistrictRepo areaDistrictRepo;
    private final AreaStreetRepo areaStreetRepo;
    private final AreaVillageRepo areaVillageRepo;

    /**
     * 查询省份
     */
    public List<AreaVo> queryProvince() {
        return this.getProvinceList(null);
    }

    /**
     * 查询城市
     */
    public List<AreaVo> queryCity(String provinceCode) {
        return areaCityRepo.lambdaQuery()
                .eq(AreaCityEntity::getParentCode, provinceCode)
                .select(AreaCityEntity::getParentCode, AreaCityEntity::getCode, AreaCityEntity::getName, BaseEntity::getId)
                .list()
                .stream().map(e -> {
                    AreaVo vo = new AreaVo();
                    vo.setId(e.getId());
                    vo.setParentCode(e.getParentCode());
                    vo.setCode(e.getCode());
                    vo.setName(e.getName());
                    vo.setChildren(new ArrayList<>());
                    return vo;
                }).collect(Collectors.toList());
    }

    /**
     * 查询区县
     */
    public List<AreaVo> queryDistrict(String cityCode) {
        return areaDistrictRepo.lambdaQuery()
                .eq(AreaDistrictEntity::getParentCode, cityCode)
                .select(AreaDistrictEntity::getParentCode, AreaDistrictEntity::getCode, AreaDistrictEntity::getName, BaseEntity::getId)
                .list()
                .stream().map(e -> {
                    AreaVo vo = new AreaVo();
                    vo.setId(e.getId());
                    vo.setParentCode(e.getParentCode());
                    vo.setCode(e.getCode());
                    vo.setName(e.getName());
                    vo.setChildren(new ArrayList<>());
                    return vo;
                }).collect(Collectors.toList());
    }

    public List<AreaVo> tree(Integer maxLevel) {
        List<AreaVo> listVillage = null;
        List<AreaVo> listStreet = null;
        List<AreaVo> listDistrict = null;
        List<AreaVo> listCity = null;
        List<AreaVo> listProvince = null;

        if (maxLevel >= 5) {
            listStreet = this.getStreetList(listStreet);
            Map<String, AreaVo> mapStreet = listStreet.stream().collect(Collectors.toMap(AreaVo::getCode, e -> e));

            listVillage = this.getVillageList(listVillage);
            Map<String, List<AreaVo>> mapVillages =
                    listVillage.stream().collect(Collectors.groupingBy(AreaVo::getParentCode));

            // 遍历mapVillage
            for (Map.Entry<String, List<AreaVo>> entry : mapVillages.entrySet()) {
                String key = entry.getKey();
                List<AreaVo> value = entry.getValue();

                AreaVo parent = mapStreet.get(key);
                if (parent != null) {
                    parent.setChildren(value);
                }
            }
        }

        if (maxLevel >= 4) {
            listDistrict = this.getDistrictList(listDistrict);
            listStreet = this.getStreetList(listStreet);
            Map<String, AreaVo> mapDistrict = listDistrict.stream().collect(Collectors.toMap(AreaVo::getCode, e -> e));
            Map<String, List<AreaVo>> mapStreets =
                    listStreet.stream().collect(Collectors.groupingBy(AreaVo::getParentCode));
            // 遍历mapStreet
            for (Map.Entry<String, List<AreaVo>> entry : mapStreets.entrySet()) {
                String key = entry.getKey();
                List<AreaVo> value = entry.getValue();

                AreaVo parent = mapDistrict.get(key);
                if (parent != null) {
                    parent.setChildren(value);
                }
            }
        }

        if (maxLevel >= 3) {
            listCity = this.getCityList(listCity);
            listDistrict = this.getDistrictList(listDistrict);
            Map<String, AreaVo> mapCity = listCity.stream().collect(Collectors.toMap(AreaVo::getCode, e -> e));
            Map<String, List<AreaVo>> mapDistricts =
                    listDistrict.stream().collect(Collectors.groupingBy(AreaVo::getParentCode));
            // 遍历mapDistrict
            for (Map.Entry<String, List<AreaVo>> entry : mapDistricts.entrySet()) {
                String key = entry.getKey();
                List<AreaVo> value = entry.getValue();

                AreaVo parent = mapCity.get(key);
                if (parent != null) {
                    parent.setChildren(value);
                }
            }
        }

        if (maxLevel >= 2) {
            listProvince = this.getProvinceList(listProvince);
            listCity = this.getCityList(listCity);
            Map<String, AreaVo> mapProvince = listProvince.stream().collect(Collectors.toMap(AreaVo::getCode, e -> e));
            Map<String, List<AreaVo>> mapCitys =
                    listCity.stream().collect(Collectors.groupingBy(AreaVo::getParentCode));
            // 遍历mapCity
            for (Map.Entry<String, List<AreaVo>> entry : mapCitys.entrySet()) {
                String key = entry.getKey();
                List<AreaVo> value = entry.getValue();

                AreaVo parent = mapProvince.get(key);
                if (parent != null) {
                    parent.setChildren(value);
                }
            }
        }

        if (maxLevel >= 1) {
            listProvince = this.getProvinceList(listProvince);
        }

        return listProvince;
    }

    // ================================================================================

    private List<AreaVo> getVillageList(List<AreaVo> list) {
        if (list != null) {
            return list;
        }
        return areaVillageRepo.lambdaQuery()
                .select(AreaVillageEntity::getParentCode, AreaVillageEntity::getCode, AreaVillageEntity::getName, BaseEntity::getId)
                .list()
                .stream().map(e -> {
                    AreaVo vo = new AreaVo();
                    vo.setId(e.getId());
                    vo.setParentCode(e.getParentCode());
                    vo.setCode(e.getCode());
                    vo.setName(e.getName());
                    vo.setChildren(new ArrayList<>());
                    return vo;
                }).collect(Collectors.toList());
    }

    private List<AreaVo> getStreetList(List<AreaVo> list) {
        if (list != null) {
            return list;
        }
        return areaStreetRepo.lambdaQuery()
                .select(AreaStreetEntity::getParentCode, AreaStreetEntity::getCode, AreaStreetEntity::getName, BaseEntity::getId)
                .list()
                .stream().map(e -> {
                    AreaVo vo = new AreaVo();
                    vo.setId(e.getId());
                    vo.setParentCode(e.getParentCode());
                    vo.setCode(e.getCode());
                    vo.setName(e.getName());
                    vo.setChildren(new ArrayList<>());
                    return vo;
                }).collect(Collectors.toList());
    }

    private List<AreaVo> getCityList(List<AreaVo> list) {
        if (list != null) {
            return list;
        }
        return areaCityRepo.lambdaQuery()
                .select(AreaCityEntity::getParentCode, AreaCityEntity::getCode, AreaCityEntity::getName, BaseEntity::getId)
                .list()
                .stream().map(e -> {
                    AreaVo vo = new AreaVo();
                    vo.setId(e.getId());
                    vo.setParentCode(e.getParentCode());
                    vo.setCode(e.getCode());
                    vo.setName(e.getName());
                    vo.setChildren(new ArrayList<>());
                    return vo;
                }).collect(Collectors.toList());
    }

    private List<AreaVo> getDistrictList(List<AreaVo> list) {
        if (list != null) {
            return list;
        }
        return areaDistrictRepo.lambdaQuery()
                .select(AreaDistrictEntity::getParentCode, AreaDistrictEntity::getCode, AreaDistrictEntity::getName, BaseEntity::getId)
                .list()
                .stream().map(e -> {
                    AreaVo vo = new AreaVo();
                    vo.setId(e.getId());
                    vo.setParentCode(e.getParentCode());
                    vo.setCode(e.getCode());
                    vo.setName(e.getName());
                    vo.setChildren(new ArrayList<>());
                    return vo;
                }).collect(Collectors.toList());
    }

    private List<AreaVo> getProvinceList(List<AreaVo> list) {
        if (list != null) {
            return list;
        }
        return areaProvinceRepo.lambdaQuery()
                .select(AreaProvinceEntity::getCode, AreaProvinceEntity::getName, BaseEntity::getId)
                .list()
                .stream().map(e -> {
                    AreaVo vo = new AreaVo();
                    vo.setId(e.getId());
                    vo.setCode(e.getCode());
                    vo.setParentCode("");
                    vo.setName(e.getName());
                    vo.setChildren(new ArrayList<>());
                    return vo;
                }).collect(Collectors.toList());
    }
}
