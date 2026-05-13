package com.macro.mall.portal.service;

/**
 * 短信发送服务接口
 */
public interface SmsService {
    /**
     * 发送验证码短信
     * @param telephone 手机号
     * @param code      验证码
     */
    void sendAuthCode(String telephone, String code);
}
