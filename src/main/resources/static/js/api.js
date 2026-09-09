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
    updateCar: (body) =>
      request("/api/cars", { method: "PUT", body: JSON.stringify(body) }),
    deleteCar: (plate) =>
      request("/api/cars?plate=" + encodeURIComponent(plate), { method: "DELETE" }),
    employees: () => request("/api/employees"),
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
      }),
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
