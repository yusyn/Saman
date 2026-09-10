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

  function toLatinDigits(s) {
    const persian = "۰۱۲۳۴۵۶۷۸۹";
    const arabic = "٠١٢٣٤٥٦٧٨٩";
    return String(s == null ? "" : s)
      .split("")
      .map(function (ch) {
        const pi = persian.indexOf(ch);
        if (pi >= 0) return String(pi);
        const ai = arabic.indexOf(ch);
        if (ai >= 0) return String(ai);
        return ch;
      })
      .join("");
  }

  /**
   * Supports both storage formats:
   * - Swing: "11 ایران 345 ب 12"  (city ایران mid letter first)
   * - Web:   "12ب345ایران11"      (first letter mid ایران city)
   */
  function parsePlate(plate) {
    if (plate == null) return null;
    let s = toLatinDigits(String(plate)).trim();
    if (!s) return null;
    s = s.replace(/ايران/g, "ایران").replace(/ايرآن/g, "ایران");

    let m = s.match(/^(\d{2})\s+ایران\s+(\d{3})\s+(\S+)\s+(\d{2})$/);
    if (m) {
      return { city: m[1], mid: m[2], letter: m[3], first: m[4] };
    }

    const compact = s.replace(/\s+/g, "");
    m = compact.match(/^(\d{2})(.+?)(\d{3})ایران(\d{2})$/);
    if (m) {
      return { first: m[1], letter: m[2], mid: m[3], city: m[4] };
    }

    m = s.match(/^(\d{2})\s+(\S+)\s+(\d{3})\s+ایران\s+(\d{2})$/);
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
      '" role="img" aria-label="' +
      escapeHtml(plate) +
      '">' +
      '<span class="iran-plate-blue">' +
      '<span class="iran-plate-flag" aria-hidden="true"></span>' +
      '<span class="iran-plate-ir">I.R.</span>' +
      '<span class="iran-plate-ir">IRAN</span>' +
      "</span>" +
      '<span class="iran-plate-main">' +
      '<span class="iran-plate-num">' +
      toPersianDigits(p.first) +
      "</span>" +
      '<span class="iran-plate-letter">' +
      escapeHtml(p.letter) +
      "</span>" +
      '<span class="iran-plate-num">' +
      toPersianDigits(p.mid) +
      "</span>" +
      "</span>" +
      '<span class="iran-plate-side">' +
      '<span class="iran-plate-iran">ایران</span>' +
      '<span class="iran-plate-city">' +
      toPersianDigits(p.city) +
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
    const cls = kind === "busy"
      ? "badge badge-busy"
      : kind === "free"
        ? "badge badge-free"
        : "badge badge-muted";
    return '<span class="' + cls + '">' + escapeHtml(label) + "</span>";
  }

  function carStatusBadge(status) {
    if (!status) return statusBadge("unknown", "—");
    if (isOnMissionStatus(status)) return statusBadge("busy", status);
    return statusBadge("free", status);
  }

  function employeeRentingBadge(renting) {
    return renting ? statusBadge("busy", "در مأموریت") : statusBadge("free", "آزاد");
  }

  function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove("hidden");
  }

  function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add("hidden");
  }

  document.querySelectorAll("[data-close]").forEach((el) => {
    el.addEventListener("click", () => closeModal(el.getAttribute("data-close")));
  });

  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      closeModal("modalCar");
      closeModal("modalEmployee");
    }
  });

  function showView(name) {
    document.querySelectorAll(".view").forEach((v) => v.classList.remove("active"));
    document.querySelectorAll(".nav-item").forEach((b) => b.classList.remove("active"));
    const view = document.getElementById("view-" + name);
    const btn = document.querySelector('.nav-item[data-view="' + name + '"]');
    if (view) view.classList.add("active");
    if (btn) btn.classList.add("active");
    const t = titles[name] || [name, ""];
    $("#pageTitle").textContent = t[0];
    $("#pageHint").textContent = t[1];
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

  async function loadHealth() {
    const badge = $("#healthBadge");
    const pre = $("#healthJson");
    if (badge) {
      badge.textContent = "در حال بررسی…";
      badge.classList.remove("up", "down");
    }
    if (pre) pre.textContent = "…";
    try {
      const h = await Api.health();
      if (pre) pre.textContent = JSON.stringify(h, null, 2);
      if (badge) {
        const up = h && h.status === "UP";
        badge.textContent = up ? "API: فعال" : "API: " + ((h && h.status) || "نامشخص");
        badge.classList.toggle("up", !!up);
        badge.classList.toggle("down", !up);
      }
    } catch (err) {
      if (pre) pre.textContent = String(err.message || err);
      if (badge) {
        badge.textContent = "API: قطع";
        badge.classList.add("down");
        badge.classList.remove("up");
      }
    }
  }

  $("#btnRefreshHealth").addEventListener("click", loadHealth);

