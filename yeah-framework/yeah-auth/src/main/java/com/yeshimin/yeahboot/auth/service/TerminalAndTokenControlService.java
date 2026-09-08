package com.yeshimin.yeahboot.auth.service;

import com.yeshimin.yeahboot.auth.common.properties.AuthTokenProperties;
import com.yeshimin.yeahboot.auth.domain.vo.JwtPayloadVo;
import com.yeshimin.yeahboot.common.common.consts.CacheKeyConsts;
import com.yeshimin.yeahboot.common.common.enums.ErrorCodeEnum;
import com.yeshimin.yeahboot.common.common.exception.BaseException;
import com.yeshimin.yeahboot.common.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 终端和token控制服务
 */
@Service
@RequiredArgsConstructor
public class TerminalAndTokenControlService {

    private final TokenService tokenService;
    private final CacheService cacheService;
    private final JwtService jwtService;

    private final AuthTokenProperties authTokenProperties;

    /**
     * 控制终端和token数量
     */
    public String doControl(String userId, String subValue, String termValue) {
        // 检查配置
        AuthTokenProperties.Subject subject = authTokenProperties.getMapSubject().get(subValue);
        if (subject == null) {
            throw new BaseException(ErrorCodeEnum.FAIL, "subject[" + subValue + "] 未配置");
        }
        AuthTokenProperties.Terminal terminal = subject.getMapTerminal().get(termValue);
        if (terminal == null) {
            throw new BaseException(ErrorCodeEnum.FAIL, "terminal[" + termValue + "] 不正确");
        }

        // 1.检查配置
        // --------------------------------------------------------------------------------
        // 2.加载缓存数据并做初预处理

        // 添加token及终端信息
        String token = tokenService.generateToken(userId, subValue, termValue);
        JwtPayloadVo jwtPayloadVo = jwtService.decodePayload(token);
        // token创建时间戳（毫秒）
        long timestampMs = jwtPayloadVo.getIatMs();

        // 查询终端信息
        Map<String, String> mapTerminalInfo = tokenService.getSubjectTerminalInfo(subValue, userId);
        Map<String, String> mapTerminalInfoValid = new HashMap<>();
        Map<String, String> mapTerminalInfoExpired = new HashMap<>();
        // 区分是否过期
        mapTerminalInfo.forEach((term, termInfo) -> {
            // termInfo格式为 "iat,expTime"，其中iat为登录时间戳，expTime为过期时间戳
            long expTime = Long.parseLong(termInfo.split(",")[1]);
            if (timestampMs >= expTime) {
                mapTerminalInfoExpired.put(term, termInfo);
            } else {
                mapTerminalInfoValid.put(term, termInfo);
            }
        });
        // 查询终端对应的token信息
        Map<String, Map<String, String>> mapTotalTerminalTokenInfo = new HashMap<>();
        Map<String, Map<String, String>> mapRemainTerminalTokenInfo = new HashMap<>();
        Map<String, String> mapTotalTerminalTokenInfoValid = new HashMap<>();
        Map<String, String> mapTotalTerminalTokenInfoExpired = new HashMap<>();
        for (Map.Entry<String, String> entry : mapTerminalInfo.entrySet()) {
            // 查询对应终端的token信息
            Map<String, String> mapTerminalTokenInfo =
                    tokenService.getTerminalTokenInfo(subValue, userId, entry.getKey());
            mapTotalTerminalTokenInfo.put(entry.getKey(), mapTerminalTokenInfo);
        }
        // 查询当前终端的token信息
        Map<String, String> mapTerminalTokenInfo = mapTotalTerminalTokenInfo.getOrDefault(termValue, new HashMap<>());
        Map<String, String> mapTerminalTokenInfoValid = new HashMap<>();
        Map<String, String> mapTerminalTokenInfoExpired = new HashMap<>();
        mapTerminalTokenInfo.forEach((timestamp, expTime) -> {
            long expTimeInt = Long.parseLong(expTime);
            if (timestampMs >= expTimeInt) {
                mapTerminalTokenInfoExpired.put(timestamp, expTime);
            } else {
                mapTerminalTokenInfoValid.put(timestamp, expTime);
            }
        });

        // 2.加载缓存数据并做初预处理
        // --------------------------------------------------------------------------------
        // 3.检查和控制subject下终端数量

        // 是否清空用户的终端和token信息
        boolean clearAllTerminal = false;
        // 要清除的终端信息
        Set<String> needDeleteTerminal = new HashSet<>();
        // 要清除的token信息，map<terminal, set<timestamp>> ； 【保留终端token信息】中的待清除部分
        Map<String, Set<String>> mapNeedDeleteTokenInfo = new HashMap<>();

        // 检查和控制终端数
        // 大于0，需要检查和控制超限情况
        if (subject.getMaxOnlineTerminalCount() > 0) {
            // 检查当前在线终端数是否达到限制
            int occurTerminalCount =
                    mapTerminalInfoValid.size() + (mapTerminalInfoValid.containsKey(termValue) ? 0 : 1);
            // 超限情况
            if (occurTerminalCount > subject.getMaxOnlineTerminalCount()) {
                // 要清除的终端数
                int needToDeleteTerminalCount = occurTerminalCount - subject.getMaxOnlineTerminalCount();
                // 优先清除旧值
                // mapTerminalInfoValid按value[0]时间戳排序，value[0]越小越旧;
                // value数据格式为iat,expTime，直接按value字符串排序效果与按iat排序一样
                Set<String> eliminatedTerminal = mapTerminalInfoValid.entrySet()
                        .stream().sorted(Map.Entry.comparingByValue()).limit(needToDeleteTerminalCount)
                        .map(Map.Entry::getKey).collect(Collectors.toSet());
                // 记录要清除的终端信息
                needDeleteTerminal.addAll(eliminatedTerminal);
            }
        } else if (subject.getMaxOnlineTerminalCount() == 0) {
            clearAllTerminal = true;
        }
        // 添加过期终端
        needDeleteTerminal.addAll(mapTerminalInfoExpired.keySet());

        // 3.检查和控制subject下终端数量
        // --------------------------------------------------------------------------------
        // 4.检查和控制subject下token数量

        // 检查和控制token信息
        // 大于0，需要检查和控制超限情况
        if (subject.getMaxOnlineTokenCount() > 0) {
            // 计算当前有效token数量（排除掉要清除的终端，即只计算要保留的终端）
            int occurTokenCount = 0;
            mapRemainTerminalTokenInfo = mapTotalTerminalTokenInfo.entrySet().stream()
                    .filter(entry -> !needDeleteTerminal.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            for (Map.Entry<String, Map<String, String>> entry : mapRemainTerminalTokenInfo.entrySet()) {
                occurTokenCount += entry.getValue().size();
                // 区分是否过期
                entry.getValue().forEach((timestamp, expTime) -> {
                    long expTimeInt = Long.parseLong(expTime);
                    if (timestampMs >= expTimeInt) {
                        mapTotalTerminalTokenInfoExpired.put(timestamp + "," + entry.getKey(), expTime);
                    } else {
                        mapTotalTerminalTokenInfoValid.put(timestamp + "," + entry.getKey(), expTime);
                    }
                });
            }
            // 超限的情况
            if (occurTokenCount >= subject.getMaxOnlineTokenCount()) {
                // 计算需要清除的token数量；+1是为当前登录操作预留的token位置
                int needDeleteCount = occurTokenCount - subject.getMaxOnlineTokenCount() + 1;
                // 优先清除旧值
                Set<String> eliminatedTokenInfo = mapTotalTerminalTokenInfoValid.entrySet()
                        .stream().sorted((e1, e2) -> {
                            String terminal1 = e1.getKey().split(",")[1];
                            int sort = Objects.equals(terminal1, termValue) ? 0 : 1;
                            String key1 = sort + ":" + e1.getKey();

                            String terminal2 = e2.getKey().split(",")[1];
                            sort = Objects.equals(terminal2, termValue) ? 0 : 1;
                            String key2 = sort + ":" + e2.getKey();

                            return key1.compareTo(key2);
                        }).limit(needDeleteCount)
                        .map(Map.Entry::getKey).collect(Collectors.toSet());
                eliminatedTokenInfo.forEach(e -> {
                    // [0]: timestamp; [1]: terminal
                    String[] split = e.split(",");
                    mapNeedDeleteTokenInfo.computeIfAbsent(split[1], k -> new HashSet<>()).add(split[0]);
                });
            }
        } else if (subject.getMaxOnlineTokenCount() == 0) {
            clearAllTerminal = true;
        }
        // 过期token信息的也添加到待清除集合
        mapTotalTerminalTokenInfoExpired.keySet().forEach(e -> {
            // [0]: timestamp; [1]: terminal
            String[] split = e.split(",");
            mapNeedDeleteTokenInfo.computeIfAbsent(split[1], k -> new HashSet<>()).add(split[0]);
        });

        // 4.检查和控制subject下token数量
        // --------------------------------------------------------------------------------
        // 5.检查和控制terminal下token数量

        // 检查和控制当前用户登录终端对应的token信息
        // 仅当当前终端不包含在待删除终端集合内，才进行处理
        if (!needDeleteTerminal.contains(termValue)) {
            // 大于0，需要检查和控制超限情况
            if (terminal.getMaxOnlineTokenCount() > 0) {
                // 超限的情况
                if (mapTerminalTokenInfoValid.size() >= terminal.getMaxOnlineTokenCount()) {
                    // 需要清除的token数量；+1是为当前登录操作预留的token位置
                    int needDeleteCount = mapTerminalTokenInfoValid.size() - terminal.getMaxOnlineTokenCount() + 1;
                    // 优先清除旧值
                    Set<String> eliminatedToken = mapTerminalTokenInfoValid.entrySet()
                            .stream().sorted(Map.Entry.comparingByKey()).limit(needDeleteCount)
                            .map(Map.Entry::getKey).collect(Collectors.toSet());
                    eliminatedToken.forEach(timestamp -> {
                        mapNeedDeleteTokenInfo.computeIfAbsent(termValue, k -> new HashSet<>()).add(timestamp);
                    });
                }
            } else if (terminal.getMaxOnlineTokenCount() == 0) {
                // 清空当前终端和对应token信息
                needDeleteTerminal.add(termValue);
            }
            // 过期的token信息也添加到待清除集合
            mapTerminalTokenInfoExpired.keySet().forEach(timestamp -> {
                mapNeedDeleteTokenInfo.computeIfAbsent(termValue, k -> new HashSet<>()).add(timestamp);
            });
        }

        // 5.检查和控制terminal下token数量
        // --------------------------------------------------------------------------------
        // 6.执行清除操作并按需添加当前登录的终端和token信息

        // 清空用户所有终端和token信息
        if (clearAllTerminal) {
            Set<String> delKeys = new HashSet<>();
            delKeys.add(String.format(CacheKeyConsts.USER_SUBJECT_TERMINAL_INFO, subValue, userId));
            mapTerminalInfo.keySet().forEach(term -> {
                delKeys.add(String.format(CacheKeyConsts.USER_TERMINAL_TOKEN_INFO, subValue, userId, term));
                mapTotalTerminalTokenInfo.getOrDefault(term, new HashMap<>()).forEach((timestamp, expTime) -> {
                    delKeys.add(String.format(CacheKeyConsts.USER_TERMINAL_TOKEN, subValue, userId, term, timestamp));
                });
            });
            cacheService.delete(delKeys.toArray(new String[0]));
            // 禁止登录
            throw new BaseException("禁止登录");
        }
        // （按需）清空部分信息，然后添加当前登录所创建的token及终端信息
        else {
            Set<String> delKeys = new HashSet<>();
            // map<cache key, fields>
            Map<String, Set<String>> delFields = new HashMap<>();

            needDeleteTerminal.forEach(term -> {
                delFields.computeIfAbsent(String.format(CacheKeyConsts.USER_SUBJECT_TERMINAL_INFO, subValue, userId),
                        k -> new HashSet<>()).add(term);

                delKeys.add(String.format(CacheKeyConsts.USER_TERMINAL_TOKEN_INFO, subValue, userId, term));
                Optional.ofNullable(mapTotalTerminalTokenInfo.get(term)).ifPresent(terminalTokenInfo -> {
                    terminalTokenInfo.keySet().forEach(timestamp -> {
                        delFields.computeIfAbsent(
                                String.format(CacheKeyConsts.USER_TERMINAL_TOKEN_INFO, subValue, userId, term),
                                k -> new HashSet<>()).add(timestamp);
                        delKeys.add(String.format(
                                CacheKeyConsts.USER_TERMINAL_TOKEN, subValue, userId, term, timestamp));
                    });
                });
            });
            mapNeedDeleteTokenInfo.forEach((term, timestampSet) -> {
                delFields.computeIfAbsent(
                        String.format(CacheKeyConsts.USER_TERMINAL_TOKEN_INFO, subValue, userId, term),
                        k -> new HashSet<>()).addAll(timestampSet);
                timestampSet.forEach(timestamp -> {
                    delKeys.add(String.format(CacheKeyConsts.USER_TERMINAL_TOKEN, subValue, userId, term, timestamp));
                });
            });
            // 执行清除
            cacheService.delete(delKeys.toArray(new String[0]));
            delFields.forEach((cacheKey, fields) -> {
                cacheService.deleteHashFields(cacheKey, fields.toArray(new Object[0]));
            });

            // 先写Token及终端下的Token索引，最后写用户主体下的终端索引，避免并发退出误删新登录终端
            tokenService.setTerminalTokenInfo(
                    subValue, userId, termValue, jwtPayloadVo.getIatMs(), jwtPayloadVo.getExpMs());
            tokenService.cacheToken(subValue, userId, termValue, token, timestampMs);
            tokenService.setSubjectTerminalInfo(
                    subValue, userId, termValue, jwtPayloadVo.getIatMs(), jwtPayloadVo.getExpMs());
            return token;
        }
    }
}
