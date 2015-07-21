Changes in Framework (Integration of GVRf with Vuforia(AR))
===========================================================

1. Framework -> org.gearvrf -> GVRSceneObject.java
---------------------------------------------------
The following code snippets need to be added in the above mentioned file.
(i)	This code snippet builds a constructor for the GVRSceneObject in which we pass a flag indicating that we are building an AR application and this will create GVRSceneObject for the camera texture.
```java
    	public GVRSceneObject(GVRContext gvrContext, GVRMesh mesh,
            GVRTexture texture, GVRMaterialShaderId shaderId, Boolean isAr) {
        this(gvrContext, mesh);	
        GVRMaterial material = new GVRMaterial(gvrContext, shaderId);
        material.setMainTexture(texture);
        material.setAR(isAr);
		material.setAROffsets();
        getRenderData().setMaterial(material);
		}

		public GVRSceneObject(GVRContext gvrContext, GVRMesh mesh, 
    		GVRTexture texture, Boolean isAr) {
        this(gvrContext, mesh, texture, STANDARD_SHADER, isAr);
		}
```

(ii) This code snippet builds a constructor for the GVRSceneObject in which we pass the flag for rendering the AR object and also we pass a flag indicating that we are in AR mode as this information is further needed for rendering.
```java
	GVRMaterial material_ar;
   	public GVRSceneObject(GVRContext gvrContext, GVRMesh mesh,
       GVRTexture texture, GVRMaterialShaderId shaderId, Boolean arObj, Boolean isAr){
        	this(gvrContext, mesh);
    material_ar = new GVRMaterial(gvrContext, shaderId);
    material_ar.setMainTexture(texture);
    material_ar.setArObject(arObj, isAr);
	material_ar.setAROffsets();
    getRenderData().setMaterial(material_ar);
   	}

   	public GVRSceneObject(GVRContext gvrContext, GVRMesh mesh, 
    		GVRTexture texture, Boolean arObj, Boolean isAr) {
    this(gvrContext, mesh, texture, STANDARD_SHADER, arObj, isAr);
    }
```

(iii) This code snippet add a function setMVPMatrix which will receive the modelviewprojection matrix for the rendering of the AR object on the marker and passes this matrix to the native side of framework for accurate rendering.
```java
    public void setMVPMatrix(float mvpmatrix[]) {
        material_ar.setMVPmat(mvpmatrix);
    }
    
    public void setMVMatrix(float mvmatrix[]) {
        material_ar.setMVPmat(mvmatrix);
    }
```

2. Framework -> org.gearvrf -> GVRMaterial.java
-----------------------------------------------
The following code snippets need to be added in the above mentioned file.
```java
//add the following static variables
public static float ar_offset_X;
public static float ar_offset_Y;
public static float ar_offset_W;
public static float ar_offset_H;
public void setArObject(Boolean arObj, Boolean ar) {
	setArObj(MAIN_TEXTURE, arObj, ar);
}
    
public void setAR(Boolean ar) {
	setAR(MAIN_TEXTURE, ar);
}

public void setMVPmat(float mat[]) {
	setMVPmatrix(MAIN_TEXTURE, mat);
}

public void setMVmat(float MVmatrix[]) {
	setMVmatrix(MAIN_TEXTURE, MVmatrix);
}

public void setAROffsets() {
	setAROffsets(MAIN_TEXTURE, ar_offset_X, ar_offset_Y, ar_offset_W, ar_offset_H);
}
//---------------
public void setArObj(String key, Boolean arObj, Boolean ar) {
	checkStringNotNullOrEmpty("key", key);
	NativeMaterial.setArObj(getNative(), key, arObj, ar);
}
    
public void setAR(String key, Boolean ar) {
	checkStringNotNullOrEmpty("key", key);
	NativeMaterial.setAR(getNative(), key, ar);
}  
    
public void setMVPmatrix(String key, float MVPmatrix[]) {
	checkStringNotNullOrEmpty("key", key);
	NativeMaterial.setMVPmatrix(getNative(), key, MVPmatrix);
}

public void setMVmatrix(String key, float MVmatrix[]) {
	checkStringNotNullOrEmpty("key", key);
	NativeMaterial.setMVmatrix(getNative(), key, MVmatrix);
}

public void setAROffsets(String key, float offsetX, float offsetY, float offsetWidth, float offsetHeight) {
	checkStringNotNullOrEmpty("key", key);
	NativeMaterial.setAROffsets(getNative(), key, offsetX, offsetY, offsetWidth, offsetHeight);
}

public static native void setArObj(long material, String key, Boolean arObj, Boolean isAR);
public static native void setAR(long material, String key, Boolean isAR);
public static native void setMVPmatrix(long material, String key,
		float MVPmatrix[]);
public static native void setMVmatrix(long material, String key,
		float MVmatrix[]);
public static native void setAROffsets(long material, String key,
            float offsetX, float offsetY, float offsetWidth, float offsetHeight);
```

