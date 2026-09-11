(() => {
  fetch("/js/app-full.js", { cache: "no-store" })
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
          String((e && e.message) || e) +
          "</p>"
      );
    });
})();
