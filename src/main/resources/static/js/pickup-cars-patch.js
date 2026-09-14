(() => {
  function waitFor(fn, tries) {
    tries = tries == null ? 100 : tries;
    return new Promise(function (resolve, reject) {
      (function tick(n) {
        try {
          if (fn()) return resolve();
        } catch (e) {}
        if (n <= 0) return reject(new Error("timeout waiting for app"));
        setTimeout(function () { tick(n - 1); }, 50);
      })(tries);
    });
  }

  function escapeHtml(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&" + "amp;")
      .replace(/</g, "&" + "lt;")
      .replace(/>/g, "&" + "gt;")
      .replace(/"/g, "&" + "quot;");
  }

  function toPersianDigits(s) {
    var map = "۰۱۲۳۴۵۶۷۸۹";
    return String(s == null ? "" : s).replace(/[0-9]/g, function (d) {
      return map[d.charCodeAt(0) - 48];
    });
  }

  function toLatinDigits(s) {
    var persian = "۰۱۲۳۴۵۶۷۸۹";
    var arabic = "٠١٢٣٤٥٦٧٨٩";
    return String(s == null ? "" : s)
      .split("")
      .map(function (ch) {
        var pi = persian.indexOf(ch);
        if (pi >= 0) return String(pi);
        var ai = arabic.indexOf(ch);
        if (ai >= 0) return String(ai);
        return ch;
      })
      .join("");
  }

  function parsePlate(plate) {
    if (plate == null) return null;
    var s = toLatinDigits(String(plate)).trim();
    if (!s) return null;
    s = s.replace(/\s+/g, "");
    var m = s.match(/^(\d{2})([\u0600-\u06FF]+)(\d{3})\u0627\u06CC\u0631\u0627\u0646(\d{2})$/);
    if (m) return { city: m[4], mid: m[3], letter: m[2], first: m[1] };
    m = s.match(/^(\d{2})([A-Za-z\u0600-\u06FF]+)(\d{3})(?:\u0627\u06CC\u0631\u0627\u0646)?(\d{2})$/);
    if (m) return { first: m[1], letter: m[2], mid: m[3], city: m[4] };
    m = s.match(/^(\d{2})[-\/]?([A-Za-z\u0600-\u06FF]+)[-\/]?(\d{3})[-\/]?(\d{2})$/);
    if (m) return { first: m[1], letter: m[2], mid: m[3], city: m[4] };
    return null;
  }

  function renderIranPlateLocal(plate) {
    var p = parsePlate(plate);
    if (!p) {
      return '<span class="iran-plate-fallback" dir="ltr">' + escapeHtml(plate || "\u2014") + "</span>";
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
      '<span class="iran-plate-iran">\u0627\u06CC\u0631\u0627\u0646</span>' +
      '<span class="iran-plate-city">' +
      escapeHtml(toPersianDigits(p.city)) +
      "</span>" +
      "</span>" +
      "</span>"
    );
  }

  function $(sel) {
    return document.querySelector(sel);
  }

  async function loadAvailablePlates() {
    var list = $("#pickupCarList");
    var hidden = $("#pickupPlate");
    if (!list || !hidden) return;
    var prev = hidden.value;
    hidden.value = "";
    list.innerHTML = '<div class="car-pick-empty">\u062f\u0631 \u062d\u0627\u0644 \u0628\u0627\u0631\u06af\u0630\u0627\u0631\u06cc\u2026</div>';
    try {
      if (typeof Api === "undefined" || !Api.carsAvailable) {
        list.innerHTML = '<div class="car-pick-empty">API \u0622\u0645\u0627\u062f\u0647 \u0646\u06cc\u0633\u062a</div>';
        return;
      }
      var cars = await Api.carsAvailable();
      if (!cars || !cars.length) {
        list.innerHTML = '<div class="car-pick-empty">\u0645\u0627\u0634\u06cc\u0646\u06cc \u0622\u0632\u0627\u062f \u0646\u06cc\u0633\u062a</div>';
        return;
      }
      list.innerHTML = cars
        .map(function (c) {
          var plate = c.plate || "";
          var selected = plate && plate === prev ? " is-selected" : "";
          return (
            '<button type="button" class="car-pick-item' +
            selected +
            '" role="option" data-plate="' +
            escapeHtml(plate) +
            '" aria-selected="' +
            (selected ? "true" : "false") +
            '">' +
            '<span class="car-pick-name">' +
            escapeHtml(c.name || "\u2014") +
            "</span>" +
            renderIranPlateLocal(plate) +
            "</button>"
          );
        })
        .join("");
      if (prev) {
        var items = list.querySelectorAll(".car-pick-item");
        for (var i = 0; i < items.length; i++) {
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
        escapeHtml((err && err.message) || "\u062e\u0637\u0627 \u062f\u0631 \u0628\u0627\u0631\u06af\u0630\u0627\u0631\u06cc") +
        "</div>";
    }
  }

  function bindOnce() {
    var listEl = document.getElementById("pickupCarList");
    if (listEl && !listEl._pickBound) {
      listEl._pickBound = true;
      listEl.addEventListener("click", function (e) {
        var item = e.target.closest(".car-pick-item");
        if (!item) return;
        var list = $("#pickupCarList");
        var hidden = $("#pickupPlate");
        if (!list) return;
        list.querySelectorAll(".car-pick-item").forEach(function (el) {
          el.classList.remove("is-selected");
          el.setAttribute("aria-selected", "false");
        });
        item.classList.add("is-selected");
        item.setAttribute("aria-selected", "true");
        if (hidden) hidden.value = item.getAttribute("data-plate") || "";
      });
    }

    var nav = document.getElementById("nav");
    if (nav && !nav._pickNavBound) {
      nav._pickNavBound = true;
      nav.addEventListener("click", function (e) {
        var btn = e.target.closest(".nav-item");
        if (btn && btn.dataset.view === "rentals") {
          setTimeout(loadAvailablePlates, 30);
        }
      });
    }

    var form = document.getElementById("formPickup");
    if (form && !form._pickSubmitBound) {
      form._pickSubmitBound = true;
      form.addEventListener(
        "submit",
        function (e) {
          var hidden = document.getElementById("pickupPlate");
          var plate = hidden && hidden.value ? String(hidden.value).trim() : "";
          if (!plate) {
            e.preventDefault();
            e.stopImmediatePropagation();
            var t = document.getElementById("toast");
            if (t) {
              t.textContent = "\u06cc\u06a9 \u0645\u0627\u0634\u06cc\u0646 \u0622\u0632\u0627\u062f \u0627\u0646\u062a\u062e\u0627\u0628 \u06a9\u0646\u06cc\u062f";
              t.classList.remove("hidden", "ok");
              t.classList.add("err");
            }
          }
        },
        true
      );
      form.addEventListener("submit", function () {
        setTimeout(loadAvailablePlates, 1200);
      });
    }

    window.loadAvailablePlates = loadAvailablePlates;
  }

  waitFor(function () {
    return typeof Api !== "undefined" && document.getElementById("pickupCarList");
  })
    .then(function () {
      bindOnce();
      loadAvailablePlates();
    })
    .catch(function (err) {
      console.warn("pickup-cars-patch:", err);
      var list = document.getElementById("pickupCarList");
      if (list) {
        list.innerHTML =
          '<div class="car-pick-empty">\u062e\u0637\u0627 \u062f\u0631 \u0622\u0645\u0627\u062f\u0647\u200c\u0633\u0627\u0632\u06cc \u0644\u06cc\u0633\u062a \u0645\u0627\u0634\u06cc\u0646\u200c\u0647\u0627</div>';
      }
    });
})();
