precision mediump float;
attribute vec4 aPosition;
attribute vec4 aNormal;
uniform vec4 vLightPos;
uniform mat4 uPMatrix;
varying vec3 vTexCoord;
void main() {
  float v;
  vec3 n;
  vec4 pos;

  pos = vec4(aPosition.xyz, 1.0);
  gl_Position = uPMatrix * pos;

  n = vec3(aPosition.w, aNormal.x, aNormal.y * -1.0);
  v = dot(n, normalize(pos.xyz - vLightPos.xyz));
//  v = dot(normalize(n), normalize(pos.xyz - vLightPos.xyz));
  v = v * 0.5 + 0.5;
  vTexCoord = vec3(aNormal.zw, v);
}
