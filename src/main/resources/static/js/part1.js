  function getPlateFromIds(firstId, letterId, midId, cityId) {
    const first = ($(firstId).value || "").trim();
    const letter = ($(letterId).value || "").trim();
    const mid = ($(midId).value || "").trim();
    const city = ($(cityId).value || "").trim();
    if (!/^[0-9]{2}$/.test(first) || !letter || !/^[0-9]{3}$/.test(mid) || !/^[0-9]{2}$/.test(city)) return null;
    return first + letter + mid + "ایران" + city;
  }

  function setPlateToIds(plate, firstId, letterId, midId, cityId) {
    const p = parsePlate(plate);
    if (!p) {
      $(firstId).value = "";
      $(midId).value = "";
      $(cityId).value = "";
      return;
    }
    $(firstId).value = p.first;
    $(midId).value = p.mid;
    $(cityId).value = p.city;
    let letter = p.letter;
    if (letter === "ه") letter = "هـ";
    const sel = $(letterId);
    let found = false;
    for (let i = 0; i < sel.options.length; i++) {
      if (sel.options[i].value === letter || sel.options[i].text === letter) {
        sel.selectedIndex = i;
        found = true;
        break;
      }
    }
    if (!found) {
      for (let i = 0; i < sel.options.length; i++) {
        if (sel.options[i].value.indexOf(letter) >= 0 || letter.indexOf(sel.options[i].value) >= 0) {
          sel.selectedIndex = i;
          found = true;
          break;
        }
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
    tbody.innerHTML = '<tr><td colspan="5">در حال بارگذاری…</td></tr>';
    try {
      const rows = await Api.cars();
      carsCache = rows || [];
      if (!rows.length) {
        tbody.innerHTML = '<tr><td colspan="5">ماشینی ثبت نشده</td></tr>';
        return;
      }
      tbody.innerHTML = rows.map(function (c, idx) {
        const busy = isOnMissionStatus(c.status);
        const disabled = busy ? "disabled" : "";
        const title = busy ? "در مأموریت — قابل ویرایش/حذف نیست" : "";
        return (
          '<tr data-idx="' + idx + '">' +
          "<td>" + escapeHtml(c.name) + "</td>" +
          "<td>" + renderIranPlate(c.plate) + "</td>" +
          "<td>" + escapeHtml(c.color) + "</td>" +
          "<td>" + carStatusBadge(c.status) + "</td>" +
          '<td class="actions">' +
          '<button type="button" class="btn btn-sm" data-action="edit-car" data-idx="' +
          idx +
          '" ' +
          disabled +
          ' title="' +
          title +
          '">ویرایش</button> ' +
          '<button type="button" class="btn btn-sm btn-danger" data-action="del-car" data-idx="' +
          idx +
          '" ' +
          disabled +
          ' title="' +
          title +
          '">حذف</button></td></tr>'
        );
      }).join("");
    } catch (err) {
      tbody.innerHTML = '<tr><td colspan="5">' + escapeHtml(err.message) + "</td></tr>";
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
    tbody.innerHTML = '<tr><td colspan="5">در حال بارگذاری…</td></tr>';
    try {
      const rows = await Api.employees();
      employeesCache = rows || [];
      if (!rows.length) {
        tbody.innerHTML = '<tr><td colspan="5">کارمندی ثبت نشده</td></tr>';
        return;
      }
      tbody.innerHTML = rows.map(function (r, idx) {
        const busy = !!r.renting;
        const disabled = busy ? "disabled" : "";
        const title = busy ? "در مأموریت — قابل ویرایش/حذف نیست" : "";
        return (
          '<tr data-idx="' + idx + '">' +
          '<td dir="ltr">' + escapeHtml(r.deviceUserId) + "</td>" +
          '<td dir="ltr">' + escapeHtml(r.name) + "</td>" +
          '<td dir="ltr">' + escapeHtml(r.phone || "") + "</td>" +
          "<td>" + employeeRentingBadge(r.renting) + "</td>" +
          '<td class="actions">' +
