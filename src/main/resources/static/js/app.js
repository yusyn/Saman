(() => {
  const titles = {
    dashboard: ["وضعیت", "سلامت API و راهنما"],
    cars: ["ناوگان", "ماشین و موتور — لیست، ثبت، ویرایش و حذف"],
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

  function isMotorcycleType(t) {
    return String(t || "").toUpperCase() === "MOTORCYCLE";
  }

  function vehicleTypeIcon(type) {
    if (isMotorcycleType(type)) {
      return '<span class="vtype-icon vtype-moto" title="موتور" aria-label="موتور">🏍</span>';
    }
    return '<span class="vtype-icon vtype-car" title="ماشین" aria-label="ماشین">🚗</span>';
  }

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

    m = compact.match(/^(\d{2})([A-Za-z\u0600-\u06FF]+)(\d{3})(\d{2})$/);
    if (m) {
      return { first: m[1], letter: m[2], mid: m[3], city: m[4] };
    }
    return null;
  }

  function parseMotorcyclePlate(plate) {
    if (plate == null) return null;
    const digits = toLatinDigits(String(plate)).replace(/\D/g, "");
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

  function formatNowFa() {
    try {
      return new Date().toLocaleString("fa-IR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hour12: false,
      });
    } catch (e) {
      return new Date().toISOString();
    }
  }

  function isOnMissionStatus(status) {
    if (!status) return false;
    return String(status).includes("مأموریت") || String(status).includes("ماموریت");
  }

  function statusBadge(kind, label) {
    const cls =
      kind === "busy"
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
    return renting
      ? statusBadge("busy", "در مأموریت")
      : statusBadge("free", "آزاد");
  }

  function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add("hidden");
  }
  function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove("hidden");
  }
  document.querySelectorAll("[data-close]").forEach((el) => {
    el.addEventListener("click", () => closeModal(el.getAttribute("data-close")));
  });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      document.querySelectorAll(".modal:not(.hidden)").forEach((m) => m.classList.add("hidden"));
    }
  });

  function showView(name) {
    document.querySelectorAll(".view").forEach((v) => v.classList.remove("active"));
    const view = document.getElementById("view-" + name);
    if (view) view.classList.add("active");
    document.querySelectorAll(".nav-item").forEach((b) => {
      b.classList.toggle("active", b.dataset.view === name);
    });
    const t = titles[name] || [name, ""];
    $("#pageTitle").textContent = t[0];
    $("#pageHint").textContent = t[1];
    if (name === "cars") loadCars();
    if (name === "employees") loadEmployees();
    if (name === "rentals") loadAvailablePlates();
    if (name === "report") loadReport();
  }

  document.getElementById("nav").addEventListener("click", (e) => {
    const btn = e.target.closest(".nav-item");
    if (!btn) return;
    showView(btn.dataset.view);
  });

  async function loadHealth() {
    try {
      const h = await Api.health();
      $("#healthJson").textContent = JSON.stringify(h, null, 2);
      $("#healthBadge").textContent = "API متصل";
    } catch (err) {
      $("#healthJson").textContent = String(err.message || err);
      $("#healthBadge").textContent = "قطع";
    }
  }
  $("#btnRefreshHealth").addEventListener("click", loadHealth);

  function getSelectedType(formPrefix) {
    const el = document.querySelector('input[name="' + formPrefix + 'Type"]:checked');
    return el ? el.value : "CAR";
  }

  function setSelectedType(formPrefix, type) {
    const t = isMotorcycleType(type) ? "MOTORCYCLE" : "CAR";
    document.querySelectorAll('input[name="' + formPrefix + 'Type"]').forEach(function (r) {
      r.checked = r.value === t;
    });
    togglePlateWidgets(formPrefix, t);
  }

  function togglePlateWidgets(formPrefix, type) {
    const isMoto = isMotorcycleType(type);
    const carBox = document.getElementById(formPrefix + "CarPlate");
    const motoBox = document.getElementById(formPrefix + "MotoPlate");
    if (carBox) carBox.classList.toggle("hidden", isMoto);
    if (motoBox) motoBox.classList.toggle("hidden", !isMoto);
  }

  function plateFromWidgets(prefix) {
    const type = getSelectedType(prefix === "plate" ? "vehicle" : "editVehicle");
    if (isMotorcycleType(type)) {
      const top = (document.getElementById(prefix + "MotoTop") || {}).value || "";
      const bottom = (document.getElementById(prefix + "MotoBottom") || {}).value || "";
      return toLatinDigits(top + bottom).replace(/\D/g, "");
    }
    const first = (document.getElementById(prefix + "First") || {}).value || "";
    const letter = (document.getElementById(prefix + "Letter") || {}).value || "";
    const mid = (document.getElementById(prefix + "Mid") || {}).value || "";
    const city = (document.getElementById(prefix + "City") || {}).value || "";
    return (first + letter + mid + "ایران" + city).trim();
  }

  function fillPlateWidgets(prefix, plate, vehicleType) {
    const set = (id, v) => {
      const el = document.getElementById(id);
      if (el) el.value = v || "";
    };
    if (isMotorcycleType(vehicleType)) {
      const p = parseMotorcyclePlate(plate) || {};
      set(prefix + "MotoTop", p.top);
      set(prefix + "MotoBottom", p.bottom);
    } else {
      const p = parsePlate(plate) || {};
      set(prefix + "First", p.first);
      set(prefix + "Letter", p.letter);
      set(prefix + "Mid", p.mid);
      set(prefix + "City", p.city);
    }
  }

  ["plate", "editPlate"].forEach((prefix) => {
    ["First", "Mid", "City", "MotoTop", "MotoBottom"].forEach((part) => {
      const el = document.getElementById(prefix + part);
      if (!el) return;
      el.addEventListener("input", () => {
        el.value = toLatinDigits(el.value).replace(/\D/g, "");
      });
    });
  });

  document.querySelectorAll('input[name="vehicleType"]').forEach((r) => {
    r.addEventListener("change", () => togglePlateWidgets("vehicle", r.value));
  });
  document.querySelectorAll('input[name="editVehicleType"]').forEach((r) => {
    r.addEventListener("change", () => togglePlateWidgets("editVehicle", r.value));
  });

  async function loadCars() {
    const tbody = $("#carsTable");
    tbody.innerHTML = "<tr><td colspan=\"6\">در حال بارگذاری…</td></tr>";
    try {
      const cars = await Api.vehicles();
      carsCache = cars || [];
      if (!carsCache.length) {
        tbody.innerHTML = "<tr><td colspan=\"6\">وسیله‌ای ثبت نشده</td></tr>";
        return;
      }
      tbody.innerHTML = carsCache
        .map(function (c) {
          return (
            "<tr><td class=\"col-type\">" +
            vehicleTypeIcon(c.vehicleType) +
            "</td><td>" +
            escapeHtml(c.name) +
            "</td><td>" +
            renderPlate(c.plate, c.vehicleType) +
            "</td><td>" +
            escapeHtml(c.color) +
            "</td><td>" +
            carStatusBadge(c.status) +
            '</td><td class="actions">' +
            '<button type="button" class="btn btn-ghost btn-sm" data-act="edit" data-plate="' +
            escapeHtml(c.plate) +
            '">ویرایش</button> ' +
            '<button type="button" class="btn btn-danger btn-sm" data-act="del" data-plate="' +
            escapeHtml(c.plate) +
            '">حذف</button></td></tr>'
          );
        })
        .join("");
    } catch (err) {
      tbody.innerHTML = "<tr><td colspan=\"6\">" + escapeHtml(err.message) + "</td></tr>";
    }
  }
  $("#btnRefreshCars").addEventListener("click", loadCars);

  $("#carsTable").addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-act]");
    if (!btn) return;
    const plate = btn.getAttribute("data-plate");
    if (btn.dataset.act === "edit") {
      const car = carsCache.find((c) => c.plate === plate);
      if (!car) return;
      $("#editCarOldPlate").value = car.plate;
      $("#editCarName").value = car.name || "";
      $("#editCarColor").value = car.color || "";
      setSelectedType("editVehicle", car.vehicleType || "CAR");
      fillPlateWidgets("editPlate", car.plate, car.vehicleType);
      openModal("modalCar");
      return;
    }
    if (btn.dataset.act === "del") {
      if (!confirm("حذف این وسیله؟")) return;
      try {
        await Api.deleteVehicle(plate);
        toast("حذف شد");
        loadCars();
      } catch (err) {
        toast(err.message || "خطا", "err");
      }
    }
  });

  $("#formCar").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const vehicleType = getSelectedType("vehicle");
    const body = {
      name: fd.get("name").toString().trim(),
      plate: plateFromWidgets("plate"),
      color: fd.get("color").toString().trim(),
      vehicleType: vehicleType,
    };
    try {
      await Api.createVehicle(body);
      toast("ثبت شد");
      e.target.reset();
      setSelectedType("vehicle", "CAR");
      loadCars();
    } catch (err) {
      toast(err.message || "خطا", "err");
    }
  });

  $("#formEditCar").addEventListener("submit", async (e) => {
    e.preventDefault();
    const body = {
      oldPlate: $("#editCarOldPlate").value,
      name: $("#editCarName").value.trim(),
      plate: plateFromWidgets("editPlate"),
      color: $("#editCarColor").value.trim(),
      vehicleType: getSelectedType("editVehicle"),
    };
    try {
      await Api.updateVehicle(body);
      toast("ذخیره شد");
      closeModal("modalCar");
      loadCars();
    } catch (err) {
      toast(err.message || "خطا", "err");
    }
  });

  async function loadEmployees() {
    const tbody = $("#employeesTable");
    tbody.innerHTML = "<tr><td colspan=\"5\">در حال بارگذاری…</td></tr>";
    try {
      const rows = await Api.employees();
      employeesCache = rows || [];
      if (!employeesCache.length) {
        tbody.innerHTML = "<tr><td colspan=\"5\">کارمندی نیست</td></tr>";
        return;
      }
      tbody.innerHTML = employeesCache
        .map(function (emp) {
          return (
            '<tr><td dir="ltr">' +
            escapeHtml(emp.deviceUserId) +
            "</td><td>" +
            escapeHtml(emp.name) +
            '</td><td dir="ltr">' +
            escapeHtml(emp.phone) +
            "</td><td>" +
            employeeRentingBadge(!!emp.renting) +
            '</td><td class="actions">' +
            '<button type="button" class="btn btn-ghost btn-sm" data-act="edit" data-id="' +
            escapeHtml(emp.deviceUserId) +
            '">ویرایش</button> ' +
            '<button type="button" class="btn btn-danger btn-sm" data-act="del" data-id="' +
            escapeHtml(emp.deviceUserId) +
            '">حذف</button></td></tr>'
          );
        })
        .join("");
    } catch (err) {
      tbody.innerHTML = "<tr><td colspan=\"5\">" + escapeHtml(err.message) + "</td></tr>";
    }
  }
  $("#btnRefreshEmployees").addEventListener("click", loadEmployees);

  $("#employeesTable").addEventListener("click", async (e) => {
    const btn = e.target.closest("button[data-act]");
    if (!btn) return;
    const id = btn.getAttribute("data-id");
    if (btn.dataset.act === "edit") {
      const emp = employeesCache.find((x) => String(x.deviceUserId) === String(id));
      if (!emp) return;
      $("#editEmpId").value = emp.deviceUserId || "";
      $("#editEmpName").value = emp.name || "";
      $("#editEmpPhone").value = emp.phone || "";
      openModal("modalEmployee");
      return;
    }
    if (btn.dataset.act === "del") {
      if (!confirm("حذف کارمند؟")) return;
      try {
        await Api.deleteEmployee(id);
        toast("حذف شد");
        loadEmployees();
      } catch (err) {
        toast(err.message || "خطا", "err");
      }
    }
  });

  $("#formEmployee").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const body = {
      name: fd.get("name").toString().trim(),
      phone: (fd.get("phone") || "").toString().trim(),
      fingerIndex: Number(fd.get("fingerIndex")),
    };
    const status = $("#empRegisterStatus");
    status.textContent = "در حال ثبت / انتظار اثر انگشت…";
    try {
      const r = await Api.registerEmployee(body);
      status.textContent = r.message || "ثبت شد";
      toast(r.message || "ثبت شد");
      e.target.reset();
      loadEmployees();
    } catch (err) {
      status.textContent = err.message || "خطا";
      toast(err.message || "خطا", "err");
    }
  });

  $("#formEditEmployee").addEventListener("submit", async (e) => {
    e.preventDefault();
    const id = $("#editEmpId").value.trim();
    const body = {
      name: $("#editEmpName").value.trim(),
      phone: $("#editEmpPhone").value.trim(),
    };
    try {
      await Api.updateEmployee(id, body);
      toast("ذخیره شد");
      closeModal("modalEmployee");
      loadEmployees();
    } catch (err) {
      toast(err.message || "خطا", "err");
    }
  });

  function ensureReturnMissionBox() {
    let box = document.getElementById("returnMissionBox");
    if (box) return box;
    const profile = document.getElementById("returnAuthProfile");
    if (!profile) return null;
    box = document.createElement("div");
    box.id = "returnMissionBox";
    box.className = "return-mission hidden";
    profile.appendChild(box);
    return box;
  }

  function renderReturnMission(auth) {
    const box = ensureReturnMissionBox();
    if (!box) return;
    const m = auth && auth.activeRental;
    if (!m) {
      box.innerHTML = "";
      box.classList.add("hidden");
      return;
    }
    box.classList.remove("hidden");
    const vehicleLabel =
      (m.carName || "—") +
      (m.carColor ? " · " + m.carColor : "");
    const returnNow = auth.returnPreviewTime || formatNowFa();
    box.innerHTML =
      '<div class="auth-row mission-head"><span>مأموریت فعال</span><strong>' +
      vehicleTypeIcon(m.vehicleType) +
      " " +
      escapeHtml(vehicleLabel) +
      "</strong></div>" +
      '<div class="auth-row"><span>پلاک</span><strong>' +
      renderPlate(m.plate, m.vehicleType) +
      "</strong></div>" +
      '<div class="auth-row"><span>مقصد</span><strong>' +
      escapeHtml(m.destination || "—") +
      "</strong></div>" +
      '<div class="auth-row"><span>زمان تحویل</span><strong dir="ltr">' +
      escapeHtml(m.pickupDate || "—") +
      "</strong></div>" +
      '<div class="auth-row"><span>زمان برگشت (الان)</span><strong dir="ltr">' +
      escapeHtml(returnNow) +
      "</strong></div>";
  }

  function renderAuth(kind) {
    const auth = kind === "pickup" ? pickupAuth : returnAuth;
    const ph = $("#" + kind + "AuthPlaceholder");
    const profile = $("#" + kind + "AuthProfile");
    const clearBtn = $("#btnClear" + (kind === "pickup" ? "Pickup" : "Return") + "Auth");
    const submitBtn = $("#btn" + (kind === "pickup" ? "Pickup" : "Return") + "Submit");
    if (!auth) {
      if (ph) ph.classList.remove("hidden");
      if (profile) profile.classList.add("hidden");
      if (clearBtn) clearBtn.classList.add("hidden");
      if (submitBtn) submitBtn.disabled = true;
      if (kind === "return") renderReturnMission(null);
      return;
    }
    if (ph) ph.classList.add("hidden");
    if (profile) profile.classList.remove("hidden");
    if (clearBtn) clearBtn.classList.remove("hidden");
    const nameEl = $("#" + kind + "AuthName");
    const idEl = $("#" + kind + "AuthId");
    const phoneEl = $("#" + kind + "AuthPhone");
    if (nameEl) nameEl.textContent = auth.name || "—";
    if (idEl) idEl.textContent = auth.deviceUserId || "—";
    if (phoneEl) phoneEl.textContent = auth.phone || "—";
    if (kind === "return") {
      renderReturnMission(auth);
      if (submitBtn) submitBtn.disabled = !(auth.renting && auth.activeRental);
    } else if (submitBtn) {
      submitBtn.disabled = false;
    }
  }

  function clearAuth(kind) {
    if (kind === "pickup") pickupAuth = null;
    else returnAuth = null;
    renderAuth(kind);
  }

  async function runVerify(kind) {
    const wait = $("#" + kind + "VerifyWait");
    const status = $("#" + kind + "VerifyStatus");
    const cancelBtn = $("#btnCancel" + (kind === "pickup" ? "Pickup" : "Return") + "Verify");
    const verifyBtn = $("#btnVerify" + (kind === "pickup" ? "Pickup" : "Return"));
    wait.classList.remove("hidden");
    cancelBtn.classList.remove("hidden");
    verifyBtn.disabled = true;
    status.textContent = "";
    try {
      const r = await Api.verify(40);
      if (r && r.deviceUserId && (!r.name || !r.phone)) {
        try {
          const emp = await Api.employee(r.deviceUserId);
          if (emp) {
            r.name = emp.name || r.name;
            r.phone = emp.phone || r.phone;
            if (typeof emp.renting === "boolean") r.renting = emp.renting;
          }
        } catch (lookupErr) {
          console.warn("employee lookup after verify", lookupErr);
        }
      }
      if (kind === "return" && r && r.deviceUserId) {
        r.returnPreviewTime = formatNowFa();
        try {
          r.activeRental = await Api.activeRental(r.deviceUserId);
          r.renting = true;
        } catch (activeErr) {
          r.activeRental = null;
          if (activeErr && activeErr.status === 404) {
            r.renting = false;
            status.textContent = "مأموریت فعالی برای این کارمند نیست";
            toast("مأموریت فعالی یافت نشد", "err");
          } else {
            console.warn("active rental lookup", activeErr);
            toast(activeErr.message || "خطا در دریافت مأموریت", "err");
          }
        }
      }
      if (kind === "pickup") pickupAuth = r;
      else returnAuth = r;
      renderAuth(kind);
      if (!(kind === "return" && !r.activeRental)) {
        status.textContent = "احراز موفق";
        toast("احراز موفق");
      }
    } catch (err) {
      status.textContent = err.message || "ناموفق";
      toast(err.message || "ناموفق", "err");
    } finally {
      wait.classList.add("hidden");
      cancelBtn.classList.add("hidden");
      verifyBtn.disabled = false;
    }
  }

  function cancelVerify(kind) {
    $("#" + kind + "VerifyWait").classList.add("hidden");
    $("#btnCancel" + (kind === "pickup" ? "Pickup" : "Return") + "Verify").classList.add("hidden");
    $("#btnVerify" + (kind === "pickup" ? "Pickup" : "Return")).disabled = false;
    try {
      Api.cancelListen && Api.cancelListen();
    } catch (e) {}
  }

  $("#btnVerifyPickup").addEventListener("click", () => runVerify("pickup"));
  $("#btnVerifyReturn").addEventListener("click", () => runVerify("return"));
  $("#btnCancelPickupVerify").addEventListener("click", () => cancelVerify("pickup"));
  $("#btnCancelReturnVerify").addEventListener("click", () => cancelVerify("return"));
  $("#btnClearPickupAuth").addEventListener("click", () => clearAuth("pickup"));
  $("#btnClearReturnAuth").addEventListener("click", () => clearAuth("return"));

  async function loadAvailablePlates() {
    const list = document.getElementById("pickupCarList");
    const hidden = document.getElementById("pickupPlate");
    if (!list) {
      console.warn("pickupCarList missing");
      return;
    }
    if (!hidden) {
      list.innerHTML = '<div class="car-pick-empty">فیلد پلاک پیدا نشد</div>';
      return;
    }
    const prev = hidden.value;
    hidden.value = "";
    list.innerHTML = '<div class="car-pick-empty">در حال بارگذاری…</div>';
    try {
      const cars = await Api.vehiclesAvailable();
      if (!cars || !cars.length) {
        list.innerHTML = '<div class="car-pick-empty">وسیله آزادی نیست</div>';
        return;
      }
      list.innerHTML = cars
        .map(function (c) {
          const plate = c.plate || "";
          const selected = plate && plate === prev ? " is-selected" : "";
          return (
            '<button type="button" class="car-pick-item' +
            selected +
            '" role="option" data-plate="' +
            escapeHtml(plate) +
            '" aria-selected="' +
            (selected ? "true" : "false") +
            '">' +
            vehicleTypeIcon(c.vehicleType) +
            '<span class="car-pick-name">' +
            escapeHtml(c.name || "—") +
            "</span>" +
            renderPlate(plate, c.vehicleType) +
            "</button>"
          );
        })
        .join("");
      if (prev) {
        const items = list.querySelectorAll(".car-pick-item");
        for (let i = 0; i < items.length; i++) {
          if (items[i].getAttribute("data-plate") === prev) {
            items[i].classList.add("is-selected");
            items[i].setAttribute("aria-selected", "true");
            hidden.value = prev;
            break;
          }
        }
      }
    } catch (err) {
      list.innerHTML =
        '<div class="car-pick-empty">' +
        escapeHtml((err && err.message) || "خطا در بارگذاری") +
        "</div>";
    }
  }

  (function bindCarPickList() {
    const listEl = document.getElementById("pickupCarList");
    if (!listEl || listEl._pickBound) return;
    listEl._pickBound = true;
    listEl.addEventListener("click", function (e) {
      const item = e.target.closest(".car-pick-item");
      if (!item) return;
      listEl.querySelectorAll(".car-pick-item").forEach(function (el) {
        el.classList.remove("is-selected");
        el.setAttribute("aria-selected", "false");
      });
      item.classList.add("is-selected");
      item.setAttribute("aria-selected", "true");
      const hidden = document.getElementById("pickupPlate");
      if (hidden) hidden.value = item.getAttribute("data-plate") || "";
    });
  })();

  $("#formPickup").addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!pickupAuth || !pickupAuth.deviceUserId) {
      toast("ابتدا برای تحویل احراز هویت کنید", "err");
      return;
    }
    if (pickupAuth.renting) {
      toast("کارمند در مأموریت است؛ نمی‌تواند وسیله دیگری بگیرد", "err");
      return;
    }
    const fd = new FormData(e.target);
    const plateVal = (fd.get("plate") || "").toString().trim();
    if (!plateVal) {
      toast("یک وسیله آزاد انتخاب کنید", "err");
      return;
    }
    const body = {
      deviceUserId: pickupAuth.deviceUserId,
      plate: plateVal,
      destination: fd.get("destination").toString().trim(),
    };
    const btn = $("#btnPickupSubmit");
    btn.disabled = true;
    try {
      const r = await Api.pickup(body);
      toast(r.message || "تحویل ثبت شد");
      clearAuth("pickup");
      e.target.reset();
      await loadAvailablePlates();
    } catch (err) {
      toast(err.message || "خطا در تحویل", "err");
      btn.disabled = false;
    }
  });

  $("#formReturn").addEventListener("submit", async (e) => {
    e.preventDefault();
    if (!returnAuth || !returnAuth.deviceUserId) {
      toast("ابتدا برای برگشت احراز هویت کنید", "err");
      return;
    }
    if (!returnAuth.renting || !returnAuth.activeRental) {
      toast("کارمند مأموریت فعالی ندارد", "err");
      return;
    }
    const body = { deviceUserId: returnAuth.deviceUserId };
    const btn = $("#btnReturnSubmit");
    btn.disabled = true;
    try {
      const r = await Api.returnCar(body);
      toast(
        r.message || (r.ok === false ? "ناموفق" : "برگشت ثبت شد"),
        r.ok === false ? "err" : "ok"
      );
      clearAuth("return");
      await loadAvailablePlates();
    } catch (err) {
      toast(err.message || "خطا در برگشت", "err");
      btn.disabled = false;
    }
  });

  async function loadReport() {
    const tbody = $("#reportTable");
    tbody.innerHTML = '<tr><td colspan="7">در حال بارگذاری…</td></tr>';
    try {
      const rows = await Api.report();
      if (!rows.length) {
        tbody.innerHTML = '<tr><td colspan="7">گزارشی نیست</td></tr>';
        return;
      }
      tbody.innerHTML = rows
        .map(function (r) {
          return (
            '<tr><td dir="ltr">' +
            escapeHtml(r.deviceUserId) +
            "</td><td>" +
            vehicleTypeIcon(r.vehicleType) +
            " " +
            escapeHtml(r.carName) +
            "</td><td>" +
            renderPlate(r.plate, r.vehicleType) +
            "</td><td>" +
            escapeHtml(r.destination) +
            '</td><td dir="ltr">' +
            escapeHtml(r.pickupDate) +
            '</td><td dir="ltr">' +
            escapeHtml(r.returnDate) +
            "</td></tr>"
          );
        })
        .join("");
    } catch (err) {
      tbody.innerHTML =
        '<tr><td colspan="7">' + escapeHtml(err.message || "خطا") + "</td></tr>";
    }
  }
  $("#btnRefreshReport").addEventListener("click", loadReport);

  loadHealth();
  showView("dashboard");
})();
