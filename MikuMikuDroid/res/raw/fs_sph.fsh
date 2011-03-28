precision mediump float;
varying vec4 vTexCoord;
varying vec2 vSphereCoord;
uniform sampler2D sToon;
uniform sampler2D sTex;
uniform sampler2D sSphere;
uniform bool bSpaEn;
uniform bool bSphEn;
uniform vec4 uDif;
uniform vec4 uSpec;

void main() {
  vec4 toon;
  vec4 tex;
  vec4 spec;
  vec4 sph;
  vec4 difamb;
  toon = texture2D(sToon, vec2(0.5, vTexCoord.z));
  sph  = texture2D(sSphere, vSphereCoord);
  tex  = texture2D(sTex,  vTexCoord.xy);
  if(bSpaEn) {
	  difamb  = (tex * uDif + sph) * toon;
  } else if (bSphEn) {
	  difamb  = tex * uDif * toon * sph;
  } else {
	  difamb  = tex * uDif * toon;
  }
  spec = uSpec  * vTexCoord.w;
  gl_FragColor = difamb + spec;
}