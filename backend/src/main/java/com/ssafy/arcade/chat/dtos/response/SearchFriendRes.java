package com.ssafy.arcade.chat.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class SearchFriendRes {
    private String name;
    private String image;
    private String email;
    private boolean canInvite;
    private Long userSeq;
    private boolean login;
}
