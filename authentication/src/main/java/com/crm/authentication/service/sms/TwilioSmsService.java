package com.crm.authentication.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Twilio SMS implementation — active when sms.provider=twilio in application.properties.
 *
 * Setup:
 *   1. Add dependency to pom.xml:
 *        <dependency>
 *            <groupId>com.twilio.sdk</groupId>
 *            <artifactId>twilio</artifactId>
 *            <version>10.1.0</version>
 *        </dependency>
 *
 *   2. Add to application.properties:
 *        sms.provider=twilio
 *        twilio.account-sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 *        twilio.auth-token=your_auth_token
 *        twilio.from-number=+1XXXXXXXXXX
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "twilio")
public class TwilioSmsService implements SmsService {

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.from-number}")
    private String fromNumber;

    @Override
    public void send(String toPhone, String message) {
        // Uncomment after adding the Twilio dependency:
        //
        // Twilio.init(accountSid, authToken);
        // Message.creator(
        //         new com.twilio.type.PhoneNumber(toPhone),
        //         new com.twilio.type.PhoneNumber(fromNumber),
        //         message
        // ).create();
        //
        // log.info("SMS sent to {}", toPhone);

        throw new UnsupportedOperationException(
                "Add the Twilio dependency and uncomment the implementation in TwilioSmsService");
    }
}