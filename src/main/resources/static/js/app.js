(() => {
  const titles = {
    dashboard: ["وضعیت", "سلامت API و راهنما"],
    cars: ["ماشین‌ها", "ثبت و مدیریت ناوگان"],
    employees: ["کارمندان", "ثبت کارمند و اثر انگشت"],
    rentals: ["تحویل / برگشت", "احراز اثرانگشت برای تحویل و برگشت"],
    report: ["گزارش", "سفرها و فیلترها"],
  };

  const $ = (sel, el = document) => el.querySelector(sel);
  const $$ = (sel, el = document) => Array.from(el.querySelectorAll(sel));

  let carsCache = [];
  let employeesCache = [];
  let pickupAuth = null;
  let returnAuth = null;
  let pickupVerifyAbort = null;
  let returnVerifyAbort = null;

  function toast(msg, type = "ok") {
    const t = $("#toast");
    if (!t) return;
    t.textContent = msg;
    t.classList.remove("hidden", "ok", "err");
    t.classList.add(type === "err" ? "err" : "ok");
    clearTimeout(toast._timer);
    toast._timer = setTimeout(() => t.classList.add("hidden"), 4200);
  }

  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&")
      .replace(/</g, "<")
      .replace(/>/g, ">")
      .replace(/"/g, """);
  }

  function toPersianDigits(s) {
    const map = "۰۱۲۳۴۵۶۷۸۹";
    return String(s == null ? "" : s).replace(/[0-9]/g, (d) => map[d.charCodeAt(0) - 48]);
  }

  function toLatinDigits(s) {
    const persian = "۰۱۲۳۴۵۶۷۸۹";
    const arabic = "٠١٢٣٤٥٦٧٨٩";
    return String(s == null ? "" : s)
      .split("")
      .map((ch) => {
        const pi = persian.indexOf(ch);
        if (pi >= 0) return String(pi);
        const ai = arabic.indexOf(ch);
        if (ai >= 0) return String(ai);
        return ch;
      })
      .join("");
  }

  function parsePlate(plate) {
    if (plate == null) return null;
    let s = toLatinDigits(String(plate)).trim();
    if (!s) return null;
    s = s.replace(/\s+/g, "");
    let m = s.match(/^(\d{2})([\u0600-\u06FF]+)(\d{3})ایران(\d{2})$/);
    if (m) {
      return { city: m[4], mid: m[3], letter: m[2], first: m[1] };
    }
    m = s.match(/^(\d{2})([A-Za-z\u0600-\u06FF]+)(\d{3})(?:ایران)?(\d{2})$/);
    if (m) {
      return { first: m[1], letter: m[2], mid: m[3], city: m[4] };
    }
    m = s.match(/^(\d{2})[-\/]?([A-Za-z\u0600-\u06FF]+)[-\/]?(\d{3})[-\/]?(\d{2})$/);
    if (m) {
      return { first: m[1], letter: m[2], mid: m[3], city: m[4] };
    }
    return null;
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

  function isOnMissionStatus(status) {
    if (!status) return false;
    return status.includes("مأموریت") || status.includes("ماموریت");
  }

  function statusBadge(kind, label) {
    const cls =
      kind === "free" ? "badge badge-free" : kind === "busy" ? "badge badge-busy" : "badge badge-muted";
    return '<span class="' + cls + '">' + escapeHtml(label) + "</span>";
  }

  function carStatusBadge(status) {
    if (!status) return statusBadge("unknown", "—");
    if (isOnMissionStatus(status)) return statusBadge("busy", status);
    if (status.includes("آزاد") || status.toLowerCase().includes("free")) return statusBadge("free", status);
    return statusBadge("muted", status);
  }

  function employeeRentingBadge(renting) {
    return renting ? statusBadge("busy", "در مأموریت") : statusBadge("free", "آزاد");
  }

  function showView(name) {
    $$(".view").forEach((v) => v.classList.remove("active"));
    const view = $("#view-" + name);
    if (view) view.classList.add("active");
    $$(".nav-item").forEach((b) => b.classList.toggle("active", b.dataset.view === name));
    const t = titles[name] || [name, ""];
    const pageTitle = $("#pageTitle");
    const pageHint = $("#pageHint");
    if (pageTitle) pageTitle.textContent = t[0];
    if (pageHint) pageHint.textContent = t[1];
    if (name === "cars") loadCars();
    if (name === "employees") loadEmployees();
    if (name === "rentals") loadAvailablePlates();
    if (name === "report") loadReport();
    if (name === "dashboard") loadHealth();
  }

  document.getElementById("nav").addEventListener("click", (e) => {
    const btn = e.target.closest(".nav-item");
    if (!btn) return;
    showView(btn.dataset.view);
  });

  // PLACEHOLDER_REST_OF_APP
})();
