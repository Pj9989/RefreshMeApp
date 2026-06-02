const path = require("path");
const { chromium } = require("playwright");

const [, , inputHtml, outputDir, fpsArg = "30", durationArg = "15"] = process.argv;

if (!inputHtml || !outputDir) {
  console.error("Usage: node capture-html-frames.js <input.html> <outputDir> [fps] [durationSeconds]");
  process.exit(1);
}

const fps = Number(fpsArg);
const durationSeconds = Number(durationArg);
const width = 1080;
const height = 1920;
const totalFrames = Math.round(fps * durationSeconds);
const chromeForTesting =
  "/Users/Phill/Library/Caches/ms-playwright/chromium-1217/chrome-mac-arm64/Google Chrome for Testing.app/Contents/MacOS/Google Chrome for Testing";

function sceneForTime(ms, timeline) {
  let active = timeline[0]?.idx ?? 1;
  for (const step of timeline) {
    if (ms >= step.t) active = step.idx;
  }
  return active;
}

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: chromeForTesting,
    args: ["--allow-file-access-from-files", "--disable-background-timer-throttling"],
  });

  const page = await browser.newPage({
    viewport: { width, height },
    deviceScaleFactor: 1,
  });

  await page.addInitScript(() => {
    const originalSetTimeout = window.setTimeout.bind(window);
    window.__scheduledTimeouts = [];
    window.setTimeout = (callback, delay = 0, ...args) => {
      window.__scheduledTimeouts.push({ delay });
      if (delay === 0) return originalSetTimeout(callback, delay, ...args);
      return 0;
    };
    window.clearTimeout = () => {};
  });

  await page.goto(`file://${path.resolve(inputHtml)}`, { waitUntil: "load" });
  await page.addStyleTag({
    content: `
      *, *::before, *::after {
        animation-play-state: paused !important;
        caret-color: transparent !important;
      }
      .end-hint { display: none !important; }
    `,
  });

  const timeline = await page.evaluate(() => {
    const scheduled = (window.__scheduledTimeouts || [])
      .map((item) => Number(item.delay))
      .filter((delay) => delay >= 0 && delay < 15000)
      .sort((a, b) => a - b);
    const sceneCount = document.querySelectorAll(".scene").length;
    return scheduled.slice(0, sceneCount).map((t, index, all) => ({
      idx: index + 1,
      t,
      duration: (all[index + 1] ?? 15000) - t,
    }));
  });

  for (let frame = 0; frame < totalFrames; frame += 1) {
    const elapsedMs = (frame / fps) * 1000;
    const activeScene = sceneForTime(elapsedMs, timeline);
    const sceneStart = timeline.find((step) => step.idx === activeScene)?.t ?? 0;
    const sceneElapsed = elapsedMs - sceneStart;

    await page.evaluate(({ activeScene, elapsedMs, sceneElapsed }) => {
      document.querySelectorAll(".scene").forEach((scene) => {
        scene.classList.toggle("active", scene.getAttribute("data-scene") === String(activeScene));
      });
      const hint = document.getElementById("endHint");
      if (hint) hint.classList.remove("show");

      requestAnimationFrame(() => {
        document.getAnimations({ subtree: true }).forEach((animation) => {
          const target = animation.effect?.target;
          const isProgress = target?.classList?.contains("progress-bar");
          animation.pause();
          animation.currentTime = isProgress ? elapsedMs : sceneElapsed;
        });
      });
    }, { activeScene, elapsedMs, sceneElapsed });

    await page.waitForTimeout(8);
    await page.screenshot({
      path: path.join(outputDir, `frame_${String(frame).padStart(5, "0")}.jpg`),
      type: "jpeg",
      quality: 94,
      animations: "allow",
      fullPage: false,
    });

    if ((frame + 1) % fps === 0 || frame + 1 === totalFrames) {
      console.log(`${path.basename(inputHtml)}: captured ${frame + 1}/${totalFrames}`);
    }
  }

  await browser.close();
})();
