# برنامه پیاده‌سازی بهبود سوابق ناوگان، گزارش‌های برگشت مأموریت و رصد مدارک

این برنامه ساختار و مراحل اجرایی بازطراحی بخش سوابق وسایل نقلیه (رویکرد A) را مشخص می‌کند.

**هدف:** اصلاح فرایند برگشت وسیله نقلیه با ثبت اجباری کیلومتر، ثبت گزارش‌های اختیاری مأموریت (بنزین، خدمات/سرویس، تصادف، خلافی) همراه با تصویر پیوست، رصد انقضای مدارک و بیمه، و رفع پرش رابط کاربری مودال سوابق.

**معماری:**
1. **پایگاه داده (SQLite):**
   - افزودن ستون `return_odometer` به `RentalTable`.
   - ایجاد جداول `Attachment` (ذخیره اطلاعات فایل‌ها و تصاویر)، `VehicleMissionReport` (گزارش‌های متصل به مأموریت برای بنزین، خدمات، تصادفات و خلافی‌ها) و `VehicleDocument` (بیمه ثالث، بیمه بدنه، معاینه فنی و مدارک مرتبط با تاریخ انقضا).
   - افزودن تاریخ انقضای گواهینامه به `EmployeeTable`.
2. **سرویس‌ها و کنترلرها:**
   - سرویس `AttachmentService` و کنترلر آپلود/مشاهده فایل‌های پیوست در مسیر ذخیره‌سازی محلی دیسک.
   - به‌روزرسانی `RentalService.returnCar` برای دریافت الزامی کیلومتر و اتصال گزارش‌های همزمان برگشت.
   - سرویس `VehicleHistoryService`: حذف الگوریتم تخمینی ۵۰۰۰ کیلومتر و جایگزینی با نشان‌های انقضای مدارک، تصادفات باز و خلافی پرداخت‌نشده.
   - متدها و کنترلر ثبت عطف‌به‌ماسبق گزارش برای مأموریت‌های گذشته.
3. **رابط کاربری:**
   - پایدارسازی ساختار CSS مودال سوابق با هدر و تب‌های ثابت و بدنه اسکرول‌پذیر اختصاصی برای جلوگیری از جابجایی دکمه‌ها و عناصر.
   - فرم برگشت خودرو در تب تحویل/برگشت با فیلد اجباری کیلومتر و بخش‌های بازشونده (آکاردئون) برای پیوست اختیاری بنزین، خدمات، تصادف یا خلافی.
   - بازطراحی تب‌های مودال سوابق (خلاصه، مأموریت‌ها و گزارش‌ها، خدمات و بنزین، تصادفات، خلافی‌ها، مدارک و بیمه‌نامه‌ها).

**تکنولوژی‌ها:** Java 17, Spring Boot 3.3.5, SQLite JDBC, JavaScript ES6, HTML5/CSS3.

---

### تسک ۱: اصلاح و توسعه اسکیمای پایگاه داده (SchemaInitializer)
- **فایل‌ها:**
  - ویرایش: `src/com/car/rental/db/SchemaInitializer.java`
  - تست: `src/test/java/com/car/rental/db/ForeignKeyEnforcementTest.java`
- **اقدامات:**
  - افزودن ستون `return_odometer` به `RentalTable` در صورت عدم وجود (`ALTER TABLE`).
  - ایجاد جدول `Attachment`: شناسه، نام اصلی، مسیر فایل روی دیسک، نوع محتوا، حجم و زمان ثبت.
  - ایجاد جدول `VehicleMissionReport`: شناسه، `rental_id`، `vehicle_id`، `employee_id`، نوع گزارش (`FUEL`، `SERVICE`، `ACCIDENT`، `FINE`)، جزئیات، هزینه، تصویر پیوست، تاریخ و وضعیت.
  - ایجاد جدول `VehicleDocument`: شناسه، `vehicle_id`، نوع مدرک (`THIRD_PARTY_INSURANCE`، `BODY_INSURANCE`، `TECHNICAL_INSPECTION`، `OTHER`)، شماره، تاریخ انقضا، پیوست و توضیحات.
  - افزودن ستون‌های `driver_license_number` و `driver_license_expiry` به `EmployeeTable`.

