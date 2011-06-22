#include <string.h>
#include <jni.h>


extern "C" void Java_jp_gauzau_MikuMikuDroid_Miku_initFaceNative(JNIEnv* env, jobject thiz, jobject vertex, jint count, jobject index, jobject offset)
{
	float* vert = (float*)env->GetDirectBufferAddress(vertex);
	int*   idx  = (int*)env->GetDirectBufferAddress(index);
	float* ofs  = (float*)env->GetDirectBufferAddress(offset);
	
	for(int i = 0; i < count; i++) {
		for(int j = 0; j < 3; j++) {
			vert[idx[i] * 8 + j] = ofs[i * 3 + j];
		}
	}
	
	return ;
}

extern "C" void Java_jp_gauzau_MikuMikuDroid_Miku_setFaceNative(JNIEnv* env, jobject thiz, jobject vertex, jobject pointer, jint count, jobject index, jobject offset, jfloat weight)
{
	float* vert = (float*)env->GetDirectBufferAddress(vertex);
	int*   ptr  = (int*)env->GetDirectBufferAddress(pointer);
	int*   idx  = (int*)env->GetDirectBufferAddress(index);
	float* ofs  = (float*)env->GetDirectBufferAddress(offset);
	
	for(int i = 0; i < count; i++) {
		for(int j = 0; j < 3; j++) {
			vert[ptr[idx[i]] * 8 + j] += ofs[i * 3 + j] * weight;
		}
	}
	
	return ;
}
