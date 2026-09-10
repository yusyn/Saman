(() => {
  const parts = ["/js/app_part_0.js", "/js/app_part_1.js", "/js/app_part_2.js", "/js/app_part_3.js", "/js/app_part_4.js"];
  async function boot() {
    let code = "";
    for (const p of parts) {
      const r = await fetch(p, { cache: "no-store" });
      if (!r.ok) throw new Error("load " + p + " " + r.status);
      code += await r.text();
    }
    (0, eval)("(() => {\n" + code + "\n})();");
  }
  boot().catch(function (e) {
    console.error(e);
    document.body.insertAdjacentHTML("afterbegin",
      "<p dir=\"rtl\" style=\"padding:1rem;background:#400;color:#fff\">خطا در بارگذاری app.js: " +
      String(e.message || e) + "</p>");
  });
})();
