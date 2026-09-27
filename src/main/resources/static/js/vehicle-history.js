/**
 * Vehicle history UI (stage 7) — badges, history button, modal.
 * Loaded after app.js; enhances the fleet table without forking the whole app.
 */
(function () {
  const $ = (sel) => document.querySelector(sel);

  const HISTORY_BADGE_META = {
    DAMAGED: { cls: "badge-danger", label: "آسیب", title: "خرابی جدی باز" },
    IN_REPAIR: { cls: "badge-warn", label: "تعمیر", title: "در حال تعمیر" },
    NEEDS_SERVICE: { cls: "badge-service", label: "سرویس", title: "نیاز به سرویس" },
    HAS_FINE: { cls: "badge-fine", label: "خلافی", title: "خلافی پرداخت‌نشده" },
  };

  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
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

  function renderHistoryBadges(flags) {
    if (!flags || !String(flags).trim()) return "";
    return String(flags)
      .split(",")
      .map(function (f) {
        f = f.trim();
        if (!f) return "";
        const meta = HISTORY_BADGE_META[f] || { cls: "badge-muted", label: f, title: f };
        return (
          '<span class="badge ' +
          meta.cls +
          '" title="' +
          escapeHtml(meta.title) +
          '">' +
          escapeHtml(meta.label) +
          "</span>"
        );
      })
      .join(" ");
  }

  function formatMoney(n) {
    const v = Number(n) || 0;
    return toPersianDigits(v.toLocaleString("en-US"));
  }

  function serviceTypeLabel(t) {
    const map = {
      OIL_CHANGE: "تعویض روغن",
      FILTER: "فیلتر",
      TIRE: "لاستیک",
      BATTERY: "باتری",
      BRAKE: "ترمز",
      INSPECTION: "معاینه / بازرسی",
      OTHER: "سایر",
    };
    return map[t] || t || "—";
  }

  function issueSeverityLabel(s) {
    const map = { LOW: "کم", MEDIUM: "متوسط", HIGH: "زیاد", CRITICAL: "بحرانی" };
    return map[s] || s || "—";
  }

  function issueStatusLabel(s) {
    const map = {
      OPEN: "باز",
      IN_PROGRESS: "در حال تعمیر",
      RESOLVED: "رفع شده",
      CLOSED: "بسته",
    };
    return map[s] || s || "—";
  }

  function fineTypeLabel(t) {
    const map = {
      SPEED: "سرعت",
      PARKING: "پارک",
      RED_LIGHT: "چراغ قرمز",
      OTHER: "سایر",
    };
    return map[t] || t || "—";
  }

  function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove("hidden");
  }
  function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add("hidden");
  }

  function toast(message, type) {
    const toastEl = $("#toast");
    if (!toastEl) return;
    toastEl.textContent = message;
    toastEl.classList.remove("hidden", "ok", "err");
    toastEl.classList.add(type === "err" ? "err" : "ok");
    clearTimeout(toastEl._t);
    toastEl._t = setTimeout(() => toastEl.classList.add("hidden"), 4500);
  }

  let historyPlate = null;
  let historyData = null;
  let vehicleFlagsCache = {};

  function switchHistoryTab(tab) {
    document.querySelectorAll(".history-tab").forEach(function (b) {
      b.classList.toggle("active", b.getAttribute("data-htab") === tab);
    });
    document.querySelectorAll(".history-pane").forEach(function (p) {
      p.classList.toggle("active", p.getAttribute("data-hpane") === tab);
    });
  }

  async function openVehicleHistory(plate) {
    historyPlate = plate;
    historyData = null;
    const title = $("#historyTitle");
    if (title) title.textContent = "سابقه وسیله — " + plate;
    const summary = $("#historySummary");
    if (summary) summary.innerHTML = '<p class="muted">در حال بارگذاری…</p>';
    ["#historyRentals", "#historyServicesList", "#historyIssuesList", "#historyFinesList"].forEach(
      function (sel) {
        const el = $(sel);
        if (el) el.innerHTML = "";
      }
    );
    switchHistoryTab("summary");
    openModal("modalHistory");
    try {
      historyData = await Api.vehicleHistory(plate);
      renderHistory(historyData);
      if (historyData.statusBadges) {
        vehicleFlagsCache[plate] = historyData.statusBadges.join(",");
        enhanceCarsTable();
      }
    } catch (err) {
      if (summary)
        summary.innerHTML =
          '<p class="status-line err">' + escapeHtml(err.message || "خطا") + "</p>";
    }
  }

  function renderHistory(h) {
    if (!h) return;
    const badges = (h.statusBadges || []).join(",");
    $("#historySummary").innerHTML =
      '<div class="history-stats">' +
      '<div class="hist-stat"><span>کیلومتر فعلی</span><strong>' +
      toPersianDigits(h.currentOdometer || 0) +
      "</strong></div>" +
      '<div class="hist-stat"><span>تعداد سفر</span><strong>' +
      toPersianDigits(h.totalRentals || 0) +
      "</strong></div>" +
      '<div class="hist-stat"><span>هزینه سرویس</span><strong>' +
      formatMoney(h.totalServiceCost) +
      "</strong></div>" +
      '<div class="hist-stat"><span>خلافی (پرداخت‌نشده)</span><strong>' +
      formatMoney(h.unpaidFineAmount) +
      "</strong></div>" +
      "</div>" +
      '<div class="history-badges-row">' +
      renderHistoryBadges(badges) +
      (badges ? "" : '<span class="muted small">بدون هشدار</span>') +
      "</div>" +
      '<p class="muted small">آخرین سرویس: ' +
      escapeHtml(h.lastServiceDate || "—") +
      (h.lastServiceOdometer != null
        ? " — کیلومتر " + toPersianDigits(h.lastServiceOdometer)
        : "") +
      "</p>";

    const rentals = h.rentals || [];
    $("#historyRentals").innerHTML = rentals.length
      ? '<div class="table-wrap"><table class="table-hist"><thead><tr><th>کارمند</th><th>تحویل</th><th>برگشت</th><th>مقصد</th></tr></thead><tbody>' +
        rentals
          .map(function (r) {
            return (
              "<tr><td>" +
              escapeHtml(r.employeeName) +
              '</td><td dir="ltr">' +
              escapeHtml(r.pickupDate) +
              '</td><td dir="ltr">' +
              escapeHtml(r.returnDate) +
              "</td><td>" +
              escapeHtml(r.destination) +
              "</td></tr>"
            );
          })
          .join("") +
        "</tbody></table></div>"
      : '<p class="muted">سفری ثبت نشده</p>';

    const services = h.services || [];
    $("#historyServicesList").innerHTML = services.length
      ? '<div class="table-wrap"><table class="table-hist"><thead><tr><th>نوع</th><th>تاریخ</th><th>کیلومتر</th><th>هزینه</th><th>توضیح</th></tr></thead><tbody>' +
        services
          .map(function (s) {
            return (
              "<tr><td>" +
              escapeHtml(serviceTypeLabel(s.serviceType)) +
              '</td><td dir="ltr">' +
              escapeHtml(s.serviceDate) +
              '</td><td dir="ltr">' +
              toPersianDigits(s.odometer != null ? s.odometer : "—") +
              "</td><td>" +
              formatMoney(s.cost) +
              "</td><td>" +
              escapeHtml(s.description || "") +
              "</td></tr>"
            );
          })
          .join("") +
        "</tbody></table></div>"
      : '<p class="muted">سرویسی ثبت نشده</p>';

    const issues = h.issues || [];
    $("#historyIssuesList").innerHTML = issues.length
      ? '<div class="table-wrap"><table class="table-hist"><thead><tr><th>عنوان</th><th>شدت</th><th>وضعیت</th><th>تاریخ</th><th>هزینه</th></tr></thead><tbody>' +
        issues
          .map(function (i) {
            return (
              "<tr><td>" +
              escapeHtml(i.title) +
              "</td><td>" +
              escapeHtml(issueSeverityLabel(i.severity)) +
              "</td><td>" +
              escapeHtml(issueStatusLabel(i.status)) +
              '</td><td dir="ltr">' +
              escapeHtml(i.reportedAt) +
              "</td><td>" +
              formatMoney(i.cost) +
              "</td></tr>"
            );
          })
          .join("") +
        "</tbody></table></div>"
      : '<p class="muted">خرابیی ثبت نشده</p>';

    const fines = h.fines || [];
    $("#historyFinesList").innerHTML = fines.length
      ? '<div class="table-wrap"><table class="table-hist"><thead><tr><th>نوع</th><th>مبلغ</th><th>تاریخ</th><th>وضعیت</th><th></th></tr></thead><tbody>' +
        fines
          .map(function (f) {
            const payBtn = f.paid
              ? '<span class="badge badge-free">پرداخت‌شده</span>'
              : '<button type="button" class="btn btn-sm btn-primary" data-pay-fine="' +
                f.id +
                '">پرداخت</button>';
            return (
              "<tr><td>" +
              escapeHtml(fineTypeLabel(f.fineType)) +
              "</td><td>" +
              formatMoney(f.amount) +
              '</td><td dir="ltr">' +
              escapeHtml(f.fineDate) +
              "</td><td>" +
              (f.paid ? "پرداخت‌شده" : "باز") +
              "</td><td>" +
              payBtn +
              "</td></tr>"
            );
          })
          .join("") +
        "</tbody></table></div>"
      : '<p class="muted">خلافیی ثبت نشده</p>';
  }

  /** Inject history button + status badges into fleet table rows. */
  function enhanceCarsTable() {
    const tbody = $("#carsTable");
    if (!tbody) return;
    tbody.querySelectorAll("tr").forEach(function (tr) {
      const actions = tr.querySelector("td.actions");
      if (!actions) return;
      const editBtn = actions.querySelector('button[data-act="edit"]');
      if (!editBtn) return;
      const plate = editBtn.getAttribute("data-plate");
      if (!plate) return;

      if (!actions.querySelector('button[data-act="history"]')) {
        const histBtn = document.createElement("button");
        histBtn.type = "button";
        histBtn.className = "btn btn-ghost btn-sm";
        histBtn.setAttribute("data-act", "history");
        histBtn.setAttribute("data-plate", plate);
        histBtn.textContent = "سابقه";
        actions.insertBefore(histBtn, editBtn);
        actions.insertBefore(document.createTextNode(" "), editBtn);
      }

      const statusTd = tr.children[4];
      if (statusTd) {
        let flags = vehicleFlagsCache[plate];
        if (flags == null && window.__samanVehicleFlags) {
          flags = window.__samanVehicleFlags[plate];
        }
        if (flags) {
          let badgeHost = statusTd.querySelector(".hist-badges");
          if (!badgeHost) {
            badgeHost = document.createElement("span");
            badgeHost.className = "hist-badges";
            statusTd.appendChild(document.createTextNode(" "));
            statusTd.appendChild(badgeHost);
          }
          badgeHost.innerHTML = renderHistoryBadges(flags);
        }
      }
    });
  }

  /** Prefetch statusFlags from vehicles API response if present. */
  async function prefetchFlags() {
    try {
      const cars = await Api.vehicles();
      window.__samanVehicleFlags = {};
      (cars || []).forEach(function (c) {
        if (c.plate && c.statusFlags) {
          window.__samanVehicleFlags[c.plate] = c.statusFlags;
          vehicleFlagsCache[c.plate] = c.statusFlags;
        }
      });
      enhanceCarsTable();
    } catch (e) {
      /* ignore */
    }
  }

  function bindOnce() {
    document.querySelectorAll(".history-tab").forEach(function (btn) {
      btn.addEventListener("click", function () {
        switchHistoryTab(btn.getAttribute("data-htab"));
      });
    });

    document.querySelectorAll("[data-close]").forEach(function (el) {
      const target = el.getAttribute("data-close");
      if (target === "modalHistory") {
        el.addEventListener("click", function () {
          closeModal("modalHistory");
        });
      }
    });

    const carsTable = $("#carsTable");
    if (carsTable) {
      carsTable.addEventListener("click", function (e) {
        const btn = e.target.closest('button[data-act="history"]');
        if (!btn) return;
        e.stopPropagation();
        openVehicleHistory(btn.getAttribute("data-plate"));
      });

      const mo = new MutationObserver(function () {
        enhanceCarsTable();
      });
      mo.observe(carsTable, { childList: true, subtree: true });
    }

    const histFinesList = $("#historyFinesList");
    if (histFinesList) {
      histFinesList.addEventListener("click", async function (e) {
        const btn = e.target.closest("[data-pay-fine]");
        if (!btn || !historyPlate) return;
        const id = parseInt(btn.getAttribute("data-pay-fine"), 10);
        if (!confirm("این خلافی پرداخت شود؟")) return;
        try {
          await Api.payVehicleFine(historyPlate, id);
          toast("خلافی پرداخت شد");
          historyData = await Api.vehicleHistory(historyPlate);
          renderHistory(historyData);
          await prefetchFlags();
        } catch (err) {
          toast(err.message || "خطا", "err");
        }
      });
    }

    const formOdo = $("#formHistoryOdometer");
    if (formOdo) {
      formOdo.addEventListener("submit", async function (e) {
        e.preventDefault();
        if (!historyPlate) return;
        const val = parseInt(toLatinDigits($("#histOdometer").value), 10);
        if (isNaN(val)) {
          toast("کیلومتر نامعتبر", "err");
          return;
        }
        try {
          await Api.updateVehicleOdometer(historyPlate, val);
          toast("کیلومتر ذخیره شد");
          historyData = await Api.vehicleHistory(historyPlate);
          renderHistory(historyData);
          await prefetchFlags();
        } catch (err) {
          toast(err.message || "خطا", "err");
        }
      });
    }

    const formSvc = $("#formHistoryService");
    if (formSvc) {
      formSvc.addEventListener("submit", async function (e) {
        e.preventDefault();
        if (!historyPlate) return;
        const body = {
          serviceType: $("#histServiceType").value,
          description: $("#histServiceDesc").value.trim(),
          odometer: $("#histServiceOdo").value
            ? parseInt(toLatinDigits($("#histServiceOdo").value), 10)
            : null,
          cost: $("#histServiceCost").value
            ? parseFloat(toLatinDigits($("#histServiceCost").value))
            : 0,
          serviceDate: $("#histServiceDate").value.trim(),
          nextDueOdometer: $("#histServiceNextOdo").value
            ? parseInt(toLatinDigits($("#histServiceNextOdo").value), 10)
            : null,
          performedBy: $("#histServiceBy").value.trim(),
          notes: "",
        };
        if (!body.serviceDate) {
          toast("تاریخ سرویس الزامی است", "err");
          return;
        }
        try {
          await Api.addVehicleService(historyPlate, body);
          toast("سرویس ثبت شد");
          e.target.reset();
          historyData = await Api.vehicleHistory(historyPlate);
          renderHistory(historyData);
          await prefetchFlags();
        } catch (err) {
          toast(err.message || "خطا", "err");
        }
      });
    }

    const formIssue = $("#formHistoryIssue");
    if (formIssue) {
      formIssue.addEventListener("submit", async function (e) {
        e.preventDefault();
        if (!historyPlate) return;
        const body = {
          title: $("#histIssueTitle").value.trim(),
          description: $("#histIssueDesc").value.trim(),
          severity: $("#histIssueSeverity").value,
          cost: $("#histIssueCost").value
            ? parseFloat(toLatinDigits($("#histIssueCost").value))
            : 0,
          reportedAt: $("#histIssueDate").value.trim(),
          notes: "",
        };
        if (!body.title || !body.reportedAt) {
          toast("عنوان و تاریخ الزامی است", "err");
          return;
        }
        try {
          await Api.addVehicleIssue(historyPlate, body);
          toast("خرابی ثبت شد");
          e.target.reset();
          historyData = await Api.vehicleHistory(historyPlate);
          renderHistory(historyData);
          await prefetchFlags();
        } catch (err) {
          toast(err.message || "خطا", "err");
        }
      });
    }

    const formFine = $("#formHistoryFine");
    if (formFine) {
      formFine.addEventListener("submit", async function (e) {
        e.preventDefault();
        if (!historyPlate) return;
        const body = {
          fineType: $("#histFineType").value,
          amount: parseFloat(toLatinDigits($("#histFineAmount").value)),
          fineDate: $("#histFineDate").value.trim(),
          description: $("#histFineDesc").value.trim(),
        };
        if (!body.amount || !body.fineDate) {
          toast("مبلغ و تاریخ الزامی است", "err");
          return;
        }
        try {
          await Api.addVehicleFine(historyPlate, body);
          toast("خلافی ثبت شد");
          e.target.reset();
          historyData = await Api.vehicleHistory(historyPlate);
          renderHistory(historyData);
          await prefetchFlags();
        } catch (err) {
          toast(err.message || "خطا", "err");
        }
      });
    }

    document.getElementById("nav")?.addEventListener("click", function (e) {
      const btn = e.target.closest(".nav-item");
      if (btn && btn.dataset.view === "cars") {
        setTimeout(prefetchFlags, 400);
      }
    });

    setTimeout(function () {
      prefetchFlags();
      enhanceCarsTable();
    }, 600);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", bindOnce);
  } else {
    bindOnce();
  }
})();
