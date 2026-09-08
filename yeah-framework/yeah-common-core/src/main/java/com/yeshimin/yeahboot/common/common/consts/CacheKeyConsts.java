package com.yeshimin.yeahboot.common.common.consts;

/**
 * 缓存键常量类
 */
public class CacheKeyConsts {

    // subject下所有终端信息
    public static final String USER_SUBJECT_TERMINAL_INFO = "sub:%s:user:%s:token:term";
    // 终端下token信息
    public static final String USER_TERMINAL_TOKEN_INFO = "sub:%s:user:%s:token:term:%s";
    // token subject : user : terminal : token timestamp
    public static final String USER_TERMINAL_TOKEN = "sub:%s:user:%s:token:term:%s:%s";

    // 系统参数
    public static final String SYSTEM_CONFIG = "sys:config";
}
