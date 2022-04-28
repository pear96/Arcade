package com.ssafy.arcade.user;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.arcade.common.exception.CustomException;
import com.ssafy.arcade.common.exception.ErrorCode;
import com.ssafy.arcade.common.util.JwtTokenUtil;
import com.ssafy.arcade.user.entity.Friend;
import com.ssafy.arcade.user.entity.User;
import com.ssafy.arcade.user.repository.FriendRepository;
import com.ssafy.arcade.user.repository.UserRepository;
import com.ssafy.arcade.user.request.KakaoProfile;
import com.ssafy.arcade.user.request.KakaoToken;
import com.ssafy.arcade.user.response.UserResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    @Value("${kakao.client_id}")
    private String kakaoClientId;
    @Value("${kakao.redirect_uri}")
    private String kakaoRedirectUri;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;

    // refreshToken을 같이 담아 보낼수도 있음.
    public String getAccessToken(String code) {
        String accessToken = "";
        String refreshToken = "";
        String reqURL = "https://kauth.kakao.com/oauth/token";
        RestTemplate restTemplate = new RestTemplate();
        // 헤더 추가
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        // 바디 추가
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        // HttpHeader와 HttpBody를 하나의 오브젝트에 담기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(params, headers);
        // Http 요청하기 - Post방식으로 - 그리고 response 변수의 응답 받음.
        ResponseEntity<String> response = restTemplate.exchange(
                reqURL,
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class);
        // Gson, Json Simple, ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        KakaoToken kakaoToken = null;
        try {
            kakaoToken = objectMapper.readValue(response.getBody(), KakaoToken.class);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return kakaoToken.getAccess_token();
    }

    public KakaoProfile getProfileByToken(String accessToken) {
        String reqURL = "https://kapi.kakao.com/v2/user/me";
        RestTemplate restTemplate = new RestTemplate();
        // HttpHeader 오브젝트 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HttpHeader와 HttpBody를 하나의 오브젝트에 담기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                reqURL,
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class);
        // Gson, Json Simple, ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        // 내가 필드로 선언한 데이터들만 파싱.
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        KakaoProfile kakaoProfile = null;
        try {
            kakaoProfile = objectMapper.readValue(response.getBody(), KakaoProfile.class);
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return kakaoProfile;
    }
    // 회원 가입
    public User signUp(String email, String image, String name) {
        User user = User.builder()
                .email(email).image(image).name(name).build();
        userRepository.save(user);
        return user;
    }

    // JWT 토큰으로 유저 조회
    public String getEmailByToken(String token) {
        JWTVerifier verifier = JwtTokenUtil.getVerifier();
        if ("".equals(token)) {
            throw new CustomException(ErrorCode.NOT_OUR_USER);
        }
        JwtTokenUtil.handleError(token);
        DecodedJWT decodedJWT = verifier.verify(token.replace(JwtTokenUtil.TOKEN_PREFIX, ""));
        return decodedJWT.getSubject();
    }

    // 유저 검색
    public List<UserResDto> getUserByName(String token, String name) {
        User me = userRepository.findByEmail(getEmailByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));

        List<User> userList = userRepository.findByNameContains(name).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        List<UserResDto> userResDtoList = new ArrayList<>();
        for (User user : userList) {
            // 본인 제외
            if (user == me) {
                continue;
            }
            Friend targetfriend = friendRepository.findByRequestAndTarget(me, user).orElse(null);
            Friend reqfriend =  friendRepository.findByRequestAndTarget(user, me).orElse(null);

            Friend friend = targetfriend == null ? reqfriend : targetfriend;
            Integer status;
            if (friend == null) {
                status = -1;
            }
            else if (!friend.isApproved()) {
                status = 0;
            }
            else {
                status = 1;
            }

            UserResDto userResDto = new UserResDto();
            userResDto.setUserSeq(user.getUserSeq());
            userResDto.setEmail(user.getEmail());
            userResDto.setName(user.getName());
            userResDto.setImage(user.getImage());
            userResDto.setStatus(status);

            userResDtoList.add(userResDto);
        }
        return userResDtoList;
    }
    // 친구 제외 유저 검색
    public List<UserResDto> getUserByNameNoRelate(String token, String name) {
        User me = userRepository.findByEmail(getEmailByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));

        List<User> userList = userRepository.findByNameContains(name).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        List<UserResDto> userResDtoList = new ArrayList<>();
        Integer status = -1;
        for (User user : userList) {
            // 본인 제외
            if (user == me) {
                continue;
            }
            Friend targetfriend = friendRepository.findByRequestAndTarget(me, user).orElse(null);
            Friend reqfriend =  friendRepository.findByRequestAndTarget(user, me).orElse(null);

            Friend friend = targetfriend == null ? reqfriend : targetfriend;

            // 친구관계면 pass
            if (friend != null) {
                continue;
            }
            UserResDto userResDto = new UserResDto();
            userResDto.setUserSeq(user.getUserSeq());
            userResDto.setEmail(user.getEmail());
            userResDto.setName(user.getName());
            userResDto.setImage(user.getImage());
            userResDto.setStatus(status);

            userResDtoList.add(userResDto);
        }
        return userResDtoList;
    }


    // 친구 요청
    public void requestFriend(String token, String targetEmail) {
        User reqUser = userRepository.findByEmail(getEmailByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));
        User targetUser = userRepository.findByEmail(targetEmail).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));


        Friend targetFriend = friendRepository.findByRequestAndTarget(reqUser, targetUser).orElse(null);
        Friend reqFriend =  friendRepository.findByRequestAndTarget(targetUser, reqUser).orElse(null);

        // 본인 친구추가 불가
        if (targetUser == reqUser) {
            throw new CustomException(ErrorCode.CANNOT_FOLLOW_MYSELF);
        }
        // 둘다 null이어야만 입력 가능
        if (targetFriend == null && reqFriend == null) {
            Friend friend = new Friend();

            friend.setRequest(reqUser);
            friend.setTarget(targetUser);
            friend.setApproved(false);
            friendRepository.save(friend);
        }
        else {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        }
    }
        
    // 친구 수락
    public void approveFriend(String token, String reqEmail) {
        User targetUser = userRepository.findByEmail(getEmailByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));
        User reqUser = userRepository.findByEmail(reqEmail).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        System.out.println("reqUser: " + reqUser +  "targetUser: " + targetUser);
        if (targetUser == reqUser) {
            throw new CustomException(ErrorCode.CANNOT_FOLLOW_MYSELF);
        }
        Friend targetFriend = friendRepository.findByRequestAndTarget(reqUser, targetUser).orElse(null);

        // 수락할 때는 수락하는 user는 target에 저장된 경우여야만 한다.
        Friend reqFriend = friendRepository.findByRequestAndTarget(targetUser, reqUser).orElse(null);
        if (reqFriend != null) {
            throw new CustomException(ErrorCode.CANNOT_ACCEPT_MYSELF);
        }

        // 요청한 친구관계가 없을떄
        if (targetFriend == null) {
            throw new CustomException(ErrorCode.DATA_NOT_FOUND);
        }
        else {
            if (targetFriend.isApproved()) {
                throw new CustomException(ErrorCode.ALREADY_ACCEPT);
            }
            targetFriend.setApproved(true);
            friendRepository.save(targetFriend);
        }
    }
    // 친구 삭제, (상대가 수락하기 전이라면 친구 요청 취소인것)
    public void deleteFriend(String token, String userEmail) {
        User reqUser = userRepository.findByEmail(getEmailByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));
        User targetUser = userRepository.findByEmail(userEmail).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        Friend targetFriend = friendRepository.findByRequestAndTarget(reqUser, targetUser).orElse(null);
        Friend reqFriend = friendRepository.findByRequestAndTarget(targetUser, reqUser).orElse(null);

        Friend friend = targetFriend == null ? reqFriend : targetFriend;
        if (friend == null) {
            throw new CustomException(ErrorCode.DATA_NOT_FOUND);
        }
        else {
            friendRepository.delete(friend);
        }
    }

    // 친구리스트 조회
    public List<UserResDto> getFriendList(String token) {
        User user = userRepository.findByEmail(getEmailByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));

        List<UserResDto> userResDtoList = new ArrayList<>();

        List<Friend> friendList = friendRepository.findAllByRequestOrTarget(user, user).orElse(null);
        if (friendList == null) {
            throw new CustomException(ErrorCode.NOT_OUR_USER);
        }

        for (Friend friend : friendList) {
            // 친구 수락 상태인 애들만
            if (!friend.isApproved()) {
                continue;
            }
            User friend_user = (friend.getRequest() == user) ? friend.getTarget() : friend.getRequest();
            UserResDto userResDto = new UserResDto();
            userResDto.setUserSeq(friend_user.getUserSeq());
            userResDto.setEmail(friend_user.getEmail());
            userResDto.setName(friend_user.getName());
            userResDto.setImage(friend_user.getImage());
            userResDto.setStatus(1);
            userResDtoList.add(userResDto);
        }
        return userResDtoList;
    }
    // 친구 검색
    public List<UserResDto> searchFriend(String token, String name) {
        User me = userRepository.findByEmail(getEmailByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));
        List<User> userList = userRepository.findByNameContains(name).orElseThrow(() ->
                new CustomException(ErrorCode.DATA_NOT_FOUND));

        List<UserResDto> userResDtoList = new ArrayList<>();
        for (User user : userList) {
            Friend targetFriend = friendRepository.findByRequestAndTarget(me, user).orElse(null);
            Friend requestFriend = friendRepository.findByRequestAndTarget(user, me).orElse(null);

            Friend friend = targetFriend == null ? requestFriend : targetFriend;
            // 친구가 아니거나, 아직 수락전이면 제외
            if (friend == null || !friend.isApproved()) {
                continue;
            }
            UserResDto userResDto = new UserResDto();
            userResDto.setUserSeq(user.getUserSeq());
            userResDto.setEmail(user.getEmail());
            userResDto.setName(user.getName());
            userResDto.setImage(user.getImage());
            userResDto.setStatus(1);

            userResDtoList.add(userResDto);
        }
        return userResDtoList;
    }

    /**
     * 테스트용 Service (토큰 쓰기 번거로워서 이메일로만 소통)
     */
    public void requestFriendTest(String fromEmail, String toEmail) {
        User reqUser = userRepository.findByEmail(fromEmail).get();
        User targetUser = userRepository.findByEmail(toEmail).get();

        Friend targetfriend = friendRepository.findByRequestAndTarget(reqUser, targetUser).orElse(null);
        Friend reqfriend =  friendRepository.findByRequestAndTarget(targetUser, reqUser).orElse(null);

        // 둘다 null이어야만 입력 가능
        if (targetfriend == null && reqfriend == null) {
            Friend friend = new Friend();

            friend.setRequest(reqUser);
            friend.setTarget(targetUser);
            friend.setApproved(false);
            friendRepository.save(friend);
        }
        else {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        }
    }
    public void approveFriendTest(String toEmail, String fromEmail) {
        User targetUser = userRepository.findByEmail(toEmail).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));
        User reqUser = userRepository.findByEmail(fromEmail).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        Friend targetFriend = friendRepository.findByRequestAndTarget(reqUser, targetUser).orElse(null);
        Friend reqFriend = friendRepository.findByRequestAndTarget(targetUser, reqUser).orElse(null);

        // 요청한 친구관계가 없을떄
        if (targetFriend == null && reqFriend == null) {
            throw new CustomException(ErrorCode.DATA_NOT_FOUND);
        }
        else {
            Friend friend = targetFriend == null ? reqFriend : targetFriend;

            if (friend.isApproved()) {
                throw new CustomException(ErrorCode.ALREADY_ACCEPT);
            }
            friend.setApproved(true);
            friendRepository.save(friend);
        }
    }

    // 친구리스트 조회
    public List<UserResDto> getFriendListTest(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));
        List<UserResDto> userResDtoList = new ArrayList<>();

        List<Friend> friendList = friendRepository.findAllByRequestOrTarget(user, user).orElse(null);
        if (friendList == null) {
            throw new CustomException(ErrorCode.NOT_OUR_USER);
        }

        for (Friend friend : friendList) {
            if (!friend.isApproved()) {
                continue;
            }
            User friend_user = (friend.getRequest() == user) ? friend.getTarget() : friend.getRequest();
            UserResDto userResDto = new UserResDto();
            userResDto.setEmail(friend_user.getEmail());
            userResDto.setName(friend_user.getName());
            userResDto.setImage(friend_user.getImage());

            userResDtoList.add(userResDto);
        }
        return userResDtoList;
    }

    // 친구 삭제, (상대가 수락하기 전이라면 친구 요청 취소인것)
    public void deleteFriendTest(String myEmail, String userEmail) {
        User reqUser = userRepository.findByEmail(myEmail).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));
        User targetUser = userRepository.findByEmail(userEmail).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        Friend targetFriend = friendRepository.findByRequestAndTarget(reqUser, targetUser).orElse(null);
        Friend reqFriend = friendRepository.findByRequestAndTarget(targetUser, reqUser).orElse(null);

        Friend friend = targetFriend == null ? reqFriend : targetFriend;
        if (friend == null) {
            throw new CustomException(ErrorCode.DATA_NOT_FOUND);
        }
        else {
            friendRepository.delete(friend);
        }
    }

}
