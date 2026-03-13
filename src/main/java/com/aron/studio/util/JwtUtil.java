package com.aron.studio.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    public static void main(String[] args) {
        byte[] secretKey = Jwts.SIG.HS256.key().build().getEncoded();
        String secret = Base64.getEncoder().encodeToString(secretKey);
        System.out.println(secret);

        JwtUtil jwtUtil = new JwtUtil();
        jwtUtil.secret = "D4fbQ4Pv1l9O0ajIZTQLCqckWKdPGctMPG90/jtcOR0=";
        jwtUtil.expire = 60000L;
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiIyNzczNzAxODgwMTY1MjEyMTYiLCJ1c2VybmFtZSI6Imx5bCIsInJvbGUiOiJST0xFX0FETUlOIiwiaWF0IjoxNzcwMzg3NzMxLCJleHAiOjE3NzEyNTE3MzF9.STAHa21_TZ_hi_xwq7mLXBRpE6WEFHciUjH9yU5_B0g";
        Claims claims = jwtUtil.parseToken(token);
        boolean isExpired = jwtUtil.isExpired(token);
        System.out.println(claims);
        System.out.println("ok");

    }

    // secret 用于签名和验证 JWT，必须保密
    @Value("${jwt.secret}")
    private String secret;

    // token 过期时间，毫秒
    @Value("${jwt.expire}")
    private long expire;

    /**
     * 获取 SecretKey 对象
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

//    {
//        "sub": "1930293019230",
//            "role": "ROLE_ADMIN",
//            "iat": 1700000000,
//            "exp": 1703600000
//    }

    /**
     * 生成 JWT
     */
    public String generateToken(String username, String role, String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expire);

//        return Jwts.builder().setSubject(username)      // JWT 的主体（一般存用户名）
//                .setIssuedAt(now)          // 签发时间
//                .setExpiration(expiryDate) // 过期时间
//                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 签名算法
//                .compact();
        return Jwts.builder().claims(Map.of("username", username, "role", role, "userId", userId))
                .issuedAt(now).expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 token 并返回 Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser().verifyWith(getSigningKey()) // 验证签名
                    .build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("JWT 已过期", e);
        } catch (Exception e) {
            throw new RuntimeException("JWT 无效", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false; // 已过期
        } catch (Exception e) {
            return false; // 非法 token
        }
    }

    /**
     * 从 token 中获取用户名
     */
    public String getUsername(String token) {
        return parseToken(token).get("username").toString();
    }

    /**
     * 判断 token 是否过期
     */
    public boolean isExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }


}
