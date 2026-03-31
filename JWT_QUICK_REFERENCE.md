# JWT Authentication Quick Reference

## Files Modified/Created

### New Files
- ✅ `src/main/java/com/rbms/renbo/util/JwtUtil.java` - JWT token generation and validation
- ✅ `src/main/java/com/rbms/renbo/model/LoginRequestDto.java` - Login request DTO
- ✅ `src/main/java/com/rbms/renbo/model/LoginResponseDto.java` - Login response with token

### Modified Files
- ✅ `pom.xml` - Added JJWT and Spring Security dependencies
- ✅ `src/main/resources/application.properties` - Added JWT configuration
- ✅ `src/main/java/com/rbms/renbo/config/WebConfig.java` - Added BCryptPasswordEncoder bean
- ✅ `src/main/java/com/rbms/renbo/service/userService.java` - Updated with password hashing and login
- ✅ `src/main/java/com/rbms/renbo/controller/UserController.java` - Added /user/login endpoint
- ✅ `src/test/java/com/rbms/renbo/service/UserServiceTest.java` - Added login tests

## Login Flow

```
User Input (email + password)
         ↓
UserController.login()
         ↓
userService.login(LoginRequestDto)
         ↓
Find user by email
         ↓
Match password using BCrypt.matches()
         ↓
Generate JWT token with userId, email, role
         ↓
Return LoginResponseDto (token + user info)
```

## Password Hashing Flow (Registration)

```
User Input (password)
         ↓
userService.insertNewUser()
         ↓
Generate DTO entity from request
         ↓
Hash password using BCrypt.encode()
         ↓
Save user to database
         ↓
Return UserResponseDto
```

## JWT Token Structure

```
Header: {
  "alg": "HS256",
  "typ": "JWT"
}

Payload: {
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "role": "RENTER",
  "iat": 1704067200,
  "exp": 1704153600
}

Signature: HMAC-SHA256(header.payload, secret)
```

## Configuration

### JWT Secret (application.properties)
```properties
jwt.secret=your-super-secret-key-change-this-in-production-at-least-32-characters-long
jwt.expiration=86400000
```

**Important:** Change the secret in production!

## API Endpoints

### POST /user/signup/register
**Request:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "password": "SecurePassword123",
  "role": "RENTER"
}
```
**Response:** UserResponseDto
- Password automatically hashed before storage

### POST /user/login
**Request:**
```json
{
  "email": "john@example.com",
  "password": "SecurePassword123"
}
```
**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "fullName": "John Doe",
  "email": "john@example.com",
  "role": "RENTER"
}
```

## Security Features Implemented

✅ **Password Security**
- BCrypt hashing with strength 10
- Unique salt per password
- Secure password matching

✅ **Token Security**
- HS256 signature algorithm
- 24-hour expiration
- Claims: userId, email, role

✅ **No Plain Text**
- Removed password string comparisons
- All comparisons use PasswordEncoder.matches()

## Testing

**Test Results:** 27/27 passing ✅

**Test Coverage:**
- User registration with password hashing
- Successful login with JWT generation
- Login with non-existent user
- Login with incorrect password
- Token generation with correct claims

## Next Steps (Optional Enhancements)

1. **JWT Filter** - Validate token on protected endpoints
2. **RBAC** - Role-based access control
3. **Token Refresh** - Implement refresh tokens
4. **Logout** - Token blacklist/revocation
5. **API Documentation** - Update Swagger for auth

## Troubleshooting

**Issue:** JWT secret too short
**Solution:** Use at least 32 characters for HS256

**Issue:** Token expired
**Solution:** Increase `jwt.expiration` value (in milliseconds)

**Issue:** Password not matching during login
**Solution:** Verify password format and ensure BCrypt encoding is applied

## Performance Considerations

- BCrypt encoding is intentionally slow (~ 100ms)
- JWT validation is very fast (< 1ms)
- Consider async processing for large batch operations

