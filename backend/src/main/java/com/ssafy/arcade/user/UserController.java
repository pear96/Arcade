package com.ssafy.arcade.user;

import com.ssafy.arcade.common.util.JwtTokenUtil;
import com.ssafy.arcade.user.entity.User;
import com.ssafy.arcade.user.repository.UserRepository;
import com.ssafy.arcade.user.request.KakaoProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    // 카카오 로그인
    // 인가코드를 받아온 후 부터 진행
    @GetMapping("/kakao")
    public ResponseEntity<Map<String, Object>> kakaoLogin(@RequestParam String code, @RequestParam(value = "provider") String provider) {
        User user = null;
        String email = null;
        String image = null;
        String name = null;
        if("카카오".equals(provider)){
            // 1. 인가 코드로 액세스 토큰을 받아온다.
            String accessToken = userService.getAccessToken(code);
            // 2. 액세스 토큰으로 카카오 정보를 가져온다.
            KakaoProfile kakaoProfile = userService.getProfileByToken(accessToken);
            // 3. 카카오 정보로 회원인지 아닌지 검사한다.
            user = userRepository.findByEmail(kakaoProfile.getKakao_account().getEmail()).orElseGet(User::new);
            // 카카오 정보를 email, image, name에 각각 입력
            email = kakaoProfile.getKakao_account().getEmail();
            image = kakaoProfile.getKakao_account().getProfile().getProfile_image_url();
            name = kakaoProfile.getKakao_account().getProfile().getNickname();
        }
        else if("네이버".equals(provider)){

        }else{

        }

        // 3-1. 회원이 아니라면 회원가입 절차 진행
        // 이미 회원이라면 user가 null이 아님. 하지만 각 소셜마다 email, image, name을 가져왔으므로 그 정보들로 회원가입 진행.
        // 음.. DTO로 묶을까?
        Map<String, Object> map = new HashMap<>();
        if (user.getUserSeq() == null) {
            // 회원가입 후 토큰 발급
            user = userService.signUp(email,image,name);
        }
        // 4. 커스텀 토큰 발급
        map.put("token", "Bearer " + JwtTokenUtil.getToken(user.getEmail()));
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("image", user.getImage());
        return new ResponseEntity<>(map, HttpStatus.OK);
    }

}