#version 300 es
precision highp float;
in vec3 vWorld;
in vec3 vNormal;
in vec3 vColor;
in float vEmissive;
in vec3 vLocal;
uniform vec3 uCamPos;
uniform vec3 uSunDir;
uniform float uTime;
uniform float uDevelopment; // 0..1
uniform float uPulse;       // 0..1 progress of an energy pulse wave, <0 when inactive
uniform int uMaterial;      // 0 = planet surface, 1 = structures
uniform vec3 uAccent;       // Electric Cyan
uniform vec3 uPulseColor;   // Neon Green
out vec4 fragColor;

void main() {
    vec3 n = normalize(vNormal);
    vec3 v = normalize(uCamPos - vWorld);
    vec3 l = normalize(uSunDir);
    float ndl = dot(n, l);
    float diff = max(ndl, 0.0);
    float wrap = max((ndl + 0.35) / 1.35, 0.0); // soft terminator
    vec3 h = normalize(l + v);
    float fres = pow(1.0 - max(dot(n, v), 0.0), 4.0);

    vec3 sun = vec3(1.0, 0.95, 0.88);
    vec3 ambient = vec3(0.035, 0.07, 0.13);
    vec3 col;
    vec3 emissive;
    float dayness = smoothstep(-0.15, 0.35, dot(normalize(vWorld), l));

    if (uMaterial == 0) {
        // Ocean is glossy, land is matte metallic plating.
        float ocean = 1.0 - step(0.0001, length(vLocal) - 1.0);
        float shininess = mix(28.0, 140.0, ocean);
        float spec = pow(max(dot(n, h), 0.0), shininess) * mix(0.18, 0.9, ocean) * diff;
        col = vColor * (ambient + wrap * sun * 0.95) + spec * vec3(0.75, 0.88, 1.0);
        col += fres * vec3(0.08, 0.42, 0.85) * 0.6; // atmospheric scattering at the limb
        // City lights: the mask is revealed as development grows; brightest on the night side.
        float threshold = 1.0 - uDevelopment * 0.95;
        float region = smoothstep(threshold, threshold + 0.08, vEmissive) * step(0.02, vEmissive);
        // Break the region into individual settlements: sparse lit cells, denser as development grows.
        vec3 cell = floor(vLocal * 140.0);
        float h = fract(sin(dot(cell, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
        vec3 f = fract(vLocal * 140.0) - 0.5;
        float dotMask = smoothstep(0.5, 0.15, length(f)) * step(1.0 - (0.18 + 0.4 * uDevelopment), h);
        float flicker = 0.8 + 0.2 * sin(uTime * 1.7 + h * 50.0);
        emissive = mix(uAccent, vec3(1.0, 0.85, 0.55), 0.3) * region * dotMask * flicker * mix(0.9, 0.12, dayness);
    } else {
        float spec = pow(max(dot(n, h), 0.0), 64.0) * 0.6 * diff;
        col = vColor * (ambient * 1.6 + wrap * sun) + spec * vec3(0.85, 0.92, 1.0) + fres * uAccent * 0.18;
        float band = 0.75 + 0.25 * sin(uTime * 2.2 + vWorld.y * 60.0 + vWorld.x * 13.0);
        emissive = uAccent * vEmissive * band * mix(1.1, 0.55, dayness);
    }

    // Energy pulse: a wave travelling from the Nexus Tower (north pole) across the surface.
    if (uPulse >= 0.0 && uPulse <= 1.0) {
        float ang = acos(clamp(normalize(vWorld).y, -1.0, 1.0));
        float front = uPulse * 2.6;
        float wave = exp(-pow((ang - front) * 14.0, 2.0)) * (1.0 - uPulse);
        emissive += uPulseColor * wave * 1.6;
    }

    vec3 c = col + emissive;
    c = vec3(1.0) - exp(-c * 1.35); // exposure tonemap keeps highlights controlled ("not overloaded with neon")
    fragColor = vec4(pow(c, vec3(0.92)), 1.0);
}
