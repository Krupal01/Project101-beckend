package com.crm.authentication.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Default SmsService implementation — active when sms.provider=console (or not set).
 * Logs the OTP to the console instead of sending a real SMS.
 *
 * Use this in development and testing.
 * Switch to a real provider by setting sms.provider=twilio in application.properties.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "console", matchIfMissing = true)
public class ConsoleSmsService implements SmsService {

    @Override
    public void send(String toPhone, String message) {
        // ⚠️ Never log OTP codes in production — remove this implementation and
        // set sms.provider=twilio (or another real provider) before going live.
        log.info("[SMS → {}] {}", toPhone, message);
    }
}