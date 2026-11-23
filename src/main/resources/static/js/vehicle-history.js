/**
 * Vehicle history UI — comprehensive dossier with tabs:
 * 1. Summary (خلاصه)
 * 2. Missions (مأموریت‌ها با گزارش‌ها و ثبت عطف‌به‌ماسبق)
 * 3. Services & Fuel (خدمات و بنزین)
 * 4. Accidents (تصادفات و آسیب‌ها)
 * 5. Fines (خلافی‌ها با قابلیت پرداخت)
 * 6. Documents & Insurance (مدارک و بیمه‌نامه‌ها)
 */
(function () {
  const $ = (sel) => document.querySelector(sel);

  const HISTORY_BADGE_META = {
    DAMAGED: { cls: "badge-accident", label: "آسیب‌دیده", title: "تصادف یا خسارت باز" },
    IN_REPAIR: { cls: "badge-warn", label: "تعمیر", title: "در حال تعمیر" },
    HAS_FINE: { cls: "badge-fine", label: "خلافی باز", title: "جریمه پرداخت‌نشده" },
    INSURANCE_EXPIRING: { cls: "badge-warn", label: "انقضای بیمه", title: "بیمه‌نامه تا ۳۰ روز آینده منقضی می‌شود" },
    INSURANCE_EXPIRED: { cls: "badge-danger", label: "بیمه منقضی", title: "بیمه‌نامه منقضی شده است!" },
    INSPECTION_EXPIRING: { cls: "badge-warn", label: "انقضای معاینه", title: "معاینه فنی تا ۳۰ روز آینده منقضی می‌شود" },
    INSPECTION_EXPIRED: { cls: "badge-danger", label: "معاینه منقضی", title: "معاینه فنی منقضی شده است!" },
  };

  const DOC_TYPE_META = {
    THIRD_PARTY_INSURANCE: { label: "بیمه شخص ثالث", icon: "🛡️" },
    BODY_INSURANCE: { label: "بیمه بدنه", icon: "🚗" },
    TECHNICAL_INSPECTION: { label: "معاینه فنی", icon: "📋" },
    OTHER: { label: "سایر مدارک", icon: "📄" },
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
        const meta = HISTORY_BADGE_META[f] || { cls: "badge-neutral", label: f, title: f };
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
    return toPersianDigits(v.toLocaleString("fa-IR")) + " تومان";
  }

  function serviceTypeLabel(t) {
    const map = {
      CARWASH: "کارواش",
      OIL_CHANGE: "تعویض روغن",
      WINDSHIELD_WASHER: "مایع شیشه‌شور / ضدیخ",
      FILTER: "فیلتر",
      TIRE: "لاستیک",
      BATTERY: "باتری",
      BRAKE: "ترمز",
      INSPECTION: "معاینه / بازرسی",
      OTHER: "سایر خدمات",
    };
    return map[t] || t || "—";
  }

  function accidentSeverityLabel(s) {
    const map = {
      MINOR: "جزئی",
      MODERATE: "متوسط",
      SEVERE: "شدید",
      LOW: "کم",
      MEDIUM: "متوسط",
      HIGH: "زیاد",
      CRITICAL: "بحرانی",
    };
    return map[s] || s || "—";
  }

  function fineTypeLabel(t) {
    const map = {
      SPEED: "سرعت غیرمجاز",
      PARKING: "پارک ممنوع",
      RED_LIGHT: "عبور از چراغ قرمز",
      OTHER: "سایر تخلفات",
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

  function showImagePreview(src, title) {
    const modal = document.getElementById("modalImageViewer");
    const img = document.getElementById("imageViewerImg");
    const titleEl = document.getElementById("imageViewerTitle");
    if (img) img.src = src;
    if (titleEl && title) titleEl.textContent = title;
    if (modal) modal.classList.remove("hidden");
  }

  let historyPlate = null;
  let historyData = null;
  let historyDocs = [];
  let vehicleFlagsCache = {};
  let retroAttachmentId = null;

  function switchHistoryTab(tab) {
    document.querySelectorAll(".history-tab").forEach(function (b) {
      b.classList.toggle("active", b.getAttribute("data-htab") === tab);
    });
    document.querySelectorAll(".history-pane").forEach(function (p) {
      p.classList.toggle("active", p.getAttribute("data-hpane") === tab);
    });
    if (tab === "documents" && historyPlate) {
      loadVehicleDocuments(historyPlate);
    }
  }

  async function openVehicleHistory(plate) {
    historyPlate = plate;
    historyData = null;
    historyDocs = [];
    const title = $("#historyTitle");
    if (title) title.textContent = "پرونده و سوابق وسیله";

    const cars = (window.Saman && window.Saman.getCars) ? window.Saman.getCars() : [];
    const vehicle = cars.find((c) => c.plate === plate) || {};
    const plateBox = $("#historyPlateBox");
    if (plateBox) {
      if (window.Saman && window.Saman.renderPlate) {
        plateBox.innerHTML = window.Saman.renderPlate(plate, vehicle.vehicleType);
      } else {
        plateBox.textContent = plate;
      }
    }
    const subtitle = $("#historyVehicleSubtitle");
    if (subtitle) {
      const parts = [];
      if (vehicle.name) parts.push(vehicle.name);
      if (vehicle.color) parts.push(vehicle.color);
      subtitle.textContent = parts.length ? parts.join(" · ") : "";
    }

    const summary = $("#historySummary");
    if (summary) summary.innerHTML = '<p class="muted">در حال بارگذاری پرونده وسیله…</p>';

    ["#historyRentals", "#historyServicesList", "#historyAccidentsList", "#historyFinesList", "#historyDocumentsList"].forEach(
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
      if (summary) {
        summary.innerHTML = '<p class="status-line err">' + escapeHtml(err.message || "خطا در بارگذاری سوابق") + "</p>";
      }
    }
  }

  function renderHistory(h) {
    if (!h) return;
    const badges = (h.statusBadges || []).join(",");

    // 1. Summary pane
    const rentals = h.rentals || [];
    let totalFuelLiters = 0;
    let totalFuelCost = 0;
    let totalServiceReportCost = 0;

    rentals.forEach(function (r) {
      (r.reports || []).forEach(function (rep) {
        if (rep.reportType === "FUEL") {
          const l = rep.liters != null && !isNaN(rep.liters) ? rep.liters : 0;
          const c = rep.cost != null && !isNaN(rep.cost) ? rep.cost : (rep.amount != null && !isNaN(rep.amount) ? rep.amount : 0);
          totalFuelLiters += l;
          totalFuelCost += c;
        } else if (rep.reportType === "SERVICE") {
          const c = rep.cost != null && !isNaN(rep.cost) ? rep.cost : (rep.amount != null && !isNaN(rep.amount) ? rep.amount : 0);
          totalServiceReportCost += c;
        }
      });
    });

    const directServiceCost = h.totalServiceCost || 0;
    const combinedServiceCost = h.totalServiceCost != null ? h.totalServiceCost : (directServiceCost + totalServiceReportCost);
    const summaryFuelLiters = h.totalFuelLiters != null ? h.totalFuelLiters : totalFuelLiters;
    const summaryFuelCost = h.totalFuelCost != null ? h.totalFuelCost : totalFuelCost;

    $("#historySummary").innerHTML =
      '<div class="history-stats">' +
      '<div class="hist-stat"><span>کیلومتر فعلی</span><strong>' +
      toPersianDigits((h.currentOdometer || 0).toLocaleString("fa-IR")) +
      "</strong></div>" +
      '<div class="hist-stat"><span>تعداد مأموریت‌ها</span><strong>' +
      toPersianDigits(h.totalRentals || 0) +
      "</strong></div>" +
      '<div class="hist-stat"><span>مجموع بنزین مصرفی</span><strong>' +
      toPersianDigits(summaryFuelLiters.toFixed(1)) +
      " لیتر (" +
      formatMoney(summaryFuelCost) +
      ")</strong></div>" +
      '<div class="hist-stat"><span>کل هزینه خدمات و سرویس</span><strong>' +
      formatMoney(combinedServiceCost) +
      "</strong></div>" +
      '<div class="hist-stat"><span>خلافی پرداخت‌نشده</span><strong>' +
      formatMoney(h.unpaidFineAmount || 0) +
      "</strong></div>" +
      "</div>" +
      '<div class="history-badges-row">' +
      renderHistoryBadges(badges) +
      (badges ? "" : '<span class="badge badge-success">بدون هشدار — وضعیت مطلوب</span>') +
      "</div>" +
      '<p class="muted small">آخرین سرویس دوره‌ای: ' +
      escapeHtml(h.lastServiceDate || "—") +
      (h.lastServiceOdometer != null
        ? " — در کیلومتر " + toPersianDigits(h.lastServiceOdometer.toLocaleString("fa-IR"))
        : "") +
      "</p>";

    const histOdoInput = $("#histOdometer");
    if (histOdoInput && h.currentOdometer != null) {
      histOdoInput.value = h.currentOdometer;
    }

    // 2. Rentals (Missions) pane
    renderRentalsList(rentals);

    // 3. Services & Fuel combined pane
    renderServicesList(h.services || [], rentals);

    // 4. Accidents pane
    renderAccidentsList(h.issues || [], rentals);

    // 5. Fines pane
    renderFinesList(h.fines || [], rentals);
  }

  function renderRentalsList(rentals) {
    const host = $("#historyRentals");
    if (!host) return;
    if (!rentals || rentals.length === 0) {
      host.innerHTML = '<p class="muted">مأموریتی برای این وسیله ثبت نشده است.</p>';
      return;
    }

    host.innerHTML = rentals
      .map(function (r) {
        const reports = r.reports || [];
        let reportsHtml = "";
        if (reports.length > 0) {
          reportsHtml =
            '<div style="margin-top: 0.5rem; display: flex; flex-direction: column; gap: 0.35rem;">' +
            reports
              .map(function (rep) {
                let badgeClass = "badge-neutral";
                let typeIcon = "📄";
                let typeText = rep.reportType;
                let details = "";

                if (rep.reportType === "FUEL") {
                  badgeClass = "badge-warn";
                  typeIcon = "⛽";
                  typeText = "بنزین";
                  const litersVal = rep.liters != null && !isNaN(rep.liters) ? rep.liters : 0;
                  const costVal = rep.cost != null && !isNaN(rep.cost) ? rep.cost : rep.amount;
                  if (litersVal) details += toPersianDigits(litersVal) + " لیتر ";
                  if (costVal) details += "— " + formatMoney(costVal);
                } else if (rep.reportType === "SERVICE") {
                  badgeClass = "badge-service";
                  typeIcon = "🔧";
                  typeText = "سرویس: " + serviceTypeLabel(rep.title);
                  const costVal = rep.cost != null && !isNaN(rep.cost) ? rep.cost : rep.amount;
                  if (costVal) details += "— " + formatMoney(costVal);
                } else if (rep.reportType === "ACCIDENT") {
                  badgeClass = "badge-accident";
                  typeIcon = "⚠️";
                  typeText = "تصادف (" + accidentSeverityLabel(rep.title) + ")";
                  const costVal = rep.cost != null && !isNaN(rep.cost) ? rep.cost : rep.amount;
                  if (costVal) details += "— خسارت: " + formatMoney(costVal);
                } else if (rep.reportType === "FINE") {
                  badgeClass = rep.paid ? "badge-success" : "badge-fine";
                  typeIcon = "📄";
                  typeText = "خلافی: " + escapeHtml(rep.title || "");
                  const costVal = rep.cost != null && !isNaN(rep.cost) ? rep.cost : rep.amount;
                  if (costVal) details += "— " + formatMoney(costVal);
                  if (rep.paid) details += " (پرداخت‌شده)";
                  else details += ' <button type="button" class="btn btn-sm btn-ghost" data-pay-report-fine="' + rep.id + '">پرداخت</button>';
                }

                let thumbHtml = "";
                const imgUrl = rep.attachmentUrl || (rep.attachment ? Api.attachmentUrl(rep.attachment.id) : (rep.attachmentId ? Api.attachmentUrl(rep.attachmentId) : null));
                if (imgUrl) {
                  thumbHtml =
                    '<span class="report-receipt-thumb" data-view-img="' +
                    imgUrl +
                    '" data-img-title="' +
                    escapeHtml(typeText) +
                    '">' +
                    '<img src="' +
                    imgUrl +
                    '" alt="رسید" /></span>';
                }

                return (
                  '<div class="report-card">' +
                  '<div class="report-card-head">' +
                  '<span class="badge ' +
                  badgeClass +
                  '">' +
                  typeIcon +
                  " " +
                  escapeHtml(typeText) +
                  "</span>" +
                  '<span class="muted small" dir="ltr">' +
                  escapeHtml(rep.createdAt || "") +
                  "</span>" +
                  "</div>" +
                  '<div style="font-size: 0.85rem;">' +
                  escapeHtml(details) +
                  (rep.description ? ' <span class="muted">(' + escapeHtml(rep.description) + ")</span>" : "") +
                  "</div>" +
                  thumbHtml +
                  "</div>"
                );
              })
              .join("") +
            "</div>";
        }

        return (
          '<div class="report-card" style="margin-bottom: 0.85rem; padding: 0.85rem;">' +
          '<div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--border); padding-bottom: 0.5rem; margin-bottom: 0.5rem;">' +
          '<div><strong>' +
          escapeHtml(r.employeeName || "کارمند") +
          '</strong> <span class="muted small">· مقصد: ' +
          escapeHtml(r.destination || "—") +
          "</span></div>" +
          '<button type="button" class="btn btn-sm btn-ghost" data-retro-rental-id="' +
          r.id +
          '" data-rental-emp="' +
          escapeHtml(r.employeeName || "") +
          '">+ ثبت گزارش برای این مأموریت</button>' +
          "</div>" +
          '<div style="display: flex; flex-wrap: wrap; gap: 1rem; font-size: 0.83rem;" class="muted">' +
          '<div>زمان تحویل: <strong dir="ltr" class="text-white">' +
          escapeHtml(r.pickupDate || "—") +
          "</strong></div>" +
          '<div>زمان بازگشت: <strong dir="ltr" class="text-white">' +
          escapeHtml(r.returnDate || "—") +
          "</strong></div>" +
          '<div>کیلومتر ثبت بازگشت: <strong class="text-white">' +
          (r.returnOdometer != null ? toPersianDigits(r.returnOdometer.toLocaleString("fa-IR")) : "—") +
          "</strong></div>" +
          "</div>" +
          reportsHtml +
          "</div>"
        );
      })
      .join("");
  }

  function renderServicesList(directServices, rentals) {
    const host = $("#historyServicesList");
    if (!host) return;

    const items = [];
    directServices.forEach(function (s) {
      items.push({
        type: "SERVICE",
        title: serviceTypeLabel(s.serviceType),
        date: s.serviceDate,
        cost: s.cost,
        odometer: s.odometer,
        desc: s.description,
        performedBy: s.performedBy,
        attachmentUrl: null,
      });
    });

    rentals.forEach(function (r) {
      (r.reports || []).forEach(function (rep) {
        const costVal = rep.cost != null && !isNaN(rep.cost) ? rep.cost : rep.amount;
        const imgUrl = rep.attachmentUrl || (rep.attachment ? Api.attachmentUrl(rep.attachment.id) : (rep.attachmentId ? Api.attachmentUrl(rep.attachmentId) : null));
        if (rep.reportType === "SERVICE") {
          items.push({
            type: "SERVICE",
            title: serviceTypeLabel(rep.title),
            date: rep.reportDate || (r.returnDate ? r.returnDate.split(" ")[0] : ""),
            cost: costVal,
            odometer: r.returnOdometer,
            desc: rep.description,
            performedBy: r.employeeName,
            attachmentUrl: imgUrl,
          });
        } else if (rep.reportType === "FUEL") {
          const litersVal = rep.liters != null && !isNaN(rep.liters) ? rep.liters : "";
          items.push({
            type: "FUEL",
            title: "سوخت‌گیری (" + toPersianDigits(litersVal) + " لیتر)",
            date: rep.reportDate || (r.returnDate ? r.returnDate.split(" ")[0] : ""),
            cost: costVal,
            odometer: r.returnOdometer,
            desc: rep.description,
            performedBy: r.employeeName,
            attachmentUrl: imgUrl,
          });
        }
      });
    });

    if (items.length === 0) {
      host.innerHTML = '<p class="muted">سرویس یا سوخت‌گیری ثبت نشده است.</p>';
      return;
    }

    items.sort(function (a, b) {
      return String(b.date || "").localeCompare(String(a.date || ""));
    });

    host.innerHTML =
      '<div class="table-wrap"><table class="table-hist"><thead><tr><th>نوع</th><th>تاریخ</th><th>کیلومتر</th><th>هزینه</th><th>انجام‌دهنده</th><th>رسید</th><th>توضیح</th></tr></thead><tbody>' +
      items
        .map(function (it) {
          const icon = it.type === "FUEL" ? "⛽" : "🔧";
          let thumb = "—";
          if (it.attachmentUrl) {
            thumb =
              '<span class="report-receipt-thumb" data-view-img="' +
              it.attachmentUrl +
              '" data-img-title="' +
              escapeHtml(it.title) +
              '"><img src="' +
              it.attachmentUrl +
              '" alt="رسید" /></span>';
          }
          return (
            "<tr>" +
            "<td>" +
            icon +
            " " +
            escapeHtml(it.title) +
            "</td>" +
            '<td dir="ltr">' +
            escapeHtml(it.date || "—") +
            "</td>" +
            '<td dir="ltr">' +
            (it.odometer != null ? toPersianDigits(it.odometer.toLocaleString("fa-IR")) : "—") +
            "</td>" +
            "<td>" +
            formatMoney(it.cost) +
            "</td>" +
            "<td>" +
            escapeHtml(it.performedBy || "—") +
            "</td>" +
            "<td>" +
            thumb +
            "</td>" +
            "<td>" +
            escapeHtml(it.desc || "—") +
            "</td>" +
            "</tr>"
          );
        })
        .join("") +
      "</tbody></table></div>";
  }

  function renderAccidentsList(directIssues, rentals) {
    const host = $("#historyAccidentsList");
    if (!host) return;

    const items = [];
    directIssues.forEach(function (i) {
      items.push({
        title: i.title,
        severity: i.severity,
        date: i.reportedAt,
        cost: i.cost,
        desc: i.description,
        status: i.status,
        attachmentUrl: null,
      });
    });

    rentals.forEach(function (r) {
      (r.reports || []).forEach(function (rep) {
        if (rep.reportType === "ACCIDENT") {
          const costVal = rep.cost != null && !isNaN(rep.cost) ? rep.cost : rep.amount;
          const imgUrl = rep.attachmentUrl || (rep.attachment ? Api.attachmentUrl(rep.attachment.id) : (rep.attachmentId ? Api.attachmentUrl(rep.attachmentId) : null));
          items.push({
            title: "تصادف در مأموریت " + (r.destination ? "مقصد " + r.destination : ""),
            severity: rep.title,
            date: rep.reportDate || (r.returnDate ? r.returnDate.split(" ")[0] : ""),
            cost: costVal,
            desc: (r.employeeName ? "راننده: " + r.employeeName + " — " : "") + (rep.description || ""),
            status: "OPEN",
            attachmentUrl: imgUrl,
          });
        }
      });
    });

    if (items.length === 0) {
      host.innerHTML = '<p class="muted">هیچ تصادف یا سانحه‌ای برای این وسیله گزارش نشده است.</p>';
      return;
    }

    host.innerHTML =
      '<div class="table-wrap"><table class="table-hist"><thead><tr><th>شرح سانحه</th><th>شدت</th><th>تاریخ</th><th>هزینه برآوردی</th><th>تصویر خسارت</th><th>جزئیات</th></tr></thead><tbody>' +
      items
        .map(function (it) {
          let thumb = "—";
          if (it.attachmentUrl) {
            thumb =
              '<span class="report-receipt-thumb" data-view-img="' +
              it.attachmentUrl +
              '" data-img-title="' +
              escapeHtml(it.title) +
              '"><img src="' +
              it.attachmentUrl +
              '" alt="خسارت" /></span>';
          }
          return (
            "<tr>" +
            "<td>" +
            escapeHtml(it.title) +
            "</td>" +
            '<td><span class="badge badge-accident">' +
            escapeHtml(accidentSeverityLabel(it.severity)) +
            "</span></td>" +
            '<td dir="ltr">' +
            escapeHtml(it.date || "—") +
            "</td>" +
            "<td>" +
            formatMoney(it.cost) +
            "</td>" +
            "<td>" +
            thumb +
            "</td>" +
            "<td>" +
            escapeHtml(it.desc || "—") +
            "</td>" +
            "</tr>"
          );
        })
        .join("") +
      "</tbody></table></div>";
  }

  function renderFinesList(directFines, rentals) {
    const host = $("#historyFinesList");
    if (!host) return;

    const items = [];
    directFines.forEach(function (f) {
      items.push({
        id: f.id,
        isMissionReport: false,
        type: fineTypeLabel(f.fineType),
        amount: f.amount,
        date: f.fineDate,
        paid: f.paid,
        desc: f.description,
      });
    });

    rentals.forEach(function (r) {
      (r.reports || []).forEach(function (rep) {
        if (rep.reportType === "FINE") {
          const costVal = rep.cost != null && !isNaN(rep.cost) ? rep.cost : rep.amount;
          items.push({
            id: rep.id,
            isMissionReport: true,
            type: rep.title || "جریمه مأموریت",
            amount: costVal,
            date: rep.reportDate || (r.returnDate ? r.returnDate.split(" ")[0] : ""),
            paid: rep.paid,
            desc: (r.employeeName ? "راننده: " + r.employeeName + " — " : "") + (rep.description || ""),
          });
        }
      });
    });

    if (items.length === 0) {
      host.innerHTML = '<p class="muted">خلافی یا جریمه‌ای ثبت نشده است.</p>';
      return;
    }

    host.innerHTML =
      '<div class="table-wrap"><table class="table-hist"><thead><tr><th>نوع تخلف</th><th>مبلغ</th><th>تاریخ</th><th>وضعیت</th><th>توضیحات</th><th>عملیات</th></tr></thead><tbody>' +
      items
        .map(function (f) {
          let payBtn = '<span class="badge badge-success">تسویه شده</span>';
          if (!f.paid) {
            payBtn = f.isMissionReport
              ? '<button type="button" class="btn btn-sm btn-primary" data-pay-report-fine="' +
                f.id +
                '">پرداخت</button>'
              : '<button type="button" class="btn btn-sm btn-primary" data-pay-fine="' +
                f.id +
                '">پرداخت</button>';
          }
          return (
            "<tr>" +
            "<td>" +
            escapeHtml(f.type) +
            "</td>" +
            "<td>" +
            formatMoney(f.amount) +
            "</td>" +
            '<td dir="ltr">' +
            escapeHtml(f.date || "—") +
            "</td>" +
            "<td>" +
            (f.paid ? "پرداخت‌شده" : '<span class="badge badge-fine">پرداخت‌نشده</span>') +
            "</td>" +
            "<td>" +
            escapeHtml(f.desc || "—") +
            "</td>" +
            "<td>" +
            payBtn +
            "</td>" +
            "</tr>"
          );
        })
        .join("") +
      "</tbody></table></div>";
  }

  async function loadVehicleDocuments(plate) {
    const host = $("#historyDocumentsList");
    if (!host) return;
    host.innerHTML = '<p class="muted">در حال بارگذاری مدارک و بیمه‌نامه‌ها…</p>';
    try {
      historyDocs = await Api.vehicleDocuments(plate);
      renderDocumentsList(historyDocs);
    } catch (err) {
      host.innerHTML = '<p class="status-line err">' + escapeHtml(err.message || "خطا در دریافت مدارک") + "</p>";
    }
  }

  function renderDocumentsList(docs) {
    const host = $("#historyDocumentsList");
    if (!host) return;
    if (!docs || docs.length === 0) {
      host.innerHTML = '<p class="muted">هیچ مدرک، بیمه‌نامه یا گواهی معاینه‌ای برای این وسیله ثبت نشده است.</p>';
      return;
    }

    host.innerHTML =
      '<div style="display: flex; flex-direction: column; gap: 0.5rem;">' +
      docs
        .map(function (d) {
          const typeMeta = DOC_TYPE_META[d.documentType] || { label: d.documentType, icon: "📄" };
          let badgeStatus = "";
          if (d.expired) {
            badgeStatus = '<span class="badge badge-danger">منقضی شده</span>';
          } else if (d.expiringSoon) {
            badgeStatus =
              '<span class="badge badge-warn">' +
              toPersianDigits(d.daysUntilExpiry != null ? d.daysUntilExpiry : "—") +
              " روز مانده</span>";
          } else {
            badgeStatus =
              '<span class="badge badge-success">' +
              toPersianDigits(d.daysUntilExpiry != null ? d.daysUntilExpiry : "—") +
              " روز تا انقضا</span>";
          }

          return (
            '<div class="doc-item report-card">' +
            '<div class="doc-meta">' +
            '<span style="font-size: 1.25rem;">' +
            typeMeta.icon +
            "</span>" +
            "<div>" +
            "<div><strong>" +
            escapeHtml(typeMeta.label) +
            "</strong>" +
            (d.company ? ' <span class="muted">(' + escapeHtml(d.company) + ")</span>" : "") +
            "</div>" +
            '<div class="muted small">' +
            (d.documentNumber ? "شماره: " + escapeHtml(d.documentNumber) + " · " : "") +
            "انقضا: " +
            escapeHtml(d.expiryDate) +
            (d.notes ? " · " + escapeHtml(d.notes) : "") +
            "</div>" +
            "</div>" +
            "</div>" +
            '<div style="display: flex; align-items: center; gap: 0.5rem;">' +
            badgeStatus +
            '<button type="button" class="btn btn-sm btn-danger" data-doc-del-id="' +
            d.id +
            '">حذف</button>' +
            "</div>" +
            "</div>"
          );
        })
        .join("") +
      "</div>";
  }

  let carsTableObserver = null;
  let enhanceScheduled = false;

  function enhanceCarsTable() {
    const tbody = $("#carsTable");
    if (!tbody) return;
    if (carsTableObserver) carsTableObserver.disconnect();
    try {
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
            const html = renderHistoryBadges(flags);
            if (badgeHost.innerHTML !== html) badgeHost.innerHTML = html;
          }
        }
      });
    } finally {
      if (carsTableObserver) {
        carsTableObserver.observe(tbody, { childList: true, subtree: true });
      }
    }
  }

  function scheduleEnhanceCarsTable() {
    if (enhanceScheduled) return;
    enhanceScheduled = true;
    requestAnimationFrame(function () {
      enhanceScheduled = false;
      enhanceCarsTable();
    });
  }

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
    // Tab switching
    document.querySelectorAll(".history-tab").forEach(function (btn) {
      btn.addEventListener("click", function () {
        switchHistoryTab(btn.getAttribute("data-htab"));
      });
    });

    // Modal close buttons
    document.querySelectorAll("[data-close]").forEach(function (el) {
      const target = el.getAttribute("data-close");
      el.addEventListener("click", function () {
        closeModal(target);
      });
    });

    // Cars table 'سابقه' button
    const carsTable = $("#carsTable");
    if (carsTable) {
      carsTable.addEventListener("click", function (e) {
        const btn = e.target.closest('button[data-act="history"]');
        if (!btn) return;
        e.stopPropagation();
        openVehicleHistory(btn.getAttribute("data-plate"));
      });

      carsTableObserver = new MutationObserver(function () {
        scheduleEnhanceCarsTable();
      });
      carsTableObserver.observe(carsTable, { childList: true, subtree: true });
    }

    // Modal body image preview click delegate
    const modalHist = $("#modalHistory");
    if (modalHist) {
      modalHist.addEventListener("click", function (e) {
        const thumb = e.target.closest("[data-view-img]");
        if (thumb) {
          const url = thumb.getAttribute("data-view-img");
          const title = thumb.getAttribute("data-img-title") || "تصویر پیوست";
          showImagePreview(url, title);
        }
      });
    }

    // Rentals list retroactive report button delegate
    const rentalsHost = $("#historyRentals");
    if (rentalsHost) {
      rentalsHost.addEventListener("click", function (e) {
        const btn = e.target.closest("[data-retro-rental-id]");
        if (!btn) return;
        const rentalId = btn.getAttribute("data-retro-rental-id");
        const empName = btn.getAttribute("data-rental-emp") || "";
        const titleEl = $("#retroReportTitle");
        if (titleEl) titleEl.textContent = "ثبت گزارش برای مأموریت (" + empName + ")";
        const hiddenId = $("#retroRentalId");
        if (hiddenId) hiddenId.value = rentalId;
        const form = $("#formRetroReport");
        if (form) form.reset();
        retroAttachmentId = null;
        const prev = $("#retroReportPreview");
        if (prev) prev.innerHTML = "";
        openModal("modalRetroReport");
      });

      // Pay mission fine delegate
      rentalsHost.addEventListener("click", async function (e) {
        const btn = e.target.closest("[data-pay-report-fine]");
        if (!btn) return;
        const id = parseInt(btn.getAttribute("data-pay-report-fine"), 10);
        if (!confirm("آیا این جریمه مأموریت پرداخت شده است؟")) return;
        try {
          await Api.payMissionFine(id);
          toast("جریمه با موفقیت تسویه شد");
          if (historyPlate) {
            historyData = await Api.vehicleHistory(historyPlate);
            renderHistory(historyData);
            await prefetchFlags();
          }
        } catch (err) {
          toast(err.message || "خطا در تسویه جریمه", "err");
        }
      });
    }

    // Direct pay fine in fines pane
    const histFinesList = $("#historyFinesList");
    if (histFinesList) {
      histFinesList.addEventListener("click", async function (e) {
        const btnMission = e.target.closest("[data-pay-report-fine]");
        if (btnMission) {
          const id = parseInt(btnMission.getAttribute("data-pay-report-fine"), 10);
          if (!confirm("این خلافی مأموریت پرداخت شود؟")) return;
          try {
            await Api.payMissionFine(id);
            toast("خلافی پرداخت شد");
            if (historyPlate) {
              historyData = await Api.vehicleHistory(historyPlate);
              renderHistory(historyData);
              await prefetchFlags();
            }
          } catch (err) {
            toast(err.message || "خطا", "err");
          }
          return;
        }

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

    // Documents delete delegate
    const docsList = $("#historyDocumentsList");
    if (docsList) {
      docsList.addEventListener("click", async function (e) {
        const btn = e.target.closest("[data-doc-del-id]");
        if (!btn || !historyPlate) return;
        const docId = parseInt(btn.getAttribute("data-doc-del-id"), 10);
        if (!confirm("آیا از حذف این مدرک اطمینان دارید؟")) return;
        try {
          await Api.deleteVehicleDocument(historyPlate, docId);
          toast("مدرک حذف شد");
          await loadVehicleDocuments(historyPlate);
          historyData = await Api.vehicleHistory(historyPlate);
          renderHistory(historyData);
          await prefetchFlags();
        } catch (err) {
          toast(err.message || "خطا در حذف مدرک", "err");
        }
      });
    }

    // Form: Odometer
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

    // Form: Service
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

    // Form: Accident (replacing old issue form)
    const formAcc = $("#formHistoryAccident");
    if (formAcc) {
      formAcc.addEventListener("submit", async function (e) {
        e.preventDefault();
        if (!historyPlate) return;
        const body = {
          title: $("#histIssueTitle") ? $("#histIssueTitle").value.trim() : "سانحه / خسارت",
          description: $("#histIssueDesc") ? $("#histIssueDesc").value.trim() : "",
          severity: $("#histIssueSeverity") ? $("#histIssueSeverity").value : "MEDIUM",
          cost: $("#histIssueCost") && $("#histIssueCost").value
            ? parseFloat(toLatinDigits($("#histIssueCost").value))
            : 0,
          reportedAt: $("#histIssueDate") ? $("#histIssueDate").value.trim() : "",
          notes: "",
        };
        if (!body.title || !body.reportedAt) {
          toast("عنوان و تاریخ الزامی است", "err");
          return;
        }
        try {
          await Api.addVehicleIssue(historyPlate, body);
          toast("گزارش سانحه ثبت شد");
          e.target.reset();
          historyData = await Api.vehicleHistory(historyPlate);
          renderHistory(historyData);
          await prefetchFlags();
        } catch (err) {
          toast(err.message || "خطا", "err");
        }
      });
    }

    // Form: Fine
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

    // Form: Document
    const formDoc = $("#formHistoryDocument");
    if (formDoc) {
      formDoc.addEventListener("submit", async function (e) {
        e.preventDefault();
        if (!historyPlate) return;
        const body = {
          documentType: $("#histDocType").value,
          documentNumber: $("#histDocNumber").value.trim(),
          expiryDate: $("#histDocExpiry").value.trim(),
          company: $("#histDocCompany").value.trim(),
          notes: $("#histDocNotes").value.trim(),
        };
        if (!body.expiryDate) {
          toast("تاریخ انقضا الزامی است", "err");
          return;
        }
        try {
          await Api.addVehicleDocument(historyPlate, body);
          toast("مدرک با موفقیت ثبت شد");
          e.target.reset();
          await loadVehicleDocuments(historyPlate);
          historyData = await Api.vehicleHistory(historyPlate);
          renderHistory(historyData);
          await prefetchFlags();
        } catch (err) {
          toast(err.message || "خطا در ثبت مدرک", "err");
        }
      });
    }

    // Retroactive file upload
    const retroFileInput = document.getElementById("retroReportFile");
    const retroPrev = document.getElementById("retroReportPreview");
    if (retroFileInput && retroPrev) {
      retroFileInput.addEventListener("change", async function (e) {
        const file = e.target.files && e.target.files[0];
        if (!file) return;
        retroPrev.innerHTML = '<span class="spinner" style="width:14px;height:14px;display:inline-block;"></span> آپلود...';
        try {
          const att = await Api.uploadAttachment(file);
          retroAttachmentId = att.id;
          retroPrev.innerHTML =
            '<img src="' + Api.attachmentUrl(att.id) + '" alt="رسید" />' +
            '<span>' + escapeHtml(att.originalFilename || file.name) + '</span>' +
            '<button type="button" class="file-upload-remove" title="حذف">✕</button>';
          const rm = retroPrev.querySelector(".file-upload-remove");
          if (rm) {
            rm.addEventListener("click", function () {
              retroAttachmentId = null;
              retroFileInput.value = "";
              retroPrev.innerHTML = "";
            });
          }
        } catch (err) {
          toast(err.message || "خطا در آپلود فایل", "err");
          retroPrev.innerHTML = "";
          retroFileInput.value = "";
        }
      });
    }

    // Form: Retroactive Report
    const formRetro = $("#formRetroReport");
    if (formRetro) {
      formRetro.addEventListener("submit", async function (e) {
        e.preventDefault();
        const rentalId = parseInt($("#retroRentalId").value, 10);
        if (isNaN(rentalId)) {
          toast("شناسه مأموریت نامعتبر است", "err");
          return;
        }
        const repType = $("#retroReportType").value;
        const repTitle = ($("#retroReportTitleInput") ? $("#retroReportTitleInput").value : "").trim();
        const repAmount = parseFloat(toLatinDigits($("#retroReportAmount") ? $("#retroReportAmount").value : ""));
        const repCost = parseInt(toLatinDigits($("#retroReportCost") ? $("#retroReportCost").value : ""), 10);
        const repDate = ($("#retroReportDate") ? $("#retroReportDate").value : "").trim();
        const repDesc = ($("#retroReportDesc") ? $("#retroReportDesc").value : "").trim();

        const costVal = !isNaN(repCost) ? repCost : (!isNaN(repAmount) && repType !== "FUEL" ? repAmount : null);
        const litersVal = repType === "FUEL" && !isNaN(repAmount) ? repAmount : null;

        const body = {
          reportType: repType,
          title: repTitle || (repType === "FUEL" ? "بنزین" : repType === "SERVICE" ? "سرویس" : repType === "ACCIDENT" ? "تصادف" : "خلافی"),
          amount: costVal,
          cost: costVal,
          liters: litersVal,
          reportDate: repDate || null,
          description: repDesc || null,
          attachmentId: retroAttachmentId || null,
        };

        const btn = $("#btnRetroSubmit");
        if (btn) btn.disabled = true;
        try {
          await Api.addRetroactiveReport(rentalId, body);
          toast("گزارش با موفقیت برای مأموریت ثبت شد");
          closeModal("modalRetroReport");
          if (historyPlate) {
            historyData = await Api.vehicleHistory(historyPlate);
            renderHistory(historyData);
            await prefetchFlags();
          }
        } catch (err) {
          toast(err.message || "خطا در ثبت گزارش", "err");
        } finally {
          if (btn) btn.disabled = false;
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
