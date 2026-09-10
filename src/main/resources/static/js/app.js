(() => {
  const titles = {
    dashboard: ["وضعیت", "سلامت API و راهنما"],
    cars: ["ماشین‌ها", "لیست، ثبت، ویرایش و حذف"],
    employees: ["کارمندان", "لیست، ثبت، ویرایش و حذف"],
    rentals: ["تحویل / برگشت", "احراز جدا برای هر عملیات"],
    report: ["گزارش", "سفرهای ثبت‌شده"],
  };

  const $ = (sel) => document.querySelector(sel);
  const toastEl = $("#toast");

  let carsCache = [];
  let employeesCache = [];
  let pickupAuth = null;
  let returnAuth = null;

  function toast(message, type = "ok") {
    toastEl.textContent = message;
    toastEl.classList.remove("hidden", "ok", "err");
    toastEl.classList.add(type === "err" ? "err" : "ok");
    clearTimeout(toastEl._t);
    toastEl._t = setTimeout(() => toastEl.classList.add("hidden"), 4500);
  }

  function escapeHtml(s) {
    const amp = String.fromCharCode(38) + "amp;";
    const lt = String.fromCharCode(38) + "lt;";
    const gt = String.fromCharCode(38) + "gt;";
    const quot = String.fromCharCode(38) + "quot;";
    return String(s == null ? "" : s)
      .split(String.fromCharCode(38)).join(amp)
      .split(String.fromCharCode(60)).join(lt)
      .split(String.fromCharCode(62)).join(gt)
      .split(String.fromCharCode(34)).join(quot);
  }

  function toPersianDigits(s) {
    const map = "۰۱۲۳۴۵۶۷۸۹";
    return String(s == null ? "" : s).replace(/[0-9]/g, (d) => map[d.charCodeAt(0) - 48]);
  }

  // PLACEHOLDER_WILL_BE_REPLACED - this is truncated intentionally if tool rejects large payloads
  console.log("REPORT_FILTERS_BOOT");
})();