---

### تسک ۲: ماژول ذخیره‌سازی و سرویس پیوست‌ها (Attachment)
- **فایل‌ها:**
  - ساخت: `src/com/car/rental/model/Attachment.java`
  - ساخت: `src/com/car/rental/db/AttachmentRepository.java`
  - ساخت: `src/com/car/rental/service/AttachmentService.java`
  - ساخت: `src/com/car/rental/api/AttachmentController.java`
  - تست: `src/test/java/com/car/rental/api/AttachmentControllerTest.java`
- **اقدامات:**
  - ذخیره فایل‌های ارسالی فرم در پوشه `data/uploads/` به شیوه امن و جلوگیری از نفوذ دایرکتوری (Path Traversal).
  - ارائه اندپوینت‌های `POST /api/attachments/upload` و `GET /api/attachments/{id}` برای مشاهده یا دانلود تصویر رسید و خسارت.

---

### تسک ۳: سرویس گزارش‌های مأموریت و ثبت پیونددار وسایل
- **فایل‌ها:**
  - ساخت: `src/com/car/rental/model/VehicleMissionReport.java`
  - ساخت: `src/com/car/rental/db/VehicleMissionReportRepository.java`
  - ویرایش: `src/com/car/rental/service/VehicleHistoryService.java`
  - ساخت DTOهای مربوطه در پکیج `com.car.rental.api.dto`
- **اقدامات:**
  - پیاده‌سازی متدهای درج گزارش‌های بنزین، خدمات دوره‌ای (کارواش، روغن، مایع شیشه‌شور)، تصادفات و خلافی با ذخیره همزمان `rental_id` و `employee_id`.
  - ایجاد قابلیت استعلام گزارش‌ها بر اساس وسیله و تفکیک بر اساس مأموریت یا به صورت تایم‌لاین.

---

### تسک ۴: بازنگری در نشان‌های وضعیت (حذف تخمین ۵۰۰۰ کیلومتر و افزودن هشدارهای مدارک)
- **فایل‌ها:**
  - ویرایش: `src/com/car/rental/service/VehicleHistoryService.java`
  - ساخت: `src/com/car/rental/db/VehicleDocumentRepository.java`
  - ساخت: `src/com/car/rental/model/VehicleDocument.java`
  - تست: `src/test/java/com/car/rental/service/VehicleHistoryIdempotencyTest.java`
- **اقدامات:**
  - حذف بررسی شرط ۵۰۰۰ کیلومتر (`DEFAULT_SERVICE_INTERVAL_KM`) از کد `needsService`.
  - افزودن نشان‌های هشدار مدارک:
    - `INSURANCE_EXPIRED` یا `INSURANCE_EXPIRING` (انقضای نزدیک بیمه شخص ثالث یا بدنه ظرف ۳۰ روز).
    - `INSPECTION_EXPIRED` یا `INSPECTION_EXPIRING` (معاینه فنی).
    - `ACCIDENT_OPEN` (تصادف نیازمند پیگیری).
    - `HAS_FINE` (خلافی پرداخت نشده).

---

### تسک ۵: به‌روزرسانی فرایند برگشت وسیله (ثبت اجباری کیلومتر و گزارش‌های برگشت)
- **فایل‌ها:**
  - ویرایش: `src/com/car/rental/api/RentalController.java`
  - ویرایش: `src/com/car/rental/service/RentalService.java`
  - ویرایش: `src/com/car/rental/db/RentalRepository.java`
  - تست: `src/test/java/com/car/rental/api/RentalControllerTest.java`
- **اقدامات:**
  - افزودن فیلد `returnOdometer` (اجباری) و فهرست گزارش‌های همراه به درخواست برگشت خودرو.
  - اعتبارسنجی: کیلومتر برگشت نباید کمتر از کیلومتر فعلی ثبت‌شده وسیله باشد.
  - ثبت تراکنشی: ذخیره کیلومتر برگشت در مأموریت، به‌روزرسانی کیلومتر فعلی وسیله نقلیه، ثبت گزارش‌های ضمیمه (در صورت وجود)، و آزاد کردن وضعیت کارمند و وسیله.

---

