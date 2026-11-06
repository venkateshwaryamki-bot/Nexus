#version 300 es
precision mediump float;
in vec3 vColor;
out vec4 fragColor;
void main() {
    vec2 q = gl_PointCoord * 2.0 - 1.0;
    float d = dot(q, q);
    float core = exp(-d * 18.0);
    float halo = exp(-d * 3.5) * 0.35;
    fragColor = vec4(vColor * (core + halo) * step(d, 1.0), 1.0);
}
