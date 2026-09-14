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

  function plateFromWidgets(prefix) {
    const first = (document.getElementById(prefix + "First") || {}).value || "";
    const letter = (document.getElementById(prefix + "Letter") || {}).value || "";
    const mid = (document.getElementById(prefix + "Mid") || {}).value || "";
    const city = (document.getElementById(prefix + "City") || {}).value || "";
    return (first + letter + mid + "ایران" + city).trim();
  }

  function fillPlateWidgets(prefix, plate) {
    const p = parsePlate(plate) || {};
    const set = (id, v) => {
      const el = document.getElementById(id);
      if (el) el.value = v || "";
    };
    set(prefix + "First", p.first);
    set(prefix + "Letter", p.letter);
    set(prefix + "Mid", p.mid);
    set(prefix + "City", p.city);
  }

  ["plate", "editPlate"].forEach((prefix) => {
    ["First", "Mid", "City"].forEach((part) => {
      const el = document.getElementById(prefix + part);
      if (!el) return;
      el.addEventListener("input", () => {
        el.value = toLatinDigits(el.value).replace(/\D/g, "");
      });
    });
  });

  async function loadCars() {
    const tbody = $("#carsTable");
    tbody.innerHTML = "<tr><td colspan=\"5\">در حال بارگذاری…</td></tr>";
    try {
      const cars = await Api.cars();
      carsCache = cars || [];
      if (!carsCache.length) {
        tbody.innerHTML = "<tr><td colspan=\"5\">ماشینی ثبت نشده</td></tr>";
        return;
      }
      tbody.innerHTML = carsCache
        .map(function (c) {
          return (
            "<tr><td>" +
            escapeHtml(c.name) +
            "</td><td>" +
            renderIranPlate(c.plate) +
            "</td><td>" +
            escapeHtml(c.color) +
            "</td><td>" +
            escapeHtml(c.status) +
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
      tbody.innerHTML = "<tr><td colspan=\"5\">" + escapeHtml(err.message) + "</td></tr>";
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
      fillPlateWidgets("editPlate", car.plate);
      openModal("modalCar");
      return;
    }
    if (btn.dataset.act === "del") {
      if (!confirm("حذف ماشین؟")) return;
      try {
        await Api.deleteCar(plate);
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
    const body = {
      name: fd.get("name").toString().trim(),
      plate: plateFromWidgets("plate"),
      color: fd.get("color").toString().trim(),
    };
    try {
      await Api.createCar(body);
      toast("ثبت شد");
      e.target.reset();
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
    };
    try {
      await Api.updateCar(body);
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
            (emp.renting ? "در مأموریت" : "آزاد") +
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

  function renderAuth(kind) {
    const auth = kind === "pickup" ? pickupAuth : returnAuth;
    const ph = $("#" + kind + "AuthPlaceholder");
    const profile = $("#" + kind + "AuthProfile");
    const clearBtn = $("#btnClear" + (kind === "pickup" ? "Pickup" : "Return") + "Auth");
    if (!auth) {
      ph.classList.remove("hidden");
      profile.classList.add("hidden");
      clearBtn.classList.add("hidden");
      $("#btn" + (kind === "pickup" ? "Pickup" : "Return") + "Submit").disabled = true;
      return;
    }
    ph.classList.add("hidden");
    profile.classList.remove("hidden");
    clearBtn.classList.remove("hidden");
    $("#" + kind + "AuthName").textContent = auth.name || "—";
    $("#" + kind + "AuthId").textContent = auth.deviceUserId || "—";
    $("#" + kind + "AuthPhone").textContent = auth.phone || "—";
    $("#" + kind + "AuthRenting").textContent = auth.renting ? "در مأموریت" : "آزاد";
    $("#btn" + (kind === "pickup" ? "Pickup" : "Return") + "Submit").disabled = false;
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
      if (kind === "pickup") pickupAuth = r;
      else returnAuth = r;
      renderAuth(kind);
      status.textContent = "احراز موفق";
      toast("احراز موفق");
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
    try { Api.cancelListen && Api.cancelListen(); } catch (e) {}
  }

  $("#btnVerifyPickup").addEventListener("click", () => runVerify("pickup"));
  $("#btnVerifyReturn").addEventListener("click", () => runVerify("return"));
  $("#btnCancelPickupVerify").addEventListener("click", () => cancelVerify("pickup"));
  $("#btnCancelReturnVerify").addEventListener("click", () => cancelVerify("return"));
  $("#btnClearPickupAuth").addEventListener("click", () => clearAuth("pickup"));
  $("#btnClearReturnAuth").addEventListener("click", () => clearAuth("return"));

  async function loadAvailablePlates() {
    const list = $("#pickupCarList");
    const hidden = $("#pickupPlate");
    if (!list || !hidden) return;
    const prev = hidden.value;
    hidden.value = "";
    list.innerHTML = '<div class="car-pick-empty">در حال بارگذاری…</div>';
    try {
      const cars = await Api.carsAvailable();
      if (!cars || !cars.length) {
        list.innerHTML = '<div class="car-pick-empty">ماشینی آزاد نیست</div>';
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
            '<span class="car-pick-name">' +
            escapeHtml(c.name || "—") +
            "</span>" +
            renderIranPlate(plate) +
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
      const hidden = $("#pickupPlate");
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
      toast("کارمند در مأموریت است؛ نمی‌تواند ماشین دیگری بگیرد", "err");
      return;
    }
    const fd = new FormData(e.target);
    const plateVal = (fd.get("plate") || "").toString().trim();
    if (!plateVal) {
      toast("یک ماشین آزاد انتخاب کنید", "err");
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
    if (!returnAuth.renting) {
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
            escapeHtml(r.employeeName) +
            "</td><td>" +
            escapeHtml(r.carName) +
            "</td><td>" +
            renderIranPlate(r.plate) +
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
      tbody.innerHTML = '<tr><td colspan="7">' + escapeHtml(err.message) + "</td></tr>";
      toast(err.message, "err");
    }
  }

  $("#btnRefreshReport").addEventListener("click", loadReport);

  renderAuth("pickup");
  renderAuth("return");
  showView("dashboard");
  loadHealth();
})();