3. Framework -> org.gearvrf -> GVRRenderTexture.java
-----------------------------------------------------
The following code snippets needs to be added to get the frame buffer ID which is needed to bind the camera texture to frame buffer.
```java
public int getFBOId() {
	return NativeRenderTexture.getFBOId(getNative());   
}

public static native int getFBOId(long renderTexture);
```

4. Framework -> jni -> objects -> textures -> render_texture_jni.cpp
---------------------------------------------------------------------
The following code snippets need to be added in the above mentioned file.
```java
JNIEXPORT jint JNICALL Java_org_gearvrf_NativeRenderTexture_getFBOId(JNIEnv * env,
	jobject obj, jlong jrender_texture);
JNIEXPORT jint JNICALL Java_org_gearvrf_NativeRenderTexture_getFBOId(JNIEnv * env, jobject obj, jlong jrender_texture) {
	RenderTexture *render_texture =
	reinterpret_cast<RenderTexture*>(jrender_texture);
	return render_texture->getFrameBufferId();
}
```

5. Framework -> jni -> objects -> material_jni.cpp
--------------------------------------------------
The following code snippets need to be added in the above mentioned file.
(i) Passing the flag for AR object and AR mode.
```cpp
JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setArObj(JNIEnv * env,
	jobject obj,jlong jmaterial, jstring key, jboolean arObj, jboolean isAR);

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_ setArObj(JNIEnv * env,
	jobject obj, jlong jmaterial,jstring key, jboolean arObj, jboolean isAR) {
	Material* material = reinterpret_cast<Material*>(jmaterial);
	const char* char_key = env->GetStringUTFChars(key, 0);
	std::string native_key = std::string(char_key);
	material->setObjectAR(native_key, arObj, isAR);
}
```

(ii) Passing a flag indicating that we need to render in AR mode.
```cpp
JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setAR(JNIEnv * env,
	jobject obj,jlong jmaterial, jstring key, jboolean isAR);

 JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setAR(JNIEnv * env,
	jobject obj,jlong jmaterial, jstring key, jboolean isAR) {
	Material* material = reinterpret_cast<Material*>(jmaterial);
	const char* char_key = env->GetStringUTFChars(key, 0);
	std::string native_key = std::string(char_key);
	material->setAR(native_key, isAR);
	env->ReleaseStringUTFChars(key, char_key);
}
```

(iii) Passing the MVP matrix for the rendering of the AR object on the marker.
```cpp
JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setMVPmatrix(JNIEnv * env,
        jobject obj,jlong jmaterial, jstring key, jfloatArray MVPmatrix);

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setMVmatrix(JNIEnv * env,
        jobject obj,jlong jmaterial, jstring key, jfloatArray MVmatrix);

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setMVPmatrix(JNIEnv * env, 
		jobject obj,jlong jmaterial, jstring key, jfloatArray MVPmatrix) {
	Material* material = reinterpret_cast<Material*>(jmaterial);
	const char* char_key = env->GetStringUTFChars(key, 0);
	std::string native_key = std::string(char_key);
	jfloat* MVP_matrix = env->GetFloatArrayElements(MVPmatrix,0);
	glm::mat4 mat(MVP_matrix[0], MVP_matrix[1], MVP_matrix[2], MVP_matrix[3], MVP_matrix[4], MVP_matrix[5], MVP_matrix[6], MVP_matrix[7], MVP_matrix[8], MVP_matrix[9], MVP_matrix[10], MVP_matrix[11], MVP_matrix[12], MVP_matrix[13], MVP_matrix[14], MVP_matrix[15]);
	material->setMVPmatrix(native_key, mat);
	env->ReleaseStringUTFChars(key, char_key);
	env->ReleaseFloatArrayElements(MVPmatrix,MVP_matrix,0);
}

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setMVmatrix(JNIEnv * env, 
		jobject obj,jlong jmaterial, jstring key, jfloatArray MVmatrix) {
	Material* material = reinterpret_cast<Material*>(jmaterial);
	const char* char_key = env->GetStringUTFChars(key, 0);
	std::string native_key = std::string(char_key);
	jfloat* MV_matrix = env->GetFloatArrayElements(MVmatrix,0);
	glm::mat4 mat(MV_matrix[0], MV_matrix[1], MV_matrix[2], MV_matrix[3], MV_matrix[4], MV_matrix[5], MV_matrix[6], MV_matrix[7], MV_matrix[8], MV_matrix[9], MV_matrix[10], MV_matrix[11], MV_matrix[12], MV_matrix[13], MV_matrix[14], MV_matrix[15]);
	material->setMVmatrix(native_key, mat);
	env->ReleaseStringUTFChars(key, char_key);
	env->ReleaseFloatArrayElements(MVmatrix,MV_matrix,0);
}
```

