(() => {
  const titles = {
    dashboard: ["وضعیت", "سلامت API و راهنما"],
    cars: ["ماشین‌ها", "لیست، ثبت، ویرایش و حذف"],
    employees: ["کارمندان", "لیست، ثبت، ویرایش و حذف"],
    rentals: ["تحویل / برگشت", "احراز، تحویل و برگشت"],
    report: ["گزارش", "سفرهای ثبت‌شده"],
  };

  const $ = (sel) => document.querySelector(sel);
  const toastEl = $("#toast");

  let carsCache = [];
  let employeesCache = [];

  function toast(message, type = "ok") {
    toastEl.textContent = message;
    toastEl.classList.remove("hidden", "ok", "err");
    toastEl.classList.add(type === "err" ? "err" : "ok");
    clearTimeout(toastEl._t);
    toastEl._t = setTimeout(() => toastEl.classList.add("hidden"), 4500);
  }

  // Build entities without raw & in source (avoids tool/XML stripping)
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

  function isOnMissionStatus(status) {
    if (!status) return false;
    return status.includes("مأموریت") || status.includes("ماموریت");
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
    try {
      const h = await Api.health();
      pre.textContent = JSON.stringify(h, null, 2);
      badge.textContent = h.status === "UP" ? "API: فعال" : "API: " + h.status;
      badge.classList.toggle("up", h.status === "UP");
      badge.classList.toggle("down", h.status !== "UP");
    } catch (err) {
      pre.textContent = String(err.message || err);
      badge.textContent = "API: قطع";
      badge.classList.add("down");
      badge.classList.remove("up");
    }
  }

  $("#btnRefreshHealth").addEventListener("click", loadHealth);

  function getPlateFromIds(firstId, letterId, midId, cityId) {
    const first = ($(firstId).value || "").trim();
    const letter = ($(letterId).value || "").trim();
    const mid = ($(midId).value || "").trim();
    const city = ($(cityId).value || "").trim();
    if (!/^[0-9]{2}$/.test(first) || !letter || !/^[0-9]{3}$/.test(mid) || !/^[0-9]{2}$/.test(city)) {
      return null;
    }
    return first + letter + mid + "ایران" + city;
  }

  function setPlateToIds(plate, firstId, letterId, midId, cityId) {
    const raw = (plate || "").replace(/\s+/g, "");
    const m = raw.match(/^(\d{2})(.+?)(\d{3})ایران(\d{2})$/);
    if (!m) {
      $(firstId).value = "";
      $(midId).value = "";
      $(cityId).value = "";
      return;
    }
    $(firstId).value = m[1];
    $(midId).value = m[3];
    $(cityId).value = m[4];
    let letter = m[2];
    if (letter === "ه") letter = "هـ";
    const sel = $(letterId);
    let found = false;
    for (let i = 0; i < sel.options.length; i++) {
      if (sel.options[i].value === letter) {
        sel.selectedIndex = i;
        found = true;
        break;
      }
    }
    if (!found) sel.selectedIndex = 9;
  }

  function clearPlateWidget() {
    $("#plateFirst").value = "";
    $("#plateMid").value = "";
    $("#plateCity").value = "";
    $("#plateLetter").selectedIndex = 9;
  }

  ["plateFirst", "plateMid", "plateCity", "editPlateFirst", "editPlateMid", "editPlateCity"].forEach((id) => {
    const el = document.getElementById(id);
    if (!el) return;
    el.addEventListener("input", () => {
      el.value = el.value.replace(/\D/g, "");
    });
  });

  async function loadCars() {
    const tbody = $("#carsTable");
    tbody.innerHTML = "<tr><td colspan=\"5\">در حال بارگذاری…</td></tr>";
    try {
      const rows = await Api.cars();
      carsCache = rows || [];
      if (!rows.length) {
        tbody.innerHTML = "<tr><td colspan=\"5\">ماشینی ثبت نشده</td></tr>";
        return;
      }
      tbody.innerHTML = rows
        .map((c, idx) => {
          const busy = isOnMissionStatus(c.status);
          const disabled = busy ? "disabled" : "";
          const title = busy ? "در مأموریت — قابل ویرایش/حذف نیست" : "";
          return "<tr data-idx=\"" + idx + "\">" +
            "<td>" + escapeHtml(c.name) + "</td>" +
            "<td dir=\"ltr\">" + escapeHtml(c.plate) + "</td>" +
            "<td>" + escapeHtml(c.color) + "</td>" +
            "<td>" + escapeHtml(c.status || "—") + "</td>" +
            "<td class=\"actions\">" +
            "<button type=\"button\" class=\"btn btn-sm\" data-action=\"edit-car\" data-idx=\"" + idx + "\" " + disabled + " title=\"" + title + "\">ویرایش</button> " +
            "<button type=\"button\" class=\"btn btn-sm btn-danger\" data-action=\"del-car\" data-idx=\"" + idx + "\" " + disabled + " title=\"" + title + "\">حذف</button>" +
            "</td></tr>";
        })
        .join("");
    } catch (err) {
      tbody.innerHTML = "<tr><td colspan=\"5\">" + escapeHtml(err.message) + "</td></tr>";
      toast(err.message, "err");
    }
  }

  $("#btnRefreshCars").addEventListener("click", loadCars);

  $("#carsTable").addEventListener("click", async (e) => {
    const btn = e.target.closest("[data-action]");
    if (!btn || btn.disabled) return;
    const idx = Number(btn.dataset.idx);
    const car = carsCache[idx];
    if (!car) return;

    if (btn.dataset.action === "edit-car") {
      $("#editCarOldPlate").value = car.plate || "";
      $("#editCarName").value = car.name || "";
      $("#editCarColor").value = car.color || "";
      setPlateToIds(car.plate, "#editPlateFirst", "#editPlateLetter", "#editPlateMid", "#editPlateCity");
      openModal("modalCar");
      return;
    }

    if (btn.dataset.action === "del-car") {
      if (!confirm("ماشین «" + car.name + "» با پلاک " + car.plate + " حذف شود؟")) return;
      btn.disabled = true;
      try {
        await Api.deleteCar(car.plate);
        toast("ماشین حذف شد");
        await loadCars();
      } catch (err) {
        toast(err.message || "خطا در حذف", "err");
        btn.disabled = false;
      }
    }
  });

  $("#formCar").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const plate = getPlateFromIds("#plateFirst", "#plateLetter", "#plateMid", "#plateCity");
    if (!plate) {
      toast("پلاک ناقص است. مثال: 32 ل 316 ایران 53", "err");
      return;
    }
    const body = {
      name: fd.get("name").toString().trim(),
      plate: plate,
      color: fd.get("color").toString().trim(),
    };
    const btn = e.target.querySelector("[type=submit]");
    btn.disabled = true;
    try {
      await Api.createCar(body);
      toast("ماشین ثبت شد");
      e.target.reset();
      clearPlateWidget();
      await loadCars();
    } catch (err) {
      toast(err.message || "خطا در ثبت ماشین", "err");
    } finally {
      btn.disabled = false;
    }
  });

  $("#formEditCar").addEventListener("submit", async (e) => {
    e.preventDefault();
    const plate = getPlateFromIds("#editPlateFirst", "#editPlateLetter", "#editPlateMid", "#editPlateCity");
    if (!plate) {
      toast("پلاک ناقص است", "err");
      return;
    }
    const body = {
      oldPlate: $("#editCarOldPlate").value,
      name: $("#editCarName").value.trim(),
      plate: plate,
      color: $("#editCarColor").value.trim(),
    };
    const btn = e.target.querySelector("[type=submit]");
    btn.disabled = true;
    try {
      await Api.updateCar(body);
      toast("تغییرات ماشین ذخیره شد");
      closeModal("modalCar");
      await loadCars();
    } catch (err) {
      toast(err.message || "خطا در ویرایش ماشین", "err");
    } finally {
      btn.disabled = false;
    }
  });

  async function loadEmployees() {
    const tbody = $("#employeesTable");
    tbody.innerHTML = "<tr><td colspan=\"5\">در حال بارگذاری…</td></tr>";
    try {
      const rows = await Api.employees();
      employeesCache = rows || [];
      if (!rows.length) {
        tbody.innerHTML = "<tr><td colspan=\"5\">کارمندی ثبت نشده</td></tr>";
        return;
      }
      tbody.innerHTML = rows
        .map((r, idx) => {
          const busy = !!r.renting;
          const disabled = busy ? "disabled" : "";
          const title = busy ? "در مأموریت — قابل ویرایش/حذف نیست" : "";
          return "<tr data-idx=\"" + idx + "\">" +
            "<td dir=\"ltr\">" + escapeHtml(r.deviceUserId) + "</td>" +
            "<td dir=\"ltr\">" + escapeHtml(r.name) + "</td>" +
            "<td dir=\"ltr\">" + escapeHtml(r.phone || "") + "</td>" +
            "<td>" + (r.renting ? "بله" : "خیر") + "</td>" +
            "<td class=\"actions\">" +
            "<button type=\"button\" class=\"btn btn-sm\" data-action=\"edit-emp\" data-idx=\"" + idx + "\" " + disabled + " title=\"" + title + "\">ویرایش</button> " +
            "<button type=\"button\" class=\"btn btn-sm btn-danger\" data-action=\"del-emp\" data-idx=\"" + idx + "\" " + disabled + " title=\"" + title + "\">حذف</button>" +
            "</td></tr>";
        })
        .join("");
    } catch (err) {
      tbody.innerHTML = "<tr><td colspan=\"5\">" + escapeHtml(err.message) + "</td></tr>";
      toast(err.message, "err");
    }
  }

  $("#btnRefreshEmployees").addEventListener("click", loadEmployees);

  $("#employeesTable").addEventListener("click", async (e) => {
    const btn = e.target.closest("[data-action]");
    if (!btn || btn.disabled) return;
    const idx = Number(btn.dataset.idx);
    const emp = employeesCache[idx];
    if (!emp) return;

    if (btn.dataset.action === "edit-emp") {
      $("#editEmpId").value = emp.deviceUserId || "";
      $("#editEmpName").value = emp.name || "";
      $("#editEmpPhone").value = emp.phone || "";
      openModal("modalEmployee");
      return;
    }

    if (btn.dataset.action === "del-emp") {
      if (!confirm("کارمند «" + emp.name + "» (" + emp.deviceUserId + ") حذف شود؟")) return;
      btn.disabled = true;
      try {
        await Api.deleteEmployee(emp.deviceUserId);
        toast("کارمند حذف شد");
        await loadEmployees();
      } catch (err) {
        toast(err.message || "خطا در حذف", "err");
        btn.disabled = false;
      }
    }
  });

  $("#formEmployee").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const body = {
      name: fd.get("name").toString().trim(),
      phone: fd.get("phone").toString().trim(),
      fingerIndex: Number(fd.get("fingerIndex")),
    };

    const btn = $("#btnRegisterEmp");
    const status = $("#empRegisterStatus");
    btn.disabled = true;
    status.textContent = "در حال ارتباط با دستگاه اثر انگشت… انگشت را روی سنسور بگذارید.";
    try {
      const saved = await Api.registerEmployee(body);
      status.textContent = "ثبت شد — شناسه: " + (saved.deviceUserId || "");
      toast("کارمند با اثر انگشت ثبت شد");
      e.target.reset();
      await loadEmployees();
    } catch (err) {
      status.textContent = err.message || "ناموفق";
      toast(err.message || "خطا در ثبت کارمند", "err");
    } finally {
      btn.disabled = false;
    }
  });

  $("#formEditEmployee").addEventListener("submit", async (e) => {
    e.preventDefault();
    const id = $("#editEmpId").value.trim();
    const body = {
      name: $("#editEmpName").value.trim(),
      phone: $("#editEmpPhone").value.trim(),
    };
    const btn = e.target.querySelector("[type=submit]");
    btn.disabled = true;
    try {
      await Api.updateEmployee(id, body);
      toast("تغییرات کارمند ذخیره شد");
      closeModal("modalEmployee");
      await loadEmployees();
    } catch (err) {
      toast(err.message || "خطا در ویرایش کارمند", "err");
    } finally {
      btn.disabled = false;
    }
  });

  async function loadAvailablePlates() {
    const sel = $("#pickupPlate");
    sel.innerHTML = "<option value=\"\">در حال بارگذاری…</option>";
    try {
      const cars = await Api.carsAvailable();
      if (!cars.length) {
        sel.innerHTML = "<option value=\"\">ماشینی آزاد نیست</option>";
        return;
      }
      sel.innerHTML = cars
        .map(function (c) {
          return "<option value=\"" + escapeHtml(c.plate) + "\">" +
            escapeHtml(c.name) + " — " + escapeHtml(c.plate) + "</option>";
        })
        .join("");
    } catch (err) {
      sel.innerHTML = "<option value=\"\">" + escapeHtml(err.message) + "</option>";
    }
  }

  $("#btnVerify").addEventListener("click", async () => {
    const pre = $("#verifyResult");
    const btn = $("#btnVerify");
    btn.disabled = true;
    pre.textContent = "منتظر اثر انگشت روی دستگاه…";
    try {
      const r = await Api.verify(40);
      pre.textContent = JSON.stringify(r, null, 2);
      if (r.deviceUserId) {
        $("#pickupUserId").value = r.deviceUserId;
        $("#returnUserId").value = r.deviceUserId;
      }
      toast("احراز موفق: " + (r.deviceUserId || ""));
    } catch (err) {
      pre.textContent = err.message || String(err);
      toast(err.message || "احراز ناموفق", "err");
    } finally {
      btn.disabled = false;
    }
  });

  $("#formPickup").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const body = {
      deviceUserId: fd.get("deviceUserId").toString().trim(),
      plate: fd.get("plate").toString().trim(),
      destination: fd.get("destination").toString().trim(),
    };
    const btn = e.target.querySelector("[type=submit]");
    btn.disabled = true;
    try {
      const r = await Api.pickup(body);
      toast(r.message || "تحویل ثبت شد");
      await loadAvailablePlates();
    } catch (err) {
      toast(err.message || "خطا در تحویل", "err");
    } finally {
      btn.disabled = false;
    }
  });

  $("#formReturn").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const body = {
      deviceUserId: fd.get("deviceUserId").toString().trim(),
    };
    const btn = e.target.querySelector("[type=submit]");
    btn.disabled = true;
    try {
      const r = await Api.returnCar(body);
      toast(r.message || (r.ok === false ? "ناموفق" : "برگشت ثبت شد"), r.ok === false ? "err" : "ok");
      await loadAvailablePlates();
    } catch (err) {
      toast(err.message || "خطا در برگشت", "err");
    } finally {
      btn.disabled = false;
    }
  });

  async function loadReport() {
    const tbody = $("#reportTable");
    tbody.innerHTML = "<tr><td colspan=\"7\">در حال بارگذاری…</td></tr>";
    try {
      const rows = await Api.report();
      if (!rows.length) {
        tbody.innerHTML = "<tr><td colspan=\"7\">گزارشی نیست</td></tr>";
        return;
      }
      tbody.innerHTML = rows
        .map(function (r) {
          return "<tr>" +
            "<td dir=\"ltr\">" + escapeHtml(r.deviceUserId) + "</td>" +
            "<td>" + escapeHtml(r.employeeName) + "</td>" +
            "<td>" + escapeHtml(r.carName) + "</td>" +
            "<td dir=\"ltr\">" + escapeHtml(r.plate) + "</td>" +
            "<td>" + escapeHtml(r.destination) + "</td>" +
            "<td dir=\"ltr\">" + escapeHtml(r.pickupDate) + "</td>" +
            "<td dir=\"ltr\">" + escapeHtml(r.returnDate) + "</td>" +
            "</tr>";
        })
        .join("");
    } catch (err) {
      tbody.innerHTML = "<tr><td colspan=\"7\">" + escapeHtml(err.message) + "</td></tr>";
      toast(err.message, "err");
    }
  }

  $("#btnRefreshReport").addEventListener("click", loadReport);

  showView("dashboard");
  loadHealth();
})();
