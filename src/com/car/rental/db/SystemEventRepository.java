package com.car.rental.db;

import com.car.rental.model.SystemEvent;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Repository
public class SystemEventRepository {
    private final DataSource dataSource;
    public SystemEventRepository(DataSource dataSource) { this.dataSource = dataSource; }

    public void insert(String category, String type, String result, String description,
                       String actor, String entityType, String entityId, String details) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            insert(c, category, type, result, description, actor, entityType, entityId, details);
        }
    }

    public void insert(Connection c, String category, String type, String result, String description,
                       String actor, String entityType, String entityId, String details) throws SQLException {
        String sql = "INSERT INTO SystemEvent(category,event_type,result,description,actor,entity_type,entity_id,details_json) VALUES(?,?,?,?,?,?,?,?)";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, category); p.setString(2, type); p.setString(3, result); p.setString(4, description);
            p.setString(5, actor); p.setString(6, entityType); p.setString(7, entityId); p.setString(8, details);
            p.executeUpdate();
        }
    }

    public List<SystemEvent> find(String from, String to, String category, String type, String result,
                                 String actor, String search, int page, int size) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id,occurred_at,category,event_type,result,description,actor,entity_type,entity_id,details_json FROM SystemEvent WHERE 1=1");
        List<String> args = new ArrayList<>();
        if (from != null) { sql.append(" AND occurred_at >= ?"); args.add(from); }
        if (to != null) { sql.append(" AND occurred_at < ?"); args.add(to); }
        if (category != null && !category.isBlank()) { sql.append(" AND category=?"); args.add(category); }
        if (type != null && !type.isBlank()) { sql.append(" AND event_type=?"); args.add(type); }
        if (result != null && !result.isBlank()) { sql.append(" AND result=?"); args.add(result); }
        if (actor != null && !actor.isBlank()) { sql.append(" AND actor LIKE ? ESCAPE '\\'"); args.add("%" + escapeLike(actor) + "%"); }
        if (search != null && !search.isBlank()) { sql.append(" AND (description LIKE ? ESCAPE '\\' OR entity_id LIKE ? ESCAPE '\\')"); String q="%"+escapeLike(search)+"%"; args.add(q); args.add(q); }
        sql.append(" ORDER BY occurred_at DESC,id DESC LIMIT ? OFFSET ?");
        try (Connection c = dataSource.getConnection(); PreparedStatement p = c.prepareStatement(sql.toString())) {
            int i=1; for (String a:args) p.setString(i++,a); p.setInt(i++,size); p.setInt(i,page*size);
            try (ResultSet r=p.executeQuery()) { List<SystemEvent> out=new ArrayList<>(); while(r.next()) out.add(new SystemEvent(
                    r.getLong("id"),r.getString("occurred_at"),r.getString("category"),r.getString("event_type"),r.getString("result"),
                    r.getString("description"),r.getString("actor"),r.getString("entity_type"),r.getString("entity_id"),r.getString("details_json"))); return out; }
        }
    }

    public long count(String from, String to, String category, String type, String result, String actor, String search) throws SQLException {
        StringBuilder sql=new StringBuilder("SELECT COUNT(*) FROM SystemEvent WHERE 1=1"); List<String> args=new ArrayList<>();
        if(from!=null){sql.append(" AND occurred_at >= ?");args.add(from);} if(to!=null){sql.append(" AND occurred_at < ?");args.add(to);}
        if(category!=null&&!category.isBlank()){sql.append(" AND category=?");args.add(category);} if(type!=null&&!type.isBlank()){sql.append(" AND event_type=?");args.add(type);}
        if(result!=null&&!result.isBlank()){sql.append(" AND result=?");args.add(result);} if(actor!=null&&!actor.isBlank()){sql.append(" AND actor LIKE ? ESCAPE '\\'");args.add("%"+escapeLike(actor)+"%");}
        if(search!=null&&!search.isBlank()){sql.append(" AND (description LIKE ? ESCAPE '\\' OR entity_id LIKE ? ESCAPE '\\')");String q="%"+escapeLike(search)+"%";args.add(q);args.add(q);}
        try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement(sql.toString())){int i=1;for(String a:args)p.setString(i++,a);try(ResultSet r=p.executeQuery()){return r.next()?r.getLong(1):0;}}
    }
    private static String escapeLike(String s){return s.replace("\\","\\\\").replace("%","\\%").replace("_","\\_");}
}
