precision mediump float;
varying vec3 vPos;
uniform sampler2D sPos;
uniform float uFrame;

float toFloat(vec4 v) {
	v = v * 256.0;
	return (v.x * 65536.0 + v.y * 256.0 + v.z) * pow(2.0, v.w);
}

vec4 toVec4(float v) {
	return vec4(0, 0, 0, 0);
}

vec4 interp(vec4 v1, vec4 v2, float t) {
	return v1 + (v2 - v1) * t;
}

vec4 slerp(vec4 q, vec4 r, float t) {
	vec4 p;
	
	vec4 mul = q * r;
	float qr = mul.x + mul.y + mul.z + mul.w;
	float ss = qr * qr;
	if(qr < 0.0) {
		qr = -qr;
		float sp = sqrt(ss);
		float ph = acos(qr);
		float pt = ph * t;
		float t1 = sin(pt) / sp;
		float t0 = sin(ph - pt) / sp;
		p = q * t0 + r * t1;
	} else {
		float sp = sqrt(ss);
		float ph = acos(qr);
		float pt = ph * t;
		float t1 = sin(pt) / sp;
		float t0 = sin(ph - pt) / sp;
		p = q * t0 - r * t1;
	}
	
	return p;
}

void main() {
  vec4 pos1, pos2;
  float t;
  
  pos1 = texture2D(sPos, vec2(vPos.x, 0));
  pos2 = texture2D(sPos, vec2(vPos.y, 0));
  t = (uFrame - pos1.w) / (pos2.w - pos1.w);
  
  gl_FragColor = slerp(pos1, pos2, t);
}

