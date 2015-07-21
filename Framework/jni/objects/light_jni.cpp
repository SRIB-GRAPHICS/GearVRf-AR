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

#include "light.h"

#include "util/gvr_jni.h"

namespace gvr {
extern "C" {
JNIEXPORT jlong JNICALL
Java_org_gearvrf_NativeLight_ctor(JNIEnv * env, jobject obj);

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeLight_enable(JNIEnv * env, jobject obj, jlong jlight);

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeLight_disable(JNIEnv * env, jobject obj, jlong jlight);

JNIEXPORT jfloat JNICALL
Java_org_gearvrf_NativeLight_getFloat(JNIEnv * env,
        jobject obj, jlong jlight, jstring key);

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeLight_setFloat(JNIEnv * env,
        jobject obj, jlong jlight, jstring key, jfloat value);

JNIEXPORT jfloatArray JNICALL
Java_org_gearvrf_NativeLight_getVec3(JNIEnv * env,
        jobject obj, jlong jlight, jstring key);

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeLight_setVec3(JNIEnv * env,
        jobject obj, jlong jlight, jstring key, jfloat x, jfloat y,
        jfloat z);

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeLight_setVec4(JNIEnv * env,
        jobject obj, jlong jlight, jstring key, jfloat x, jfloat y, jfloat z,
        jfloat w);
JNIEXPORT jfloatArray JNICALL
Java_org_gearvrf_NativeLight_getVec4(JNIEnv * env,
        jobject obj, jlong jlight, jstring key);
}
;

JNIEXPORT jlong JNICALL
Java_org_gearvrf_NativeLight_ctor(JNIEnv * env, jobject obj) {
    return reinterpret_cast<jlong>(new Light());
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeLight_enable(JNIEnv * env,
    jobject obj, jlong jlight) {
    Light* light = reinterpret_cast<Light*>(jlight);
    light->enable();
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeLight_disable(JNIEnv * env,
    jobject obj, jlong jlight) {
    Light* light = reinterpret_cast<Light*>(jlight);
    light->disable();
}

JNIEXPORT jfloat JNICALL
Java_org_gearvrf_NativeLight_getFloat(JNIEnv * env,
    jobject obj, jlong jlight, jstring key) {
    Light* light = reinterpret_cast<Light*>(jlight);
    const char* char_key = env->GetStringUTFChars(key, 0);
    std::string native_key = std::string(char_key);
    jfloat light_float = light->getFloat(native_key);
    env->ReleaseStringUTFChars(key, char_key);
    return light_float;
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeLight_setFloat(JNIEnv * env,
    jobject obj, jlong jlight, jstring key, jfloat value) {
    Light* light = reinterpret_cast<Light*>(jlight);
    const char* char_key = env->GetStringUTFChars(key, 0);
    std::string native_key = std::string(char_key);
    light->setFloat(native_key, value);
    env->ReleaseStringUTFChars(key, char_key);
}

JNIEXPORT jfloatArray JNICALL
Java_org_gearvrf_NativeLight_getVec3(JNIEnv * env,
    jobject obj, jlong jlight, jstring key) {
    Light* light = reinterpret_cast<Light*>(jlight);
    const char* char_key = env->GetStringUTFChars(key, 0);
    std::string native_key = std::string(char_key);
    glm::vec3 light_vec3 = light->getVec3(native_key);
    jfloatArray jvec3 = env->NewFloatArray(3);
    env->SetFloatArrayRegion(jvec3, 0, 3,
            reinterpret_cast<jfloat*>(&light_vec3));
    env->ReleaseStringUTFChars(key, char_key);
    return jvec3;
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeLight_setVec3(JNIEnv * env,
    jobject obj, jlong jlight, jstring key, jfloat x, jfloat y,
    jfloat z) {
    Light* light = reinterpret_cast<Light*>(jlight);
    const char* char_key = env->GetStringUTFChars(key, 0);
    std::string native_key = std::string(char_key);
    light->setVec3(native_key, glm::vec3(x, y, z));
    env->ReleaseStringUTFChars(key, char_key);
}

JNIEXPORT jfloatArray JNICALL
Java_org_gearvrf_NativeLight_getVec4(JNIEnv * env,
    jobject obj, jlong jlight, jstring key) {
    Light* light = reinterpret_cast<Light*>(jlight);
    const char* char_key = env->GetStringUTFChars(key, 0);
    std::string native_key = std::string(char_key);
    glm::vec4 light_vec4 = light->getVec4(native_key);
    jfloatArray jvec4 = env->NewFloatArray(4);
    env->SetFloatArrayRegion(jvec4, 0, 4,
            reinterpret_cast<jfloat*>(&light_vec4));
    env->ReleaseStringUTFChars(key, char_key);
    return jvec4;
}

JNIEXPORT void JNICALL
Java_org_gearvrf_NativeLight_setVec4(JNIEnv * env,
    jobject obj, jlong jlight, jstring key, jfloat x, jfloat y, jfloat z,
    jfloat w) {
    Light* light = reinterpret_cast<Light*>(jlight);
    const char* char_key = env->GetStringUTFChars(key, 0);
    std::string native_key = std::string(char_key);
    light->setVec4(native_key, glm::vec4(x, y, z, w));
    env->ReleaseStringUTFChars(key, char_key);
}
}
