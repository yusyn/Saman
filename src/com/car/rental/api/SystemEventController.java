package com.car.rental.api;

import com.car.rental.api.dto.SystemEventPage;
import com.car.rental.service.SystemEventService;
import com.car.rental.util.JalaliDate;
import org.springframework.web.bind.annotation.*;
import java.sql.SQLException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/system-events")
public class SystemEventController {
    private final SystemEventService events;
    public SystemEventController(SystemEventService events){this.events=events;}

    @GetMapping
    public SystemEventPage list(@RequestParam(required=false) String dateFrom,
                                @RequestParam(required=false) String dateTo,
                                @RequestParam(required=false) String category,
                                @RequestParam(required=false) String type,
                                @RequestParam(required=false) String result,
                                @RequestParam(required=false) String actor,
                                @RequestParam(required=false) String q,
                                @RequestParam(defaultValue="0") int page,
                                @RequestParam(defaultValue="50") int size) throws SQLException {
        if(result!=null&&!result.isBlank()&&!result.equals("موفق")&&!result.equals("ناموفق"))throw new IllegalArgumentException("نتیجه نامعتبر است");
        return events.search(dayStart(dateFrom),dayEnd(dateTo),category,type,result,actor,q,page,size);
    }
    private static String dayStart(String value){return value==null||value.isBlank()?null:parse(value).toString()+" 00:00:00";}
    private static String dayEnd(String value){return value==null||value.isBlank()?null:parse(value).plusDays(1).toString()+" 00:00:00";}
    private static LocalDate parse(String value){String[] p=value.trim().replace('-','/').split("/");if(p.length!=3)throw new IllegalArgumentException("تاریخ باید شمسی و به شکل yyyy/mm/dd باشد");return new JalaliDate(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2])).toGregorian();}
}
