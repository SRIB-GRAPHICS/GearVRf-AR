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

#include "view_manager.h"
#include "activity_jni.h"
#include <jni.h>
#include "../engine/renderer/renderer.h"
#include "../objects/components/camera.h"

namespace gvr {

extern "C" {

void Java_org_gearvrf_GVRViewManager_renderCamera(JNIEnv * jni, jclass clazz,
		jlong appPtr, jlong jscene, jlong jcamera, jlong jrender_texture,
		jlong jshader_manager, jlong jpost_effect_shader_manager,
		jlong jpost_effect_render_texture_a,
		jlong jpost_effect_render_texture_b) {
	GVRActivity *activity = (GVRActivity*) ((OVR::App *) appPtr)->GetAppInterface();

	Scene* scene = reinterpret_cast<Scene*>(jscene);
	Camera* camera = reinterpret_cast<Camera*>(jcamera);
	RenderTexture* render_texture = reinterpret_cast<RenderTexture*>(jrender_texture);
	ShaderManager* shader_manager = reinterpret_cast<ShaderManager*>(jshader_manager);
	PostEffectShaderManager* post_effect_shader_manager =
			reinterpret_cast<PostEffectShaderManager*>(jpost_effect_shader_manager);
	RenderTexture* post_effect_render_texture_a =
			reinterpret_cast<RenderTexture*>(jpost_effect_render_texture_a);
	RenderTexture* post_effect_render_texture_b =
			reinterpret_cast<RenderTexture*>(jpost_effect_render_texture_b);

	activity->viewManager->renderCamera(activity->Scene, scene, camera,
			render_texture, shader_manager, post_effect_shader_manager,
			post_effect_render_texture_a, post_effect_render_texture_b,
			activity->viewManager->mvp_matrix);
}

void Java_org_gearvrf_GVRViewManager_readRenderResultNative(JNIEnv * jni,
		jclass clazz, jlong jrender_texture, jobject jreadback_buffer) {

	uint8_t *pReadbackBuffer = (uint8_t *) jni->GetDirectBufferAddress(
			jreadback_buffer);
	RenderTexture* render_texture = reinterpret_cast<RenderTexture*>(jrender_texture);
	int width = render_texture->width();
	int height = render_texture->height();

	// remember current FBO bindings
	GLint currentReadFBO, currentDrawFBO;
	glGetIntegerv(GL_READ_FRAMEBUFFER_BINDING, &currentReadFBO);
	glGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING, &currentDrawFBO);

	// blit the multisampled FBO to a normal FBO and read from it
	GLuint renderTextureFBO = render_texture->getFrameBufferId();
	glBindFramebuffer(GL_DRAW_FRAMEBUFFER, renderTextureFBO);
	glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
			GL_COLOR_BUFFER_BIT, GL_NEAREST);
	glBindFramebuffer(GL_READ_FRAMEBUFFER, renderTextureFBO);

	glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE,
			pReadbackBuffer);

	// recover FBO bindings
	glBindFramebuffer(GL_READ_FRAMEBUFFER, currentReadFBO);
	glBindFramebuffer(GL_DRAW_FRAMEBUFFER, currentDrawFBO);
}

} // extern "C"

//=============================================================================
//                             GVRViewManager
//=============================================================================

GVRViewManager::GVRViewManager(JNIEnv & jni_, jobject activityObject_) {
	// initial
	m_frameRendered = 0;
	m_fps = 0.0;
	m_startTime = m_currentTime = 0.0f;
	gNumFrame = 0;

	LOG("GVRViewManager::GVRViewManager");
}

GVRViewManager::~GVRViewManager() {
	LOG("GVRViewManager::~GVRViewManager()");
}

void GVRViewManager::renderCamera(OVR::OvrSceneView &ovr_scene,
		Scene* scene,
		Camera* camera,
		RenderTexture* render_texture,
		ShaderManager* shader_manager,
		PostEffectShaderManager* post_effect_shader_manager,
		RenderTexture* post_effect_render_texture_a,
		RenderTexture* post_effect_render_texture_b,
		glm::mat4 mvp) {
#ifdef GVRF_FBO_FPS
	// starting to collect rendering time
	// first flash GPU tasks
	glFinish();

	// collect data
	struct timeval start, end;
	double t1, t2;
	float fps = 0.0;
	gettimeofday(&start, NULL);
#endif

	if (camera->render_mask() == 1) {
		glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
	} else {
		glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
	}
	glClear (GL_COLOR_BUFFER_BIT);

	Renderer::renderCamera(scene, camera, render_texture, shader_manager,
			post_effect_shader_manager, post_effect_render_texture_a,
			post_effect_render_texture_b, mvp);

#ifdef GVRF_FBO_FPS
	// finish rendering
	glFinish();// force rendering

	gettimeofday(&end, NULL);
	t1 = start.tv_sec + (start.tv_usec / 1000000.0);
	t2 = end.tv_sec + (end.tv_usec / 1000000.0);
	gTotalSec += (t2 - t1);
	gNumFrame++;

	fps = (float)gNumFrame/gTotalSec;
	if(!(gNumFrame%50) )
	{
		LOGI("FPS is %.2f gNumFrame=%d, gTotalSec = %.2f", fps, gNumFrame, gTotalSec);
	}
#endif

}
}
