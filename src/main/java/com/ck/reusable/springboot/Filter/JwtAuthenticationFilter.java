package com.ck.reusable.springboot.Filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ck.reusable.springboot.domain.user.User;
import com.ck.reusable.springboot.security.PrincipalDetails;
import com.ck.reusable.springboot.web.jwt.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.Assert;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

// 스프링 시큐리티 정책
// login 요청해서 username, password 전송하면 (post)
// UsernamePasswordAuthenticationFilter 동작함

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
     private final AuthenticationManager authenticationManager;

     private JwtProperties jwtProperties;

     // login 요청을 하면 실행되는 함수
     @Override
     public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
          System.out.println("JwtAuthenticationFilter : 로그인 시도중");

          // 1. email,password 받아서
          try{
               /*
               JSON 형식으로 찍기
                */
//               BufferedReader br = request.getReader();
//
//               String input = null;
//               while((input = br.readLine())  != null )
//               {
//                    System.out.println(input);
//               }
               ObjectMapper om = new ObjectMapper();
               User user = om.readValue(request.getInputStream(), User.class);
               System.out.println(user);

               UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());

               System.out.println("authenticationToken : " + authenticationToken);
               Assert.notNull(authenticationToken, "autenticationToken is must not be null");

               // CustomUserDetail의 loadUserByUsername() 함수가 실행되고 정상적인 결과면 authentication이 반한됨.
               // DB에 있는 email과 password 비교
               Authentication authentication = authenticationManager.authenticate(authenticationToken);

               System.out.println("authentication : " + authentication);

               // authentication 객체가 session 영역에 저장됨.
               PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

               // 출력 ==> 로그인이 성공.
               System.out.println("로그인 완료됨 : " + principalDetails.getUser().getEmail());
               System.out.println("1==========================================");

               // authentication 객체가 session영역에 저장을 하고 return
               // 리턴을 하는 이유는 authentication을 security가 대신 해주기 떄문에 편하려고 반환
               // 단지 권한 처리 때문에 반환
               return authentication;

          }catch (IOException e)
          {
               e.printStackTrace();
          }
          System.out.println("2==============================================");
          // 2. 정상인지 로그인 시도. authenticationManager로 로그인 시도를 하면 CustomUserDetailService의 loadUserByName() 호출

          // 3. CustomUserDetail를 세션에 담고(권한 관리를 위해서)

          // 4. JWT token 응답

          // 오류가 난다면 return
          return null;
     }

     // attemptAuthentication 실행 후 인증이 정상적으로 되었으면 successfulAuthentication이 정상적으로 실행됨.
     // JWT 토큰 만들어서 request 요청한 사용자에게 JWT 토큰을 응답해주면 됨.
     @Override
     protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
          System.out.println("successfulAuthentication 정상 작동 : 인증 완료");
          PrincipalDetails userDetail = (PrincipalDetails) authResult.getPrincipal();

          Algorithm algorithm = Algorithm.HMAC256(jwtProperties.SECRET);

          // RSA방식이아니라 HASH 방식으로 일단 테스트
          String JwtToken = JWT.create()
                  .withSubject("tokenTest")
                  .withIssuer(jwtProperties.TOKEN_ISSUR) // 토큰 유효시간 30분
                          .withExpiresAt(new Date(System.currentTimeMillis() + (jwtProperties.EXPIRATION_TIME)))
                                  .withClaim("id", userDetail.getUser().getMember_seq())
                                          .withClaim("email", userDetail.getUser().getEmail())
                                                  .sign(algorithm);
          response.addHeader(jwtProperties.HEADER_STRING, jwtProperties.TOKEN_PREFIX + JwtToken);
     }
}
