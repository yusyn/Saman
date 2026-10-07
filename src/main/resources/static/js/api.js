/**
 * Thin HTTP client for Saman API (same origin as this page).
 * Works for local laptop and production server without code changes.
 */
const Api = (() => {
  const DEFAULT_TIMEOUT_MS = 15000;
  /** Register/enroll: device + finger can exceed 2 minutes (gate wait + enroll). */
  const ENROLL_TIMEOUT_MS = 180000;
  /** Device name update may hit soft-disable / one retry. */
  const DEVICE_WRITE_TIMEOUT_MS = 60000;

  /** Cached CSRF token for mutating requests (CookieCsrfTokenRepository / X-XSRF-TOKEN). */
  let csrfToken = null;
  let csrfHeaderName = "X-XSRF-TOKEN";

  function readCookie(name) {
    const parts = ("; " + document.cookie).split("; " + name + "=");
    if (parts.length === 2) {
      return decodeURIComponent(parts.pop().split(";").shift() || "");
    }
    return null;
  }

  async function ensureCsrf() {
    const fromCookie = readCookie("XSRF-TOKEN");
    if (fromCookie) {
      csrfToken = fromCookie;
      return csrfToken;
    }
    try {
      const res = await fetch("/api/auth/csrf", {
        method: "GET",
        credentials: "same-origin",
        headers: { Accept: "application/json" },
      });
      if (res.ok) {
        const data = await res.json();
        if (data && data.token) {
          csrfToken = data.token;
          if (data.headerName) csrfHeaderName = data.headerName;
        }
      }
    } catch (_) {
      /* ignore; mutating call may still fail with 403 */
    }
    if (!csrfToken) {
      csrfToken = readCookie("XSRF-TOKEN");
    }
    return csrfToken;
  }

  function isMutating(method) {
    const m = (method || "GET").toUpperCase();
    return m === "POST" || m === "PUT" || m === "PATCH" || m === "DELETE";
  }

  async function request(path, options = {}) {
    const timeoutMs = options.timeoutMs != null ? options.timeoutMs : DEFAULT_TIMEOUT_MS;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);

    const method = (options.method || "GET").toUpperCase();
    const headers = {
      Accept: "application/json",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    };

    if (isMutating(method)) {
      const token = await ensureCsrf();
      if (token) {
        headers[csrfHeaderName] = token;
      }
    }

    const opts = {
      ...options,
      method,
      headers,
      credentials: "same-origin",
      signal: controller.signal,
    };
    delete opts.timeoutMs;

    try {
      const res = await fetch(path, opts);
      const refreshed = readCookie("XSRF-TOKEN");
      if (refreshed) csrfToken = refreshed;
      const text = await res.text();
      let data = null;
      if (text) {
        try {
          data = JSON.parse(text);
        } catch {
          data = text;
        }
      }

      if (!res.ok) {
        const msg =
          (data && (data.message || data.error)) ||
          (typeof data === "string" ? data : null) ||
          "HTTP " + res.status;
        const err = new Error(msg);
        err.status = res.status;
        err.data = data;
        // انقضای session برای UI (مسیرهای /api/auth/* را رد کن)
        if (res.status === 401 && path && path.indexOf("/api/auth/") !== 0) {
          try {
            window.dispatchEvent(
              new CustomEvent("saman:unauthorized", {
                detail: { path: path, method: method },
              })
            );
          } catch (_) { /* ignore */ }
        }
        throw err;
      }
      return data;
    } catch (e) {
      if (e && e.name === "AbortError") {
        throw new Error(
          "پاسخی از سرور نیامد (timeout پس از " +
            Math.round(timeoutMs / 1000) +
            " ثانیه). اگر در حال ثبت اثر انگشت هستید، کمی صبر کنید و لیست را بروزرسانی کنید."
        );
      }
      throw e;
    } finally {
      clearTimeout(timer);
    }
  }

  return {
    health: () => request("/api/health", { timeoutMs: 8000 }),
    login: (username, password) =>
      request("/api/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
      }),
    logout: () => request("/api/auth/logout", { method: "POST", body: "{}" }),
    me: () => request("/api/auth/me"),
    csrf: () => request("/api/auth/csrf"),
    vehicles: () => request("/api/vehicles"),
    vehiclesAvailable: () => request("/api/vehicles/available"),
    createVehicle: (body) =>
      request("/api/vehicles", { method: "POST", body: JSON.stringify(body) }),
    updateVehicle: (body) =>
      request("/api/vehicles", { method: "PUT", body: JSON.stringify(body) }),
    deleteVehicle: (plate) =>
      request("/api/vehicles?plate=" + encodeURIComponent(plate), { method: "DELETE" }),
    /** @deprecated alias */
    cars: () => request("/api/vehicles"),
    carsAvailable: () => request("/api/vehicles/available"),
    createCar: (body) =>
      request("/api/vehicles", { method: "POST", body: JSON.stringify(body) }),
    updateCar: (body) =>
      request("/api/vehicles", { method: "PUT", body: JSON.stringify(body) }),
    deleteCar: (plate) =>
      request("/api/vehicles?plate=" + encodeURIComponent(plate), { method: "DELETE" }),
    employees: () => request("/api/employees"),
    employee: (deviceUserId) =>
      request("/api/employees/" + encodeURIComponent(deviceUserId)),
    registerEmployee: (body) =>
      request("/api/employees/register", {
        method: "POST",
        body: JSON.stringify(body),
        timeoutMs: ENROLL_TIMEOUT_MS,
      }),
    updateEmployee: (deviceUserId, body) =>
      request("/api/employees/" + encodeURIComponent(deviceUserId), {
        method: "PUT",
        body: JSON.stringify(body),
        timeoutMs: DEVICE_WRITE_TIMEOUT_MS,
      }),
    deleteEmployee: (deviceUserId) =>
      request("/api/employees/" + encodeURIComponent(deviceUserId), {
        method: "DELETE",
        timeoutMs: DEVICE_WRITE_TIMEOUT_MS,
      }),
    addFinger: (deviceUserId, body) =>
      request("/api/employees/" + encodeURIComponent(deviceUserId) + "/fingers", {
        method: "POST",
        body: JSON.stringify(body),
        timeoutMs: ENROLL_TIMEOUT_MS,
      }),
    verify: (timeoutSeconds = 40) =>
      request("/api/fingerprint/verify", {
        method: "POST",
        body: JSON.stringify({ timeoutSeconds }),
        timeoutMs: (timeoutSeconds + 15) * 1000,
      }),
    cancelListen: () =>
      request("/api/fingerprint/cancel-listen", { method: "POST", timeoutMs: 5000 }),
    pickup: (body) =>
      request("/api/rentals/pickup", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    returnCar: (body) =>
      request("/api/rentals/return", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    activeRental: (deviceUserId) =>
      request("/api/rentals/active/" + encodeURIComponent(deviceUserId)),
    report: (params = {}) => {
      const q = new URLSearchParams();
      Object.keys(params || {}).forEach((k) => {
        const v = params[k];
        if (v != null && String(v).trim() !== "") q.set(k, String(v).trim());
      });
      const qs = q.toString();
      return request("/api/rentals/report" + (qs ? "?" + qs : ""));
    },
    vehicleHistory: (plate) =>
      request("/api/vehicles/history?plate=" + encodeURIComponent(plate)),
    addVehicleService: (plate, body) =>
      request("/api/vehicles/history/services?plate=" + encodeURIComponent(plate), {
        method: "POST",
        body: JSON.stringify(body),
      }),
    addVehicleIssue: (plate, body) =>
      request("/api/vehicles/history/issues?plate=" + encodeURIComponent(plate), {
        method: "POST",
        body: JSON.stringify(body),
      }),
    updateVehicleIssue: (plate, issueId, body) =>
      request(
        "/api/vehicles/history/issues/" + issueId + "?plate=" + encodeURIComponent(plate),
        { method: "PATCH", body: JSON.stringify(body) }
      ),
    addVehicleFine: (plate, body) =>
      request("/api/vehicles/history/fines?plate=" + encodeURIComponent(plate), {
        method: "POST",
        body: JSON.stringify(body),
      }),
    payVehicleFine: (plate, fineId, paymentDate) => {
      let url =
        "/api/vehicles/history/fines/" +
        fineId +
        "/pay?plate=" +
        encodeURIComponent(plate);
      if (paymentDate) url += "&paymentDate=" + encodeURIComponent(paymentDate);
      return request(url, { method: "PATCH", body: "{}" });
    },
    updateVehicleOdometer: (plate, odometer) =>
      request("/api/vehicles/history/odometer?plate=" + encodeURIComponent(plate), {
        method: "PUT",
        body: JSON.stringify({ odometer }),
      }),
  };
})();
