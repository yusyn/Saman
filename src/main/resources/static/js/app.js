(() => {
  const titles = {
    dashboard: ["وضعیت", "سلامت API و راهنما"],
    cars: ["ماشین‌ها", "لیست و ثبت ماشین"],
    employees: ["کارمندان", "لیست و ثبت با اثر انگشت"],
    rentals: ["تحویل / برگشت", "احراز، تحویل و برگشت"],
    report: ["گزارش", "سفرهای ثبت‌شده"],
  };

  const $ = (sel) => document.querySelector(sel);
  const toastEl = $("#toast");

  function toast(message, type = "ok") {
    toastEl.textContent = message;
    toastEl.classList.remove("hidden", "ok", "err");
    toastEl.classList.add(type === "err" ? "err" : "ok");
    clearTimeout(toastEl._t);
    toastEl._t = setTimeout(() => toastEl.classList.add("hidden"), 4500);
  }

  function showView(name) {
    document.querySelectorAll(".view").forEach((v) => v.classList.remove("active"));
    document.querySelectorAll(".nav-item").forEach((b) => b.classList.remove("active"));
    const view = document.getElementById("view-" + name);
    const btn = document.querySelector(`.nav-item[data-view="${name}"]`);
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

  function escapeHtml(s) {
    return String(s ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  async function loadCars() {
    const tbody = $("#carsTable");
    tbody.innerHTML = "<tr><td colspan=\"4\">در حال بارگذاری…</td></tr>";
    try {
      const rows = await Api.cars();
      if (!rows.length) {
        tbody.innerHTML = "<tr><td colspan=\"4\">ماشینی ثبت نشده</td></tr>";
        return;
      }
      tbody.innerHTML = rows
        .map(
          (c) => `<tr>
          <td>${escapeHtml(c.name)}</td>
          <td dir="ltr">${escapeHtml(c.plate)}</td>
          <td>${escapeHtml(c.color)}</td>
          <td>${escapeHtml(c.status || "—")}</td>
        </tr>`
        )
        .join("");
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan=\"4\">${escapeHtml(err.message)}</td></tr>`;
      toast(err.message, "err");
    }
  }

  $("#btnRefreshCars").addEventListener("click", loadCars);

  $("#formCar").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const body = {
      name: fd.get("name").toString().trim(),
      plate: fd.get("plate").toString().trim(),
      color: fd.get("color").toString().trim(),
    };
    const btn = e.target.querySelector("[type=submit]");
    btn.disabled = true;
    try {
      await Api.createCar(body);
      toast("ماشین ثبت شد");
      e.target.reset();
      await loadCars();
    } catch (err) {
      toast(err.message || "خطا در ثبت ماشین", "err");
    } finally {
      btn.disabled = false;
    }
  });

  async function loadEmployees() {
    const tbody = $("#employeesTable");
    tbody.innerHTML = "<tr><td colspan=\"4\">در حال بارگذاری…</td></tr>";
    try {
      const rows = await Api.employees();
      if (!rows.length) {
        tbody.innerHTML = "<tr><td colspan=\"4\">کارمندی ثبت نشده</td></tr>";
        return;
      }
      tbody.innerHTML = rows
        .map(
          (r) => `<tr>
          <td dir="ltr">${escapeHtml(r.deviceUserId)}</td>
          <td dir="ltr">${escapeHtml(r.name)}</td>
          <td dir="ltr">${escapeHtml(r.phone || "")}</td>
          <td>${r.renting ? "بله" : "خیر"}</td>
        </tr>`
        )
        .join("");
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan=\"4\">${escapeHtml(err.message)}</td></tr>`;
      toast(err.message, "err");
    }
  }

  $("#btnRefreshEmployees").addEventListener("click", loadEmployees);

  $("#formEmployee").addEventListener("submit", async (e) => {
    e.preventDefault();
    const fd = new FormData(e.target);
    const idRaw = fd.get("deviceUserId").toString().trim();
    const body = {
      name: fd.get("name").toString().trim(),
      phone: fd.get("phone").toString().trim(),
      fingerIndex: Number(fd.get("fingerIndex")),
    };
    if (idRaw) body.deviceUserId = idRaw;

    const btn = $("#btnRegisterEmp");
    const status = $("#empRegisterStatus");
    btn.disabled = true;
    status.textContent = "در حال ارتباط با دستگاه اثر انگشت… انگشت را روی سنسور بگذارید.";
    try {
      const saved = await Api.registerEmployee(body);
      status.textContent = "ثبت شد: " + (saved.deviceUserId || "");
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
        .map(
          (c) =>
            `<option value="${escapeHtml(c.plate)}">${escapeHtml(c.name)} — ${escapeHtml(c.plate)}</option>`
        )
        .join("");
    } catch (err) {
      sel.innerHTML = `<option value="">${escapeHtml(err.message)}</option>`;
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
        .map(
          (r) => `<tr>
          <td dir="ltr">${escapeHtml(r.deviceUserId)}</td>
          <td>${escapeHtml(r.employeeName)}</td>
          <td>${escapeHtml(r.carName)}</td>
          <td dir="ltr">${escapeHtml(r.plate)}</td>
          <td>${escapeHtml(r.destination)}</td>
          <td dir="ltr">${escapeHtml(r.pickupDate)}</td>
          <td dir="ltr">${escapeHtml(r.returnDate)}</td>
        </tr>`
        )
        .join("");
    } catch (err) {
      tbody.innerHTML = `<tr><td colspan=\"7\">${escapeHtml(err.message)}</td></tr>`;
      toast(err.message, "err");
    }
  }

  $("#btnRefreshReport").addEventListener("click", loadReport);

  showView("dashboard");
  loadHealth();
})();
