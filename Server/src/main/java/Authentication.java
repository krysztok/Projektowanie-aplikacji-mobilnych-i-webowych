import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import redis.clients.jedis.Jedis;

import java.util.Date;


public class Authentication {

    private String secretKey = "adafafafasfsaf";

    public boolean checkPassword(String username, String password){

        Jedis jedis = new Jedis("192.168.99.100",9001);
        String redisPass = jedis.hget(username, "password");

        if(password.compareTo(redisPass) == 0){
            return true;
        }

        return false;
    }

    public String createJWTToken(String username, int timeMs){

         return JWT.create().withClaim("username", username).withExpiresAt(new Date(System.currentTimeMillis() + timeMs)).sign(Algorithm.HMAC256(secretKey));
    }

    public boolean verifyJWTToken(String token){

        try {
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(secretKey)).acceptExpiresAt(0).build();
            jwtVerifier.verify(JWT.decode(token));
            return true;
        } catch (TokenExpiredException | SignatureVerificationException | JWTDecodeException e){
            return false;
        }
    }

    public String getUsername(String token){
        return JWT.decode(token).getClaim("username").asString();
    }

}
