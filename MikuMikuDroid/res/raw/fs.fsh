precision mediump float;
varying vec4 vTexCoord;
uniform sampler2D sToon;
uniform sampler2D sTex;
uniform bool bTexEn;
uniform vec4 uDif;
uniform vec4 uSpec;
uniform vec4 uAmb;
void main() {
  vec4 toon;
  vec4 tex;
  vec4 spec;
  vec4 difamb;
  toon = texture2D(sToon, vec2(0.5, vTexCoord.z));
  if(bTexEn) {
    tex  = texture2D(sTex,  vTexCoord.xy);
  } else {
    tex  = vec4(uDif.a, uDif.a, uDif.a, 1);	// premultiplied alpha for workaround GLUtils.texImage2D
  }
  spec   = uSpec * vTexCoord.w;
  difamb = uDif  * toon + uAmb;
  gl_FragColor = tex * min(difamb, 1.0) + spec;
}