### تسک ۶: ثبت عطف‌به‌ماسبق گزارش برای مأموریت‌های گذشته
- **فایل‌ها:**
  - ویرایش: `src/com/car/rental/api/VehicleHistoryController.java`
  - ویرایش: `src/com/car/rental/service/VehicleHistoryService.java`
- **اقدامات:**
  - اندپوینت `POST /api/vehicles/history/rentals/{rentalId}/reports` برای ثبت خلافی یا گزارش بنزین و خسارت پس از اتمام مأموریت با استخراج خودکار راننده از اطلاعات همان مأموریت.

---

### تسک ۷: مدیریت مدارک و بیمه‌نامه‌های وسایل نقلیه و رانندگان
- **فایل‌ها:**
  - ساخت: `src/com/car/rental/api/VehicleDocumentController.java`
  - ویرایش: `src/com/car/rental/api/EmployeeController.java`
- **اقدامات:**
  - اندپوینت‌های ثبت، ویرایش، حذف و مشاهده مدارک وسیله (`/api/vehicles/{plate}/documents`).
  - به‌روزرسانی تاریخ انقضای گواهینامه راننده در بخش کارمندان و بررسی انقضا در گزارش‌ها.

---

### تسک ۸: اصلاح پرش و چیدمان CSS مودال سوابق
- **فایل‌ها:**
  - ویرایش: `src/main/resources/static/css/vehicle-history.css`
  - ویرایش: `src/main/resources/static/css/app.css`
- **اقدامات:**
  - تنظیم ارتفاع ثابت و ساختار Flexbox عمودی برای `.modal-panel-wide`.
  - ثابت نگه‌داشتن `.modal-head` و `.history-tabs` در بالای پنل.
  - انتقال اسکرول عمودی به داخل بدنه محتوایی هر تب (`.history-pane { overflow-y: auto; height: 100%; }`).
  - تضمین عدم تغییر ابعاد کادر مودال و پرش دکمه بستن یا عنوان هنگام تعویض تب‌ها.

---

### تسک ۹: بازطراحی فرم برگشت وسیله در فرانت‌اند
- **فایل‌ها:**
  - ویرایش: `src/main/resources/static/index.html`
  - ویرایش: `src/main/resources/static/js/app.js`
- **اقدامات:**
  - نمایش کیلومتر ثبت‌شده فعلی وسیله بلافاصله پس از احراز هویت کارمند.
  - فیلد اجباری ورود کیلومتر برگشت.
  - بخش‌های آکاردئونی اختیاری:
    - بنزین: لیتر، مبلغ، آپلود فایل رسید.
    - خدمات: نوع (کارواش، روغن، شیشه‌شور، سایر)، هزینه، فایل فاکتور، توضیحات.
    - تصادف: شدت، شرح واقعه، عکس خسارت.
    - خلافی: نوع تخلف، مبلغ، تاریخ، توضیحات.
  - دکمه ثبت با ارسال یکپارچه و بازنشانی فرم پس از موفقیت.

---

### تسک ۱۰: بازطراحی تب‌ها و اطلاعات مودال سوابق
- **فایل‌ها:**
  - ویرایش: `src/main/resources/static/index.html`
  - ویرایش: `src/main/resources/static/js/vehicle-history.js`
- **اقدامات:**
  - تب‌های جدید مودال سوابق:
    1. **خلاصه:** نمایش وضعیت، کیلومتر، کل سفرهای انجام‌شده، کل هزینه‌های بنزین و خدمات، و هشدارهای انقضای مدارک.
    2. **مأموریت‌ها:** فهرست سفرها همراه با راننده و گزارش‌های ثبت‌شده هر سفر + دکمه افزودن گزارش جدید به مأموریت گذشته.
    3. **خدمات و بنزین:** تایم‌لاین کارواش، تعویض روغن، مکمل/شیشه‌شور و بنزین با قابلیت مشاهده تصاویر رسیدها.
    4. **تصادفات:** تاریخچه سوانح با جزئیات خسارت و مشاهده عکس‌ها.
    5. **خلافی‌ها:** لیست جریمه‌ها با وضعیت پرداخت و تسویه.
    6. **مدارک و بیمه:** مدیریت بیمه ثالث، بدنه و معاینه فنی با شمارش روزهای مانده تا انقضا.
