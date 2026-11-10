(() => {
  const $ = (s) => document.querySelector(s);
  const escapeHtml = (v) => String(v == null ? "" : v).replace(/[&<>"']/g, (c) => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;", "'":"&#39;"}[c]));
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

  const KEY_LABELS = {
    employee: "نام کارمند",
    employeeId: "شناسه کارمند",
    plate: "پلاک وسیله",
    destination: "مقصد مأموریت",
    pickupTime: "زمان تحویل",
    returnTime: "زمان برگشت",
    color: "رنگ",
    type: "نوع وسیله",
    name: "نام",
    error: "خطا",
    reason: "علت",
    note: "یادداشت",
    actor: "عامل",
    new: "مقادیر جدید",
    old: "مقادیر پیشین",
    ip: "نشانی IP",
    status: "وضعیت",
    state: "وضعیت",
    deviceSucceeded: "نتیجه دستگاه",
    operationType: "نوع عملیات",
    operationId: "شناسه عملیات"
  };

  function formatFriendlyVal(k, val) {
    if (val == null) return '<span class="muted">—</span>';
    if (typeof val === "boolean") return val ? "بله" : "خیر";
    if (typeof val === "object" && !Array.isArray(val)) {
      const keys = Object.keys(val);
      if (!keys.length) return '<span class="muted">—</span>';
      const rows = keys.map(subK => {
        const subLabel = KEY_LABELS[subK] || subK;
        return `<tr><td class="details-kv-key">${escapeHtml(subLabel)}</td><td class="details-kv-val">${formatFriendlyVal(subK, val[subK])}</td></tr>`;
      }).join("");
      return `<table class="details-kv-table">${rows}</table>`;
    }
    const str = String(val);
    const lowerK = String(k).toLowerCase();
    if (lowerK.includes("plate") || parsePlate(str) || parseMotorcyclePlate(str)) {
      return renderPlate(str);
    }
    return escapeHtml(str);
  }

  function renderFriendlyEventDetails(evt) {
    const resultBadge = evt.result === "موفق"
      ? '<span class="badge badge-free">موفق</span>'
      : evt.result === "ناموفق"
        ? '<span class="badge badge-busy">ناموفق</span>'
        : escapeHtml(evt.result);

    let detailsHtml = "";
    if (evt.detailsJson && evt.detailsJson !== "{}" && evt.detailsJson !== "null") {
      try {
        const parsed = JSON.parse(evt.detailsJson);
        if (typeof parsed === "object" && parsed !== null) {
          const keys = Object.keys(parsed);
          if (keys.length > 0) {
            const rows = keys.map(k => {
              const label = KEY_LABELS[k] || k;
              return `<tr><td class="details-kv-key">${escapeHtml(label)}</td><td class="details-kv-val">${formatFriendlyVal(k, parsed[k])}</td></tr>`;
            }).join("");
            detailsHtml = `
              <div class="details-section-title">تغییرات و پارامترهای رویداد</div>
              <table class="details-kv-table">${rows}</table>
            `;
          }
        }
      } catch(e) {
        detailsHtml = `
          <div class="details-section-title">جزئیات متنی</div>
          <div class="details-kv-val" dir="ltr">${escapeHtml(evt.detailsJson)}</div>
        `;
      }
    }

    return `
      <div class="details-meta-grid">
        <div class="details-meta-item"><span class="details-meta-label">زمان ثبت:</span><span class="details-meta-val" dir="ltr">${escapeHtml(evt.occurredAt || "—")}</span></div>
        <div class="details-meta-item"><span class="details-meta-label">نتیجه:</span><span class="details-meta-val">${resultBadge}</span></div>
        <div class="details-meta-item"><span class="details-meta-label">دسته / نوع:</span><span class="details-meta-val">${escapeHtml(evt.category)} · ${escapeHtml(evt.eventType)}</span></div>
        <div class="details-meta-item"><span class="details-meta-label">عامل / کاربر:</span><span class="details-meta-val">${escapeHtml(evt.actor || "—")}</span></div>
        <div class="details-meta-item" style="grid-column: 1 / -1;"><span class="details-meta-label">موجودیت مرتبط:</span><span class="details-meta-val">${renderEntity(evt.entityType, evt.entityId)}</span></div>
        <div class="details-meta-item" style="grid-column: 1 / -1;"><span class="details-meta-label">شرح رویداد:</span><span class="details-meta-val" style="line-height:1.6;">${escapeHtml(evt.description || "—")}</span></div>
      </div>
      ${detailsHtml}
    `;
  }

  function renderFriendlyDevDetails(op) {
    const devResult = op.deviceSucceeded == null
      ? '<span class="badge badge-muted">نامشخص</span>'
      : op.deviceSucceeded
        ? '<span class="badge badge-free">موفق</span>'
        : '<span class="badge badge-busy">ناموفق</span>';

    let detailsHtml = "";
    if (op.detailsJson && op.detailsJson !== "{}" && op.detailsJson !== "null") {
      try {
        const parsed = JSON.parse(op.detailsJson);
        if (typeof parsed === "object" && parsed !== null) {
          const keys = Object.keys(parsed);
          if (keys.length > 0) {
            const rows = keys.map(k => {
              const label = KEY_LABELS[k] || k;
              return `<tr><td class="details-kv-key">${escapeHtml(label)}</td><td class="details-kv-val">${formatFriendlyVal(k, parsed[k])}</td></tr>`;
            }).join("");
            detailsHtml = `
              <div class="details-section-title">پارامترهای عملیات</div>
              <table class="details-kv-table">${rows}</table>
            `;
          }
        }
      } catch(e) {
        detailsHtml = `
          <div class="details-section-title">جزئیات متنی</div>
          <div class="details-kv-val" dir="ltr">${escapeHtml(op.detailsJson)}</div>
        `;
      }
    }

    return `
      <div class="details-meta-grid">
        <div class="details-meta-item"><span class="details-meta-label">شناسه پیگیری:</span><span class="details-meta-val" dir="ltr">${escapeHtml(op.operationId || "—")}</span></div>
        <div class="details-meta-item"><span class="details-meta-label">نوع عملیات:</span><span class="details-meta-val">${escapeHtml(op.operationType || "—")}</span></div>
        <div class="details-meta-item"><span class="details-meta-label">وضعیت دفتر:</span><span class="details-meta-val">${escapeHtml(op.state || "—")}</span></div>
        <div class="details-meta-item"><span class="details-meta-label">نتیجه دستگاه:</span><span class="details-meta-val">${devResult}</span></div>
        <div class="details-meta-item" style="grid-column: 1 / -1;"><span class="details-meta-label">موجودیت:</span><span class="details-meta-val">${renderEntity(op.entityType, op.entityId)}</span></div>
        <div class="details-meta-item" style="grid-column: 1 / -1;"><span class="details-meta-label">شرح:</span><span class="details-meta-val">${escapeHtml(op.note || "—")}</span></div>
        <div class="details-meta-item" style="grid-column: 1 / -1;"><span class="details-meta-label">آخرین به‌روزرسانی:</span><span class="details-meta-val" dir="ltr">${escapeHtml(op.updatedAt || "—")}</span></div>
      </div>
      ${detailsHtml}
    `;
  }

  function openEventDetailsModal(evt) {
    const modal = $("#modalEventDetails");
    const body = $("#modalEventDetailsBody");
    const title = $("#modalEventDetailsTitle");
    if (!modal || !body) return;
    if (title) title.textContent = "جزئیات رویداد سامانه";
    body.innerHTML = renderFriendlyEventDetails(evt);
    modal.classList.remove("hidden");
  }

  function openDevOperationDetailsModal(op) {
    const modal = $("#modalEventDetails");
    const body = $("#modalEventDetailsBody");
    const title = $("#modalEventDetailsTitle");
    if (!modal || !body) return;
    if (title) title.textContent = "جزئیات عملیات دستگاه";
    body.innerHTML = renderFriendlyDevDetails(op);
    modal.classList.remove("hidden");
  }

  let page = 0, total = 0, busy = false;
  let currentEvents = [];
  let currentOperations = [];

  function filters(p = page) {
    return { dateFrom:toLatin($("#eventDateFrom").value), dateTo:toLatin($("#eventDateTo").value),
      category:$("#eventCategory").value, type:$("#eventType").value, result:$("#eventResult").value,
      actor:$("#eventActor").value, q:$("#eventSearch").value, page: p, size:50 };
  }

  function render(items, append) {
    const tbody = $("#systemEventTable");
    if (!append) {
      tbody.innerHTML = "";
      currentEvents = items ? [...items] : [];
    } else {
      currentEvents = currentEvents.concat(items || []);
    }

    const resultBadge = (r) => r === "موفق"
      ? '<span class="badge badge-free">موفق</span>'
      : r === "ناموفق"
        ? '<span class="badge badge-busy">ناموفق</span>'
        : escapeHtml(r);

    const rows = (items || []).map(e => {
      const hasDetails = (e.detailsJson && e.detailsJson !== "{}" && e.detailsJson !== "null") || e.description || e.actor;
      const detailsBtn = hasDetails
        ? `<button type="button" class="btn btn-sm btn-ghost btn-view-details" data-event-id="${e.id}">مشاهده جزئیات</button>`
        : '<span class="muted">—</span>';

      return `<tr>
        <td dir="ltr">${escapeHtml(e.occurredAt || "—")}</td>
        <td>${escapeHtml(e.category)}<br><span class="muted small">${escapeHtml(e.eventType)}</span></td>
        <td>${resultBadge(e.result)}</td>
        <td><div class="event-desc-cell">${escapeHtml(e.description)}</div></td>
        <td>${escapeHtml(e.actor || "—")}</td>
        <td>${renderEntity(e.entityType, e.entityId)}</td>
        <td style="text-align: center;">${detailsBtn}</td>
      </tr>`;
    }).join("");

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
      currentOperations = items;
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

      body.innerHTML = items.map(o => {
        const hasDevDetails = (o.detailsJson && o.detailsJson !== "{}" && o.detailsJson !== "null") || o.note;
        const devDetailsBtn = hasDevDetails
          ? `<button type="button" class="btn btn-sm btn-ghost btn-view-dev-details" data-op-id="${escapeHtml(o.operationId)}">مشاهده جزئیات</button>`
          : '<span class="muted">—</span>';

        return `<tr>
          <td dir="ltr">${escapeHtml(o.operationId)}</td>
          <td>${escapeHtml(o.operationType)}</td>
          <td>${renderEntity(o.entityType, o.entityId)}</td>
          <td>${escapeHtml(o.state)}</td>
          <td>${devResult(o)}</td>
          <td>${escapeHtml(o.note || "—")}</td>
          <td style="text-align: center;">${devDetailsBtn}</td>
          <td dir="ltr">${escapeHtml(o.updatedAt || "—")}</td>
        </tr>`;
      }).join("");
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

    document.addEventListener("click", (ev) => {
      const btn = ev.target.closest(".btn-view-details");
      if (btn) {
        const id = Number(btn.getAttribute("data-event-id"));
        const evt = currentEvents.find(x => x.id === id);
        if (evt) openEventDetailsModal(evt);
        return;
      }
      const devBtn = ev.target.closest(".btn-view-dev-details");
      if (devBtn) {
        const opId = devBtn.getAttribute("data-op-id");
        const op = currentOperations.find(x => x.operationId === opId);
        if (op) openDevOperationDetailsModal(op);
        return;
      }
      const closeBtn = ev.target.closest('[data-close="modalEventDetails"]');
      if (closeBtn) {
        const m = $("#modalEventDetails");
        if (m) m.classList.add("hidden");
      }
    });

    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") {
        const m = $("#modalEventDetails");
        if (m && !m.classList.contains("hidden")) m.classList.add("hidden");
      }
    });
  });
})();
