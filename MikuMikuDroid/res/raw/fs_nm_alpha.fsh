precision mediump float;
varying vec3 vTexCoord;
uniform sampler2D sToon;
uniform sampler2D sTex;
uniform vec4 uDif;
void main() {
  vec4 toon;
  vec4 tex;
  vec4 color;
  toon = texture2D(sToon, vec2(0.5, vTexCoord.z));
  tex  = texture2D(sTex,  vTexCoord.xy);
  color = uDif * toon * tex;
  if(color.a == 0.0) {
  	discard;
  } else {
  	gl_FragColor = color;
  }
}