package com.ssafy.arcade.user;

import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.arcade.chat.entity.ChatRoom;
import com.ssafy.arcade.chat.repository.ChatRoomRepository;
import com.ssafy.arcade.common.exception.CustomException;
import com.ssafy.arcade.common.exception.ErrorCode;
import com.ssafy.arcade.common.util.Code;
import com.ssafy.arcade.common.util.JwtTokenUtil;
import com.ssafy.arcade.game.GameService;
import com.ssafy.arcade.game.entity.Game;
import com.ssafy.arcade.game.entity.GameUser;
import com.ssafy.arcade.game.entity.Picture;
import com.ssafy.arcade.game.repositroy.GameUserRepository;
import com.ssafy.arcade.game.repositroy.PictureRepository;
import com.ssafy.arcade.game.request.GameResDto;
import com.ssafy.arcade.game.response.PictureResDto;
import com.ssafy.arcade.messege.entity.Message;
import com.ssafy.arcade.messege.repository.MessageRepository;
import com.ssafy.arcade.user.entity.Friend;
import com.ssafy.arcade.user.entity.User;
import com.ssafy.arcade.user.repository.FriendRepository;
import com.ssafy.arcade.user.repository.UserRepository;
import com.ssafy.arcade.user.request.KakaoProfile;
import com.ssafy.arcade.user.request.KakaoToken;
import com.ssafy.arcade.user.response.ProfileResDto;
import com.ssafy.arcade.user.response.UserResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.listener.ChannelTopic;
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

@Service
@RequiredArgsConstructor
public class UserService {
    @Value("${kakao.client_id}")
    private String kakaoClientId;
    @Value("${kakao.redirect_uri}")
    private String kakaoRedirectUri;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final GameService gameService;
    private final GameUserRepository gameUserRepository;
    private final PictureRepository pictureRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageRepository messageRepository;
    private final OnlineService onlineService;

    // refreshToken??? ?????? ?????? ???????????? ??????.
    public String getAccessToken(String code) {
        String accessToken = "";
        String refreshToken = "";
        String reqURL = "https://kauth.kakao.com/oauth/token";
        RestTemplate restTemplate = new RestTemplate();
        // ?????? ??????
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
        // ?????? ??????
        LinkedMultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        // HttpHeader??? HttpBody??? ????????? ??????????????? ??????
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(params, headers);
        // Http ???????????? - Post???????????? - ????????? response ????????? ?????? ??????.
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
        // HttpHeader ???????????? ??????
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HttpHeader??? HttpBody??? ????????? ??????????????? ??????
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                reqURL,
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class);
        // Gson, Json Simple, ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();
        // ?????? ????????? ????????? ??????????????? ??????.
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

    // ?????? ??????
    public User signUp(String email, String image, String name, String provider) {
        User user = User.builder()
                .email(email).image(image).name(name).provider(provider).build();
        userRepository.save(user);
        return user;
    }

    // JWT ???????????? ?????? ??????
//    public String getEmailByToken(String token) {
//        JWTVerifier verifier = JwtTokenUtil.getVerifier();
//        if ("".equals(token)) {
//            throw new CustomException(ErrorCode.NOT_OUR_USER);
//        }
//        JwtTokenUtil.handleError(token);
//        DecodedJWT decodedJWT = verifier.verify(token.replace(JwtTokenUtil.TOKEN_PREFIX, ""));
//        return decodedJWT.getSubject();
//    }
    // JWT ???????????? ?????? ??????
    public Long getUserSeqByToken(String token) {
        JWTVerifier verifier = JwtTokenUtil.getVerifier();
        if ("".equals(token)) {
            throw new CustomException(ErrorCode.NOT_OUR_USER);
        }
        JwtTokenUtil.handleError(token);
        DecodedJWT decodedJWT = verifier.verify(token.replace(JwtTokenUtil.TOKEN_PREFIX, ""));
        return Long.parseLong(decodedJWT.getSubject());
    }


    // ?????? ??????
    public List<UserResDto> getUserByName(String token, String name) {
        User me = userRepository.findByUserSeq(getUserSeqByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));

