/**
 * Thin HTTP client for Saman API (same origin as this page).
 * Works for local laptop and production server without code changes.
 */
const Api = (() => {
  const DEFAULT_TIMEOUT_MS = 12000;

  async function request(path, options = {}) {
    const timeoutMs = options.timeoutMs != null ? options.timeoutMs : DEFAULT_TIMEOUT_MS;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);

    const opts = {
      headers: {
        Accept: "application/json",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...options.headers,
      },
      ...options,
      signal: controller.signal,
    };
    delete opts.timeoutMs;

    try {
      const res = await fetch(path, opts);
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
        throw err;
      }
      return data;
    } catch (e) {
      if (e && e.name === "AbortError") {
        throw new Error("پاسخی از سرور نیامد (timeout)");
      }
      throw e;
    } finally {
      clearTimeout(timer);
    }
  }

  return {
    health: () => request("/api/health", { timeoutMs: 8000 }),
    cars: () => request("/api/cars"),
    carsAvailable: () => request("/api/cars/available"),
    createCar: (body) =>
      request("/api/cars", { method: "POST", body: JSON.stringify(body) }),
    updateCar: (body) =>
      request("/api/cars", { method: "PUT", body: JSON.stringify(body) }),
    deleteCar: (plate) =>
      request("/api/cars?plate=" + encodeURIComponent(plate), { method: "DELETE" }),
    employees: () => request("/api/employees"),
    employee: (deviceUserId) =>
      request("/api/employees/" + encodeURIComponent(deviceUserId)),
    registerEmployee: (body) =>
      request("/api/employees/register", {
        method: "POST",
        body: JSON.stringify(body),
      }),
    updateEmployee: (deviceUserId, body) =>
      request("/api/employees/" + encodeURIComponent(deviceUserId), {
        method: "PUT",
        body: JSON.stringify(body),
      }),
    deleteEmployee: (deviceUserId) =>
      request("/api/employees/" + encodeURIComponent(deviceUserId), {
        method: "DELETE",
      }),
    verify: (timeoutSeconds = 40) =>
      request("/api/fingerprint/verify", {
        method: "POST",
        body: JSON.stringify({ timeoutSeconds }),
        timeoutMs: (timeoutSeconds + 10) * 1000,
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
    report: () => request("/api/rentals/report"),
  };
})();
