package com.example.web.controller;

import com.example.web.dto.user.UserInfoDto;
import com.example.web.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "user", description = "유저 관련")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(
      summary = "유저 정보 조회",
      description = "JWT 헤더에 추가 필요(로그인 하세요)",
      responses = @ApiResponse(
          description = "OK",
          responseCode = "200",
          content = @Content(
              mediaType = "application/json",
              examples = {
                  @ExampleObject(
                      value = "{\"ResultVal\":0,\"ResultMsg\":\"Success\"," +
                          "\"UserInfo\":{\"userIndex\":1," +
                          "\"emailAddress\":\"qjarud32@naver.com\"," +
                          "\"nickName\":\"김범경\",\"createdAt\":\"2023-11-13T18:39:02.09767\"," +
                          "\"money\":10000,\"lastLoginAt\":\"2023-11-13T18:39:02.10481\"}}")},
              schema = @Schema(
                  implementation = UserInfoDto.Response.class))))
  @GetMapping("/info")
  public UserInfoDto.Response getUseInfo() {
    return userService.getUserInfo();
  }
}
