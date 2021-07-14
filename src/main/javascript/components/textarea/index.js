const expandedNumberOfRows = 4;

const textareas = new WeakMap();

document.addEventListener("focusin", function (event) {
  if (event.target.tagName === "TEXTAREA") {
    textareas.set(event.target, event.target.rows);
    event.target.rows = expandedNumberOfRows;
  }
});

document.addEventListener("focusout", function (event) {
  if (event.target.tagName === "TEXTAREA") {
    event.target.rows = textareas.get(event.target) || 1;
  }
});
