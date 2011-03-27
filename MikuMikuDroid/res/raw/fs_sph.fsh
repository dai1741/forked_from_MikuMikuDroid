precision mediump float;
varying vec4 vTexCoord;
varying vec2 vSphereCoord;
uniform sampler2D sToon;
uniform sampler2D sTex;
uniform sampler2D sSphere;
uniform bool bTexEn;
uniform bool bSpaEn;
uniform bool bSphEn;
uniform vec4 uDif;
uniform vec4 uSpec;
uniform vec4 uAmb;

void main() {
  vec4 toon;
  vec4 tex;
  vec4 spec;
  vec4 sph;
  vec4 difamb;
  toon = texture2D(sToon, vec2(0.5, vTexCoord.z));
  if(bSpaEn || bSphEn) {
    sph  = texture2D(sSphere, vSphereCoord);
  }
  if(bTexEn) {
    tex  = texture2D(sTex,  vTexCoord.xy);
  } else {
    tex  = vec4(uDif.a, uDif.a, uDif.a, 1);	// premultiplied alpha for workaround GLUtils.texImage2D
  }
  if(bSpaEn) {
	  difamb  = uDif * toon       + sph + uAmb;
  } else if (bSphEn) {
	  difamb  = uDif * toon * sph       + uAmb;
  } else {
	  difamb  = uDif * toon             + uAmb;
  }
  spec = uSpec  * vTexCoord.w;
  gl_FragColor = tex * min(difamb, 1.0) + spec;
}