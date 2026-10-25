package com.car.rental.db;

import com.car.rental.model.DeviceOperation;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DeviceOperationRepository {
    private final DataSource dataSource;
    public DeviceOperationRepository(DataSource dataSource) { this.dataSource = dataSource; }

    public void create(String id, String operationType, String entityType, String entityId, String actor, String detailsJson) throws SQLException {
        try (Connection c = dataSource.getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO DeviceOperation(operation_id,operation_type,entity_type,entity_id,actor,state,device_succeeded,details_json) VALUES(?,?,?,?,?,'PENDING',-1,?)")) {
            p.setString(1,id); p.setString(2,operationType); p.setString(3,entityType); p.setString(4,entityId); p.setString(5,actor); p.setString(6,detailsJson); p.executeUpdate();
        }
    }
    public void update(String id, String state, Boolean deviceSucceeded, String note) throws SQLException {
        try (Connection c = dataSource.getConnection()) { update(c,id,state,deviceSucceeded,note); }
    }
    public void update(Connection c, String id, String state, Boolean deviceSucceeded, String note) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("UPDATE DeviceOperation SET state=?,device_succeeded=?,note=?,updated_at=datetime('now','localtime') WHERE operation_id=?")) {
            p.setString(1,state); p.setInt(2,deviceSucceeded==null?-1:(deviceSucceeded?1:0)); p.setString(3,note); p.setString(4,id);
            if (p.executeUpdate()!=1) throw new SQLException("دفتر عملیات دستگاه یافت نشد");
        }
    }
    public DeviceOperation findUnresolved(String entityType, String entityId) throws SQLException {
        String sql="SELECT * FROM DeviceOperation WHERE entity_type=? AND entity_id IS ? AND state NOT IN ('COMPLETED','FAILED') ORDER BY created_at DESC LIMIT 1";
        try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setString(1,entityType);p.setString(2,entityId);try(ResultSet r=p.executeQuery()){return r.next()?map(r):null;}
        }
    }
    public List<DeviceOperation> listUnresolved() throws SQLException {
        String sql="SELECT * FROM DeviceOperation WHERE state NOT IN ('COMPLETED','FAILED') ORDER BY updated_at DESC,created_at DESC";
        try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){
            List<DeviceOperation> out=new ArrayList<>();while(r.next())out.add(map(r));return out;
        }
    }
    private static DeviceOperation map(ResultSet r)throws SQLException{
        int deviceOutcome=r.getInt("device_succeeded");
        return new DeviceOperation(r.getString("operation_id"),r.getString("operation_type"),r.getString("entity_type"),
                r.getString("entity_id"),r.getString("actor"),r.getString("state"),deviceOutcome<0?null:deviceOutcome==1,
                r.getString("note"),r.getString("details_json"),r.getString("created_at"),r.getString("updated_at"));
    }
}
