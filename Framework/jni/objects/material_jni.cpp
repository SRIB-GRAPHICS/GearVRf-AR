/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/***************************************************************************
 * JNI
 ***************************************************************************/

#include "material.h"

#include "util/gvr_jni.h"

namespace gvr {
extern "C" {
JNIEXPORT jlong JNICALL
Java_org_gearvrf_NativeMaterial_ctor(JNIEnv * env, jobject obj,
        jint shader_type);

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setShaderType(JNIEnv * env,
        jobject obj, jlong jmaterial, jint shader_type);

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setTexture(JNIEnv * env,
        jobject obj, jlong jmaterial, jstring key, jlong texture);
JNIEXPORT jfloat JNICALL
Java_org_gearvrf_NativeMaterial_getFloat(JNIEnv * env,
        jobject obj, jlong jmaterial, jstring key);

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setFloat(JNIEnv * env,
        jobject obj, jlong jmaterial, jstring key, jfloat value);
JNIEXPORT jfloatArray JNICALL
Java_org_gearvrf_NativeMaterial_getVec2(JNIEnv * env,
        jobject obj, jlong jmaterial, jstring key);

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setVec2(JNIEnv * env,
        jobject obj, jlong jmaterial, jstring key, jfloat x, jfloat y);
JNIEXPORT jfloatArray JNICALL
Java_org_gearvrf_NativeMaterial_getVec3(JNIEnv * env,
        jobject obj, jlong jmaterial, jstring key);

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setVec3(JNIEnv * env,
        jobject obj, jlong jmaterial, jstring key, jfloat x, jfloat y,
        jfloat z);

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setVec4(JNIEnv * env,
        jobject obj, jlong jmaterial, jstring key, jfloat x, jfloat y, jfloat z,
        jfloat w);
JNIEXPORT jfloatArray JNICALL
Java_org_gearvrf_NativeMaterial_getVec4(JNIEnv * env,
        jobject obj, jlong jmaterial, jstring key);

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setMat4(JNIEnv * env,
        jobject obj, jlong jmaterial, jstring key, jfloat x1, jfloat y1,
        jfloat z1, jfloat w1, jfloat x2, jfloat y2, jfloat z2, jfloat w2,
        jfloat x3, jfloat y3, jfloat z3, jfloat w3, jfloat x4, jfloat y4,
        jfloat z4, jfloat w4);

// ---------------------------------------------AR------------------------{
JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setArObj(JNIEnv * env,
        jobject obj,jlong jmaterial, jstring key, jboolean arObj, jboolean isAR);

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setMVPmatrix(JNIEnv * env,
        jobject obj,jlong jmaterial, jstring key, jfloatArray MVPmatrix);

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setMVmatrix(JNIEnv * env,
        jobject obj,jlong jmaterial, jstring key, jfloatArray MVmatrix);

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setAR(JNIEnv * env,
        jobject obj,jlong jmaterial, jstring key, jboolean isAR);

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setAROffsets(JNIEnv * env,
        jobject obj,jlong jmaterial, jstring key, jfloat ar_offset_X, jfloat ar_offset_Y, jfloat ar_offset_W, jfloat ar_offset_H);
// ---------------------------------------------AR------------------------}

}
;

JNIEXPORT jlong JNICALL
Java_org_gearvrf_NativeMaterial_ctor(JNIEnv * env, jobject obj,
    jint shader_type) {
return reinterpret_cast<jlong>(new Material(static_cast<Material::ShaderType>(shader_type)));
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setShaderType(JNIEnv * env,
    jobject obj, jlong jmaterial, jint shader_type) {
Material* material = reinterpret_cast<Material*>(jmaterial);
return material->set_shader_type(
        static_cast<Material::ShaderType>(shader_type));
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setTexture(JNIEnv * env,
    jobject obj, jlong jmaterial, jstring key, jlong jtexture) {
Material* material = reinterpret_cast<Material*>(jmaterial);
Texture* texture = reinterpret_cast<Texture*>(jtexture);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
material->setTexture(native_key, texture);
env->ReleaseStringUTFChars(key, char_key);
}

JNIEXPORT jfloat JNICALL
Java_org_gearvrf_NativeMaterial_getFloat(JNIEnv * env,
    jobject obj, jlong jmaterial, jstring key) {
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
jfloat material_float = material->getFloat(native_key);
env->ReleaseStringUTFChars(key, char_key);
return material_float;
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setFloat(JNIEnv * env,
    jobject obj, jlong jmaterial, jstring key, jfloat value) {
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
material->setFloat(native_key, value);
env->ReleaseStringUTFChars(key, char_key);
}

JNIEXPORT jfloatArray JNICALL
Java_org_gearvrf_NativeMaterial_getVec2(JNIEnv * env,
    jobject obj, jlong jmaterial, jstring key) {
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
glm::vec2 material_vec2 = material->getVec2(native_key);
jfloatArray jvec2 = env->NewFloatArray(2);
env->SetFloatArrayRegion(jvec2, 0, 2,
        reinterpret_cast<jfloat*>(&material_vec2));
env->ReleaseStringUTFChars(key, char_key);
return jvec2;
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setVec2(JNIEnv * env,
    jobject obj, jlong jmaterial, jstring key, jfloat x, jfloat y) {
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
material->setVec2(native_key, glm::vec2(x, y));
env->ReleaseStringUTFChars(key, char_key);
}

JNIEXPORT jfloatArray JNICALL
Java_org_gearvrf_NativeMaterial_getVec3(JNIEnv * env,
    jobject obj, jlong jmaterial, jstring key) {
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
glm::vec3 material_vec3 = material->getVec3(native_key);
jfloatArray jvec3 = env->NewFloatArray(3);
env->SetFloatArrayRegion(jvec3, 0, 3,
        reinterpret_cast<jfloat*>(&material_vec3));
env->ReleaseStringUTFChars(key, char_key);
return jvec3;
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setVec3(JNIEnv * env,
    jobject obj, jlong jmaterial, jstring key, jfloat x, jfloat y,
    jfloat z) {
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
material->setVec3(native_key, glm::vec3(x, y, z));
env->ReleaseStringUTFChars(key, char_key);
}

JNIEXPORT jfloatArray JNICALL
Java_org_gearvrf_NativeMaterial_getVec4(JNIEnv * env,
    jobject obj, jlong jmaterial, jstring key) {
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
glm::vec4 material_vec4 = material->getVec4(native_key);
jfloatArray jvec4 = env->NewFloatArray(4);
env->SetFloatArrayRegion(jvec4, 0, 4,
        reinterpret_cast<jfloat*>(&material_vec4));
env->ReleaseStringUTFChars(key, char_key);
return jvec4;
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setVec4(JNIEnv * env,
    jobject obj, jlong jmaterial, jstring key, jfloat x, jfloat y, jfloat z,
    jfloat w) {
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
material->setVec4(native_key, glm::vec4(x, y, z, w));
env->ReleaseStringUTFChars(key, char_key);
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeMaterial_setMat4(JNIEnv * env,
    jobject obj, jlong jmaterial, jstring key, jfloat x1, jfloat y1,
    jfloat z1, jfloat w1, jfloat x2, jfloat y2, jfloat z2, jfloat w2,
    jfloat x3, jfloat y3, jfloat z3, jfloat w3, jfloat x4, jfloat y4,
    jfloat z4, jfloat w4) {
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
glm::mat4 mat(x1, y1, z1, w1, x2, y2, z2, w2, x3, y3, z3, w3, x4, y4, z4,
        w4);
material->setMat4(native_key, mat);
env->ReleaseStringUTFChars(key, char_key);
}

// ---------------------------------------------AR------------------------{
JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setArObj(JNIEnv * env,
    jobject obj, jlong jmaterial,jstring key, jboolean arObj, jboolean isAR) {
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
material->setObjectAR(native_key, arObj, isAR);
}

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setAR(JNIEnv * env,
    jobject obj,jlong jmaterial, jstring key, jboolean isAR) {

Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
material->setAR(native_key, isAR);
env->ReleaseStringUTFChars(key, char_key);

}

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setMVPmatrix(JNIEnv * env, jobject obj,jlong jmaterial, jstring key, jfloatArray MVPmatrix) {
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
jfloat* MVP_matrix = env->GetFloatArrayElements(MVPmatrix,0);
glm::mat4 mat(MVP_matrix[0], MVP_matrix[1], MVP_matrix[2], MVP_matrix[3], MVP_matrix[4], MVP_matrix[5], MVP_matrix[6], MVP_matrix[7], MVP_matrix[8], MVP_matrix[9], MVP_matrix[10], MVP_matrix[11], MVP_matrix[12], MVP_matrix[13], MVP_matrix[14], MVP_matrix[15]);
material->setMVPmatrix(native_key, mat);
env->ReleaseStringUTFChars(key, char_key);
env->ReleaseFloatArrayElements(MVPmatrix,MVP_matrix,0);
}

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setMVmatrix(JNIEnv * env, jobject obj,jlong jmaterial, jstring key, jfloatArray MVmatrix) {
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
jfloat* MV_matrix = env->GetFloatArrayElements(MVmatrix,0);
glm::mat4 mat(MV_matrix[0], MV_matrix[1], MV_matrix[2], MV_matrix[3], MV_matrix[4], MV_matrix[5], MV_matrix[6], MV_matrix[7], MV_matrix[8], MV_matrix[9], MV_matrix[10], MV_matrix[11], MV_matrix[12], MV_matrix[13], MV_matrix[14], MV_matrix[15]);
material->setMVmatrix(native_key, mat);
env->ReleaseStringUTFChars(key, char_key);
env->ReleaseFloatArrayElements(MVmatrix,MV_matrix,0);
}

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setAROffsets(JNIEnv * env,
    jobject obj,jlong jmaterial, jstring key, jfloat ar_offset_X, jfloat ar_offset_Y, jfloat ar_offset_W, jfloat ar_offset_H)
{
Material* material = reinterpret_cast<Material*>(jmaterial);
const char* char_key = env->GetStringUTFChars(key, 0);
std::string native_key = std::string(char_key);
material->setAROffsets(native_key, ar_offset_X, ar_offset_Y, ar_offset_W, ar_offset_H);
env->ReleaseStringUTFChars(key, char_key);
}
// ---------------------------------------------AR------------------------}

}