(iv) Passing the offset values for setting the viewport to render in AR mode for see through experience.
```cpp
JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setAROffsets(JNIEnv * env,
jobject obj,jlong jmaterial, jstring key, jfloat ar_offset_X, jfloat ar_offset_Y, jfloat ar_offset_W, jfloat ar_offset_H);

JNIEXPORT void JNICALL Java_org_gearvrf_NativeMaterial_setAROffsets(JNIEnv * env,
jobject obj,jlong jmaterial, jstring key, jfloat ar_offset_X, jfloat ar_offset_Y, jfloat ar_offset_W, jfloat ar_offset_H)
{
	Material* material = reinterpret_cast<Material*>(jmaterial);
	const char* char_key = env->GetStringUTFChars(key, 0);
	std::string native_key = std::string(char_key);
	material->setAROffsets(native_key, ar_offset_X, ar_offset_Y, ar_offset_W, ar_offset_H);
	env->ReleaseStringUTFChars(key, char_key);
}
```

5. Framework -> jni -> objects -> material.h
--------------------------------------------------
(i)	Add the following variables:
```cpp
public:
bool isAR_=false;
bool isARobj_=false; 
float arOffX_=0.0f, arOffY_=0.0f, arOffWidth_=0.0f, arOffHeight_=0.0f;
glm::mat4 mvpMatrix;
glm::mat4 mvMatrix;
```

(ii) Add the following functions:
```cpp
void setObjectAR(std::string key, bool arObj, bool ar) {
	isAR_=ar;
	isARobj=arObj;
}

void setAR(std::string key, bool ar) {
	isAR_=ar;
}

void setMVPmatrix(std::string key, glm::mat4 mat) {
	mvpMatrix = mat;
}

void setMVmatrix(std::string key, glm::mat4 mat) {
	mvMatrix = mat;
}

void setAROffsets(std::string key, float ar_offset_X, float ar_offset_Y, float ar_offset_W, float ar_offset_H) {
        arOffsetX_ = ar_offset_X;
        arOffsetY_ = ar_offset_Y;
        arOffsetWidth_ = ar_offset_W;
        arOffsetHeight_ = ar_offset_H;
}
```

7.	Framework -> jni -> engine -> renderer -> renderer.cpp
-----------------------------------------------------------
(i)	Add the following variables:
```cpp
bool setIsAr;
float MVmatrix[16];
float ar_offset_X, ar_offset_Y, ar_offset_W, ar_offset_H;
```

(ii) In the whole file, replace  
```cpp 
glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
```
with:
```cpp
if(setIsAr)
   glViewport(viewportX + ar_offset_X, viewportY + ar_offset_Y,
			viewportWidth - ar_offset_W, viewportHeight - ar_offset_H); 
else glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
```

We are changing the viewport parameters for the AR mode so as to have a see through experience. The default viewport settings will give a zoomed view of the scene we see from camera and it will not give the feel of see through reality. So viewport needs to be adjusted accordingly.

