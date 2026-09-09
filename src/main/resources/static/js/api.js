/**
 * Thin HTTP client for Saman API (same origin as this page).
 */
const Api = (() => {
  async function request(path, options = {}) {
    const opts = {
      headers: {
        Accept: "application/json",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...options.headers,
      },
      ...options,
    };

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
        `HTTP ${res.status}`;
      const err = new Error(msg);
      err.status = res.status;
      err.data = data;
      throw err;
    }
    return data;
  }

  return {
    health: () => request("/api/health"),
    cars: () => request("/api/cars"),
    carsAvailable: () => request("/api/cars/available"),
    createCar: (body) =>
      request("/api/cars", { method: "POST", body: JSON.stringify(body) }),
    employees: () => request("/api/employees"),
    employee: (id) => request("/api/employees/" + encodeURIComponent(id)),
    registerEmployee: (body, signal) =>
      request("/api/employees/register", {
        method: "POST",
        body: JSON.stringify(body),
        signal,
      }),
    deleteEmployee: (id) =>
      request("/api/employees/" + encodeURIComponent(id), { method: "DELETE" }),
    addFinger: (id, fingerIndex, signal) =>
      request("/api/employees/" + encodeURIComponent(id) + "/fingers", {
        method: "POST",
        body: JSON.stringify({ fingerIndex }),
        signal,
      }),
    verify: (timeoutSeconds = 40, signal) =>
      request("/api/fingerprint/verify", {
        method: "POST",
        body: JSON.stringify({ timeoutSeconds }),
        signal,
      }),
    cancelListen: () =>
      request("/api/fingerprint/cancel-listen", { method: "POST" }),
    cancelEnroll: () =>
      request("/api/fingerprint/cancel-enroll", { method: "POST" }),
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
    activeRental: (id) =>
      request("/api/rentals/active/" + encodeURIComponent(id)),
    report: () => request("/api/rentals/report"),
  };
})();
