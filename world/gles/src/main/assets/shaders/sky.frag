#version 300 es
precision highp float;
in vec3 vRay;
uniform float uTime;
uniform vec3 uSunDir;
uniform float uStarDensity; // 0..1, also used by the intro sequence to fade stars in
uniform vec3 uNebulaA;
uniform vec3 uNebulaB;
out vec4 fragColor;

float hash(vec3 p) { p = fract(p * 0.3183099 + 0.1); p *= 17.0; return fract(p.x * p.y * p.z * (p.x + p.y + p.z)); }
float noise(vec3 x) {
    vec3 i = floor(x); vec3 f = fract(x); f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i), hash(i + vec3(1,0,0)), f.x), mix(hash(i + vec3(0,1,0)), hash(i + vec3(1,1,0)), f.x), f.y),
               mix(mix(hash(i + vec3(0,0,1)), hash(i + vec3(1,0,1)), f.x), mix(hash(i + vec3(0,1,1)), hash(i + vec3(1,1,1)), f.x), f.y), f.z);
}
float fbm(vec3 p) { float s = 0.0, a = 0.5; for (int i = 0; i < 4; i++) { s += a * noise(p); p *= 2.1; a *= 0.5; } return s; }

void main() {
    vec3 d = normalize(vRay);
    // Base: deep space navy with a faint vertical gradient.
    vec3 col = mix(vec3(0.010, 0.030, 0.060), vec3(0.024, 0.075, 0.13), smoothstep(-0.6, 0.8, d.y));
    // Nebula clouds (two-tone: cyan-blue and violet).
    float n = fbm(d * 2.2 + vec3(0.0, 0.0, uTime * 0.004));
    float n2 = fbm(d * 4.5 - vec3(uTime * 0.003));
    col += uNebulaA * pow(smoothstep(0.45, 0.85, n), 2.0) * 0.55;
    col += uNebulaB * pow(smoothstep(0.5, 0.9, n2 * n), 1.5) * 0.45;
    // Stars: sparse cells with twinkle.
    vec3 cell = floor(d * 220.0);
    float h = hash(cell);
    float star = step(1.0 - 0.012 * uStarDensity, h);
    vec3 sp = (cell + 0.5) / 220.0;
    float dist = length(d - normalize(sp)) * 220.0;
    float tw = 0.65 + 0.35 * sin(uTime * (1.0 + h * 3.0) + h * 40.0);
    col += star * smoothstep(0.55, 0.0, dist) * tw * mix(vec3(0.75, 0.88, 1.0), vec3(1.0, 0.9, 0.7), fract(h * 13.0)) * 1.6;
    // Distant sun glare.
    float s = max(dot(d, normalize(uSunDir)), 0.0);
    col += vec3(1.0, 0.92, 0.78) * pow(s, 900.0) * 3.0 + vec3(0.25, 0.55, 0.9) * pow(s, 18.0) * 0.18;
    fragColor = vec4(col, 1.0);
}