(iii) Add the following lines of code in the given function :
```cpp
void Renderer::renderRenderData(…)
{
	if (render_data->pass(0)->material()->isAR_) {
			setIsAr = true;
			ar_offset_X = render_data->pass(0)->material()->arOffX_;
			ar_offset_Y = render_data->pass(0)->material()->arOffY_;
			ar_offset_W = render_data->pass(0)->material()->arOffWidth_;
			ar_offset_H = render_data->pass(0)->material()->arOffHeight_;
			MVPmatrix = render_data->pass(0)->material()->mvpMatrix;
			MVmatrix = render_data->pass(0)->material()->mvMatrix;
    }
    setIsArObj = render_data->pass(0)->material()->isARobj_;
    MVPmatrix = render_data->pass(0)->material()->mvpMatrix;
    MVmatrix = render_data->pass(0)->material()->mvMatrix;

//further lines of code
//………

try{

//…………

	case Material::ShaderType::TEXTURE_SHADER: {
		  if(render_data->material()->isAR && render_data->material()->isARobj) {
				/* We will pass the mvp matrix which we received from the AR tracking so as to render the AR object perfectly on the position of the marker in the real world. */
				// ---------------------------------------------AR------------------------{
				if (setIsAr && setIsArObj) {
					shader_manager->getTextureShader()->render(
					MVmatrix,
					glm::inverseTranspose(MVmatrix),
					MVPmatrix, render_data, curr_material);
				} else
				// ---------------------------------------------AR------------------------}
				shader_manager->getTextureShader()->render(
				mv_matrix,
				glm::inverseTranspose(mv_matrix),
				mvp_matrix, render_data, curr_material);
			}
				break;

//further lines of code
//………
//………
}
```

8.	Framework -> org.gearvrf -> GVRXMLParser.java
--------------------------------------------------
The following code snippet needs to be added to get the offset values to the set the viewport in the AR mode.
```cpp
GVRXMLParser(AssetManager assets, String fileName) {
	try {
//……………….
//……
} else if (attributeName.equals("fbo-width")) {
							mFBOWidth = Integer.parseInt(xpp
									.getAttributeValue(i));
//……………….
//……add the following code
} else if (attributeName.equals("ar_offsetX")) {
							mAr_offsetX = Float.parseFloat(xpp
									.getAttributeValue(i));
		  } else if (attributeName.equals("ar_offsetY")) {
							mAr_offsetY = Float.parseFloat(xpp
									.getAttributeValue(i));
		  } else if (attributeName.equals("ar_offsetWidth")) {
							mAr_offsetWidth = Float.parseFloat(xpp
									.getAttributeValue(i));
		  } else if (attributeName.equals("ar_offsetHeight")) {
							mAr_offsetHeight = Float.parseFloat(xpp
									.getAttributeValue(i));
}
//……………….
//……
}

//also add the following functions
float getAROffsetX() {
return mAr_offsetX;
}

float getAROffsetY() {
	return mAr_offsetY;
}

float getAROffsetWidth() {
	return mAr_offsetWidth;
}

float getAROffHeight() {
	return mAr_offsetHeight;
}
```

9.	Framework -> org.gearvrf -> GVRViewManager.java
----------------------------------------------------
The following code snippet needs to be added to set the offset values for the viewport in the AR mode.
```cpp
GVRViewManager(GVRActivity gvrActivity, GVRScript gvrScript,
		String distortionDataFileName) {
	super(gvrActivity);
	//………………..
	//………………..add the following lines of code
	float x,y,w,h;
	x=xmlParser.getAROffsetX();
	y=xmlParser.getAROffsetY();
	w=xmlParser.getAROffsetWidth();
	h=xmlParser.getAROffHeight();
	GVRMaterial.ar_offset_X=x;
	GVRMaterial.ar_offset_Y=y;
	GVRMaterial.ar_offset_W=w;
	GVRMaterial.ar_offset_H=h;
```

10.	OurArApplication -> assets -> gvr_note4.xml
-----------------------------------------------
Four more variables (i.e. offset, offset, offsetWidth, offsetHeight) need to be added in this xml so that these values can be used by framework for further computation for viewport in AR mode.
```xml
<lens name="N4">
<scene fov-y="90.0" camera-separation-distance="0.062" fbo-width="2560" fbo-height="1440" msaa="1" ar_offsetX="250" ar_offsetY="320" ar_offsetWidth="500" ar_offsetHeight="674" />
</lens>
```

11.	A sample AppViewManager.java for an AR Application
-------------------------------------------------------
	AppActivity.java
	AppViewManager.java
