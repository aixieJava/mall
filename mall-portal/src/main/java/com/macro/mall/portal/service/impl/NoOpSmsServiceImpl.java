package com.macro.mall.portal.service.impl;

import com.macro.mall.portal.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 短信服务默认实现，不实际发送短信，仅打日志
 */
@Service
public class NoOpSmsServiceImpl implements SmsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpSmsServiceImpl.class);

    @Override
    public void sendAuthCode(String telephone, String code) {
        LOGGER.info("验证码已生成（未接入短信服务商）: telephone={}, code={}", telephone, code);
    }
}
