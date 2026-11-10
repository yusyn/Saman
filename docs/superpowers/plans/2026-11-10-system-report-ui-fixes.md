# System Report UI Fixes — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all visual and UX issues identified in the system report page code review — colspan mismatch, missing loading spinner, raw JSON display, result badge coloring, CSS duplication, and pagination UX.

**Architecture:** All changes are front-end only (HTML/CSS/JS). We modify `system-report.js` for rendering logic, `index.html` for structural HTML, and `report-extra.css` for styles. No backend changes.

**Tech Stack:** Vanilla JS, HTML5, CSS3 (CSS custom properties from `app.css`)

**Spec:** [system-report-review.md](file:///C:/Users/Tanin/.gemini/antigravity/brain/5ba98655-255e-4337-9738-c32785edc181/system-report-review.md)

## Global Constraints

- No new JS libraries or build tools — project uses plain `<script>` tags
- Follow existing code conventions: IIFE pattern, `$()` alias for `querySelector`, Persian UI text
- Use existing CSS variables: `--muted`, `--primary`, `--ok`, `--danger`, `--bg`, `--panel`, `--border`, `--radius`, `--shadow`
- Use existing `.spinner`, `.badge-*`, `.modal-*` CSS classes already defined in `app.css`
- Keep RTL/`dir` attributes consistent with current usage

## Review Focus

1. **colspan values must stay in sync with actual column count** — if a table header changes column count, all `colspan` references in both success and error paths must update. Verify every `colspan` in `system-report.js` matches its table's `<th>` count in `index.html`.
2. **JSON display toggle must not break when `detailsJson` is malformed** — `JSON.parse` on a non-JSON string throws; the detail cell must fall back gracefully to raw text.
3. **Badge classes must match existing `app.css` definitions** — only `badge-free`, `badge-busy`, `badge-muted` exist; any new badge class needs a CSS rule.
4. **Modal HTML structure must follow the existing pattern** — `.modal > .modal-backdrop + .modal-panel > .modal-head + content` as used in `modalCar`, `modalEmployee`, `modalLogin`.
5. **Spinner visibility must toggle correctly** — the loading spinner must be hidden after both success and error paths in `load()`.

---

### Task 1: Fix colspan Mismatch and Add Result Badges

**Files:**
- Modify: `src/main/resources/static/js/system-report.js:17-18,27,30,37,46`

**Interfaces:**
- Consumes: existing `escapeHtml()` function (line 3), existing `.badge-free` / `.badge-busy` CSS classes from `app.css:534-535`
- Produces: corrected `render()` and `loadPendingDeviceOperations()` functions with accurate colspan values and badge-styled result column

- [ ] **Step 1: Fix `colspan="7"` → `colspan="8"` in `loadPendingDeviceOperations` catch block**

In `system-report.js` line 30, change:
```javascript
body.innerHTML='<tr><td colspan="7">دریافت دفتر عملیات دستگاه انجام نشد.</td></tr>';
```
to:
```javascript
body.innerHTML='<tr><td colspan="8">دریافت دفتر عملیات دستگاه انجام نشد.</td></tr>';
```

- [ ] **Step 2: Add badge styling to result column in `render()` function**

In the `render()` function (line 17), replace the plain `escapeHtml(e.result)` for the result `<td>` with a badge:
```javascript
const resultBadge = e.result === "موفق"
  ? '<span class="badge badge-free">موفق</span>'
  : e.result === "ناموفق"
    ? '<span class="badge badge-busy">ناموفق</span>'
    : escapeHtml(e.result);
```
Use `resultBadge` in the `<td>` instead of `escapeHtml(e.result)`.

- [ ] **Step 3: Add badge styling to device-succeeded column in `loadPendingDeviceOperations`**

In line 27, replace the plain text for `deviceSucceeded` with badge-styled output:
```javascript
const devResult = o.deviceSucceeded == null
  ? '<span class="badge badge-muted">نامشخص</span>'
  : o.deviceSucceeded
    ? '<span class="badge badge-free">موفق</span>'
    : '<span class="badge badge-busy">ناموفق</span>';
```

- [ ] **Step 4: Manually verify colspan counts**

Count `<th>` elements in `index.html`:
- Line 334 (pending device operations table): 8 columns ✓
- Line 336 (system event table): 7 columns ✓

Verify every `colspan` in `system-report.js`:
- Line 18 (render empty): `colspan="7"` ✓
- Line 27 (device ops empty): `colspan="8"` ✓
- Line 30 (device ops error): `colspan="8"` ✓ (after fix)
- Line 37 (loading): `colspan="7"` ✓
- Line 46 (load error): `colspan="7"` ✓

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/js/system-report.js
git commit -m "fix: correct colspan mismatch and add result badges in system report"
```

---

### Task 2: Add Loading Spinner

**Files:**
- Modify: `src/main/resources/static/index.html:330`
- Modify: `src/main/resources/static/js/system-report.js:34-49`

**Interfaces:**
- Consumes: existing `.spinner` CSS class from `app.css:514-522`, existing `#systemEventMessage` element
- Produces: a `<span class="spinner">` element toggled by the `load()` function

- [ ] **Step 1: Add spinner element to HTML next to the message `<p>`**

In `index.html` line 330, replace:
```html
<p id="systemEventMessage" class="muted small" aria-live="polite"></p>
```
with:
```html
<div class="report-status-row">
  <span class="spinner hidden" id="systemEventSpinner" aria-hidden="true"></span>
  <p id="systemEventMessage" class="muted small" aria-live="polite"></p>
</div>
```

- [ ] **Step 2: Add `.report-status-row` and `.spinner.hidden` CSS rule to `report-extra.css`**

Append to `report-extra.css`:
```css
.report-status-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin: 0.5rem 0;
}
.spinner.hidden { display: none; }
```

- [ ] **Step 3: Toggle spinner visibility in `load()` function**

In `system-report.js`, modify the `load()` function:
- At the start (after `busy=true`): show spinner via `$("#systemEventSpinner").classList.remove("hidden")`
- In `finally` block (after `busy=false`): hide spinner via `$("#systemEventSpinner").classList.add("hidden")`

- [ ] **Step 4: Open the app in browser, navigate to "گزارش سیستم" tab, and verify:**
- Spinner appears during load
- Spinner disappears after success
- Spinner disappears after error (e.g. when not logged in)

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/index.html src/main/resources/static/js/system-report.js src/main/resources/static/css/report-extra.css
git commit -m "feat: add loading spinner to system report page"
```

---

### Task 3: Format JSON Details Column with Expandable Display

**Files:**
- Modify: `src/main/resources/static/js/system-report.js:3,17,27`
- Modify: `src/main/resources/static/css/report-extra.css` (append)

**Interfaces:**
- Consumes: existing `escapeHtml()` function, existing `.muted` and `.small` CSS classes
- Produces: `formatDetails(jsonStr)` function that renders JSON as a truncated preview with click-to-expand, used in both `render()` and `loadPendingDeviceOperations()`

- [ ] **Step 1: Add `formatDetails(jsonStr)` helper function after `escapeHtml` in `system-report.js`**

```javascript
function formatDetails(jsonStr) {
  if (!jsonStr || jsonStr === "{}") return '<span class="muted">—</span>';
  try {
    const obj = JSON.parse(jsonStr);
    const pretty = JSON.stringify(obj, null, 2);
    const short = jsonStr.length > 40 ? jsonStr.substring(0, 40) + "…" : jsonStr;
    return '<span class="details-cell" tabindex="0" title="کلیک برای نمایش جزئیات">'
      + '<span class="details-short">' + escapeHtml(short) + '</span>'
      + '<pre class="details-full hidden" dir="ltr">' + escapeHtml(pretty) + '</pre>'
      + '</span>';
  } catch (e) {
    return '<span dir="ltr">' + escapeHtml(jsonStr) + '</span>';
  }
}
```

- [ ] **Step 2: Add CSS for `.details-cell`, `.details-short`, `.details-full` to `report-extra.css`**

```css
.details-cell { cursor: pointer; }
.details-short { font-size: 0.8rem; color: var(--muted); }
.details-full {
  margin: 0.3rem 0 0;
  padding: 0.5rem;
  background: var(--bg);
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 0.75rem;
  max-height: 12rem;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.details-full.hidden { display: none; }
```

- [ ] **Step 3: Add click event delegation for `.details-cell` toggle in `DOMContentLoaded`**

In the `DOMContentLoaded` listener, add after the existing listeners:
```javascript
document.addEventListener("click", function(e) {
  const cell = e.target.closest(".details-cell");
  if (!cell) return;
  const full = cell.querySelector(".details-full");
  const short = cell.querySelector(".details-short");
  if (full) full.classList.toggle("hidden");
  if (short) short.classList.toggle("hidden");
});
```

- [ ] **Step 4: Replace `escapeHtml(e.detailsJson||"{}")` with `formatDetails(e.detailsJson)` in `render()`**

In line 17, change the last `<td>` from:
```javascript
'<td dir="ltr">'+escapeHtml(e.detailsJson||"{}")+'</td>'
```
to:
```javascript
'<td dir="ltr">'+formatDetails(e.detailsJson)+'</td>'
```

- [ ] **Step 5: Replace `escapeHtml(o.detailsJson||"{}")` with `formatDetails(o.detailsJson)` in `loadPendingDeviceOperations()`**

In line 27, change the details `<td>` from:
```javascript
'<td dir="ltr">'+escapeHtml(o.detailsJson||"{}")+'</td>'
```
to:
```javascript
'<td dir="ltr">'+formatDetails(o.detailsJson)+'</td>'
```

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/js/system-report.js src/main/resources/static/css/report-extra.css
git commit -m "feat: add expandable formatted JSON display for details column"
```

---

### Task 4: Fix CSS Duplication and Improve Readability of Render Functions

**Files:**
- Modify: `src/main/resources/static/css/report-extra.css:98-119`
- Modify: `src/main/resources/static/js/system-report.js:14-19,20-32`

**Interfaces:**
- Consumes: `escapeHtml()`, `formatDetails()` (from Task 3), badge rendering (from Task 1)
- Produces: cleaned-up `render()` and `loadPendingDeviceOperations()` using template literals, deduplicated CSS

- [ ] **Step 1: Remove duplicate `#reportFilterSummary` rule from `report-extra.css`**

Remove lines 116-119:
```css
#reportFilterSummary {
  margin: 0;
  align-self: center;
}
```

And update lines 98-100 to merge both intents:
```css
#reportFilterSummary {
  margin: 0;
  align-self: center;
}
```

- [ ] **Step 2: Refactor `render()` to use template literals**

Replace the current single-line `.map()` in `render()` with a readable multi-line template literal:
```javascript
function render(items, append) {
  const tbody = $("#systemEventTable");
  if (!append) tbody.innerHTML = "";
  const resultBadge = (r) => r === "موفق"
    ? '<span class="badge badge-free">موفق</span>'
    : r === "ناموفق"
      ? '<span class="badge badge-busy">ناموفق</span>'
      : escapeHtml(r);
  const rows = (items || []).map(e => `<tr>
    <td dir="ltr">${escapeHtml(e.occurredAt || "—")}</td>
    <td>${escapeHtml(e.category)}<br><span class="muted small">${escapeHtml(e.eventType)}</span></td>
    <td>${resultBadge(e.result)}</td>
    <td>${escapeHtml(e.description)}</td>
    <td>${escapeHtml(e.actor || "—")}</td>
    <td>${escapeHtml((e.entityType || "") + (e.entityId ? " · " + e.entityId : ""))}</td>
    <td dir="ltr">${formatDetails(e.detailsJson)}</td>
  </tr>`).join("");
  if (append) tbody.insertAdjacentHTML("beforeend", rows);
  else tbody.innerHTML = rows || '<tr><td colspan="7">رویدادی با این فیلتر یافت نشد.</td></tr>';
}
```

- [ ] **Step 3: Refactor `loadPendingDeviceOperations()` to use template literals**

Replace the current single-line `.map()` with readable template literals:
```javascript
const devResult = (o) => o.deviceSucceeded == null
  ? '<span class="badge badge-muted">نامشخص</span>'
  : o.deviceSucceeded
    ? '<span class="badge badge-free">موفق</span>'
    : '<span class="badge badge-busy">ناموفق</span>';

body.innerHTML = items.map(o => `<tr>
  <td dir="ltr">${escapeHtml(o.operationId)}</td>
  <td>${escapeHtml(o.operationType)}</td>
  <td>${escapeHtml((o.entityType || "") + (o.entityId ? " · " + o.entityId : ""))}</td>
  <td>${escapeHtml(o.state)}</td>
  <td>${devResult(o)}</td>
  <td>${escapeHtml(o.note || "—")}</td>
  <td dir="ltr">${formatDetails(o.detailsJson)}</td>
  <td dir="ltr">${escapeHtml(o.updatedAt || "—")}</td>
</tr>`).join("") || '<tr><td colspan="8">عملیات ناتمامی برای بررسی وجود ندارد.</td></tr>';
```

- [ ] **Step 4: Verify no visual regression**

Open the app, navigate to "گزارش سیستم", and check:
- Event rows render correctly with badge-styled results
- Device operations rows render correctly
- Empty states show full-width messages
- Details column shows expandable JSON

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/js/system-report.js src/main/resources/static/css/report-extra.css
git commit -m "refactor: improve readability of render functions and fix CSS duplication"
```

---

### Task 5: Decouple Device Operations Loading from Event Pagination

**Files:**
- Modify: `src/main/resources/static/js/system-report.js:34-50,52-58`

**Interfaces:**
- Consumes: `loadPendingDeviceOperations()` (existing), `load()` (existing), `window.loadSystemEvents` (existing, called by `app.js:279`)
- Produces: modified `load()` that only calls `loadPendingDeviceOperations()` on initial load (not pagination), updated `DOMContentLoaded` to separate concerns

- [ ] **Step 1: Remove `loadPendingDeviceOperations()` call from inside `load()`**

In the `load()` function, remove line 44:
```javascript
await loadPendingDeviceOperations();
```

- [ ] **Step 2: Update `window.loadSystemEvents` to call both independently**

Change line 51 from:
```javascript
window.loadSystemEvents=()=>load(false);
```
to:
```javascript
window.loadSystemEvents = () => { load(false); loadPendingDeviceOperations(); };
```

This ensures device operations load when the tab is first opened (via `app.js` calling `window.loadSystemEvents()`), but **not** when the user clicks "رویدادهای قدیمی‌تر" (pagination) or "اعمال فیلتر" or "به‌روزرسانی", since those only concern system events.

- [ ] **Step 3: Also call `loadPendingDeviceOperations()` from the refresh button**

Update the `btnRefreshSystemEvents` click handler to also refresh device operations:
```javascript
$("#btnRefreshSystemEvents").addEventListener("click", () => { load(false); loadPendingDeviceOperations(); });
```

- [ ] **Step 4: Verify behavior**

- Navigate to "گزارش سیستم" → both tables load ✓
- Click "رویدادهای قدیمی‌تر" → only events table updates, device operations unchanged ✓
- Click "اعمال فیلتر" → only events table re-filters, device operations unchanged ✓
- Click "به‌روزرسانی" → both tables refresh ✓

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/js/system-report.js
git commit -m "fix: decouple device operations loading from event pagination"
```

---

### Task 6: Add Pagination "Back to Top" and Page Info Improvement

**Files:**
- Modify: `src/main/resources/static/index.html:337`
- Modify: `src/main/resources/static/js/system-report.js:42,54-57`

**Interfaces:**
- Consumes: existing `page` variable, existing `load()` function, existing `.btn-ghost` CSS class
- Produces: a "بازگشت به ابتدا" button that resets pagination, improved page info display with Persian digits

- [ ] **Step 1: Add "بازگشت به ابتدا" button in HTML**

In `index.html` line 337, replace:
```html
<div class="card-head"><button type="button" class="btn btn-ghost" id="btnOlderSystemEvents">رویدادهای قدیمی‌تر</button><span id="systemEventPageInfo" class="muted small"></span></div>
```
with:
```html
<div class="card-head"><button type="button" class="btn btn-ghost" id="btnBackToTopSystemEvents" disabled>بازگشت به ابتدا</button><button type="button" class="btn btn-ghost" id="btnOlderSystemEvents">رویدادهای قدیمی‌تر</button><span id="systemEventPageInfo" class="muted small"></span></div>
```

- [ ] **Step 2: Wire click handler for "بازگشت به ابتدا" in `DOMContentLoaded`**

Add after line 57:
```javascript
$("#btnBackToTopSystemEvents").addEventListener("click", () => load(false));
```

- [ ] **Step 3: Toggle "بازگشت به ابتدا" disabled state based on current page**

In `load()`, after updating `#systemEventPageInfo` and `#btnOlderSystemEvents`, add:
```javascript
$("#btnBackToTopSystemEvents").disabled = page === 0;
```

Also add in the `catch` block:
```javascript
$("#btnBackToTopSystemEvents").disabled = true;
```

- [ ] **Step 4: Verify behavior**

- On initial load, "بازگشت به ابتدا" is disabled ✓
- After clicking "رویدادهای قدیمی‌تر" once, "بازگشت به ابتدا" becomes enabled ✓
- Clicking "بازگشت به ابتدا" resets to page 0 and disables itself ✓

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/index.html src/main/resources/static/js/system-report.js
git commit -m "feat: add back-to-top button for system report pagination"
```
