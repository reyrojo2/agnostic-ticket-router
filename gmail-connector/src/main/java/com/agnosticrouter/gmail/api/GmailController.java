package com.agnosticrouter.gmail.api;

import com.agnosticrouter.gmail.service.GmailService;
import java.net.URI;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class GmailController {
 private final GmailService service;
 public GmailController(GmailService service){this.service=service;}
 public record Settings(String profileCode,String query,boolean enabled){}
 @GetMapping("/api/v1/admin/gmail/status")
 public Map<String,Object> status(){return service.status();}
 @GetMapping("/api/v1/admin/gmail/authorize")
 public Map<String,String> authorize(){return service.authorize();}
 @PutMapping("/api/v1/admin/gmail/settings")
 public Map<String,Object> save(@RequestBody Settings data){return service.save(data.profileCode(),data.query(),data.enabled());}
 @PostMapping("/api/v1/admin/gmail/sync")
 public Map<String,Object> sync(){return service.sync();}
 @DeleteMapping("/api/v1/admin/gmail/connection")
 public Map<String,Object> disconnect(){return service.disconnect();}
 @GetMapping("/api/v1/gmail/oauth/callback")
 public ResponseEntity<Void> callback(@RequestParam(required=false) String code,
       @RequestParam(required=false) String state,@RequestParam(required=false) String error){
  return ResponseEntity.status(HttpStatus.SEE_OTHER)
      .location(URI.create(service.callback(code,state,error))).build();
 }
}
