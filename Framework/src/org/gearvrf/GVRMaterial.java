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

package org.gearvrf;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.gearvrf.utility.Colors;
import org.gearvrf.utility.Threads;
import static org.gearvrf.utility.Assert.*;

import android.graphics.Color;

/**
 * This is one of the key GVRF classes: it holds shaders with textures.
 * 
 * You can have invisible {@linkplain GVRSceneObject scene objects:} these have
 * a location and a set of child objects. This can be useful, to move a set of
 * scene objects as a unit, preserving their relative geometry. Invisible scene
 * objects don't need any {@linkplain GVRSceneObject#getRenderData() render
 * data.}
 * 
 * <p>
 * Visible scene objects must have render data
 * {@linkplain GVRSceneObject#attachRenderData(GVRRenderData) attached.} Each
 * {@link GVRRenderData} has a {@link GVRMesh GL mesh} that defines its
 * geometry, and a {@link GVRMaterial} that defines its surface.
 * 
 * <p>
 * Each {@link GVRMaterial} contains two main things:
 * <ul>
 * <li>The id of a (stock or custom) shader, which is used to draw the mesh. See
 * {@link GVRShaderType} and {@link GVRContext#getMaterialShaderManager()}.
 * 
 * <li>Data to pass to the shader. This usually - but not always - means a
 * {@link GVRTexture} and can include other named values to pass to the shader.
 * </ul>
 * 
 * <p>
 * The simplest way to create a {@link GVRMaterial} is to call the
 * {@linkplain GVRMaterial#GVRMaterial(GVRContext) constructor that takes only a
 * GVRContext.} Then you just {@link GVRMaterial#setMainTexture(GVRTexture)
 * setMainTexture()} and you're ready to draw with the default shader, which is
 * called 'unlit' because it simply drapes the texture over the mesh, without
 * any lighting or reflection effects.
 * 
 * <pre>
 * // for example
 * GVRMaterial material = new GVRMaterial(gvrContext);
 * material.setMainTexture(texture);
 * </pre>
 */
