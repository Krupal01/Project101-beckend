package com.crm.authentication.service.sms;

/**
 * Abstraction over SMS dispatch.
 * Provide a @Service implementation backed by Twilio, AWS SNS,
 * MSG91, or any other gateway — OtpService depends only on this interface.
 *
 * Example Twilio implementation:
 *
 *   @Service
 *   @RequiredArgsConstructor
 *   public class TwilioSmsService implements SmsService {
 *       private final TwilioConfig config;
 *
 *       @Override
 *       public void send(String toPhone, String message) {
 *           Twilio.init(config.getAccountSid(), config.getAuthToken());
 *           Message.creator(
 *               new PhoneNumber(toPhone),
 *               new PhoneNumber(config.getFromNumber()),
 *               message
 *           ).create();
 *       }
 *   }
 */
public interface SmsService {

    /**
     * Sends a text message to the given E.164 phone number.
     *
     * @param toPhone  recipient in E.164 format, e.g. "+919876543210"
     * @param message  plaintext body of the SMS
     */
    void send(String toPhone, String message);
}