/**
 * Reusable 3-part Jalali (Shamsi) Date Picker Component
 * Saman Fleet Management System
 */
(function (global) {
  "use strict";

  const MONTH_NAMES = [
    "فروردین", "اردیبهشت", "خرداد",
    "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر",
    "دی", "بهمن", "اسفند"
  ];

  function jalaliDaysInMonth(jy, jm) {
    if (jm <= 6) return 31;
    if (jm <= 11) return 30;
    const leaps = [1, 5, 9, 13, 17, 22, 26, 30];
    return leaps.indexOf(((jy % 33) + 33) % 33) >= 0 ? 30 : 29;
  }

  function toLatin(s) {
    if (!s) return "";
    return String(s)
      .replace(/[۰-۹]/g, d => String.fromCharCode(d.charCodeAt(0) - 1728))
      .replace(/[٠-٩]/g, d => String.fromCharCode(d.charCodeAt(0) - 1584));
  }

  function toPersian(s) {
    if (s == null) return "";
    return String(s)
      .replace(/[0-9]/g, d => String.fromCharCode(d.charCodeAt(0) + 1728));
  }

  function pad(n) {
    n = Number(n);
    return n < 10 ? "0" + n : String(n);
  }

  function parseDate(str) {
    if (!str) return null;
    const cleaned = toLatin(str).trim().replace(/[-.]/g, "/");
    const m = cleaned.match(/^(\d{4})\/(\d{1,2})\/(\d{1,2})$/);
    if (!m) return null;
    return {
      year: parseInt(m[1], 10),
      month: parseInt(m[2], 10),
      day: parseInt(m[3], 10)
    };
  }

  function getTodayJalaliString() {
    try {
      const parts = new Intl.DateTimeFormat("en-US-u-ca-persian", {
        year: "numeric",
        month: "numeric",
        day: "numeric",
      }).formatToParts(new Date());
      let y = 1403, m = 1, d = 1;
      for (const p of parts) {
        if (p.type === "year") y = parseInt(p.value, 10);
        if (p.type === "month") m = parseInt(p.value, 10);
        if (p.type === "day") d = parseInt(p.value, 10);
      }
      return y + "/" + pad(m) + "/" + pad(d);
    } catch (e) {
      return "1403/01/01";
    }
  }

  function attach(container, targetInput, options) {
    if (!container) return null;
    if (typeof targetInput === "string") {
      targetInput = document.getElementById(targetInput) || document.querySelector(targetInput);
    }
    options = options || {};
    const startYear = options.startYear || 1395;
    const endYear = options.endYear || 1420;

    container.innerHTML = "";
    container.classList.add("jalali-picker-group");

    // Year select
    const selYear = document.createElement("select");
    selYear.className = "picker-year";
    selYear.innerHTML = '<option value="">سال</option>';
    for (let y = endYear; y >= startYear; y--) {
      selYear.innerHTML += `<option value="${y}">${toPersian(y)}</option>`;
    }

    // Month select
    const selMonth = document.createElement("select");
    selMonth.className = "picker-month";
    selMonth.innerHTML = '<option value="">ماه</option>';
    MONTH_NAMES.forEach((name, i) => {
      selMonth.innerHTML += `<option value="${pad(i + 1)}">${toPersian(pad(i + 1))} — ${name}</option>`;
    });

    // Day select
    const selDay = document.createElement("select");
    selDay.className = "picker-day";
    selDay.innerHTML = '<option value="">روز</option>';
    for (let d = 1; d <= 31; d++) {
      selDay.innerHTML += `<option value="${pad(d)}">${toPersian(pad(d))}</option>`;
    }

    container.appendChild(selYear);
    container.appendChild(selMonth);
    container.appendChild(selDay);

    function updateDayOptions() {
      const y = parseInt(selYear.value, 10);
      const m = parseInt(selMonth.value, 10);
      if (y && m) {
        const maxDays = jalaliDaysInMonth(y, m);
        Array.from(selDay.options).forEach(opt => {
          if (!opt.value) return;
          const val = parseInt(opt.value, 10);
          opt.disabled = val > maxDays;
        });
        if (selDay.value && parseInt(selDay.value, 10) > maxDays) {
          selDay.value = pad(maxDays);
        }
      }
    }

    function syncToTarget() {
      updateDayOptions();
      if (!targetInput) return;
      const y = selYear.value;
      const m = selMonth.value;
      const d = selDay.value;
      if (y && m && d) {
        targetInput.value = `${y}/${m}/${d}`;
      } else {
        targetInput.value = "";
      }
      targetInput.dispatchEvent(new Event("change", { bubbles: true }));
    }

    function setValue(dateStr) {
      const parsed = parseDate(dateStr);
      if (parsed) {
        selYear.value = String(parsed.year);
        selMonth.value = pad(parsed.month);
        updateDayOptions();
        selDay.value = pad(parsed.day);
      } else {
        selYear.value = "";
        selMonth.value = "";
        selDay.value = "";
      }
      if (targetInput) {
        targetInput.value = dateStr ? toLatin(dateStr).trim() : "";
      }
    }

    function clear() {
      setValue("");
    }

    selYear.addEventListener("change", syncToTarget);
    selMonth.addEventListener("change", syncToTarget);
    selDay.addEventListener("change", syncToTarget);

    if (targetInput && targetInput.value) {
      setValue(targetInput.value);
    }

    return {
      setValue,
      clear,
      container,
      targetInput
    };
  }

  function initJalaliDateWidget(containerId, options) {
    const container = typeof containerId === "string" ? document.getElementById(containerId) : containerId;
    if (!container) return null;

    options = options || {};
    let hiddenInput = container.querySelector('input[type="hidden"]');
    if (!hiddenInput) {
      hiddenInput = document.createElement("input");
      hiddenInput.type = "hidden";
      if (options.name) hiddenInput.name = options.name;
      container.appendChild(hiddenInput);
    }

    const defaultToday = options.defaultToday !== false;
    let initialDate = options.initialDate;
    if (initialDate === undefined) {
      initialDate = defaultToday ? getTodayJalaliString() : "";
    }

    const picker = attach(container, hiddenInput, {
      startYear: options.startYear || 1400,
      endYear: options.endYear || 1415,
      ...options
    });

    if (initialDate) {
      picker.setValue(initialDate);
    }

    return {
      getValue: () => hiddenInput.value || "",
      setValue: (val) => picker.setValue(val),
      clear: () => picker.clear(),
      container: container,
      targetInput: hiddenInput,
      picker: picker
    };
  }

  global.Saman = global.Saman || {};
  global.Saman.initJalaliDateWidget = initJalaliDateWidget;
  global.Saman.getTodayJalali = getTodayJalaliString;
  global.Saman.parseJalaliDate = parseDate;

  global.JalaliDatePicker = {
    attach,
    initJalaliDateWidget,
    getTodayJalali: getTodayJalaliString,
    parseDate,
    jalaliDaysInMonth,
    MONTH_NAMES
  };
})(window);
