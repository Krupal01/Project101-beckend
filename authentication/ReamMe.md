

---

## Register Flow

### With OTP (`auth.otp.enabled: true`)

```
Client                          Server
  │                               │
  │  POST /send-otp               │
  │  { phone, purpose: REGISTER } │
  │ ─────────────────────────────▶│
  │                               │  1. Deletes old OTPs for phone+purpose
  │                               │  2. Generates 6-digit code
  │                               │  3. Saves bcrypt(code) + purpose to phone_otps
  │                               │  4. Sends SMS via SmsService
  │                               │  5. Issues short-lived otpToken JWT (10 min)
  │  { otpToken }                 │
  │◀─────────────────────────────│
  │                               │
  │  POST /verify-otp             │
  │  { phone, otp, otpToken }     │
  │ ─────────────────────────────▶│
  │                               │  1. Extracts phone from otpToken (JWT validation)
  │                               │  2. Cross-checks phone in request == phone in token
  │                               │  3. Finds OTP record (phone + purpose + usedAt IS NULL)
  │                               │  4. Checks expiry (5 min), attempt count (max 5)
  │                               │  5. bcrypt.matches(otp, otpHash) → correct
  │                               │  6. Sets usedAt = now, deletes all OTPs for phone+purpose
  │                               │  7. Checks userRepository.findByPhone → NOT FOUND
  │                               │  8. Issues a new phone-verified otpToken JWT
  │  { accessToken: otpToken,     │
  │    refreshToken: null,        │
  │    isNewUser: true }          │
  │◀─────────────────────────────│
  │                               │
  │  POST /register               │
  │  { phone, otpToken,           │
  │    email, password }          │
  │ ─────────────────────────────▶│
  │                               │  1. Extracts phone from otpToken (JWT validation)
  │                               │  2. Cross-checks phone in request == phone in token
  │                               │  3. Checks phone not already registered
  │                               │  4. Checks email not already registered
  │                               │  5. Creates User { phoneVerified: true }
  │                               │  6. Saves bcrypt(password) as passwordHash
  │                               │  7. Generates accessToken + refreshToken
  │  { accessToken,               │
  │    refreshToken,              │
  │    isNewUser: false }         │
  │◀─────────────────────────────│
```

### Without OTP (`auth.otp.enabled: false`)

```
Client                          Server
  │                               │
  │  POST /register               │
  │  { phone, otpToken: "",       │
  │    email, password }          │
  │ ─────────────────────────────▶│
  │                               │  1. otpToken check SKIPPED (OTP disabled)
  │                               │  2. Takes phone directly from request body
  │                               │  3. Checks phone not already registered
  │                               │  4. Checks email not already registered
  │                               │  5. Creates User { phoneVerified: false }
  │                               │     (false because OTP was not verified)
  │                               │  6. Saves bcrypt(password) as passwordHash
  │                               │  7. Generates accessToken + refreshToken
  │  { accessToken,               │
  │    refreshToken,              │
  │    isNewUser: false }         │
  │◀─────────────────────────────│
```

---

## Login Flow

### With OTP (`auth.otp.enabled: true`) — OTP login

```
Client                          Server
  │                               │
  │  POST /send-otp               │
  │  { phone, purpose: LOGIN }    │
  │ ─────────────────────────────▶│
  │                               │  1. Same as register send-otp
  │                               │     but purpose = LOGIN stored on record
  │  { otpToken }                 │
  │◀─────────────────────────────│
  │                               │
  │  POST /verify-otp             │
  │  { phone, otp, otpToken }     │
  │ ─────────────────────────────▶│
  │                               │  1. Validates otpToken, cross-checks phone
  │                               │  2. Finds OTP record, checks expiry + attempts
  │                               │  3. bcrypt.matches → correct
  │                               │  4. Sets usedAt, cleans up OTP records
  │                               │  5. Checks userRepository.findByPhone → FOUND
  │                               │  6. Checks user status (not SUSPENDED/BANNED)
  │                               │  7. Sets phoneVerified = true, lastLoginAt = now
  │                               │  8. Generates accessToken + refreshToken
  │  { accessToken,               │
  │    refreshToken,              │
  │    isNewUser: false }         │
  │◀─────────────────────────────│
```

### Without OTP — Password login (works regardless of `auth.otp.enabled`)

```
Client                          Server
  │                               │
  │  POST /login                  │
  │  { phone, password }          │
  │ ─────────────────────────────▶│
  │                               │  1. Finds user by phone
  │                               │  2. Checks passwordHash is not null
  │                               │     (if null → "use OTP login" error)
  │                               │  3. bcrypt.matches(password, passwordHash)
  │                               │  4. Checks globalStatus not SUSPENDED/BANNED
  │                               │  5. Sets lastLoginAt = now
  │                               │  6. Generates accessToken + refreshToken
  │  { accessToken,               │
  │    refreshToken,              │
  │    isNewUser: false }         │
  │◀─────────────────────────────│
```

---

## Token Lifecycle (both flows)

```
accessToken  → short-lived JWT (e.g. 15 min), carries userId + phone + role
refreshToken → "{uuid}:{secret}" stored as bcrypt hash in refresh_tokens table

POST /refresh  → validates refreshToken, issues new accessToken + expiresIn
POST /logout   → marks single refreshToken as isRevoked = true
POST /password/change → marks ALL refreshTokens for user as isRevoked = true
                        (forces re-login on every device)
```

---

## Key Design Decisions worth knowing

**`otpToken` is used twice with different meanings:**
- After `/send-otp` → it's a session-binding token (proves the send and verify are from the same client)
- After `/verify-otp` (new user) → it's a phone-verified proof token (proves the phone was verified, carried into `/register`)

**`purpose` on the OTP record** means a user can have independent OTP sessions for `LOGIN` and `RESET_PASSWORD` at the same time without them interfering.

**`usedAt`** is the consumed marker — the entity has no `verified` boolean. Once `usedAt` is set and the record is deleted, the OTP can never be replayed.

**`isNewUser` flag in the response** tells the client what to do next — `true` means redirect to registration screen and carry the `otpToken` forward, `false` means login is complete and store the tokens.