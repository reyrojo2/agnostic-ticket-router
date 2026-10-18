package com.agnosticrouter.gmail.gateway;

import java.util.Map;

public interface GoogleMailClient {
 Map<String,Object> exchangeCode(String code);
 Map<String,Object> refresh(String refreshToken);
 Map<String,Object> profile(String token);
 Map<String,Object> list(String token,String query,String page);
 Map<String,Object> message(String token,String id);
}