        List<User> userList = userRepository.findByNameContains(name).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        List<UserResDto> userResDtoList = new ArrayList<>();
        for (User user : userList) {
            // ?????? ??????
            if (user == me) {
                continue;
            }
            Friend targetfriend = friendRepository.findByRequestAndTarget(me, user).orElse(null);
            Friend reqfriend = friendRepository.findByRequestAndTarget(user, me).orElse(null);

            Friend friend = targetfriend == null ? reqfriend : targetfriend;
            Integer status;
            if (friend == null) {
                status = -1;
            } else if (!friend.isApproved()) {
                status = 0;
            } else {
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

    // ?????? ?????? ?????? ??????
    public List<UserResDto> getUserByNameNoRelate(String token, String name) {
        User me = userRepository.findByUserSeq(getUserSeqByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));

        List<User> userList = userRepository.findByNameContains(name).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        List<UserResDto> userResDtoList = new ArrayList<>();

        Integer status = 0;
        for (User user : userList) {
            // ?????? ??????
            if (user == me) {
                continue;
            }
            Friend targetfriend = friendRepository.findByRequestAndTarget(me, user).orElse(null);
            Friend reqfriend = friendRepository.findByRequestAndTarget(user, me).orElse(null);

            Friend friend = targetfriend == null ? reqfriend : targetfriend;

            // ??????????????????, ?????? ????????? ?????? ?????? ??????
            if (friend != null && !friend.isApproved()) {
                status = 0;
            }

            if (friend == null) {
                status = -1;
            }

            if (friend != null && friend.isApproved()) {
                continue;
            }
            // ?????? ????????? ??????
            UserResDto userResDto = new UserResDto();
            ChannelTopic topic = onlineService.getOnlineTopic(user.getUserSeq());
            boolean flag = topic != null;
            userResDto.setLogin(flag);
            userResDto.setUserSeq(user.getUserSeq());
            userResDto.setEmail(user.getEmail());
            userResDto.setName(user.getName());
            userResDto.setImage(user.getImage());
            userResDto.setStatus(status);

            userResDtoList.add(userResDto);
        }
        return userResDtoList;
    }


    // ?????? ??????
    public void requestFriend(String token, Long targetUserSeq) {
        User reqUser = userRepository.findByUserSeq(getUserSeqByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));
        User targetUser = userRepository.findByUserSeq(targetUserSeq).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        User user = userRepository.findByUserSeq(getUserSeqByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));


        Friend targetFriend = friendRepository.findByRequestAndTarget(reqUser, targetUser).orElse(null);
        Friend reqFriend = friendRepository.findByRequestAndTarget(targetUser, reqUser).orElse(null);

