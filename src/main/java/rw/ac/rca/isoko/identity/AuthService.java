package rw.ac.rca.isoko.identity;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.isoko.cart.CartService;
import rw.ac.rca.isoko.common.exception.ApiException;
import rw.ac.rca.isoko.config.IsokoProperties;
import rw.ac.rca.isoko.security.JwtService;
import rw.ac.rca.isoko.marketplace.MarketplaceRecords;
import java.time.Instant;
import java.util.*;
import java.security.SecureRandom;

@Service
public class AuthService {
 private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(AuthService.class);
 private static final String ERROR="email or password is incorrect";
 private final UserRepository users;private final PasswordEncoder encoder;private final JwtService jwt;
 private final IsokoProperties properties;private final CartService carts;private final AuthChallengeRepository challenges;
 private final MarketplaceRecords records;private final boolean labCodes;private final SecureRandom random=new SecureRandom();
 public AuthService(UserRepository users,PasswordEncoder encoder,JwtService jwt,IsokoProperties properties,
 CartService carts,AuthChallengeRepository challenges,MarketplaceRecords records,@Value("${isoko.auth.lab-codes:true}") boolean labCodes){
 this.users=users;this.encoder=encoder;this.jwt=jwt;this.properties=properties;this.carts=carts;this.challenges=challenges;this.records=records;this.labCodes=labCodes;
 }
 @Transactional
 public TokenResponse register(RegisterRequest r,String guest){
 String email=r.email().trim().toLowerCase(Locale.ROOT);
 if(users.existsByEmailIgnoreCase(email) || users.existsByPhone(r.phone())) throw ApiException.conflict("An account already uses this email or mobile number.");
 assertPasswordPolicy(r.password());
 AuthChallenge c=new AuthChallenge();c.setPurpose("REGISTER");c.setEmail(email);c.setPhone(r.phone());c.setPasswordHash(encoder.encode(r.password()));c.setGuestToken(guest);
 return issue(c);
 }
 @Transactional(noRollbackFor=ApiException.class)
 public TokenResponse login(LoginRequest r,String guest){
 log.info("login_attempt inputLen={}",r.password()==null?0:r.password().length());
 String identifier=r.email().trim().toLowerCase(Locale.ROOT);
 User candidate=(identifier.startsWith("07")?users.findByPhone(identifier):users.findByEmailIgnoreCase(identifier)).orElse(null);
 if(candidate==null)throw ApiException.unauthorized(ERROR);
 User user=users.lockById(candidate.getId()).orElseThrow();Instant now=Instant.now();
 if(user.getStatus()==UserStatus.DISABLED || user.isLocked(now))throw ApiException.unauthorized(ERROR);
 if(!encoder.matches(r.password(),user.getPasswordHash())){
 int attempts=user.getFailedAttempts()+1;user.setFailedAttempts(attempts);
 if(attempts>=properties.lockout().maxAttempts()){
 user.setStatus(UserStatus.LOCKED);user.setLockedUntil(now.plus(properties.lockout().duration()));
 records.notify(user.getId(),"Security: your account has been locked for 30 minutes after three failed sign-in attempts.");
 records.audit(user.getId(),"ACCOUNT_LOCKED",user.getId(),"Repeated failed login");
 }
 throw ApiException.unauthorized(ERROR);
 }
 user.setFailedAttempts(0);user.setLockedUntil(null);user.setStatus(UserStatus.ACTIVE);
 if(user.hasRole(Role.SELLER)||user.hasRole(Role.ADMIN)){
 AuthChallenge c=new AuthChallenge();c.setPurpose("MFA");c.setUserId(user.getId());c.setPhone(user.getPhone());c.setGuestToken(guest);return issue(c);
 }
 carts.mergeGuestCart(user.getId(),guest);return tokens(user);
 }
 private TokenResponse issue(AuthChallenge c){
 if(challenges.countByPhoneAndCreatedAtAfter(c.getPhone(),Instant.now().minusSeconds(3600))>=3)throw new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,"AUTH-429","Limit reached. Try again in one hour.");
 String code=String.format("%06d",random.nextInt(1000000));c.setCodeHash(encoder.encode(code));c.setExpiresAt(Instant.now().plusSeconds(300));challenges.save(c);
 // In the academic sandbox the response displays the code. No OTP is written to logs.
 return TokenResponse.challenge(c,labCodes?code:null);
 }
 @Transactional(noRollbackFor=ApiException.class)
 public TokenResponse verify(UUID id,String code){
 AuthChallenge c=challenges.lockById(id).orElseThrow(()->ApiException.badRequest("Invalid or expired verification challenge."));
 if(c.isConsumed()||c.getExpiresAt().isBefore(Instant.now())||c.getAttempts()>=3)throw ApiException.badRequest("Invalid or expired verification challenge.");
 c.setAttempts(c.getAttempts()+1);
 if(!encoder.matches(code,c.getCodeHash()))throw ApiException.badRequest("Invalid verification code.");
 User user;
 if(c.getPurpose().equals("REGISTER")){
 if(users.existsByEmailIgnoreCase(c.getEmail())||users.existsByPhone(c.getPhone()))throw ApiException.conflict("An account already uses this email or mobile number.");
 user=new User();user.setEmail(c.getEmail());user.setPhone(c.getPhone());user.setPhoneVerified(true);user.setPasswordHash(c.getPasswordHash());user.setRoles(new HashSet<>(Set.of(Role.CUSTOMER)));users.saveAndFlush(user);
 records.audit(user.getId(),"PHONE_VERIFIED",user.getId(),"Registration OTP accepted");
 }else{
 user=users.lockById(c.getUserId()).orElseThrow();if(user.getStatus()==UserStatus.DISABLED||user.isLocked(Instant.now()))throw ApiException.unauthorized(ERROR);
 records.audit(user.getId(),"MFA_VERIFIED",user.getId(),"Second factor accepted");
 }
 c.setConsumed(true);c.setPasswordHash(null);c.setCodeHash("consumed");carts.mergeGuestCart(user.getId(),c.getGuestToken());return tokens(user);
 }
 @Transactional
 public TokenResponse resend(UUID id){
 AuthChallenge old=challenges.lockById(id).orElseThrow(()->ApiException.badRequest("Invalid verification challenge."));
 if(old.isConsumed())throw ApiException.badRequest("Verification challenge already used.");
 AuthChallenge next=new AuthChallenge();next.setPurpose(old.getPurpose());next.setUserId(old.getUserId());next.setEmail(old.getEmail());next.setPhone(old.getPhone());next.setPasswordHash(old.getPasswordHash());next.setGuestToken(old.getGuestToken());
 TokenResponse result=issue(next);old.setConsumed(true);old.setPasswordHash(null);old.setCodeHash("consumed");return result;
 }
 public TokenResponse tokens(User user){return new TokenResponse(jwt.issue(user.getId(),user.getEmail(),user.getRoles().stream().map(Enum::name).toList()),UserResponse.from(user));}
 static void assertPasswordPolicy(String p){
 if(p==null||p.length()<8||p.length()>72||!p.chars().anyMatch(Character::isLetter)||!p.chars().anyMatch(Character::isDigit))throw ApiException.badRequest("Password must be 8–72 characters with at least one letter and one digit.");
 }
}
