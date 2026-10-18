package com.agnosticrouter.gmail.domain;

import java.nio.file.*;
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Imports only non-secret, deduplication metadata from the legacy read-only SQLite volume.
 * OAuth tokens are intentionally never imported: a new consent is required for the Java service.
 */
@Component
public class LegacyProcessedImporter implements ApplicationRunner {
 private static final Logger log=LoggerFactory.getLogger(LegacyProcessedImporter.class);
 private final JdbcTemplate jdbc;private final String file;
 public LegacyProcessedImporter(JdbcTemplate jdbc,@Value("${app.gmail.legacy-db:/legacy/gmail.sqlite}") String file){
  this.jdbc=jdbc;this.file=file;
 }
 @Override public void run(ApplicationArguments args) throws Exception {
  Path source=Path.of(file);
  if(!Files.isRegularFile(source)){log.info("No previous Gmail SQLite state to import");return;}
  int transferred=0;
  try(Connection old=DriverManager.getConnection("jdbc:sqlite:file:"+source.toAbsolutePath()+"?mode=ro");
      Statement stmt=old.createStatement();
      ResultSet rs=stmt.executeQuery("SELECT email,message_id,request_id,processed_at FROM processed")){
   while(rs.next()){
    jdbc.update("INSERT INTO gmail_connector.processed(email,message_id,request_id,processed_at) VALUES(?,?,?,?) ON CONFLICT DO NOTHING",
      rs.getString(1),rs.getString(2),rs.getString(3),rs.getLong(4));
    transferred++;
   }
  }catch(SQLException e){
   throw new IllegalStateException("Cannot import Gmail deduplication history; refusing to start to prevent duplicate tickets",e);
  }
  log.info("Checked {} previous Gmail message IDs (idempotent)",transferred);
 }
}
