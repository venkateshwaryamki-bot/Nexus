#version 300 es
precision highp float;
in float vIntensity;
in float vPhase;
uniform float uTime;
uniform vec3 uColor;
out vec4 fragColor;
void main() {
    // Energy flowing along the network.
    float flow = 0.55 + 0.45 * sin(vPhase * 3.0 - uTime * 3.5);
    fragColor = vec4(uColor * vIntensity * flow * 0.9, 1.0);
}
