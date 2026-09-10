(() => {
  const $ = (sel) => document.querySelector(sel);

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
      .map(function (ch) {
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
    s = s.replace(/ايران/g, "ایران").replace(/ايرآن/g, "ایران");
    let m = s.match(/^(\d{2})\s+ایران\s+(\d{3})\s+(\S+)\s+(\d{2})$/);
    if (m) return { city: m[1], mid: m[2], letter: m[3], first: m[4] };
    const compact = s.replace(/\s+/g, "");
    m = compact.match(/^(\d{2})(.+?)(\d{3})ایران(\d{2})$/);
    if (m) return { first: m[1], letter: m[2], mid: m[3], city: m[4] };
    m = s.match(/^(\d{2})\s+(\S+)\s+(\d{3})\s+ایران\s+(\d{2})$/);
    if (m) return { first: m[1], letter: m[2], mid: m[3], city: m[4] };
    return null;
  }

  function renderIranPlate(plate) {
    const p = parsePlate(plate);
    if (!p) {
      return '<span class="iran-plate-fallback" dir="ltr">' + escapeHtml(plate || "—") + "</span>";
    }
    return (
      '<span class="iran-plate" dir="ltr" title="' + escapeHtml(plate) + '">' +
      '<span class="iran-plate-blue">' +
      '<span class="iran-plate-flag" aria-hidden="true"></span>' +
      '<span class="iran-plate-ir">I.R.</span>' +
      '<span class="iran-plate-ir">IRAN</span></span>' +
      '<span class="iran-plate-main">' +
      '<span class="iran-plate-num">' + toPersianDigits(p.first) + "</span>" +
      '<span class="iran-plate-letter">' + escapeHtml(p.letter) + "</span>" +
      '<span class="iran-plate-num">' + toPersianDigits(p.mid) + "</span></span>" +
      '<span class="iran-plate-side">' +
      '<span class="iran-plate-iran">ایران</span>' +
      '<span class="iran-plate-city">' + toPersianDigits(p.city) + "</span></span></span>"
    );
  }

  function jalaliDaysInMonth(jy, jm) {
    if (jm <= 6) return 31;
    if (jm <= 11) return 30;
    const leaps = [1, 5, 9, 13, 17, 22, 26, 30];
    return leaps.indexOf(jy % 33) >= 0 ? 30 : 29;
  }

  function syncJalaliHidden(root) {
    const ySel = root.querySelector('[data-part="year"]');
    const mSel = root.querySelector('[data-part="month"]');
    const dSel = root.querySelector('[data-part="day"]');
    if (!ySel || !mSel || !dSel) return;
    const targetId = root.getAttribute("data-jalali-target");
    const hidden = targetId ? document.getElementById(targetId) : null;
    const y = ySel.value;
    const m = mSel.value;
    const d = dSel.value;
    if (hidden) {
      hidden.value = y && m && d ? y + "/" + m + "/" + d : "";
    }
    if (y && m) {
      const maxD = jalaliDaysInMonth(Number(y), Number(m));
      Array.from(dSel.options).forEach(function (opt) {
        if (!opt.value) return;
        opt.disabled = Number(opt.value) > maxD;
      });
      if (d && Number(d) > maxD) {
        dSel.value = maxD < 10 ? "0" + maxD : String(maxD);
        if (hidden) hidden.value = y + "/" + m + "/" + dSel.value;
      }
    }
  }

  function wireJalali(root) {
    root.querySelectorAll("select").forEach(function (sel) {
      sel.addEventListener("change", function () {
        syncJalaliHidden(root);
      });
    });
    syncJalaliHidden(root);
  }

  function clearJalali(root) {
    root.querySelectorAll("select").forEach(function (s) {
      s.value = "";
    });
    const targetId = root.getAttribute("data-jalali-target");
    const hidden = targetId ? document.getElementById(targetId) : null;
    if (hidden) hidden.value = "";
  }

  function getFilters() {
    return {
      employeeName: ($("#filterEmployeeName") && $("#filterEmployeeName").value) || "",
      carName: ($("#filterCarName") && $("#filterCarName").value) || "",
      plate: ($("#filterPlate") && $("#filterPlate").value) || "",
      destination: ($("#filterDestination") && $("#filterDestination").value) || "",
      status: ($("#filterStatus") && $("#filterStatus").value) || "ALL",
      dateFrom: ($("#filterDateFrom") && $("#filterDateFrom").value) || "",
      dateTo: ($("#filterDateTo") && $("#filterDateTo").value) || "",
    };
  }

  function describeFilters(f) {
    const parts = [];
    if (f.employeeName) parts.push("کارمند: " + f.employeeName);
    if (f.carName) parts.push("ماشین: " + f.carName);
    if (f.plate) parts.push("پلاک: " + f.plate);
    if (f.destination) parts.push("مقصد: " + f.destination);
    if (f.status && f.status !== "ALL") {
      parts.push(f.status === "OPEN" ? "وضعیت: باز" : "وضعیت: بسته");
    }
    if (f.dateFrom) parts.push("از: " + f.dateFrom);
    if (f.dateTo) parts.push("تا: " + f.dateTo);
    return parts.length ? "فیلتر فعال: " + parts.join(" · ") : "بدون فیلتر — همهٔ سفرها";
  }

  async function loadReportFiltered() {
    const tbody = $("#reportTable");
    const summary = $("#reportFilterSummary");
    if (!tbody) return;
    document.querySelectorAll(".jalali-date-parts").forEach(syncJalaliHidden);
    const filters = getFilters();
    if (summary) summary.textContent = describeFilters(filters);
    tbody.innerHTML = '<tr><td colspan="7">در حال بارگذاری…</td></tr>';
    try {
      if (typeof Api === "undefined" || !Api.report) {
        throw new Error("API در دسترس نیست");
      }
      const rows = await Api.report(filters);
      if (!rows || !rows.length) {
        tbody.innerHTML = '<tr><td colspan="7">موردی با این فیلتر یافت نشد</td></tr>';
        return;
      }
      tbody.innerHTML = rows
        .map(function (r) {
          const open =
            r.returnDate == null ||
            r.returnDate === "" ||
            r.returnDate === "منتظر برگشت";
          const ret = open
            ? '<span class="badge badge-busy">باز</span>'
            : '<span dir="ltr">' + escapeHtml(r.returnDate) + "</span>";
          return (
            "<tr>" +
            '<td dir="ltr">' +
            escapeHtml(r.deviceUserId || "—") +
            "</td><td>" +
            escapeHtml(r.employeeName || "—") +
            "</td><td>" +
            escapeHtml(r.carName || "—") +
            (r.carColor
              ? ' <span class="muted small">(' + escapeHtml(r.carColor) + ")</span>"
              : "") +
            "</td><td>" +
            renderIranPlate(r.plate) +
            "</td><td>" +
            escapeHtml(r.destination || "—") +
            '</td><td dir="ltr" class="small">' +
            escapeHtml(r.pickupDate || "—") +
            '</td><td class="small">' +
            ret +
            "</td></tr>"
          );
        })
        .join("");
    } catch (err) {
      tbody.innerHTML =
        '<tr><td colspan="7">' + escapeHtml(err.message || err) + "</td></tr>";
    }
  }

  function init() {
    document.querySelectorAll(".jalali-date-parts").forEach(wireJalali);

    const form = $("#formReportFilter");
    if (form) {
      form.addEventListener("submit", function (e) {
        e.preventDefault();
        e.stopPropagation();
        loadReportFiltered();
        return false;
      });
    }

    const btnApply = $("#btnApplyReportFilter");
    if (btnApply) {
      btnApply.addEventListener("click", function (e) {
        e.preventDefault();
        e.stopPropagation();
        loadReportFiltered();
      });
    }

    const btnClear = $("#btnClearReportFilter");
    if (btnClear) {
      btnClear.addEventListener("click", function (e) {
        e.preventDefault();
        e.stopPropagation();
        if (form) form.reset();
        document.querySelectorAll(".jalali-date-parts").forEach(clearJalali);
        if ($("#filterStatus")) $("#filterStatus").value = "ALL";
        loadReportFiltered();
      });
    }

    const nav = $("#nav");
    if (nav) {
      nav.addEventListener("click", function (e) {
        const btn = e.target.closest('.nav-item[data-view="report"]');
        if (!btn) return;
        setTimeout(function () {
          document.querySelectorAll(".jalali-date-parts").forEach(syncJalaliHidden);
        }, 0);
      });
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
