package com.agnosticrouter.observability.application.service;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Pure dashboard aggregation; no JDBC or broker dependencies. */
@Component
public class MetricsReportBuilder {
    public Map<String, Object> build(List<Map<String,Object>> rows, ZoneId zone, LocalDate today) {
        LocalDate start = today.minusDays(6);
        LinkedHashMap<String,Map<String,Object>> days=new LinkedHashMap<>();
        for(int i=0;i<7;i++){
            String day=start.plusDays(i).toString();
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("day",day);row.put("received",0);row.put("created",0);row.put("review",0);row.put("errors",0);
            days.put(day,row);
        }
        int todayReceived=0,ruleOnly=0,aiUsed=0,ruleDimensions=0,aiDimensions=0,lowConfidence=0,aiUnavailable=0,review=0,crmCreated=0,crmError=0,pending=0;
        for(Map<String,Object> r:rows){
            String day=((Instant)r.get("at")).atZone(zone).toLocalDate().toString();
            Map<String,Object> d=days.get(day);
            if(d==null)continue;
            inc(d,"received");
            if(day.equals(today.toString()))todayReceived++;
            int rules=(int)r.get("rules"),ai=(int)r.get("ai"),rejected=(int)r.get("rejected");
            ruleDimensions+=rules;aiDimensions+=ai;
            var decisions=RequestExplanationBuilder.decisions(r);
            if(ai>0)aiUsed++;
            else if(rules>0 && !decisions.isEmpty() && decisions.values().stream().allMatch(value -> value instanceof Map<?,?> decision && "RULE".equals(decision.get("source"))))ruleOnly++;
            if(decisions.values().stream().anyMatch(value -> value instanceof Map<?,?> decision && RequestExplanationBuilder.lowConfidence(decision)))lowConfidence++;
            if(decisions.values().stream().anyMatch(value -> value instanceof Map<?,?> decision && RequestExplanationBuilder.unavailable(decision)))aiUnavailable++;
            switch((String)r.get("state")){
                case "CRM_CREATED"->{crmCreated++;inc(d,"created");}
                case "CRM_ERROR"->{crmError++;inc(d,"errors");}
                case "REVIEW_REQUIRED"->{review++;inc(d,"review");}
                default->pending++;
            }
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("timezone",zone.getId());result.put("windowDays",7);
        result.put("todayReceived",todayReceived);result.put("received",rows.size());
        result.put("ruleOnly",ruleOnly);result.put("aiUsed",aiUsed);
        result.put("ruleDimensions",ruleDimensions);result.put("aiDimensions",aiDimensions);
        result.put("lowConfidence",lowConfidence);result.put("review",review);
        result.put("aiUnavailable",aiUnavailable);
        result.put("crmCreated",crmCreated);result.put("crmError",crmError);result.put("pending",pending);
        result.put("daily",new ArrayList<>(days.values()));result.put("recent",rows.stream().limit(30).map(RequestExplanationBuilder::explain).toList());
        return result;
    }
    private void inc(Map<String,Object> row,String key){row.put(key,(int)row.get(key)+1);}
}
