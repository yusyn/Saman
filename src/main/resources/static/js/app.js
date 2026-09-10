(() => {
  const parts = ["/js/part0.js", "/js/part1.js", "/js/part2.js", "/js/part3.js", "/js/part4.js"];
  async function boot() {
    let code = "";
    for (const p of parts) {
      const r = await fetch(p + "?v=2", { cache: "no-store" });
      if (!r.ok) throw new Error("load " + p + " " + r.status);
      code += await r.text();
    }
    (0, eval)("(() => {\n" + code + "\n})();");
  }
  boot().catch(function (e) {
    console.error(e);
    document.body.insertAdjacentHTML("afterbegin",
      '<p dir="rtl" style="padding:1rem;background:#400;color:#fff">خطا در بارگذاری app.js: ' +
      String(e && e.message || e) + "</p>");
  });
})();
