package com.car.rental.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.car.rental.api.dto.SystemEventPage;
import com.car.rental.db.SystemEventRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.sql.SQLException;
import java.util.*;
import java.sql.Connection;
import java.util.function.Predicate;
import javax.sql.DataSource;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class SystemEventService {
    private static final Logger log = Logger.getLogger(SystemEventService.class.getName());
    private static final Set<String> SECRET_KEYS = Set.of("password","password_hash","token","cookie","fingerprint","template","rawdata","secret");
    private final SystemEventRepository repository;
    private final ObjectMapper mapper;
    private final DataSource dataSource;
    public SystemEventService(SystemEventRepository repository, ObjectMapper mapper, DataSource dataSource) {
        this.repository=repository; this.mapper=mapper; this.dataSource=dataSource;
    }

    public void record(String category, String type, String result, String description, String entityType, Object entityId, Map<String, ?> details) {
        if (!"موفق".equals(result) && !"ناموفق".equals(result)) throw new IllegalArgumentException("نتیجه نامعتبر است");
        try {
            repository.insert(clean(category,80),clean(type,100),result,clean(description,500),actor(),clean(entityType,80),entityId==null?null:clean(String.valueOf(entityId),160),safeJson(details));
        } catch (SQLException e) { log.log(Level.SEVERE, "System event persistence failed", e); throw new IllegalStateException("ثبت رویداد سیستم در پایگاه‌داده انجام نشد",e); }
    }
    public void record(String category,String type,String result,String description,String entityType,Object entityId){record(category,type,result,description,entityType,entityId,Map.of());}
    public void recordAs(String actor,String category,String type,String result,String description,String entityType,Object entityId){
        try { repository.insert(clean(category,80),clean(type,100),result,clean(description,500),clean(actor,120),clean(entityType,80),entityId==null?null:clean(String.valueOf(entityId),160),"{}"); }
        catch(SQLException e){ log.log(Level.SEVERE, "System event persistence failed", e); throw new IllegalStateException("ثبت رویداد سیستم در پایگاه‌داده انجام نشد",e); }
    }
    public void record(Connection connection, String category, String type, String result, String description,
                       String entityType, Object entityId, Map<String, ?> details) throws SQLException {
        recordAs(connection, actor(), category, type, result, description, entityType, entityId, details);
    }

    public void recordAs(Connection connection, String actor, String category, String type, String result,
                         String description, String entityType, Object entityId, Map<String, ?> details) throws SQLException {
        validateResult(result);
        repository.insert(connection, clean(category,80), clean(type,100), result, clean(description,500),
                clean(actor,120), clean(entityType,80), entityId == null ? null : clean(String.valueOf(entityId),160), safeJson(details));
    }

    public <T> T inTransaction(EventSpec success, EventSpec failure, SqlWork<T> work) throws SQLException {
        return inTransaction(success, failure, work, ignored -> true);
    }

    public <T> T inTransaction(EventSpec success, EventSpec failure, SqlWork<T> work,
                               Predicate<T> shouldRecordSuccess) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                T value = work.execute(connection);
                if (shouldRecordSuccess.test(value)) {
                    record(connection, success.category(), success.type(), "موفق", success.description(),
                            success.entityType(), success.entityId(), success.details());
                }
                connection.commit();
                return value;
            } catch (SQLException | RuntimeException e) {
                try { connection.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof SQLException sql) {
                    recordFailureSafely(failure, sql);
                    throw new AuditedOperationException(sql);
                }
                recordFailureSafely(failure, e);
                throw e;
            }
        } catch (AuditedOperationException e) {
            throw e;
        } catch (SQLException e) {
            recordFailureSafely(failure, e);
            throw new AuditedOperationException(e);
        }
    }

    public void recordFailureSafely(EventSpec event, Throwable primaryFailure) {
        try {
            record(event.category(), event.type(), "ناموفق", event.description(), event.entityType(),
                    event.entityId(), event.details());
        } catch (RuntimeException auditFailure) {
            if (primaryFailure != null) primaryFailure.addSuppressed(auditFailure);
            log.log(Level.SEVERE, "Unable to persist failure event without masking the primary failure", auditFailure);
        }
    }

    private static void validateResult(String result) {
        if (!"موفق".equals(result) && !"ناموفق".equals(result)) throw new IllegalArgumentException("نتیجه نامعتبر است");
    }

    public record EventSpec(String category, String type, String description, String entityType,
                            Object entityId, Map<String, ?> details) {
        public EventSpec(String category, String type, String description, String entityType, Object entityId) {
            this(category, type, description, entityType, entityId, Map.of());
        }
    }
    @FunctionalInterface public interface SqlWork<T> { T execute(Connection connection) throws SQLException; }
    private String safeJson(Map<String, ?> details) {
        if(details==null||details.isEmpty())return "{}";
        Map<String,Object> safe=new LinkedHashMap<>(); details.forEach((k,v)->{String key=clean(k,80);String normalized=key.toLowerCase(Locale.ROOT);if(SECRET_KEYS.stream().noneMatch(normalized::contains))safe.put(key, sanitizeValue(v));});
        try{return mapper.writeValueAsString(safe);}catch(JsonProcessingException e){log.log(Level.SEVERE, "System event serialization failed", e);throw new IllegalStateException("جزئیات رویداد قابل ذخیره‌سازی نیست",e);}
    }
    public String safeDetailsJson(Map<String, ?> details) { return safeJson(details); }
    private Object sanitizeValue(Object value) {
        if(value==null)return null;
        if(value instanceof Map<?,?> m){Map<String,Object> out=new LinkedHashMap<>();m.forEach((k,v)->{String key=clean(String.valueOf(k),80);if(SECRET_KEYS.stream().noneMatch(key.toLowerCase(Locale.ROOT)::contains))out.put(key,sanitizeValue(v));});return out;}
        if(value instanceof Number||value instanceof Boolean)return value;
        if(value instanceof Collection<?> c)return c.stream().limit(50).map(this::sanitizeValue).toList();
        return clean(String.valueOf(value),300);
    }
    private static String clean(String s,int max){if(s==null)return null;String v=s.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", " ").strip();return v.length()>max?v.substring(0,max):v;}
    private static String actor(){Authentication a=SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.isAuthenticated()&&!"anonymousUser".equals(a.getPrincipal())?clean(a.getName(),120):"سامانه";}
    public SystemEventPage search(String from,String to,String category,String type,String result,String actor,String search,int page,int size)throws SQLException{
        int p=Math.max(0,page), s=Math.max(1,Math.min(100,size));
        return new SystemEventPage(repository.find(from,to,category,type,result,actor,search,p,s),p,s,repository.count(from,to,category,type,result,actor,search));
    }
}
