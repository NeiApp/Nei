// Minimum window size for the desktop app (Edge app mode has no built-in option).
// Put this file in: frontend/min-size.js
const MIN_WIDTH = 650;
const MIN_HEIGHT = 650;

let resizing = false;

window.addEventListener('resize', () => {
  if (resizing) return;
  if (window.outerWidth < MIN_WIDTH || window.outerHeight < MIN_HEIGHT) {
    resizing = true;
    try {
      window.resizeTo(
        Math.max(window.outerWidth, MIN_WIDTH),
        Math.max(window.outerHeight, MIN_HEIGHT)
      );
    } catch (e) {
      // Some browsers refuse; the CSS fallback in the theme still protects the layout.
    }
    setTimeout(() => { resizing = false; }, 100);
  }
});