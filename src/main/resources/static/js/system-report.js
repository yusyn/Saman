(() => {
  const $ = (s) => document.querySelector(s);
  const escapeHtml = (v) => String(v == null ? "" : v).replace(/[&<>"']/g, (c) => ({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;", "'":"&#39;"}[c]));
  function formatDetails(jsonStr) {
    if (!jsonStr || jsonStr === "{}") return '<span class="muted">—</span>';
    try {
      const obj = JSON.parse(jsonStr);
      const pretty = JSON.stringify(obj, null, 2);
      const short = jsonStr.length > 40 ? jsonStr.substring(0, 40) + "…" : jsonStr;
      return '<span class="details-cell" tabindex="0" title="کلیک برای نمایش جزئیات">'
        + '<span class="details-short">' + escapeHtml(short) + '</span>'
        + '<pre class="details-full hidden" dir="ltr">' + escapeHtml(pretty) + '</pre>'
        + '</span>';
    } catch (e) {
      return '<span dir="ltr">' + escapeHtml(jsonStr) + '</span>';
    }
  }
  const toLatin = (v) => String(v || "").replace(/[۰-۹٠-٩]/g, (d) => {
    const fa="۰۱۲۳۴۵۶۷۸۹", ar="٠١٢٣٤٥٦٧٨٩"; const i=fa.indexOf(d); return String(i>=0?i:ar.indexOf(d));
  }).replace(/-/g,"/").trim();
  let page = 0, total = 0, busy = false;

  function filters() {
    return { dateFrom:toLatin($("#eventDateFrom").value), dateTo:toLatin($("#eventDateTo").value),
      category:$("#eventCategory").value, type:$("#eventType").value, result:$("#eventResult").value,
      actor:$("#eventActor").value, q:$("#eventSearch").value, page, size:50 };
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
      <td>${escapeHtml(e.description)}</td>
      <td>${escapeHtml(e.actor || "—")}</td>
      <td>${escapeHtml((e.entityType || "") + (e.entityId ? " · " + e.entityId : ""))}</td>
      <td dir="ltr">${formatDetails(e.detailsJson)}</td>
    </tr>`).join("");
    if (append) tbody.insertAdjacentHTML("beforeend", rows);
    else tbody.innerHTML = rows || '<tr><td colspan="7">رویدادی با این فیلتر یافت نشد.</td></tr>';
  }
  async function loadPendingDeviceOperations() {
    const body = $("#pendingDeviceOperationsTable"), message = $("#pendingDeviceOperationsMessage");
    if (!body || !message) return;
    message.textContent = "در حال بررسی دفتر عملیات دستگاه…";
    try {
      const data = await Api.pendingDeviceOperations();
      const items = data.items || [];
      const devResult = (o) => o.deviceSucceeded == null
        ? '<span class="badge badge-muted">نامشخص</span>'
        : o.deviceSucceeded
          ? '<span class="badge badge-free">موفق</span>'
          : '<span class="badge badge-busy">ناموفق</span>';

      body.innerHTML = items.map(o => `<tr>
        <td dir="ltr">${escapeHtml(o.operationId)}</td>
        <td>${escapeHtml(o.operationType)}</td>
        <td>${escapeHtml((o.entityType || "") + (o.entityId ? " · " + o.entityId : ""))}</td>
        <td>${escapeHtml(o.state)}</td>
        <td>${devResult(o)}</td>
        <td>${escapeHtml(o.note || "—")}</td>
        <td dir="ltr">${formatDetails(o.detailsJson)}</td>
        <td dir="ltr">${escapeHtml(o.updatedAt || "—")}</td>
      </tr>`).join("") || '<tr><td colspan="8">عملیات ناتمامی برای بررسی وجود ندارد.</td></tr>';
      message.textContent = items.length ? "این عملیات ناتمام پس از راه‌اندازی مجدد هم باقی می‌مانند؛ پیش از تکرار، وضعیت دستگاه را بررسی کنید." : "همهٔ عملیات دستگاه تعیین تکلیف شده‌اند.";
    } catch(e) {
      body.innerHTML = '<tr><td colspan="8">دریافت دفتر عملیات دستگاه انجام نشد.</td></tr>';
      message.textContent = e.message || "خطا در دریافت دفتر عملیات دستگاه";
    }
  }
  async function load(append=false) {
    if (busy) return; busy=true;
    const spinner = $("#systemEventSpinner");
    if (spinner) spinner.classList.remove("hidden");
    const message=$("#systemEventMessage"); message.textContent="در حال بارگذاری رویدادها…";
    if(!append) { page=0; $("#systemEventTable").innerHTML='<tr><td colspan="7">در حال بارگذاری…</td></tr>'; }
    try {
      const data=await Api.systemEvents(filters()); total=data.total||0; render(data.items,append);
      const count=(data.page||0)*data.pageSize+(data.items||[]).length;
      message.textContent=total?"تعداد رویدادهای منطبق: "+total:"رویدادی با این فیلتر یافت نشد.";
      $("#systemEventPageInfo").textContent=count+" از "+total;
      $("#btnOlderSystemEvents").disabled=count>=total;
    } catch(e) {
      if(!append) $("#systemEventTable").innerHTML='<tr><td colspan="7">دریافت گزارش سیستم ناموفق بود.</td></tr>';
      message.textContent=e.status===401||e.status===403?"برای مشاهدهٔ گزارش سیستم باید با حساب مدیر وارد شوید.":(e.message||"خطا در دریافت گزارش سیستم");
      $("#systemEventPageInfo").textContent="";
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
    $("#btnOlderSystemEvents").addEventListener("click",()=>{page++;load(true);});
    document.addEventListener("click", function(e) {
      const cell = e.target.closest(".details-cell");
      if (!cell) return;
      const full = cell.querySelector(".details-full");
      const short = cell.querySelector(".details-short");
      if (full) full.classList.toggle("hidden");
      if (short) short.classList.toggle("hidden");
    });
  });
})();
