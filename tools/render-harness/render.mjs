// Headless verification of the NEXUS world shaders + geometry. See TESTING.md.
// 1) ./gradlew :world:scene:run --args="$PWD/tools/render-harness/scenes HIGH"
// 2) node tools/render-harness/render.mjs
import { createRequire } from 'module';
import fs from 'fs'; import path from 'path'; import { fileURLToPath } from 'url';
const require = createRequire(import.meta.url);
const { chromium } = require('playwright');
const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '../..');
const shaderDir = path.join(root, 'world/gles/src/main/assets/shaders');
const outDir = path.join(here, 'out'); fs.mkdirSync(outDir, { recursive: true });
const browser = await chromium.launch({ args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'] });
const page = await browser.newPage({ viewport: { width: 540, height: 1170 } });
page.on('console', m => console.log('[page]', m.text()));
await page.route('http://harness.local/**', route => {
  const u = new URL(route.request().url()); const p = u.pathname.slice(1);
  let file = p.startsWith('shaders/') ? path.join(shaderDir, p.slice(8)) : path.join(here, p);
  if (!fs.existsSync(file)) return route.fulfill({ status: 404, body: 'missing ' + p });
  route.fulfill({ body: fs.readFileSync(file), contentType: p.endsWith('.html') ? 'text/html' : 'application/json' });
});
await page.goto('http://harness.local/harness.html');
let failed = false;
const scenes = fs.readdirSync(path.join(here, 'scenes')).filter(f => f.endsWith('.json')).sort();
for (const s of scenes) {
  const shots = [['', 12.0, -1]]; if (s.includes('stage2')) shots.push(['_pulse', 12.0, 0.35]);
  for (const [suffix, t, pulse] of shots) {
    try {
      const r = await page.evaluate(([u, t, p]) => window.renderScene(u, t, p), ['scenes/' + s, t, pulse]);
      const file = path.join(outDir, s.replace('.json', suffix + '.png'));
      await page.locator('#c').screenshot({ path: file });
      console.log(`${s}${suffix}: glError=${r.glError} renderer=${r.renderer} -> ${path.relative(root, file)}`);
      if (r.glError !== 0) failed = true;
    } catch (e) { console.error(`${s}: FAILED ${e.message}`); failed = true; }
  }
}
await browser.close();
process.exit(failed ? 1 : 0);
