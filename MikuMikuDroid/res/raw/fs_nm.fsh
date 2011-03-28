precision mediump float;
varying vec3 vTexCoord;
uniform sampler2D sToon;
uniform sampler2D sTex;
uniform vec4 uDif;
void main() {
  vec4 toon;
  vec4 tex;
  toon = texture2D(sToon, vec2(0.5, vTexCoord.z));
  tex  = texture2D(sTex,  vTexCoord.xy);
  gl_FragColor = uDif * toon * tex;
}
