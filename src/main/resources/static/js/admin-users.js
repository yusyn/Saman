(() => {
  const $ = (id) => document.getElementById(id);
  let accounts = [];

  function message(id, text, error = false) {
    const el = $(id);
    if (!el) return;
    el.textContent = text || "";
    el.classList.toggle("form-error", !!error && !!text);
  }

  function errorText(err) {
    if (err && err.status === 401) return "نشست شما منقضی شده است؛ دوباره وارد شوید.";
    if (err && err.status === 403) return "دسترسی به مدیریت مدیران مجاز نیست.";
    return (err && err.message) || "ارتباط با سرور برقرار نشد.";
  }

  function addButton(cell, text, action, id, disabled = false) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "btn btn-ghost btn-sm";
    button.textContent = text;
    button.dataset.adminAction = action;
    button.dataset.adminId = String(id);
    button.disabled = disabled;
    cell.appendChild(button);
  }

  function render() {
    const body = $("adminUsersTable");
    body.replaceChildren();
    const current = document.getElementById("authStatus")?.dataset.username || "";
    accounts.forEach((account) => {
      const row = document.createElement("tr");
      const username = document.createElement("td");
      username.textContent = account.username;
      const state = document.createElement("td");
      state.textContent = account.enabled ? "فعال" : "غیرفعال";
      const actions = document.createElement("td");
      actions.className = "admin-account-actions";
      addButton(actions, "تغییر نام", "rename", account.id);
      addButton(actions, "تغییر رمز", "password", account.id);
      addButton(actions, account.enabled ? "غیرفعال‌کردن" : "فعال‌کردن", "enabled", account.id,
        account.enabled && account.username === current);
      row.append(username, state, actions);
      body.appendChild(row);
    });
    message("adminListMessage", accounts.length ? `${accounts.length} حساب مدیر` : "حساب مدیری ثبت نشده است.");
  }

  async function loadAdminUsers() {
    message("adminListMessage", "در حال بارگذاری فهرست مدیران…");
    try {
      accounts = await Api.adminUsers();
      render();
    } catch (err) {
      accounts = [];
      $("adminUsersTable").replaceChildren();
      message("adminListMessage", errorText(err), true);
    }
  }
  window.loadAdminUsers = loadAdminUsers;

  $("btnRefreshAdmins").addEventListener("click", loadAdminUsers);

  $("formCreateAdmin").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const username = $("newAdminUsername").value.trim();
    const password = $("newAdminPassword").value;
    const confirmation = $("newAdminPasswordConfirm").value;
    if (password !== confirmation) return message("createAdminMessage", "تکرار رمز عبور یکسان نیست.", true);
    const button = $("btnCreateAdmin");
    button.disabled = true;
    message("createAdminMessage", "در حال ساخت حساب…");
    try {
      await Api.createAdminUser({ username, password });
      form.reset();
      message("createAdminMessage", "حساب مدیر ساخته شد.");
      await loadAdminUsers();
    } catch (err) {
      message("createAdminMessage", errorText(err), true);
    } finally {
      $("newAdminPassword").value = "";
      $("newAdminPasswordConfirm").value = "";
      button.disabled = false;
    }
  });

  $("adminUsersTable").addEventListener("click", async (event) => {
    const button = event.target.closest("button[data-admin-action]");
    if (!button) return;
    const account = accounts.find((candidate) => String(candidate.id) === button.dataset.adminId);
    if (!account) return;
    if (button.dataset.adminAction === "rename") {
      $("renameAdminId").value = String(account.id);
      $("renameAdminUsername").value = account.username;
      message("renameAdminMessage", "");
      document.getElementById("modalRenameAdmin").classList.remove("hidden");
    } else if (button.dataset.adminAction === "password") {
      $("passwordAdminId").value = String(account.id);
      $("adminNewPassword").value = "";
      $("adminNewPasswordConfirm").value = "";
      message("adminPasswordMessage", "");
      document.getElementById("modalAdminPassword").classList.remove("hidden");
    } else if (button.dataset.adminAction === "enabled") {
      const enabled = !account.enabled;
      if (!window.confirm(enabled ? `حساب «${account.username}» فعال شود؟` : `حساب «${account.username}» غیرفعال شود؟`)) return;
      button.disabled = true;
      message("adminListMessage", "در حال ذخیره تغییر…");
      try {
        await Api.setAdminEnabled(account.id, enabled);
        await loadAdminUsers();
      } catch (err) {
        message("adminListMessage", errorText(err), true);
        button.disabled = false;
      }
    }
  });

  $("formRenameAdmin").addEventListener("submit", async (event) => {
    event.preventDefault();
    const id = Number($("renameAdminId").value);
    const previous = accounts.find((account) => account.id === id)?.username;
    const button = $("btnRenameAdmin");
    button.disabled = true;
    message("renameAdminMessage", "در حال ذخیره…");
    try {
      const result = await Api.renameAdminUser(id, $("renameAdminUsername").value.trim());
      document.getElementById("modalRenameAdmin").classList.add("hidden");
      if (result.reauthenticationRequired || previous === currentUsername()) {
        window.dispatchEvent(new CustomEvent("saman:refresh-auth"));
      } else {
        await loadAdminUsers();
      }
    } catch (err) {
      message("renameAdminMessage", errorText(err), true);
    } finally {
      button.disabled = false;
    }
  });

  $("formAdminPassword").addEventListener("submit", async (event) => {
    event.preventDefault();
    const id = Number($("passwordAdminId").value);
    const password = $("adminNewPassword").value;
    const confirmation = $("adminNewPasswordConfirm").value;
    if (password !== confirmation) return message("adminPasswordMessage", "تکرار رمز عبور یکسان نیست.", true);
    const button = $("btnSaveAdminPassword");
    button.disabled = true;
    message("adminPasswordMessage", "در حال تغییر رمز…");
    try {
      const result = await Api.changeAdminPassword(id, password);
      document.getElementById("modalAdminPassword").classList.add("hidden");
      if (result.reauthenticationRequired) window.dispatchEvent(new CustomEvent("saman:refresh-auth"));
      else message("adminListMessage", "رمز عبور مدیر تغییر کرد.");
    } catch (err) {
      message("adminPasswordMessage", errorText(err), true);
    } finally {
      $("adminNewPassword").value = "";
      $("adminNewPasswordConfirm").value = "";
      button.disabled = false;
    }
  });

  function currentUsername() {
    return document.getElementById("authStatus")?.dataset.username || "";
  }
})();
