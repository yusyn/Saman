package com.car.rental.service;

import com.car.rental.api.dto.DeviceOperationPage;
import com.car.rental.db.DeviceOperationRepository;
import com.car.rental.model.DeviceOperation;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class DeviceOperationService {
    private static final Logger log=Logger.getLogger(DeviceOperationService.class.getName());
    private final DeviceOperationRepository repository;
    private final SystemEventService events;
    public DeviceOperationService(DeviceOperationRepository repository,SystemEventService events){this.repository=repository;this.events=events;}

    public String begin(String operationType,String entityType,String entityId,Map<String,?> details) throws SQLException {
        DeviceOperation pending=repository.findUnresolved(entityType,entityId);
        if(pending!=null)throw new DeviceOperationException(
                "برای این موجودیت عملیات دستگاهِ نیازمند پیگیری وجود دارد (شناسه "+pending.operationId()+", وضعیت "+pending.state()+"). پیش از بررسی، عملیات را تکرار نکنید.",
                pending.operationId(),pending.state(),409,pending.deviceSucceeded(),null);
        String id=UUID.randomUUID().toString();
        try {
            repository.create(id,operationType,entityType,entityId,actor(),events.safeDetailsJson(details));
        } catch (SQLException createFailure) {
            DeviceOperation raced;
            try { raced=repository.findUnresolved(entityType,entityId); }
            catch(SQLException lookupFailure){createFailure.addSuppressed(lookupFailure);throw createFailure;}
            if(raced!=null)throw new DeviceOperationException(
                    "برای این موجودیت عملیات دستگاهِ نیازمند پیگیری وجود دارد (شناسه "+raced.operationId()+", وضعیت "+raced.state()+"). پیش از بررسی، عملیات را تکرار نکنید.",
                    raced.operationId(),raced.state(),409,raced.deviceSucceeded(),createFailure);
            throw createFailure;
        }
        return id;
    }
    public void deviceSucceeded(String id) {
        try{repository.update(id,"DEVICE_SUCCEEDED",true,"عمل دستگاه موفق شد؛ ذخیرهٔ نهایی در پایگاه‌داده در جریان است");}
        catch(SQLException e){
            recordFailure("عمل دستگاه انجام شد اما وضعیت آن در دفتر عملیات به‌روزرسانی نشد",id,"RECONCILIATION_REQUIRED",true,e);
            throw new DeviceOperationException("دستگاه عمل را انجام داد اما ثبت وضعیت آن کامل نشد. عملیات را تکرار نکنید؛ برای پیگیری از شناسهٔ "+id+" استفاده کنید.",id,"RECONCILIATION_REQUIRED",202,true,e);
        }
    }
    public void deviceFailed(String id,String category,String eventType,String description,Throwable cause) {
        try{repository.update(id,"FAILED",false,"عمل دستگاه انجام نشد");}
        catch(SQLException e){cause.addSuppressed(e);log.log(Level.SEVERE,"Unable to close failed device operation "+id,e);}
        events.recordFailureSafely(new SystemEventService.EventSpec(category,eventType,description,"دستگاه",id,
                Map.of("operationId",id,"operationState","FAILED")),cause);
        throw new DeviceOperationException("عملیات دستگاه انجام نشد؛ شناسهٔ پیگیری "+id,id,"FAILED",502,false,cause);
    }
    public void complete(Connection connection,String id) throws SQLException {
        repository.update(connection,id,"COMPLETED",true,"عمل دستگاه و پایگاه‌داده هماهنگ شد");
    }
    public void databaseCommitted(Connection connection,String id) throws SQLException {
        repository.update(connection,id,"DB_COMMITTED",null,"تغییر پایگاه‌داده ثبت شد؛ پاک‌سازی دستگاه در انتظار است");
    }
    public void completeAfterDatabase(String id) throws SQLException {
        repository.update(id,"COMPLETED",true,"تغییر پایگاه‌داده و دستگاه تکمیل شد");
    }
    public void markAfterCompensation(String id,boolean compensated,Throwable cause) {
        try { repository.update(id,compensated?"FAILED":"RECONCILIATION_REQUIRED",true,
                compensated?"عمل دستگاه جبران شد؛ تغییر پایگاه‌داده rollback شد":"جبران دستگاه ناموفق بود؛ تطبیق دستی لازم است"); }
        catch(SQLException e){cause.addSuppressed(e);log.log(Level.SEVERE,"Unable to update compensated device operation "+id,e);}
    }
    public void failBeforeDevice(String id,Throwable cause) {
        try{repository.update(id,"FAILED",false,"عمل دستگاه شروع نشد؛ تغییر پایگاه‌داده rollback شد");}
        catch(SQLException e){cause.addSuppressed(e);log.log(Level.SEVERE,"Unable to close device operation "+id,e);}
    }
    public DeviceOperationException cleanupFailed(String id,String entityType,String entityId,Throwable cause) {
        markReconciliation(id,false,"رکورد پایگاه‌داده حذف شد اما حذف کاربر دستگاه انجام نشد",cause);
        events.recordFailureSafely(new SystemEventService.EventSpec("دستگاه اثرانگشت","پاک‌سازی کاربر دستگاه",
                "رکورد کارمند حذف شد اما حذف حساب از دستگاه نیازمند پیگیری است",entityType,entityId,
                Map.of("operationId",id,"operationState","RECONCILIATION_REQUIRED")),cause);
        return new DeviceOperationException("کارمند از پایگاه‌داده حذف شد اما حساب دستگاه حذف نشد. حذف را تکرار نکنید؛ برای تطبیق از شناسهٔ "+id+" استفاده کنید.",
                id,"RECONCILIATION_REQUIRED",202,false,cause);
    }
    public void markReconciliation(String id,String note,Throwable cause) {
        try { repository.update(id,"RECONCILIATION_REQUIRED",true,note); }
        catch(SQLException e){cause.addSuppressed(e);log.log(Level.SEVERE,"Unable to mark device operation for reconciliation "+id,e);}
    }
    public void markReconciliation(String id,boolean deviceSucceeded,String note,Throwable cause) {
        try { repository.update(id,"RECONCILIATION_REQUIRED",deviceSucceeded,note); }
        catch(SQLException e){cause.addSuppressed(e);log.log(Level.SEVERE,"Unable to mark device operation for reconciliation "+id,e);}
    }
    public void completeDeviceOnly(String id,String category,String eventType,String description,String entityType,String entityId) {
        try {
            events.inTransaction(
                    new SystemEventService.EventSpec(category,eventType,description,entityType,entityId,Map.of("operationId",id)),
                    new SystemEventService.EventSpec(category,eventType,"ثبت نهایی عملیات دستگاه انجام نشد",entityType,entityId,Map.of("operationId",id)),
                    c->{repository.update(c,id,"COMPLETED",true,"عملیات دستگاه تکمیل شد");return null;});
        } catch(SQLException e) {
            markReconciliation(id,"دستگاه عمل را انجام داد اما تکمیل دفتر و گزارش ثبت نشد",e);
            throw new DeviceOperationException("دستگاه عمل را انجام داد اما ثبت نهایی گزارش کامل نشد. عملیات را تکرار نکنید؛ شناسهٔ پیگیری "+id,id,"RECONCILIATION_REQUIRED",202,true,e);
        }
    }
    public void failAfterCompensation(String id,String category,String eventType,String entityType,String entityId,boolean compensated,Throwable cause) {
        String state=compensated?"FAILED":"RECONCILIATION_REQUIRED";
        try{repository.update(id,state,true,compensated?"عمل دستگاه جبران شد؛ تغییر پایگاه‌داده rollback شد":"جبران دستگاه ناموفق بود؛ تطبیق دستی لازم است");}
        catch(SQLException e){cause.addSuppressed(e);log.log(Level.SEVERE,"Unable to update device operation "+id,e);state="RECONCILIATION_REQUIRED";}
        String description=compensated?"عمل پایگاه‌داده انجام نشد و تغییر دستگاه جبران شد":"تغییر دستگاه انجام شد اما ذخیرهٔ پایگاه‌داده شکست خورد؛ تطبیق لازم است";
        events.recordFailureSafely(new SystemEventService.EventSpec(category,eventType,description,entityType,entityId,
                Map.of("operationId",id,"operationState",state,"deviceSucceeded",true)),cause);
        String message=compensated
                ? "تغییر پایگاه‌داده انجام نشد و تغییر دستگاه جبران شد؛ عملیات کامل نشده است. شناسهٔ پیگیری "+id
                : "دستگاه تغییر را انجام داد اما پایگاه‌داده آن را نپذیرفت و جبران هم کامل نشد. عملیات را تکرار نکنید؛ شناسهٔ پیگیری "+id;
        throw new DeviceOperationException(message,id,state,compensated?500:202,cause);
    }
    private void recordFailure(String description,String id,String state,boolean deviceSucceeded,Throwable cause){
        events.recordFailureSafely(new SystemEventService.EventSpec("دستگاه اثرانگشت","تطبیق عملیات دستگاه",description,"دستگاه",id,
                Map.of("operationId",id,"operationState",state,"deviceSucceeded",deviceSucceeded)),cause);
    }
    public DeviceOperationPage pending() throws SQLException {List<DeviceOperation> items=repository.listUnresolved();return new DeviceOperationPage(items,items.size());}
    private static String actor(){Authentication a=SecurityContextHolder.getContext().getAuthentication();return a!=null&&a.isAuthenticated()&&!"anonymousUser".equals(a.getPrincipal())?a.getName():"سامانه";}
}