        // ?????? ???????????? ??????
        if (targetUser == reqUser) {
            throw new CustomException(ErrorCode.CANNOT_FOLLOW_MYSELF);
        }
        // ?????? null???????????? ?????? ??????
        if (targetFriend == null && reqFriend == null) {
            Friend friend = new Friend();

            friend.setRequest(reqUser);
            friend.setTarget(targetUser);
            friend.setApproved(false);
            friendRepository.save(friend);
        } else {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        }

    }

    // ?????? ??????
    public void approveFriend(String token, Long userSeq) {
        User targetUser = userRepository.findByUserSeq(getUserSeqByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));
        User reqUser = userRepository.findByUserSeq(userSeq).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        if (targetUser == reqUser) {
            throw new CustomException(ErrorCode.CANNOT_FOLLOW_MYSELF);
        }
        Friend targetFriend = friendRepository.findByRequestAndTarget(reqUser, targetUser).orElse(null);

        // ????????? ?????? ???????????? user??? target??? ????????? ??????????????? ??????.
        Friend reqFriend = friendRepository.findByRequestAndTarget(targetUser, reqUser).orElse(null);
        if (reqFriend != null) {
            throw new CustomException(ErrorCode.CANNOT_ACCEPT_MYSELF);
        }

        // ????????? ??????????????? ?????????
        if (targetFriend == null) {
            throw new CustomException(ErrorCode.DATA_NOT_FOUND);
        } else {
            if (targetFriend.isApproved()) {
                throw new CustomException(ErrorCode.ALREADY_ACCEPT);
            }
            targetFriend.setApproved(true);
            friendRepository.save(targetFriend);
        }
    }

    // ?????? ??????, (????????? ???????????? ???????????? ?????? ?????? ????????????)
    public void deleteFriend(String token, Long userSeq) {
        User reqUser = userRepository.findByUserSeq(getUserSeqByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));
        User targetUser = userRepository.findByUserSeq(userSeq).orElseThrow(() ->
                new CustomException(ErrorCode.USER_NOT_FOUND));

        Friend targetFriend = friendRepository.findByRequestAndTarget(reqUser, targetUser).orElse(null);
        Friend reqFriend = friendRepository.findByRequestAndTarget(targetUser, reqUser).orElse(null);

        Friend friend = targetFriend == null ? reqFriend : targetFriend;
        if (friend == null) {
            throw new CustomException(ErrorCode.DATA_NOT_FOUND);
        } else {
            // ?????? ????????? ???????????? ?????????, ????????? ??????
            if(friend.isApproved()){
                // ?????? ????????? ?????????, ????????? ?????? ??????
                ChatRoom chatRoom = chatRoomRepository.findByUser1AndUser2(reqUser, targetUser).orElseGet(ChatRoom::new);
                if (chatRoom.getChatRoomSeq() == null) chatRoom = chatRoomRepository.findByUser1AndUser2(targetUser, reqUser).orElseGet(ChatRoom::new);
                List<Message> messages = new ArrayList<>();
                // ????????? ?????? ??????
                if(chatRoom.getChatRoomSeq() != null)
                messages = messageRepository.findAllByChatRoomSeq(chatRoom.getChatRoomSeq());
                if(messages.size()>0) {
                    messageRepository.deleteAll(messages);
                }

                // ????????? ??????
                if(chatRoom.getChatRoomSeq() != null) {
                    chatRoomRepository.delete(chatRoom);
                }
            }
            friendRepository.delete(friend);
        }
    }

    // ??????????????? ??????
    public List<UserResDto> getFriendList(String token) {
        User user = userRepository.findByUserSeq(getUserSeqByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));

        List<UserResDto> userResDtoList = new ArrayList<>();

        List<Friend> friendList = friendRepository.findAllByRequestOrTarget(user, user).orElse(null);
        if (friendList == null) {
            throw new CustomException(ErrorCode.NOT_OUR_USER);
        }

        for (Friend friend : friendList) {
            // ?????? ?????? ????????? ?????????
            if (!friend.isApproved()) {
                continue;
            }
            User friend_user = (friend.getRequest() == user) ? friend.getTarget() : friend.getRequest();
            UserResDto userResDto = new UserResDto();
            ChannelTopic topic = onlineService.getOnlineTopic(friend_user.getUserSeq());
            boolean flag = topic != null;
            userResDto.setLogin(flag);
            userResDto.setUserSeq(friend_user.getUserSeq());
            userResDto.setEmail(friend_user.getEmail());
            userResDto.setName(friend_user.getName());
            userResDto.setImage(friend_user.getImage());
            userResDto.setStatus(1);
            userResDtoList.add(userResDto);
        }
        return userResDtoList;
    }

    // ?????? ??????
    public List<UserResDto> searchFriend(String token, String name) {
        User me = userRepository.findByUserSeq(getUserSeqByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));
        List<User> userList = userRepository.findByNameContains(name).orElseThrow(() ->
                new CustomException(ErrorCode.DATA_NOT_FOUND));

        List<UserResDto> userResDtoList = new ArrayList<>();
        for (User user : userList) {
            Friend targetFriend = friendRepository.findByRequestAndTarget(me, user).orElse(null);
            Friend requestFriend = friendRepository.findByRequestAndTarget(user, me).orElse(null);

            Friend friend = targetFriend == null ? requestFriend : targetFriend;
            // ????????? ????????????, ?????? ??????????????? ??????
            if (friend == null || !friend.isApproved()) {
                continue;
            }
            ChannelTopic topic = onlineService.getOnlineTopic(user.getUserSeq());
            boolean flag = topic != null;
            UserResDto userResDto = new UserResDto();
            userResDto.setLogin(flag);
            userResDto.setUserSeq(user.getUserSeq());
            userResDto.setEmail(user.getEmail());
            userResDto.setName(user.getName());
            userResDto.setImage(user.getImage());
            userResDto.setStatus(1);


            userResDtoList.add(userResDto);

        }
        return userResDtoList;
    }

    // ?????? ?????????
    public ProfileResDto getUserProfile(String token) {
        User user = userRepository.findByUserSeq(getUserSeqByToken(token)).orElseThrow(() ->
                new CustomException(ErrorCode.NOT_OUR_USER));


        // ????????? gameResDto ??????
        List<GameResDto> gameResDtos = new ArrayList<>();
        int totalGameCnt = 0;
        int totalVicCnt = 0;


        for (Code code : Code.values()) {
            GameUser gameUser = gameUserRepository.findByUserAndGameCode(user, code).orElse(null);
            // ?????? gameUser??? ??????????????? ????????? ?????? ?????? ?????? (???????????? ??????)
            if (gameUser == null) {
                gameService.createGame(user.getUserSeq(), code);
                gameUser = gameUserRepository.findByUserAndGameCode(user, code).orElseThrow(() ->
                        new CustomException(ErrorCode.WRONG_DATA));
            }
            Game game = gameUser.getGame();

            GameResDto gameResDto = new GameResDto();

            int gameCnt = game.getGameCnt();
            int vicCnt = game.getVicCnt();
            gameResDto.setGameCode(gameUser.getGameCode());
            gameResDto.setGameCnt(gameCnt);
            gameResDto.setVicCnt(vicCnt);
            gameResDtos.add(gameResDto);

            totalGameCnt += gameCnt;
            totalVicCnt += vicCnt;
        }
        // ????????? ?????? ??????
        List<PictureResDto> pictureResDtos = new ArrayList<>();
        List<Picture> pictureList = pictureRepository.findAllByUserAndDelYn(user, false).orElse(null);
        // ???????????? ??????
        if (pictureList != null) {
            for (Picture picture : pictureList) {
                PictureResDto pictureResDto = new PictureResDto();
                pictureResDto.setPictureUrl(picture.getPictureUrl());
                pictureResDto.setCreatedDate(picture.getCreatedDate());

                pictureResDtos.add(pictureResDto);
            }
        }

        // profileResDto??? ?????? ??????
        ProfileResDto profileResDto = new ProfileResDto();

        profileResDto.setUserSeq(user.getUserSeq());
        profileResDto.setEmail(user.getEmail());
        profileResDto.setName(user.getName());
        profileResDto.setImage(user.getImage());
        profileResDto.setGameResDtos(gameResDtos);
        profileResDto.setPictureResDtos(pictureResDtos);
        profileResDto.setTotalGameCnt(totalGameCnt);
        profileResDto.setTotalVicCnt(totalVicCnt);

        return profileResDto;
    }
}
