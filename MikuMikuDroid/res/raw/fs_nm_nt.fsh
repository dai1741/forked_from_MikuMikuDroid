precision mediump float;
varying vec3 vTexCoord;
uniform sampler2D sToon;
uniform bool bTexEn;
uniform vec4 uDif;
uniform vec4 uSpec;
void main() {
  vec4 toon;
  vec4 tex;
  vec4 difamb;
  toon = texture2D(sToon, vec2(0.5, vTexCoord.z));
  difamb = uDif * toon;
  gl_FragColor = min(difamb, 1.0);
}