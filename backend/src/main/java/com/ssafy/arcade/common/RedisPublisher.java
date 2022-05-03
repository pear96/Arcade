package com.ssafy.arcade.common;

import com.ssafy.arcade.chat.dtos.ChatMessageDTO;
import com.ssafy.arcade.notification.dtos.NotiDTO;
import com.ssafy.arcade.notification.repository.NotiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotiRepository notiRepository;

    // Overloading - 알림
    public void publish(ChannelTopic topic, NotiDTO notiDTO) {
        notiRepository.save(notiDTO.toEntity());
        redisTemplate.convertAndSend(topic.getTopic(), notiDTO);
    }

    // Overloading - 채팅
    public void publish(ChannelTopic topic, ChatMessageDTO chatDTO) {
        redisTemplate.convertAndSend(topic.getTopic(), chatDTO);
    }

}