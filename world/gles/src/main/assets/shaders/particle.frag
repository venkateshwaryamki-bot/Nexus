#version 300 es
precision mediump float;
in float vMix;
in float vAlpha;
uniform vec3 uColorA;
uniform vec3 uColorB;
out vec4 fragColor;
void main() {
    vec2 q = gl_PointCoord * 2.0 - 1.0;
    float d = dot(q, q);
    float g = exp(-d * 4.0);
    fragColor = vec4(mix(uColorA, uColorB, vMix) * g * vAlpha * 0.6, 1.0);
}
