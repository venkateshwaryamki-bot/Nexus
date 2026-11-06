#version 300 es
// Fullscreen triangle; reconstructs a world-space view ray per pixel.
uniform mat4 uInvViewProj;
out vec3 vRay;
void main() {
    vec2 p = vec2(float((gl_VertexID << 1) & 2), float(gl_VertexID & 2)) * 2.0 - 1.0;
    vec4 far = uInvViewProj * vec4(p, 1.0, 1.0);
    vec4 near = uInvViewProj * vec4(p, -1.0, 1.0);
    vRay = far.xyz / far.w - near.xyz / near.w;
    gl_Position = vec4(p, 0.9999, 1.0);
}
