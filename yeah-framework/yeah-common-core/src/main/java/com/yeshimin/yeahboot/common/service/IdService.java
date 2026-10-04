package com.yeshimin.yeahboot.common.service;

import cn.hutool.core.util.IdUtil;
import com.yeshimin.yeahboot.common.common.properties.IdGeneratorProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.sqids.Sqids;

import jakarta.annotation.PostConstruct;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdService {

    private final IdGeneratorProperties idGeneratorProperties;

    private Sqids sqids;

    @PostConstruct
    public void init() {
        sqids = Sqids.builder()
                .alphabet(idGeneratorProperties.getAlphabet())
                .minLength(idGeneratorProperties.getMinLength()).build();
    }

    /**
     * next encoded snowflake id
     */
    public String nextEncodedId() {
        String s = encode(nextId());
        log.info("nextEncodedId: {}", s);
        return s;
    }

    // ================================================================================

    /**
     * id encode
     */
    private String encode(Long source) {
        if (source == null) {
            throw new IllegalArgumentException("source不能为空");
        }
        return sqids.encode(Collections.singletonList(source));
    }

    /**
     * snowflake id
     */
    private long nextId() {
        return IdUtil.getSnowflakeNextId();
    }
}
