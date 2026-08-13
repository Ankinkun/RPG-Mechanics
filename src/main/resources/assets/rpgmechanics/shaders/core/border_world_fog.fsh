#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform mat4 InvViewProjMat;
uniform vec3 CameraPos;
uniform vec4 FogColor;
uniform float FogDepth;
uniform int VertexCount;
uniform float BorderVerts[64];

in vec2 texCoord;

out vec4 fragColor;

bool pointInPolygon(vec2 p) {
    bool inside = false;
    for (int i = 0, j = VertexCount - 1; i < VertexCount; j = i++) {
        vec2 vi = vec2(BorderVerts[i * 2], BorderVerts[i * 2 + 1]);
        vec2 vj = vec2(BorderVerts[j * 2], BorderVerts[j * 2 + 1]);
        bool intersect = ((vi.y > p.y) != (vj.y > p.y))
            && (p.x < (vj.x - vi.x) * (p.y - vi.y) / (vj.y - vi.y + 1.0e-12) + vi.x);
        if (intersect) {
            inside = !inside;
        }
    }
    return inside;
}

float distanceToSegment(vec2 p, vec2 a, vec2 b) {
    vec2 ab = b - a;
    float denom = dot(ab, ab);
    if (denom < 1.0e-8) {
        return length(p - a);
    }
    float t = clamp(dot(p - a, ab) / denom, 0.0, 1.0);
    return length(p - (a + ab * t));
}

float distanceOutside(vec2 p) {
    if (VertexCount < 3 || pointInPolygon(p)) {
        return 0.0;
    }
    float best = 1.0e20;
    for (int i = 0; i < VertexCount; i++) {
        int n = (i + 1) % VertexCount;
        vec2 a = vec2(BorderVerts[i * 2], BorderVerts[i * 2 + 1]);
        vec2 b = vec2(BorderVerts[n * 2], BorderVerts[n * 2 + 1]);
        best = min(best, distanceToSegment(p, a, b));
    }
    return best;
}

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    if (VertexCount < 3) {
        fragColor = color;
        return;
    }

    float depth = texture(DepthSampler, texCoord).r;
    // Sky / no-geometry pixels (cloud gaps, leaf cutouts, clear sky) use the far-plane
    // depth. Reconstructing those yields garbage and painted the sky black / speckled leaves.
    // Leave them untouched; fog only real surfaced geometry for now.
    if (depth >= 0.999) {
        fragColor = color;
        return;
    }

    // MC ModelView is camera rotation only; vertices are camera-relative.
    // JOML perspective uses zero-to-one depth → NDC z == depth sample.
    vec4 clip = vec4(texCoord.x * 2.0 - 1.0, texCoord.y * 2.0 - 1.0, depth, 1.0);
    vec4 viewH = InvViewProjMat * clip;
    if (abs(viewH.w) < 1.0e-5) {
        fragColor = color;
        return;
    }
    vec3 viewPos = viewH.xyz / viewH.w;
    // Reject impossible reconstructions (behind camera / exploded).
    if (viewPos.z > 0.0 || length(viewPos) > 1.0e6 || any(isnan(viewPos))) {
        fragColor = color;
        return;
    }
    vec3 world = viewPos + CameraPos;

    float outside = distanceOutside(world.xz);
    if (outside <= 0.0) {
        fragColor = color;
        return;
    }

    float t = clamp(outside / max(FogDepth, 1.0), 0.0, 1.0);
    t = t * t * (3.0 - 2.0 * t);
    float fogAmount = mix(0.2, 0.92, t);
    fragColor = vec4(mix(color.rgb, FogColor.rgb, fogAmount), color.a);
}
