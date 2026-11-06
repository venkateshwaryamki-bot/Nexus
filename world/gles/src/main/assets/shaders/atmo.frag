#version 300 es
precision highp float;
in vec3 vWorld;
in vec3 vNormal;
uniform vec3 uCamPos;
uniform vec3 uSunDir;
uniform vec3 uAccent;
uniform float uStrength;
out vec4 fragColor;
void main() {
    vec3 n = normalize(vNormal);
    vec3 v = normalize(uCamPos - vWorld);
    float rim = pow(1.0 - abs(dot(n, v)), 3.5);
    float lit = 0.35 + 0.65 * smoothstep(-0.4, 0.6, dot(n, normalize(uSunDir)));
    vec3 c = mix(uAccent, vec3(0.45, 0.7, 1.0), 0.4) * rim * lit * uStrength;
    fragColor = vec4(c, 1.0); // additive blend
}
