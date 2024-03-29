package com.example.web.util.externalApi;

import com.example.web.model.exception.CustomErrorException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;

public class externalApi {

  /**
   * Post 요청으로 외부 API 응답 획득
   *
   * @param url        요청 보낼 외부 API URL
   * @param httpEntity 요청 보낼 http 엔티티 (헤더+바디)
   * @param clazz      응답시 casting 할 클래스 형태
   * @return
   */
  public static Object getResponseFromPostRequest(String url, HttpEntity<?> httpEntity, Class<?> clazz) {
    RestTemplate restTemplate = new RestTemplate();

    Object response = restTemplate.postForObject(url, httpEntity, clazz);
    checkResponse(response);

    return response;
  }

  private static Object checkResponse(Object response) {
    if (response == null) {
      throw CustomErrorException.builder().resultValue(1).build();
    }

    return response;
  }

  public static Object getResponseFromGetRequest(UriComponents uriBuilder, HttpEntity request) {
    RestTemplate restTemplate = new RestTemplate();

    Object response = restTemplate.exchange(uriBuilder.toUriString(),
        HttpMethod.GET, request, String.class);

    checkResponse(response);

    return response;
  }
}