public class GVRMaterial extends GVRHybridObject implements
        GVRShaders<GVRMaterialShaderId> {

    private GVRMaterialShaderId shaderId;
    final private Map<String, GVRTexture> textures = new HashMap<String, GVRTexture>();
    // ---------------------------------------------AR------------------------{
    public static float ar_offset_X;
    public static float ar_offset_Y;
    public static float ar_offset_W;
    public static float ar_offset_H;
    // ---------------------------------------------AR------------------------}
    /** Pre-built shader ids. */
    public abstract static class GVRShaderType {

        public abstract static class UnlitHorizontalStereo {
            public static final GVRMaterialShaderId ID = new GVRStockMaterialShaderId(
                    0);
        }

        public abstract static class UnlitVerticalStereo {
            public static final GVRMaterialShaderId ID = new GVRStockMaterialShaderId(
                    1);
        }

        public abstract static class OES {
            public static final GVRMaterialShaderId ID = new GVRStockMaterialShaderId(
                    2);
        }

        public abstract static class OESHorizontalStereo {
            public static final GVRMaterialShaderId ID = new GVRStockMaterialShaderId(
                    3);
        }

        public abstract static class OESVerticalStereo {
            public static final GVRMaterialShaderId ID = new GVRStockMaterialShaderId(
                    4);
        }

        public abstract static class Cubemap {
            public static final GVRMaterialShaderId ID = new GVRStockMaterialShaderId(
                    5);
        }

        public abstract static class CubemapReflection {
            public static final GVRMaterialShaderId ID = new GVRStockMaterialShaderId(
                    6);
        }

        public abstract static class Texture {
            public static final GVRMaterialShaderId ID = new GVRStockMaterialShaderId(
                    7);
        }
    };

    /**
     * A new holder for a shader's uniforms.
     * 
     * @param gvrContext
     *            Current {@link GVRContext}
     * @param shaderId
     *            Id of a {@linkplain GVRShaderType stock} or
     *            {@linkplain GVRMaterialShaderManager custom} shader.
     */
    public GVRMaterial(GVRContext gvrContext, GVRMaterialShaderId shaderId) {
        super(gvrContext, NativeMaterial.ctor(shaderId.ID));
        this.shaderId = shaderId;
        // if texture shader is used, set lighting coefficients to OpenGL
        // default
        // values
        if (shaderId == GVRShaderType.Texture.ID) {
            setAmbientColor(0.2f, 0.2f, 0.2f, 1.0f);
            setDiffuseColor(0.8f, 0.8f, 0.8f, 1.0f);
            setSpecularColor(0.0f, 0.0f, 0.0f, 1.0f);
            setSpecularExponent(0.0f);
        }
    }

    /**
     * A convenience overload: builds a {@link GVRMaterial} that uses the most
     * common stock shader, the {@linkplain GVRShaderType.Texture 'texture'}
     * shader.
     * 
     * @param gvrContext
     *            Current {@link GVRContext}
     */
    public GVRMaterial(GVRContext gvrContext) {
        this(gvrContext, GVRShaderType.Texture.ID);
    }

    GVRMaterial(GVRContext gvrContext, long ptr) {
        super(gvrContext, ptr);
    }

    public GVRMaterialShaderId getShaderType() {
        return shaderId;
    }

    /**
     * Set shader id
     * 
     * @param shaderId
     *            The new shader id.
     */
    public void setShaderType(GVRMaterialShaderId shaderId) {
        this.shaderId = shaderId;
        NativeMaterial.setShaderType(getNative(), shaderId.ID);
    }

    public GVRTexture getMainTexture() {
        return getTexture(MAIN_TEXTURE);
    }

    public void setMainTexture(GVRTexture texture) {
        setTexture(MAIN_TEXTURE, texture);
    }

    public void setMainTexture(Future<GVRTexture> texture) {
        setTexture(MAIN_TEXTURE, texture);
    }
    
    // ---------------------------------------------AR------------------------{
    public void setArObject(Boolean arObj, Boolean ar) {
        setArObj(MAIN_TEXTURE, arObj, ar);
    }

    public void setAR(Boolean ar) {
        setAR(MAIN_TEXTURE, ar);
    }

    public void setMVPmat(float MVPmatrix[]) {
        setMVPmatrix(MAIN_TEXTURE, MVPmatrix);
    }
    
    public void setMVmat(float MVmatrix[]) {
        setMVmatrix(MAIN_TEXTURE, MVmatrix);
    }

    public void setAROffsets() {
        setAROffsets(MAIN_TEXTURE, ar_offset_X, ar_offset_Y, ar_offset_W, ar_offset_H);
    }

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
    
    public void setAROffsets(String key, float offsetX, float offsetY,
            float offsetWidth, float offsetHeight) {
        checkStringNotNullOrEmpty("key", key);
        NativeMaterial.setAROffsets(getNative(), key, offsetX, offsetY,
                offsetWidth, offsetHeight);
    }
    // ---------------------------------------------AR------------------------}
    /**
     * Get the {@code color} uniform.
     * 
     * By convention, GVRF shaders can use a {@code vec3} uniform named
     * {@code color}. With the default {@linkplain GVRShaderType.Unlit 'unlit'
     * shader,} this allows you to add an overlay color on top of the texture.
     * 
     * @return The current {@code vec3 color} as a three-element array
     */
    public float[] getColor() {
        return getVec3("color");
    }

    /**
     * A convenience method that wraps {@link #getColor()} and returns an
     * Android {@link Color}
     * 
     * @return An Android {@link Color}
     */
    public int getRgbColor() {
        return Colors.toColor(getColor());
    }

    /**
     * Set the {@code color} uniform.
     * 
     * By convention, GVRF shaders can use a {@code vec3} uniform named
     * {@code color}. With the default {@linkplain GVRShaderType.Unlit 'unlit'
     * shader,} this allows you to add an overlay color on top of the texture.
     * Values are between {@code 0.0f} and {@code 1.0f}, inclusive.
     * 
     * @param r
     *            Red
     * @param g
     *            Green
     * @param b
     *            Blue
     */
    public void setColor(float r, float g, float b) {
        setVec3("color", r, g, b);
    }

    /**
     * A convenience overload of {@link #setColor(float, float, float)} that
     * lets you use familiar Android {@link Color} values.
     * 
     * @param color
     *            Any Android {@link Color}; the alpha byte is ignored.
     */
    public void setColor(int color) {
        setColor(Colors.byteToGl(Color.red(color)), //
                Colors.byteToGl(Color.green(color)), //
                Colors.byteToGl(Color.blue(color)));
    }

    /**
     * Get the {@code materialAmbientColor} uniform.
     * 
     * By convention, GVRF shaders can use a {@code vec4} uniform named
     * {@code materialAmbientColor}. With the {@linkplain GVRShaderType.Lit 
     * 'lit' shader,} this allows you to add an overlay color on top of the
     * texture.
     * 
     * @return The current {@code vec4 materialAmbientColor} as a four-element
     *         array
     */
    public float[] getAmbientColor() {
        return getVec4("ambient_color");
    }

    /**
     * Set the {@code materialAmbientColor} uniform for lighting.
     * 
     * By convention, GVRF shaders can use a {@code vec4} uniform named
     * {@code materialAmbientColor}. With the {@linkplain GVRShaderType.Lit 
     * 'lit' shader,} this allows you to add an overlay ambient light color on
     * top of the texture. Values are between {@code 0.0f} and {@code 1.0f},
     * inclusive.
     * 
     * @param r
     *            Red
     * @param g
     *            Green
     * @param b
     *            Blue
     * @param a
     *            Alpha
     */
    public void setAmbientColor(float r, float g, float b, float a) {
        setVec4("ambient_color", r, g, b, a);
    }

    /**
     * Get the {@code materialDiffuseColor} uniform.
     * 
     * By convention, GVRF shaders can use a {@code vec4} uniform named
     * {@code materialDiffuseColor}. With the {@linkplain GVRShaderType.Lit 
     * 'lit' shader,} this allows you to add an overlay color on top of the
     * texture.
     * 
     * @return The current {@code vec4 materialDiffuseColor} as a four-element
     *         array
     */
    public float[] getDiffuseColor() {
        return getVec4("diffuse_color");
    }

    /**
     * Set the {@code materialDiffuseColor} uniform for lighting.
     * 
     * By convention, GVRF shaders can use a {@code vec4} uniform named
     * {@code materialDiffuseColor}. With the {@linkplain GVRShaderType.Lit 
     * 'lit' shader,} this allows you to add an overlay diffuse light color on
     * top of the texture. Values are between {@code 0.0f} and {@code 1.0f},
     * inclusive.
     * 
     * @param r
     *            Red
     * @param g
     *            Green
     * @param b
     *            Blue
     * @param a
     *            Alpha
     */
    public void setDiffuseColor(float r, float g, float b, float a) {
        setVec4("diffuse_color", r, g, b, a);
    }

    /**
     * Get the {@code materialSpecularColor} uniform.
     * 
     * By convention, GVRF shaders can use a {@code vec4} uniform named
     * {@code materialSpecularColor}. With the {@linkplain GVRShaderType.Lit 
     * 'lit' shader,} this allows you to add an overlay color on top of the
     * texture.
     * 
     * @return The current {@code vec4 materialSpecularColor} as a four-element
     *         array
     */
    public float[] getSpecularColor() {
        return getVec4("specular_color");
    }

    /**
     * Set the {@code materialSpecularColor} uniform for lighting.
     * 
     * By convention, GVRF shaders can use a {@code vec4} uniform named
     * {@code materialSpecularColor}. With the {@linkplain GVRShaderType.Lit 
     * 'lit' shader,} this allows you to add an overlay specular light color on
     * top of the texture. Values are between {@code 0.0f} and {@code 1.0f},
     * inclusive.
     * 
     * @param r
     *            Red
     * @param g
     *            Green
     * @param b
     *            Blue
     * @param a
     *            Alpha
     */
    public void setSpecularColor(float r, float g, float b, float a) {
        setVec4("specular_color", r, g, b, a);
    }

    /**
     * Get the {@code materialSpecularExponent} uniform.
     * 
     * By convention, GVRF shaders can use a {@code float} uniform named
     * {@code materialSpecularExponent}. With the {@linkplain GVRShaderType.Lit
     * 'lit' shader,} this allows you to add an overlay color on top of the
     * texture.
     * 
     * @return The current {@code vec4 materialSpecularExponent} as a float
     *         value.
     */
    public float getSpecularExponent() {
        return getFloat("specular_exponent");
    }

    /**
     * Set the {@code materialSpecularExponent} uniform for lighting.
     * 
     * By convention, GVRF shaders can use a {@code float} uniform named
     * {@code materialSpecularExponent}. With the {@linkplain GVRShaderType.Lit
     * 'lit' shader,} this allows you to add an overlay specular light color on
     * top of the texture. Values are between {@code 0.0f} and {@code 128.0f},
     * inclusive.
     * 
     * @param exp
     *            Specular exponent
     */
    public void setSpecularExponent(float exp) {
        setFloat("specular_exponent", exp);
    }

    /**
     * Get the opacity.
     * 
     * This method returns the {@code opacity} uniform.
     * 
     * The {@linkplain #setOpacity(float) setOpacity() documentation} explains
     * what the {@code opacity} uniform does.
     * 
     * @return The {@code opacity} uniform used to render this material
     */
    public float getOpacity() {
        return getFloat("opacity");
    }

    /**
     * Set the opacity, in a complicated way.
     * 
     * There are two things you need to know, how opacity is applied, and how
     * opacity is implemented.
     * 
     * <p>
     * First, GVRF does not sort by distance every object it can see, then draw
     * from back to front. Rather, it sorts every object by
     * {@linkplain GVRRenderData#getRenderingOrder() render order,} then draws
     * the {@linkplain GVRScene scene graph} in traversal order. So, if you want
     * to see a scene object through another scene object, you have to
     * explicitly {@linkplain GVRRenderData#setRenderingOrder(int) set the
     * rendering order} so that the translucent object draws after the opaque
     * object. You can use any integer values you like, but GVRF supplies
     * {@linkplain GVRRenderData.GVRRenderingOrder four standard values;} the
     * {@linkplain GVRRenderData#getRenderingOrder() default value} is
     * {@linkplain GVRRenderData.GVRRenderingOrder#GEOMETRY GEOMETRY.}
     * 
     * <p>
     * Second, technically all this method does is set the {@code opacity}
     * uniform. What this does depends on the actual shader. If you don't
     * specify a shader (or you specify the
     * {@linkplain GVRMaterial.GVRShaderType.Unlit#ID unlit} shader) setting
     * {@code opacity} does exactly what you expect; you only have to worry
     * about the render order. However, it is totally up to a custom shader
     * whether or how it will handle opacity.
     * 
     * @param opacity
     *            Value between {@code 0.0f} and {@code 1.0f}, inclusive.
     */
    public void setOpacity(float opacity) {
        setFloat("opacity", opacity);
    }

    public GVRTexture getTexture(String key) {
        return textures.get(key);
    }

    public void setTexture(String key, GVRTexture texture) {
        checkStringNotNullOrEmpty("key", key);
        checkNotNull("texture", texture);
        textures.put(key, texture);
        NativeMaterial.setTexture(getNative(), key, texture.getNative());
    }

    public void setTexture(final String key, final Future<GVRTexture> texture) {
        Threads.spawn(new Runnable() {

            @Override
            public void run() {
                try {
                    setTexture(key, texture.get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public float getFloat(String key) {
        return NativeMaterial.getFloat(getNative(), key);
    }

    public void setFloat(String key, float value) {
        checkStringNotNullOrEmpty("key", key);
        checkFloatNotNaNOrInfinity("value", value);
        NativeMaterial.setFloat(getNative(), key, value);
    }

    public float[] getVec2(String key) {
        return NativeMaterial.getVec2(getNative(), key);
    }

    public void setVec2(String key, float x, float y) {
        checkStringNotNullOrEmpty("key", key);
        NativeMaterial.setVec2(getNative(), key, x, y);
    }

    public float[] getVec3(String key) {
        return NativeMaterial.getVec3(getNative(), key);
    }

    public void setVec3(String key, float x, float y, float z) {
        checkStringNotNullOrEmpty("key", key);
        NativeMaterial.setVec3(getNative(), key, x, y, z);
    }

    public float[] getVec4(String key) {
        return NativeMaterial.getVec4(getNative(), key);
    }

    public void setVec4(String key, float x, float y, float z, float w) {
        checkStringNotNullOrEmpty("key", key);
        NativeMaterial.setVec4(getNative(), key, x, y, z, w);
    }

    /**
     * Bind a {@code mat4} to the shader uniform {@code key}.
     * 
     * @param key
     *            Name of the shader uniform
     */
    public void setMat4(String key, float x1, float y1, float z1, float w1,
            float x2, float y2, float z2, float w2, float x3, float y3,
            float z3, float w3, float x4, float y4, float z4, float w4) {
        checkStringNotNullOrEmpty("key", key);
        NativeMaterial.setMat4(getNative(), key, x1, y1, z1, w1, x2, y2, z2,
                w2, x3, y3, z3, w3, x4, y4, z4, w4);
    }

}

class NativeMaterial {
    static native long ctor(int shaderType);

    static native void setShaderType(long material, long shaderType);

    static native void setTexture(long material, String key, long texture);

    static native float getFloat(long material, String key);

    static native void setFloat(long material, String key, float value);

    static native float[] getVec2(long material, String key);

    static native void setVec2(long material, String key, float x, float y);

    static native float[] getVec3(long material, String key);

    static native void setVec3(long material, String key, float x, float y,
            float z);

    static native float[] getVec4(long material, String key);

    static native void setVec4(long material, String key, float x, float y,
            float z, float w);

    static native void setMat4(long material, String key, float x1, float y1,
            float z1, float w1, float x2, float y2, float z2, float w2,
            float x3, float y3, float z3, float w3, float x4, float y4,
            float z4, float w4);

 // ---------------------------------------------AR------------------------{
    public static native void setArObj(long material, String key,
            Boolean arObj, Boolean isAR);

    public static native void setAR(long material, String key, Boolean isAR);

    public static native void setMVPmatrix(long material, String key,
            float MVPmatrix[]);
    
    public static native void setMVmatrix(long material, String key,
            float MVmatrix[]);

    public static native void setAROffsets(long material, String key,
            float offsetX, float offsetY, float offsetWidth, float offsetHeight);
 // ---------------------------------------------AR------------------------}

}
