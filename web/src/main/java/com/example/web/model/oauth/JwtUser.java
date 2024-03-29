package com.example.web.model.oauth;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
public class JwtUser {

  private long userIndex;
  //만료 시간
  private long expireTime;

  // 필요시 추후 정보 추가.
}
