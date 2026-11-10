(() => {
  const $ = (s) => document.querySelector(s);
  const escapeHtml = (v) => String(v == null ? "" : v).replace(/[&<>"']/g, (c) => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;", "'":"&#39;"}[c]));
  function formatDetails(jsonStr) {
    if (!jsonStr || jsonStr === "{}") return '<span class="muted">—</span>';
    try {
      const obj = JSON.parse(jsonStr);
      const pretty = JSON.stringify(obj, null, 2);
      const short = jsonStr.length > 40 ? jsonStr.substring(0, 40) + "…" : jsonStr;
      return '<details class="details-toggle">'
        + '<summary class="details-short" title="کلیک برای نمایش/بستن جزئیات">' + escapeHtml(short) + '</summary>'
        + '<pre class="details-full" dir="ltr">' + escapeHtml(pretty) + '</pre>'
        + '</details>';
    } catch (e) {
      return '<span dir="ltr">' + escapeHtml(jsonStr) + '</span>';
    }
  }
  const toLatin = (v) => String(v || "").replace(/[۰-۹٠-٩]/g, (d) => {
    const fa="۰۱۲۳۴۵۶۷۸۹", ar="٠١٢٣٤٥٦٧٨٩"; const i=fa.indexOf(d); return String(i>=0?i:ar.indexOf(d));
  }).replace(/-/g,"/").trim();
  function toPersianDigits(s) {
    const map = "۰۱۲۳۴۵۶۷۸۹";
    return String(s == null ? "" : s).replace(/[0-9]/g, (d) => map[d.charCodeAt(0) - 48]);
  }

  function isMotorcycleType(t) {
    return String(t || "").toUpperCase() === "MOTORCYCLE";
  }

  function parsePlate(plate) {
    if (plate == null) return null;
    let s = toLatin(String(plate)).trim();
    if (!s) return null;
    s = s.replace(/ايران/g, "ایران").replace(/ايرآن/g, "ایران");
    let m = s.match(/^(\d{2})\s+ایران\s+(\d{3})\s+(\S+)\s+(\d{2})$/);
    if (m) return { city: m[1], mid: m[2], letter: m[3], first: m[4] };
    const compact = s.replace(/\s+/g, "");
    m = compact.match(/^(\d{2})(.+?)(\d{3})ایران(\d{2})$/);
    if (m) return { first: m[1], letter: m[2], mid: m[3], city: m[4] };
    m = compact.match(/^(\d{2})([A-Za-z\u0600-\u06FF]+)(\d{3})(\d{2})$/);
    if (m) return { first: m[1], letter: m[2], mid: m[3], city: m[4] };
    m = s.match(/^(\d{2})\s+(\S+)\s+(\d{3})\s+ایران\s+(\d{2})$/);
    if (m) return { first: m[1], letter: m[2], mid: m[3], city: m[4] };
    return null;
  }

  function parseMotorcyclePlate(plate) {
    if (plate == null) return null;
    const digits = toLatin(String(plate)).replace(/\D/g, "");
    if (digits.length !== 8) return null;
    return { top: digits.slice(0, 3), bottom: digits.slice(3) };
  }

  function renderIranPlate(plate) {
    const p = parsePlate(plate);
    if (!p) {
      return '<span class="iran-plate-fallback" dir="ltr">' + escapeHtml(plate || "—") + "</span>";
    }
    return (
      '<span class="iran-plate" dir="ltr" title="' +
      escapeHtml(plate) +
      '">' +
      '<span class="iran-plate-blue">' +
      '<span class="iran-plate-flag" aria-hidden="true"></span>' +
      '<span class="iran-plate-ir">I.R.</span>' +
      '<span class="iran-plate-ir">IRAN</span>' +
      "</span>" +
      '<span class="iran-plate-main">' +
      '<span class="iran-plate-num">' +
      escapeHtml(toPersianDigits(p.first)) +
      "</span>" +
      '<span class="iran-plate-letter">' +
      escapeHtml(p.letter) +
      "</span>" +
      '<span class="iran-plate-num">' +
      escapeHtml(toPersianDigits(p.mid)) +
      "</span>" +
      "</span>" +
      '<span class="iran-plate-side">' +
      '<span class="iran-plate-iran">ایران</span>' +
      '<span class="iran-plate-city">' +
      escapeHtml(toPersianDigits(p.city)) +
      "</span>" +
      "</span>" +
      "</span>"
    );
  }

  function renderMotorcyclePlate(plate) {
    const p = parseMotorcyclePlate(plate);
    if (!p) {
      return '<span class="iran-plate-fallback" dir="ltr">' + escapeHtml(plate || "—") + "</span>";
    }
    return (
      '<span class="moto-plate" dir="ltr" title="' +
      escapeHtml(plate) +
      '">' +
      '<span class="moto-plate-blue">' +
      '<span class="iran-plate-flag" aria-hidden="true"></span>' +
      '<span class="iran-plate-ir">I.R.</span>' +
      '<span class="iran-plate-ir">IRAN</span>' +
      "</span>" +
      '<span class="moto-plate-nums">' +
      '<span class="moto-plate-top">' +
      escapeHtml(toPersianDigits(p.top)) +
      "</span>" +
      '<span class="moto-plate-bottom">' +
      escapeHtml(toPersianDigits(p.bottom)) +
      "</span>" +
      "</span>" +
      "</span>"
    );
  }

  function renderPlate(plate, vehicleType) {
    if (isMotorcycleType(vehicleType)) {
      return renderMotorcyclePlate(plate);
    }
    const car = parsePlate(plate);
    if (car) return renderIranPlate(plate);
    if (parseMotorcyclePlate(plate)) return renderMotorcyclePlate(plate);
    return renderIranPlate(plate);
  }

  function renderEntity(entityType, entityId) {
    if (!entityType && !entityId) return '<span class="muted">—</span>';
    const isPlate = entityType === "وسیله" || parsePlate(entityId) || parseMotorcyclePlate(entityId);
    if (isPlate && entityId) {
      return `<div class="entity-plate-container"><span class="entity-badge">${escapeHtml(entityType || "وسیله")}:</span> ${renderPlate(entityId)}</div>`;
    }
    return escapeHtml((entityType || "") + (entityId ? " · " + entityId : ""));
  }

  let page = 0, total = 0, busy = false;

  function filters(p = page) {
    return { dateFrom:toLatin($("#eventDateFrom").value), dateTo:toLatin($("#eventDateTo").value),
      category:$("#eventCategory").value, type:$("#eventType").value, result:$("#eventResult").value,
      actor:$("#eventActor").value, q:$("#eventSearch").value, page: p, size:50 };
  }
  function render(items, append) {
    const tbody = $("#systemEventTable");
    if (!append) tbody.innerHTML = "";
    const resultBadge = (r) => r === "موفق"
      ? '<span class="badge badge-free">موفق</span>'
      : r === "ناموفق"
        ? '<span class="badge badge-busy">ناموفق</span>'
        : escapeHtml(r);
    const rows = (items || []).map(e => `<tr>
      <td dir="ltr">${escapeHtml(e.occurredAt || "—")}</td>
      <td>${escapeHtml(e.category)}<br><span class="muted small">${escapeHtml(e.eventType)}</span></td>
      <td>${resultBadge(e.result)}</td>
      <td><div class="event-desc-cell">${escapeHtml(e.description)}</div></td>
      <td>${escapeHtml(e.actor || "—")}</td>
      <td>${renderEntity(e.entityType, e.entityId)}</td>
      <td dir="ltr">${formatDetails(e.detailsJson)}</td>
    </tr>`).join("");
    if (append) tbody.insertAdjacentHTML("beforeend", rows);
    else tbody.innerHTML = rows || '<tr><td colspan="7">رویدادی با این فیلتر یافت نشد.</td></tr>';
  }
  async function loadPendingDeviceOperations() {
    const section = $("#pendingDeviceOperationsSection");
    const body = $("#pendingDeviceOperationsTable"), message = $("#pendingDeviceOperationsMessage");
    if (!body || !message) return;
    try {
      const data = await Api.pendingDeviceOperations();
      const items = data.items || [];
      if (items.length === 0) {
        if (section) section.classList.add("hidden");
        return;
      }
      if (section) section.classList.remove("hidden");
      message.textContent = "این عملیات ناتمام پس از راه‌اندازی مجدد هم باقی می‌مانند؛ پیش از تکرار، وضعیت دستگاه را بررسی کنید.";
      const devResult = (o) => o.deviceSucceeded == null
        ? '<span class="badge badge-muted">نامشخص</span>'
        : o.deviceSucceeded
          ? '<span class="badge badge-free">موفق</span>'
          : '<span class="badge badge-busy">ناموفق</span>';

      body.innerHTML = items.map(o => `<tr>
        <td dir="ltr">${escapeHtml(o.operationId)}</td>
        <td>${escapeHtml(o.operationType)}</td>
        <td>${renderEntity(o.entityType, o.entityId)}</td>
        <td>${escapeHtml(o.state)}</td>
        <td>${devResult(o)}</td>
        <td>${escapeHtml(o.note || "—")}</td>
        <td dir="ltr">${formatDetails(o.detailsJson)}</td>
        <td dir="ltr">${escapeHtml(o.updatedAt || "—")}</td>
      </tr>`).join("");
    } catch(e) {
      if (section) section.classList.remove("hidden");
      body.innerHTML = '<tr><td colspan="8">دریافت دفتر عملیات دستگاه انجام نشد.</td></tr>';
      message.textContent = e.message || "خطا در دریافت دفتر عملیات دستگاه";
    }
  }
  async function load(append=false) {
    if (busy) return; busy=true;
    const targetPage = append ? page + 1 : 0;
    const spinner = $("#systemEventSpinner");
    if (spinner) spinner.classList.remove("hidden");
    const message=$("#systemEventMessage"); message.textContent="در حال بارگذاری رویدادها…";
    if(!append) { $("#systemEventTable").innerHTML='<tr><td colspan="7">در حال بارگذاری…</td></tr>'; }
    try {
      const data=await Api.systemEvents(filters(targetPage));
      page = targetPage;
      total=data.total||0;
      render(data.items,append);
      const count=(page||0)*data.pageSize+(data.items||[]).length;
      message.textContent=total?"تعداد رویدادهای منطبق: "+total:"رویدادی با این فیلتر یافت نشد.";
      $("#systemEventPageInfo").textContent=count+" از "+total;
      $("#btnOlderSystemEvents").disabled=count>=total;
      const btnTop = $("#btnBackToTopSystemEvents");
      if (btnTop) btnTop.disabled = page === 0;
    } catch(e) {
      if(!append) $("#systemEventTable").innerHTML='<tr><td colspan="7">دریافت گزارش سیستم ناموفق بود.</td></tr>';
      message.textContent=e.status===401||e.status===403?"برای مشاهدهٔ گزارش سیستم باید با حساب مدیر وارد شوید.":(e.message||"خطا در دریافت گزارش سیستم");
      $("#systemEventPageInfo").textContent="";
      const btnTop = $("#btnBackToTopSystemEvents");
      if (btnTop) btnTop.disabled = page === 0;
    } finally {
      if (spinner) spinner.classList.add("hidden");
      busy=false;
    }
  }
  window.loadSystemEvents = () => { load(false); loadPendingDeviceOperations(); };
  document.addEventListener("DOMContentLoaded",()=>{
    const form=$("#systemEventFilters"); if(!form)return;
    form.addEventListener("submit",e=>{e.preventDefault();load(false);});
    $("#btnRefreshSystemEvents").addEventListener("click", () => { load(false); loadPendingDeviceOperations(); });
    $("#btnClearSystemEvents").addEventListener("click",()=>{form.reset();load(false);});
    $("#btnOlderSystemEvents").addEventListener("click",()=>load(true));
    const btnTop = $("#btnBackToTopSystemEvents");
    if (btnTop) btnTop.addEventListener("click", () => {
      load(false);
      document.getElementById("view-system-report")?.scrollIntoView({ behavior: "smooth" });
    });
  });
})();
