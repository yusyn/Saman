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
    })
    .catch(function (e) {
      console.error(e);
      document.body.insertAdjacentHTML(
        "afterbegin",
        '<p dir="rtl" style="padding:1rem;background:#400;color:#fff">خطا در بارگذاری app.js: ' +
          String(e && e.message || e) +
          " — از git checkout e5a10648 -- src/main/resources/static/js/app.js استفاده کنید</p>"
      );
    });
})();
