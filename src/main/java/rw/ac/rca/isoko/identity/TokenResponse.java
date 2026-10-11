package rw.ac.rca.isoko.identity;
import java.util.UUID;
public record TokenResponse(String accessToken,String tokenType,UserResponse user,UUID challengeId,String challengePurpose,String labCode) {
 public TokenResponse(String token,UserResponse user) {this(token,"Bearer",user,null,null,null);}
 public static TokenResponse challenge(AuthChallenge c,String labCode) {return new TokenResponse(null,null,null,c.getId(),c.getPurpose(),labCode);}
}
