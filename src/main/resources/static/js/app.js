(() => {
  const URL =
    "https://raw.githubusercontent.com/yusyn/Saman/e5a10648df39cca704a2aaf11ca4181569683174/src/main/resources/static/js/app.js";
  fetch(URL, { cache: "no-store" })
    .then(function (r) {
      if (!r.ok) throw new Error("HTTP " + r.status);
      return r.text();
    })
    .then(function (code) {
      (0, eval)(code);
      // patch: available cars list with iran-plate after base app loads
      if (typeof window !== "undefined") {
        try {
          var s = document.createElement("script");
          s.src = "/js/pickup-cars-patch.js";
          s.defer = true;
          document.body.appendChild(s);
        } catch (e) {}
      }
    })
    .catch(function (e) {
      console.error(e);
      document.body.insertAdjacentHTML(
        "afterbegin",
        '<p dir="rtl" style="padding:1rem;background:#400;color:#fff">خطا در بارگذاری app.js: ' +
          String((e && e.message) || e) +
          "</p>"
      );
    });
})();
