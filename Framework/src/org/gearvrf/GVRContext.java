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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.gearvrf.GVRAndroidResource.BitmapTextureCallback;
import org.gearvrf.GVRAndroidResource.CompressedTextureCallback;
import org.gearvrf.GVRAndroidResource.MeshCallback;
import org.gearvrf.GVRAndroidResource.TextureCallback;
import org.gearvrf.animation.GVRAnimation;
import org.gearvrf.animation.GVRAnimationEngine;
import org.gearvrf.asynchronous.GVRAsynchronousResourceLoader;
import org.gearvrf.asynchronous.GVRCompressedTexture;
import org.gearvrf.asynchronous.GVRCompressedTextureLoader;
import org.gearvrf.jassimp.AiColor;
import org.gearvrf.jassimp.AiMaterial;
import org.gearvrf.jassimp.AiMatrix4f;
import org.gearvrf.jassimp.AiNode;
import org.gearvrf.jassimp.AiScene;
import org.gearvrf.jassimp.AiTextureType;
import org.gearvrf.jassimp.AiWrapperProvider;
import org.gearvrf.jassimp.AiMaterial.Property;
import org.gearvrf.periodic.GVRPeriodicEngine;
import org.gearvrf.utility.Log;
import org.gearvrf.utility.ResourceCache;


import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.KeyEvent;

/**
 * Like the Android {@link Context} class, {@code GVRContext} provides core
 * services, and global information about an application environment.
 * 
 * Use {@code GVRContext} to {@linkplain #createQuad(float, float) create} and
 * {@linkplain #loadMesh(GVRAndroidResource) load} GL meshes, Android
 * {@linkplain #loadBitmap(String) bitmaps}, and
 * {@linkplain #loadTexture(GVRAndroidResource) GL textures.} {@code GVRContext}
 * also holds the {@linkplain GVRScene main scene} and miscellaneous information
 * like {@linkplain #getFrameTime() the frame time.}
 */
public abstract class GVRContext {
    private static final String TAG = Log.tag(GVRContext.class);

    private final GVRActivity mContext;

    /*
     * Fields and constants
     */

    // Priorities constants, for asynchronous loading

    /**
     * GVRF can't use every {@code int} as a priority - it needs some sentinel
     * values. It will probably never need anywhere near this many, but raising
     * the number of reserved values narrows the 'dynamic range' available to
     * apps mapping some internal score to the {@link #LOWEST_PRIORITY} to
     * {@link #HIGHEST_PRIORITY} range, and might change app behavior in subtle
     * ways that seem best avoided.
     * 
     * @since 1.6.1
     */
    public static final int RESERVED_PRIORITIES = 1024;

    /**
     * GVRF can't use every {@code int} as a priority - it needs some sentinel
     * values. A simple approach to generating priorities is to score resources
     * from 0 to 1, and then map that to the range {@link #LOWEST_PRIORITY} to
     * {@link #HIGHEST_PRIORITY}.
     * 
     * @since 1.6.1
     */
    public static final int LOWEST_PRIORITY = Integer.MIN_VALUE
            + RESERVED_PRIORITIES;

    /**
     * GVRF can't use every {@code int} as a priority - it needs some sentinel
     * values. A simple approach to generating priorities is to score resources
     * from 0 to 1, and then map that to the range {@link #LOWEST_PRIORITY} to
     * {@link #HIGHEST_PRIORITY}.
     * 
     * @since 1.6.1
     */
    public static final int HIGHEST_PRIORITY = Integer.MAX_VALUE;

    /**
     * The priority used by
     * {@link #loadBitmapTexture(GVRAndroidResource.BitmapTextureCallback, GVRAndroidResource)}
     * and
     * {@link #loadMesh(GVRAndroidResource.MeshCallback, GVRAndroidResource)}
     * 
     * @since 1.6.1
     */
    public static final int DEFAULT_PRIORITY = 0;

    /**
     * The ID of the GLthread. We use this ID to prevent non-GL thread from
     * calling GL functions.
     * 
     * @since 1.6.5
     */
    protected long mGLThreadID;

    /*
     * Methods
     */

    GVRContext(GVRActivity context) {
        mContext = context;
    }

    /**
     * Get the Android {@link Context}, which provides access to system services
     * and to your application's resources. Since version 2.0.1, this is
     * actually your {@link GVRActivity} implementation, but you should probably
     * use the new {@link #getActivity()} method, rather than casting this
     * method to an {@code (Activity)} or {@code (GVRActivity)}.
     * 
     * @return An Android {@code Context}
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Get the Android {@link Activity} which launched your GVRF app.
     * 
     * An {@code Activity} is-a {@link Context} and so provides access to system
     * services and to your application's resources; the {@code Activity} class
     * also provides additional services, including
     * {@link Activity#runOnUiThread(Runnable)}.
     * 
     * @return The {@link GVRActivity} which launched your GVRF app. The
     *         {@link GVRActivity} class doesn't actually add much useful
     *         functionality besides
     *         {@link GVRActivity#setScript(GVRScript, String)}, but returning
     *         the most-derived class here may prevent someone from having to
     *         write {@code (GVRActivity) gvrContext.getActivity();}.
     * 
     * @since 2.0.1
     */
    public GVRActivity getActivity() {
        return mContext;
    }

    /**
     * Loads a file as a {@link GVRMesh}.
     * 
     * Note that this method can be quite slow; we recommend never calling it
     * from the GL thread. The asynchronous version
     * {@link #loadMesh(GVRAndroidResource.MeshCallback, GVRAndroidResource)} is
     * better because it moves most of the work to a background thread, doing as
     * little as possible on the GL thread.
     * 
     * @param androidResource
     *            Basically, a stream containing a 3D model. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @return The file as a GL mesh.
     * 
     * @since 1.6.2
     */
    public GVRMesh loadMesh(GVRAndroidResource androidResource) {
        GVRMesh mesh = sMeshCache.get(androidResource);
        if (mesh == null) {
            GVRAssimpImporter assimpImporter = GVRImporter
                    .readFileFromResources(this, androidResource);
            mesh = assimpImporter.getMesh(0);
            sMeshCache.put(androidResource, mesh);
        }
        return mesh;
    }

    private final static ResourceCache<GVRMesh> sMeshCache = new ResourceCache<GVRMesh>();

    /**
     * Loads a mesh file, asynchronously, at a default priority.
     * 
     * This method and the
     * {@linkplain #loadMesh(GVRAndroidResource.MeshCallback, GVRAndroidResource, int)
     * overload that takes a priority} are generally going to be your best
     * choices for loading {@link GVRMesh} resources: mesh loading can take
     * hundreds - and even thousands - of milliseconds, and so should not be
     * done on the GL thread in either {@link GVRScript#onInit(GVRContext)
     * onInit()} or {@link GVRScript#onStep() onStep()}.
     * 
     * <p>
     * The asynchronous methods improve throughput in three ways. First, by
     * doing all the work on a background thread, then delivering the loaded
     * mesh to the GL thread on a {@link #runOnGlThread(Runnable)
     * runOnGlThread()} callback. Second, they use a throttler to avoid
     * overloading the system and/or running out of memory. Third, they do
     * 'request consolidation' - if you issue any requests for a particular file
     * while there is still a pending request, the file will only be read once,
     * and each callback will get the same {@link GVRMesh}.
     * 
     * @param callback
     *            App supplied callback, with three different methods.
     *            <ul>
     *            <li>Before loading, GVRF may call
     *            {@link GVRAndroidResource.MeshCallback#stillWanted(GVRAndroidResource)
     *            stillWanted()} (on a background thread) to give you a chance
     *            to abort a 'stale' load.
     * 
     *            <li>Successful loads will call
     *            {@link GVRAndroidResource.Callback#loaded(GVRHybridObject, GVRAndroidResource)
     *            loaded()} on the GL thread.
     * 
     *            <li>Any errors will call
     *            {@link GVRAndroidResource.MeshCallback#failed(Throwable, GVRAndroidResource)
     *            failed(),} with no promises about threading.
     *            </ul>
     * @param androidResource
     *            Basically, a stream containing a 3D model. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @throws IllegalArgumentException
     *             If either parameter is {@code null} or if you 'abuse' request
     *             consolidation by passing the same {@link GVRAndroidResource}
     *             descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     * @since 1.6.2
     */
    public void loadMesh(MeshCallback callback,
            GVRAndroidResource androidResource) throws IllegalArgumentException {
        loadMesh(callback, androidResource, DEFAULT_PRIORITY);
    }

    /**
     * Loads a mesh file, asynchronously, at an explicit priority.
     * 
     * This method and the
     * {@linkplain #loadMesh(GVRAndroidResource.MeshCallback, GVRAndroidResource)
     * overload that supplies a default priority} are generally going to be your
     * best choices for loading {@link GVRMesh} resources: mesh loading can take
     * hundreds - and even thousands - of milliseconds, and so should not be
     * done on the GL thread in either {@link GVRScript#onInit(GVRContext)
     * onInit()} or {@link GVRScript#onStep() onStep()}.
     * 
     * <p>
     * The asynchronous methods improve throughput in three ways. First, by
     * doing all the work on a background thread, then delivering the loaded
     * mesh to the GL thread on a {@link #runOnGlThread(Runnable)
     * runOnGlThread()} callback. Second, they use a throttler to avoid
     * overloading the system and/or running out of memory. Third, they do
     * 'request consolidation' - if you issue any requests for a particular file
     * while there is still a pending request, the file will only be read once,
     * and each callback will get the same {@link GVRMesh}.
     * 
     * @param callback
     *            App supplied callback, with three different methods.
     *            <ul>
     *            <li>Before loading, GVRF may call
     *            {@link GVRAndroidResource.MeshCallback#stillWanted(GVRAndroidResource)
     *            stillWanted()} (on a background thread) to give you a chance
     *            to abort a 'stale' load.
     * 
     *            <li>Successful loads will call
     *            {@link GVRAndroidResource.Callback#loaded(GVRHybridObject, GVRAndroidResource)
     *            loaded()} on the GL thread.
     * 
     *            <li>Any errors will call
     *            {@link GVRAndroidResource.MeshCallback#failed(Throwable, GVRAndroidResource)
     *            failed(),} with no promises about threading.
     *            </ul>
     * @param resource
     *            Basically, a stream containing a 3D model. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>.
     * 
     * @throws IllegalArgumentException
     *             If either {@code callback} or {@code resource} is
     *             {@code null}, or if {@code priority} is out of range - or if
     *             you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     * @since 1.6.2
     */
    public void loadMesh(MeshCallback callback, GVRAndroidResource resource,
            int priority) throws IllegalArgumentException {
        GVRAsynchronousResourceLoader.loadMesh(this, callback, resource,
                priority);
    }

    /**
     * Simple, high-level method to load a mesh asynchronously, for use with
     * {@link GVRRenderData#setMesh(Future)}.
     * 
     * This method uses a default priority; use
     * {@link #loadFutureMesh(GVRAndroidResource, int)} to specify a priority;
     * use one of the lower-level
     * {@link #loadMesh(GVRAndroidResource.MeshCallback, GVRAndroidResource)}
     * methods to get more control over loading.
     * 
     * @param resource
     *            Basically, a stream containing a 3D model. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @return A {@link Future} that you can pass to
     *         {@link GVRRenderData#setMesh(Future)}
     * 
     * @since 1.6.7
     * 
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public Future<GVRMesh> loadFutureMesh(GVRAndroidResource resource) {
        return loadFutureMesh(resource, DEFAULT_PRIORITY);
    }

    /**
     * Simple, high-level method to load a mesh asynchronously, for use with
     * {@link GVRRenderData#setMesh(Future)}.
     * 
     * This method trades control for convenience; use one of the lower-level
     * {@link #loadMesh(GVRAndroidResource.MeshCallback, GVRAndroidResource)}
     * methods if, say, you want to do something more than just
     * {@link GVRRenderData#setMesh(GVRMesh)} when the mesh loads.
     * 
     * @param resource
     *            Basically, a stream containing a 3D model. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>.
     * @return A {@link Future} that you can pass to
     *         {@link GVRRenderData#setMesh(Future)}
     * 
     * @since 1.6.7
     * 
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public Future<GVRMesh> loadFutureMesh(GVRAndroidResource resource,
            int priority) {
        return GVRAsynchronousResourceLoader.loadFutureMesh(this, resource,
                priority);
    }

    /**
     * Simple, high-level method to load a scene as {@link GVRSceneObject} from
     * 3D model.
     * 
     * @param assetRelativeFilename
     *            A filename, relative to the {@code assets} directory. The file
     *            can be in a sub-directory of the {@code assets} directory:
     *            {@code "foo/bar.png"} will open the file
     *            {@code assets/foo/bar.png}
     * 
     * @return A {@link GVRSceneObject} that contains the meshes with textures
     * 
     * @throws IOException
     *             File does not exist or cannot be read
     */
    public GVRSceneObject getAssimpModel(String assetRelativeFilename)
            throws IOException {
        GVRAssimpImporter assimpImporter = GVRImporter.readFileFromResources(
                this, new GVRAndroidResource(this, assetRelativeFilename));

        GVRSceneObject wholeSceneObject = new GVRSceneObject(this);

        AiScene assimpScene = assimpImporter.getAssimpScene();

        AiWrapperProvider<byte[], AiMatrix4f, AiColor, AiNode, byte[]> wrapperProvider = new AiWrapperProvider<byte[], AiMatrix4f, AiColor, AiNode, byte[]>() {

            /**
             * Wraps a RGBA color.
             * <p>
             * 
             * A color consists of 4 float values (r,g,b,a) starting from offset
             * 
             * @param buffer
             *            the buffer to wrap
             * @param offset
             *            the offset into buffer
             * @return the wrapped color
             */
            @Override
            public AiColor wrapColor(ByteBuffer buffer, int offset) {
                AiColor color = new AiColor(buffer, offset);
                return color;
            }

            /**
             * Wraps a 4x4 matrix of floats.
             * <p>
             * 
             * The calling code will allocate a new array for each invocation of
             * this method. It is safe to store a reference to the passed in
             * array and use the array to store the matrix data.
             * 
             * @param data
             *            the matrix data in row-major order
             * @return the wrapped matrix
             */
            @Override
            public AiMatrix4f wrapMatrix4f(float[] data) {

                AiMatrix4f transformMatrix = new AiMatrix4f(data);
                return transformMatrix;
            }

            /**
             * Wraps a quaternion.
             * <p>
             * 
             * A quaternion consists of 4 float values (w,x,y,z) starting from
             * offset
             * 
             * @param buffer
             *            the buffer to wrap
             * @param offset
             *            the offset into buffer
             * @return the wrapped quaternion
             */
            @Override
            public byte[] wrapQuaternion(ByteBuffer buffer, int offset) {
                byte[] quaternion = new byte[4];
                buffer.get(quaternion, offset, 4);
                return quaternion;
            }

            /**
             * Wraps a scene graph node.
             * <p>
             * 
             * See {@link AiNode} for a description of the scene graph structure
             * used by assimp.
             * <p>
             * 
             * The parent node is either null or an instance returned by this
             * method. It is therefore safe to cast the passed in parent object
             * to the implementation specific type
             * 
             * @param parent
             *            the parent node
             * @param matrix
             *            the transformation matrix
             * @param meshReferences
             *            array of mesh references (indexes)
             * @param name
             *            the name of the node
             * @return the wrapped scene graph node
             */
            @Override
            public AiNode wrapSceneNode(Object parent, Object matrix,
                    int[] meshReferences, String name) {

                AiNode node = new AiNode(null, matrix, meshReferences, name);

                return node;
            }

            /**
             * Wraps a vector.
             * <p>
             * 
             * Most vectors are 3-dimensional, i.e., with 3 components. The
             * exception are texture coordinates, which may be 1- or
             * 2-dimensional. A vector consists of numComponents floats (x,y,z)
             * starting from offset
             * 
             * @param buffer
             *            the buffer to wrap
             * @param offset
             *            the offset into buffer
             * @param numComponents
             *            the number of components
             * @return the wrapped vector
             */
            @Override
            public byte[] wrapVector3f(ByteBuffer buffer, int offset,
                    int numComponents) {
                byte[] warpedVector = new byte[numComponents];
                buffer.get(warpedVector, offset, numComponents);
                return warpedVector;
            }
        };

        AiNode rootNode = assimpScene.getSceneRoot(wrapperProvider);

        List<AiNode> childrenNodes = rootNode.getChildren();

        try {
            for (AiNode childNode : childrenNodes) {
                for (int i = 0; i < childNode.getNumMeshes(); i++) {

                    FutureWrapper<GVRMesh> futureMesh = new FutureWrapper<GVRMesh>(
                            this.getNodeMesh(new GVRAndroidResource(this,
                                    assetRelativeFilename),
                                    childNode.getName(), i));

                    AiMaterial material = this
                            .getMeshMaterial(new GVRAndroidResource(this,
                                    assetRelativeFilename),
                                    childNode.getName(), i);

                    Property property = material.getProperty("$tex.file");
                    if (property != null) {
                        int textureIndex = property.getIndex();
                        String texFileName = material.getTextureFile(
                                AiTextureType.DIFFUSE, textureIndex);

                        Future<GVRTexture> futureMeshTexture = this
                                .loadFutureTexture(new GVRAndroidResource(this,
                                        texFileName));

                        GVRSceneObject sceneObject = new GVRSceneObject(this,
                                futureMesh, futureMeshTexture);

                        // add the scene object to the scene graph
                        wholeSceneObject.addChildObject(sceneObject);

                    } else {
                        // The case when there is no texture
                        // This block also takes care for the case when there
                        // are no texture or color for the mesh as the methods
                        // that are used for getting the colors of the material
                        // returns a default when they are not present
                        AiColor diffuseColor = material
                                .getDiffuseColor(wrapperProvider);
                        AiColor ambientColor = material
                                .getAmbientColor(wrapperProvider);
                        float opacity = material.getOpacity();

                        final String DIFFUSE_COLOR_KEY = "diffuse_color";
                        final String AMBIENT_COLOR_KEY = "ambient_color";
                        final String COLOR_OPACITY_KEY = "opacity";

                        final String VERTEX_SHADER = "attribute vec4 a_position;\n"
                                + "uniform mat4 u_mvp;\n"
                                + "void main() {\n"
                                + "  gl_Position = u_mvp * a_position;\n"
                                + "}\n";

                        final String FRAGMENT_SHADER = "precision mediump float;\n"
                                + "uniform vec4 diffuse_color;\n" //
                                + "uniform vec4 ambient_color;\n"
                                + "uniform float opacity;\n"
                                + "void main() {\n" //
                                + "  gl_FragColor = ( diffuse_color * opacity ) + ambient_color;\n"
                                + "}\n";

                        GVRCustomMaterialShaderId mShaderId;
                        GVRMaterialMap mCustomShader = null;

                        final GVRMaterialShaderManager shaderManager = this
                                .getMaterialShaderManager();
                        mShaderId = shaderManager.addShader(VERTEX_SHADER,
                                FRAGMENT_SHADER);
                        mCustomShader = shaderManager.getShaderMap(mShaderId);
                        mCustomShader.addUniformVec4Key("diffuse_color",
                                DIFFUSE_COLOR_KEY);
                        mCustomShader.addUniformVec4Key("ambient_color",
                                AMBIENT_COLOR_KEY);
                        mCustomShader.addUniformFloatKey("opacity",
                                COLOR_OPACITY_KEY);

                        GVRMaterial meshMaterial = new GVRMaterial(this,
                                mShaderId);

                        meshMaterial
                                .setVec4(DIFFUSE_COLOR_KEY,
                                        diffuseColor.getRed(),
                                        diffuseColor.getGreen(),
                                        diffuseColor.getBlue(),
                                        diffuseColor.getAlpha());

                        meshMaterial
                                .setVec4(AMBIENT_COLOR_KEY,
                                        ambientColor.getRed(),
                                        ambientColor.getGreen(),
                                        ambientColor.getBlue(),
                                        ambientColor.getAlpha());

                        meshMaterial.setFloat(COLOR_OPACITY_KEY, opacity);

                        GVRSceneObject sceneObject = new GVRSceneObject(this);
                        GVRRenderData sceneObjectRenderData = new GVRRenderData(
                                this);
                        sceneObjectRenderData.setMesh(futureMesh);
                        sceneObjectRenderData.setMaterial(meshMaterial);
                        sceneObject.attachRenderData(sceneObjectRenderData);

                        wholeSceneObject.addChildObject(sceneObject);
                    }
                }
            }
        } catch (Exception e) {
            // Error while recursing the Scene Graph
            e.printStackTrace();
        }
        return wholeSceneObject;
    }

    /**
     * Retrieves the particular index mesh for the given node.
     * 
     * @return The mesh, encapsulated as a {@link GVRMesh}.
     */
    public GVRMesh getNodeMesh(GVRAndroidResource androidResource,
            String nodeName, int meshIndex) {
        GVRAssimpImporter assimpImporter = GVRImporter.readFileFromResources(
                this, androidResource);
        return assimpImporter.getNodeMesh(nodeName, meshIndex);
    }

    /**
     * Retrieves the material for the mesh of the given node..
     * 
     * @return The material, encapsulated as a {@link AiMaterial}.
     */
    public AiMaterial getMeshMaterial(GVRAndroidResource androidResource,
            String nodeName, int meshIndex) {
        GVRAssimpImporter assimpImporter = GVRImporter.readFileFromResources(
                this, androidResource);
        return assimpImporter.getMeshMaterial(nodeName, meshIndex);
    }

    /**
     * Creates a quad consisting of two triangles, with the specified width and
     * height.
     * 
     * @param width
     *            the quad's width
     * @param height
     *            the quad's height
     * @return A 2D, rectangular mesh with four vertices and two triangles
     */
    public GVRMesh createQuad(float width, float height) {
        GVRMesh mesh = new GVRMesh(this);

        float[] vertices = { width * -0.5f, height * 0.5f, 0.0f, width * -0.5f,
                height * -0.5f, 0.0f, width * 0.5f, height * 0.5f, 0.0f,
                width * 0.5f, height * -0.5f, 0.0f };
        mesh.setVertices(vertices);

        final float[] normals = { 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f, 1.0f };
        mesh.setNormals(normals);

        final float[] texCoords = { 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f,
                1.0f };
        mesh.setTexCoords(texCoords);

        char[] triangles = { 0, 1, 2, 1, 3, 2 };
        mesh.setTriangles(triangles);

        return mesh;
    }

    /**
     * Loads file placed in the assets folder, as a {@link Bitmap}.
     * 
     * <p>
     * Note that this method may take hundreds of milliseconds to return: unless
     * the bitmap is quite tiny, you probably don't want to call this directly
     * from your {@link GVRScript#onStep() onStep()} callback as that is called
     * once per frame, and a long call will cause you to miss frames.
     * 
     * <p>
     * Note also that this method does no scaling, and will return a full-size
     * {@link Bitmap}. Loading (say) an unscaled photograph may abort your app:
     * Use pre-scaled images, or {@link BitmapFactory} methods which give you
     * more control over the image size.
     * 
     * @param fileName
     *            The name of a file, relative to the assets directory. The
     *            assets directory may contain an arbitrarily complex tree of
     *            subdirectories; the file name can specify any location in or
     *            under the assets directory.
     * @return The file as a bitmap, or {@code null} if file path does not exist
     *         or the file can not be decoded into a Bitmap.
     */
    public Bitmap loadBitmap(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("File name should not be null.");
        }
        InputStream stream = null;
        Bitmap bitmap = null;
        try {
            try {
                stream = mContext.getAssets().open(fileName);
                return bitmap = BitmapFactory.decodeStream(stream);
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Don't discard a valid Bitmap because of an IO error closing the
            // file!
            return bitmap;
        }
    }

    /**
     * Loads file placed in the assets folder, as a {@link GVRBitmapTexture}.
     * 
     * <p>
     * Note that this method may take hundreds of milliseconds to return: unless
     * the texture is quite tiny, you probably don't want to call this directly
     * from your {@link GVRScript#onStep() onStep()} callback as that is called
     * once per frame, and a long call will cause you to miss frames. For large
     * images, you should use either
     * {@link #loadBitmapTexture(GVRAndroidResource.BitmapTextureCallback, GVRAndroidResource)
     * loadBitmapTexture()} (faster) or
     * {@link #loadCompressedTexture(GVRAndroidResource.CompressedTextureCallback, GVRAndroidResource)}
     * (fastest <em>and</em> least memory pressure).
     * 
     * <p>
     * Note also that this method does no scaling, and will return a full-size
     * {@link Bitmap}. Loading (say) an unscaled photograph may abort your app:
     * Use
     * <ul>
     * <li>Pre-scaled images
     * <li>{@link BitmapFactory} methods which give you more control over the
     * image size, or
     * <li>
     * {@link #loadTexture(GVRAndroidResource)} or
     * {@link #loadBitmapTexture(GVRAndroidResource.BitmapTextureCallback, GVRAndroidResource)}
     * which automatically scale large images to fit the GPU's restrictions and
     * to avoid {@linkplain OutOfMemoryError out of memory errors.}
     * </ul>
     * 
     * @param fileName
     *            The name of a file, relative to the assets directory. The
     *            assets directory may contain an arbitrarily complex tree of
     *            sub-directories; the file name can specify any location in or
     *            under the assets directory.
     * @return The file as a texture, or {@code null} if file path does not
     *         exist or the file can not be decoded into a Bitmap.
     * 
     * @deprecated We will remove this uncached, blocking function during Q3 of
     *             2015. We suggest that you switch to
     *             {@link #loadTexture(GVRAndroidResource)}
     * 
     */
    @SuppressWarnings("resource")
    public GVRBitmapTexture loadTexture(String fileName) {

        assertGLThread();

        if (fileName.endsWith(".png")) { // load png directly to texture
            return new GVRBitmapTexture(this, fileName);
        }

        Bitmap bitmap = loadBitmap(fileName);
        return bitmap == null ? null : new GVRBitmapTexture(this, bitmap);
    }

    /**
     * Loads file placed in the assets folder, as a {@link GVRBitmapTexture}.
     * 
     * <p>
     * Note that this method may take hundreds of milliseconds to return: unless
     * the texture is quite tiny, you probably don't want to call this directly
     * from your {@link GVRScript#onStep() onStep()} callback as that is called
     * once per frame, and a long call will cause you to miss frames. For large
     * images, you should use either
     * {@link #loadBitmapTexture(GVRAndroidResource.BitmapTextureCallback, GVRAndroidResource)
     * loadBitmapTexture()} (faster) or
     * {@link #loadCompressedTexture(GVRAndroidResource.CompressedTextureCallback, GVRAndroidResource)}
     * (fastest <em>and</em> least memory pressure).
     * 
     * <p>
     * This method automatically scales large images to fit the GPU's
     * restrictions and to avoid {@linkplain OutOfMemoryError out of memory
     * errors.} </ul>
     * 
     * @param resource
     *            Basically, a stream containing a bitmap texture. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @return The file as a texture, or {@code null} if the file can not be
     *         decoded into a Bitmap.
     * 
     * @since 1.6.5
     */
    public GVRTexture loadTexture(GVRAndroidResource resource) {

        GVRTexture texture = sTextureCache.get(resource);
        if (texture == null) {
            assertGLThread();

            Bitmap bitmap = GVRAsynchronousResourceLoader.decodeStream(
                    resource.getStream(), false);
            resource.closeStream();
            texture = bitmap == null ? null
                    : new GVRBitmapTexture(this, bitmap);
            if (texture != null) {
                sTextureCache.put(resource, texture);
            }
        }
        return texture;
    }

    private final static ResourceCache<GVRTexture> sTextureCache = new ResourceCache<GVRTexture>();

    /**
     * Loads a cube map texture synchronously.
     * 
     * <p>
     * Note that this method may take hundreds of milliseconds to return: unless
     * the cube map is quite tiny, you probably don't want to call this directly
     * from your {@link GVRScript#onStep() onStep()} callback as that is called
     * once per frame, and a long call will cause you to miss frames.
     * 
     * @param resourceArray
     *            An array containing six resources for six bitmaps. The order
     *            of the bitmaps is important to the correctness of the cube map
     *            texture. The six bitmaps should correspond to +x, -x, +y, -y,
     *            +z, and -z faces of the cube map texture respectively.
     * @return The cube map texture, or {@code null} if the length of
     *         rsourceArray is not 6.
     * 
     * @since 1.6.9
     */
    /*
     * TODO Deprecate, and replace with an overload that takes a single
     * GVRAndroidResource which specifies a zip file ... and caches result
     */
    public GVRCubemapTexture loadCubemapTexture(
            GVRAndroidResource[] resourceArray) {

        assertGLThread();

        if (resourceArray.length != 6) {
            return null;
        }

        Bitmap[] bitmapArray = new Bitmap[6];
        for (int i = 0; i < 6; i++) {
            bitmapArray[i] = GVRAsynchronousResourceLoader.decodeStream(
                    resourceArray[i].getStream(), false);
            resourceArray[i].closeStream();
        }

        return new GVRCubemapTexture(this, bitmapArray);
    }

    /**
     * Throws an exception if the current thread is not a GL thread.
     * 
     * @since 1.6.5
     * 
     */
    private void assertGLThread() {

        if (Thread.currentThread().getId() != mGLThreadID) {
            throw new RuntimeException(
                    "Should not run GL functions from a non-GL thread!");
        }

    }

    /**
     * Load a bitmap, asynchronously, with a default priority.
     * 
     * Because it is asynchronous, this method <em>is</em> a bit harder to use
     * than {@link #loadTexture(String)}, but it moves a large amount of work
     * (in {@link BitmapFactory#decodeStream(InputStream)} from the GL thread to
     * a background thread. Since you <em>can</em> create a
     * {@link GVRSceneObject} without a mesh and texture - and set them later -
     * using the asynchronous API can improve startup speed and/or reduce frame
     * misses (where an {@link GVRScript#onStep() onStep()} takes too long).
     * This API may also use less RAM than {@link #loadTexture(String)}.
     * 
     * <p>
     * This API will 'consolidate' requests: If you request a texture like
     * {@code R.raw.wood_grain} and then - before it has loaded - issue another
     * request for {@code R.raw.wood_grain}, GVRF will only read the bitmap file
     * once; only create a single {@link GVRTexture}; and then call both
     * callbacks, passing each the same texture.
     * 
     * <p>
     * Please be aware that {@link BitmapFactory#decodeStream(InputStream)} is a
     * comparatively expensive operation: it can take hundreds of milliseconds
     * and use several megabytes of temporary RAM. GVRF includes a throttler to
     * keep the total load manageable - but
     * {@link #loadCompressedTexture(GVRAndroidResource.CompressedTextureCallback, GVRAndroidResource)}
     * is <em>much</em> faster and lighter-weight: that API simply loads the
     * compressed texture into a small amount RAM (which doesn't take very long)
     * and does some simple parsing to figure out the parameters to pass
     * {@code glCompressedTexImage2D()}. The GL hardware does the decoding much
     * faster than Android's {@link BitmapFactory}!
     * 
     * <p>
     * TODO Take a boolean parameter that controls mipmap generation?
     * 
     * @since 1.6.1
     * 
     * @param callback
     *            Before loading, GVRF may call
     *            {@link GVRAndroidResource.BitmapTextureCallback#stillWanted(GVRAndroidResource)
     *            stillWanted()} several times (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * 
     *            Successful loads will call
     *            {@link GVRAndroidResource.Callback#loaded(GVRHybridObject, GVRAndroidResource)
     *            loaded()} on the GL thread;
     * 
     *            any errors will call
     *            {@link GVRAndroidResource.BitmapTextureCallback#failed(Throwable, GVRAndroidResource)
     *            failed()}, with no promises about threading.
     * 
     *            <p>
     *            This method uses a throttler to avoid overloading the system.
     *            If the throttler has threads available, it will run this
     *            request immediately. Otherwise, it will enqueue the request,
     *            and call
     *            {@link GVRAndroidResource.BitmapTextureCallback#stillWanted(GVRAndroidResource)
     *            stillWanted()} at least once (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * @param resource
     *            Basically, a stream containing a bitmapped image. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * 
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public void loadBitmapTexture(BitmapTextureCallback callback,
            GVRAndroidResource resource) {
        loadBitmapTexture(callback, resource, DEFAULT_PRIORITY);
    }

    /**
     * Load a bitmap, asynchronously, with an explicit priority.
     * 
     * Because it is asynchronous, this method <em>is</em> a bit harder to use
     * than {@link #loadTexture(String)}, but it moves a large amount of work
     * (in {@link BitmapFactory#decodeStream(InputStream)} from the GL thread to
     * a background thread. Since you <em>can</em> create a
     * {@link GVRSceneObject} without a mesh and texture - and set them later -
     * using the asynchronous API can improve startup speed and/or reduce frame
     * misses, where an {@link GVRScript#onStep() onStep()} takes too long.
     * 
     * <p>
     * This API will 'consolidate' requests: If you request a texture like
     * {@code R.raw.wood_grain} and then - before it has loaded - issue another
     * request for {@code R.raw.wood_grain}, GVRF will only read the bitmap file
     * once; only create a single {@link GVRTexture}; and then call both
     * callbacks, passing each the same texture.
     * 
     * <p>
     * Please be aware that {@link BitmapFactory#decodeStream(InputStream)} is a
     * comparatively expensive operation: it can take hundreds of milliseconds
     * and use several megabytes of temporary RAM. GVRF includes a throttler to
     * keep the total load manageable - but
     * {@link #loadCompressedTexture(GVRAndroidResource.CompressedTextureCallback, GVRAndroidResource)}
     * is <em>much</em> faster and lighter-weight: that API simply loads the
     * compressed texture into a small amount RAM (which doesn't take very long)
     * and does some simple parsing to figure out the parameters to pass
     * {@code glCompressedTexImage2D()}. The GL hardware does the decoding much
     * faster than Android's {@link BitmapFactory}!
     * 
     * @since 1.6.1
     * 
     * @param callback
     *            Before loading, GVRF may call
     *            {@link GVRAndroidResource.BitmapTextureCallback#stillWanted(GVRAndroidResource)
     *            stillWanted()} several times (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * 
     *            Successful loads will call
     *            {@link GVRAndroidResource.Callback#loaded(GVRHybridObject, GVRAndroidResource)
     *            loaded()} on the GL thread;
     * 
     *            any errors will call
     *            {@link GVRAndroidResource.BitmapTextureCallback#failed(Throwable, GVRAndroidResource)
     *            failed()}, with no promises about threading.
     * 
     *            <p>
     *            This method uses a throttler to avoid overloading the system.
     *            If the throttler has threads available, it will run this
     *            request immediately. Otherwise, it will enqueue the request,
     *            and call
     *            {@link GVRAndroidResource.BitmapTextureCallback#stillWanted(GVRAndroidResource)
     *            stillWanted()} at least once (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * @param resource
     *            Basically, a stream containing a bitmapped image. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>.
     * 
     * @throws IllegalArgumentException
     *             If {@code priority} {@literal <} {@link #LOWEST_PRIORITY} or
     *             {@literal >} {@link #HIGHEST_PRIORITY}, or either of the
     *             other parameters is {@code null} - or if you 'abuse' request
     *             consolidation by passing the same {@link GVRAndroidResource}
     *             descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public void loadBitmapTexture(BitmapTextureCallback callback,
            GVRAndroidResource resource, int priority)
            throws IllegalArgumentException {
        GVRAsynchronousResourceLoader.loadBitmapTexture(this, sTextureCache,
                callback, resource, priority);
    }

    /**
     * Load a compressed texture, asynchronously.
     * 
     * GVRF currently supports ASTC, ETC2, and KTX formats: applications can add
     * new formats by implementing {@link GVRCompressedTextureLoader}.
     * 
     * <p>
     * This method uses the fastest possible rendering. To specify higher
     * quality (but slower) rendering, you can use the
     * {@link #loadCompressedTexture(GVRAndroidResource.CompressedTextureCallback, GVRAndroidResource, int)}
     * overload.
     * 
     * @since 1.6.1
     * 
     * @param callback
     *            Successful loads will call
     *            {@link GVRAndroidResource.Callback#loaded(GVRHybridObject, GVRAndroidResource)
     *            loaded()} on the GL thread; any errors will call
     *            {@link GVRAndroidResource.CompressedTextureCallback#failed(Throwable, GVRAndroidResource)
     *            failed()}, with no promises about threading.
     * @param resource
     *            Basically, a stream containing a compressed texture. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * 
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public void loadCompressedTexture(CompressedTextureCallback callback,
            GVRAndroidResource resource) {
        GVRAsynchronousResourceLoader.loadCompressedTexture(this,
                sTextureCache, callback, resource);
    }

    /**
     * Load a compressed texture, asynchronously.
     * 
     * GVRF currently supports ASTC, ETC2, and KTX formats: applications can add
     * new formats by implementing {@link GVRCompressedTextureLoader}.
     * 
     * @since 1.6.1
     * 
     * @param callback
     *            Successful loads will call
     *            {@link GVRAndroidResource.Callback#loaded(GVRHybridObject, GVRAndroidResource)}
     *            on the GL thread; any errors will call
     *            {@link GVRAndroidResource.CompressedTextureCallback#failed(Throwable, GVRAndroidResource)}
     *            , with no promises about threading.
     * @param resource
     *            Basically, a stream containing a compressed texture. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @param quality
     *            Speed/quality tradeoff: should be one of
     *            {@link GVRCompressedTexture#SPEED},
     *            {@link GVRCompressedTexture#BALANCED}, or
     *            {@link GVRCompressedTexture#QUALITY}, but other values are
     *            'clamped' to one of the recognized values.
     * 
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public void loadCompressedTexture(CompressedTextureCallback callback,
            GVRAndroidResource resource, int quality) {
        GVRAsynchronousResourceLoader.loadCompressedTexture(this,
                sTextureCache, callback, resource, quality);
    }

    /**
     * A simplified, low-level method that loads a texture asynchronously,
     * without making you specify
     * {@link #loadBitmapTexture(GVRAndroidResource.BitmapTextureCallback, GVRAndroidResource)
     * loadBitmapTexture()} or
     * {@link #loadCompressedTexture(GVRAndroidResource.CompressedTextureCallback, GVRAndroidResource)
     * loadCompressedTexture()}.
     * 
     * This method can detect whether the resource file holds a compressed
     * texture (GVRF currently supports ASTC, ETC2, and KTX formats:
     * applications can add new formats by implementing
     * {@link GVRCompressedTextureLoader}): if the file is not a compressed
     * texture, it is loaded as a normal, bitmapped texture. This format
     * detection adds very little to the cost of loading even a compressed
     * texture, and it makes your life a lot easier: you can replace, say,
     * {@code res/raw/resource.png} with {@code res/raw/resource.etc2} without
     * having to change any code.
     * 
     * <p>
     * This method uses a default priority and a default render quality: Use
     * {@link #loadTexture(GVRAndroidResource.TextureCallback, GVRAndroidResource, int)}
     * to specify an explicit priority, and
     * {@link #loadTexture(GVRAndroidResource.TextureCallback, GVRAndroidResource, int, int)}
     * to specify an explicit quality.
     * 
     * <p>
     * We will continue to support the {@code loadBitmapTexture()} and
     * {@code loadCompressedTexture()} APIs for at least a little while: We
     * haven't yet decided whether to deprecate them or not.
     * 
     * @param callback
     *            Before loading, GVRF may call
     *            {@link GVRAndroidResource.TextureCallback#stillWanted(GVRAndroidResource)
     *            stillWanted()} several times (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * 
     *            Successful loads will call
     *            {@link GVRAndroidResource.Callback#loaded(GVRHybridObject, GVRAndroidResource)
     *            loaded()} on the GL thread;
     * 
     *            any errors will call
     *            {@link GVRAndroidResource.TextureCallback#failed(Throwable, GVRAndroidResource)
     *            failed()}, with no promises about threading.
     * 
     *            <p>
     *            This method uses a throttler to avoid overloading the system.
     *            If the throttler has threads available, it will run this
     *            request immediately. Otherwise, it will enqueue the request,
     *            and call
     *            {@link GVRAndroidResource.TextureCallback#stillWanted(GVRAndroidResource)
     *            stillWanted()} at least once (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * 
     *            <p>
     *            Use {@link #loadFutureTexture(GVRAndroidResource)} to avoid
     *            having to implement a callback.
     * @param resource
     *            Basically, a stream containing a texture file. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * 
     * @since 1.6.7
     * 
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public void loadTexture(TextureCallback callback,
            GVRAndroidResource resource) {
        loadTexture(callback, resource, DEFAULT_PRIORITY);
    }

    /**
     * A simplified, low-level method that loads a texture asynchronously,
     * without making you specify
     * {@link #loadBitmapTexture(GVRAndroidResource.BitmapTextureCallback, GVRAndroidResource)
     * loadBitmapTexture()} or
     * {@link #loadCompressedTexture(GVRAndroidResource.CompressedTextureCallback, GVRAndroidResource)
     * loadCompressedTexture()}.
     * 
     * This method can detect whether the resource file holds a compressed
     * texture (GVRF currently supports ASTC, ETC2, and KTX formats:
     * applications can add new formats by implementing
     * {@link GVRCompressedTextureLoader}): if the file is not a compressed
     * texture, it is loaded as a normal, bitmapped texture. This format
     * detection adds very little to the cost of loading even a compressed
     * texture, and it makes your life a lot easier: you can replace, say,
     * {@code res/raw/resource.png} with {@code res/raw/resource.etc2} without
     * having to change any code.
     * 
     * <p>
     * This method uses a default render quality: Use
     * {@link #loadTexture(GVRAndroidResource.TextureCallback, GVRAndroidResource, int, int)}
     * to specify an explicit quality.
     * 
     * <p>
     * We will continue to support the {@code loadBitmapTexture()} and
     * {@code loadCompressedTexture()} APIs for at least a little while: We
     * haven't yet decided whether to deprecate them or not.
     * 
     * @param callback
     *            Before loading, GVRF may call
     *            {@link GVRAndroidResource.TextureCallback#stillWanted(GVRAndroidResource)
     *            stillWanted()} several times (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * 
     *            Successful loads will call
     *            {@link GVRAndroidResource.Callback#loaded(GVRHybridObject, GVRAndroidResource)
     *            loaded()} on the GL thread;
     * 
     *            any errors will call
     *            {@link GVRAndroidResource.TextureCallback#failed(Throwable, GVRAndroidResource)
     *            failed()}, with no promises about threading.
     * 
     *            <p>
     *            This method uses a throttler to avoid overloading the system.
     *            If the throttler has threads available, it will run this
     *            request immediately. Otherwise, it will enqueue the request,
     *            and call
     *            {@link GVRAndroidResource.TextureCallback#stillWanted(GVRAndroidResource)
     *            stillWanted()} at least once (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * 
     *            <p>
     *            Use {@link #loadFutureTexture(GVRAndroidResource)} to avoid
     *            having to implement a callback.
     * @param resource
     *            Basically, a stream containing a texture file. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>. Also, please note priorities only apply to
     *            uncompressed textures (standard Android bitmap files, which
     *            can take hundreds of milliseconds to load): compressed
     *            textures load so quickly that they are not run through the
     *            request scheduler.
     * 
     * @since 1.6.7
     * 
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public void loadTexture(TextureCallback callback,
            GVRAndroidResource resource, int priority) {
        loadTexture(callback, resource, priority, GVRCompressedTexture.BALANCED);
    }

    /**
     * A simplified, low-level method that loads a texture asynchronously,
     * without making you specify
     * {@link #loadBitmapTexture(GVRAndroidResource.BitmapTextureCallback, GVRAndroidResource)
     * loadBitmapTexture()} or
     * {@link #loadCompressedTexture(GVRAndroidResource.CompressedTextureCallback, GVRAndroidResource)
     * loadCompressedTexture()}.
     * 
     * This method can detect whether the resource file holds a compressed
     * texture (GVRF currently supports ASTC, ETC2, and KTX formats:
     * applications can add new formats by implementing
     * {@link GVRCompressedTextureLoader}): if the file is not a compressed
     * texture, it is loaded as a normal, bitmapped texture. This format
     * detection adds very little to the cost of loading even a compressed
     * texture, and it makes your life a lot easier: you can replace, say,
     * {@code res/raw/resource.png} with {@code res/raw/resource.etc2} without
     * having to change any code.
     * 
     * <p>
     * We will continue to support the {@code loadBitmapTexture()} and
     * {@code loadCompressedTexture()} APIs for at least a little while: We
     * haven't yet decided whether to deprecate them or not.
     * 
     * @param callback
     *            Before loading, GVRF may call
     *            {@link GVRAndroidResource.TextureCallback#stillWanted(GVRAndroidResource)
     *            stillWanted()} several times (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * 
     *            Successful loads will call
     *            {@link GVRAndroidResource.Callback#loaded(GVRHybridObject, GVRAndroidResource)
     *            loaded()} on the GL thread;
     * 
     *            any errors will call
     *            {@link GVRAndroidResource.TextureCallback#failed(Throwable, GVRAndroidResource)
     *            failed()}, with no promises about threading.
     * 
     *            <p>
     *            This method uses a throttler to avoid overloading the system.
     *            If the throttler has threads available, it will run this
     *            request immediately. Otherwise, it will enqueue the request,
     *            and call
     *            {@link GVRAndroidResource.TextureCallback#stillWanted(GVRAndroidResource)
     *            stillWanted()} at least once (on a background thread) to give
     *            you a chance to abort a 'stale' load.
     * 
     *            <p>
     *            Use {@link #loadFutureTexture(GVRAndroidResource)} to avoid
     *            having to implement a callback.
     * @param resource
     *            Basically, a stream containing a texture file. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>. Also, please note priorities only apply to
     *            uncompressed textures (standard Android bitmap files, which
     *            can take hundreds of milliseconds to load): compressed
     *            textures load so quickly that they are not run through the
     *            request scheduler.
     * @param quality
     *            The compressed texture {@link GVRCompressedTexture#mQuality
     *            quality} parameter: should be one of
     *            {@link GVRCompressedTexture#SPEED},
     *            {@link GVRCompressedTexture#BALANCED}, or
     *            {@link GVRCompressedTexture#QUALITY}, but other values are
     *            'clamped' to one of the recognized values. Please note that
     *            this (currently) only applies to compressed textures; normal
     *            {@linkplain GVRBitmapTexture bitmapped textures} don't take a
     *            quality parameter.
     * 
     * @since 1.6.7
     * 
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public void loadTexture(TextureCallback callback,
            GVRAndroidResource resource, int priority, int quality) {
        GVRAsynchronousResourceLoader.loadTexture(this, sTextureCache,
                callback, resource, priority, quality);
    }

    /**
     * Simple, high-level method to load a texture asynchronously, for use with
     * {@link GVRShaders#setMainTexture(Future)} and
     * {@link GVRShaders#setTexture(String, Future)}.
     * 
     * This method uses a default priority and a default render quality: use
     * {@link #loadFutureTexture(GVRAndroidResource, int)} to specify a priority
     * or {@link #loadFutureTexture(GVRAndroidResource, int, int)} to specify a
     * priority and render quality.
     * 
     * <p>
     * This method is significantly easier to use than
     * {@link #loadTexture(GVRAndroidResource.TextureCallback, GVRAndroidResource)}
     * : you don't have to implement a callback; you don't have to pay attention
     * to the low-level details of
     * {@linkplain GVRSceneObject#attachRenderData(GVRRenderData) attaching} a
     * {@link GVRRenderData} to your scene object. What's more, you don't even
     * lose any functionality: {@link Future#cancel(boolean)} lets you cancel a
     * 'stale' request, just like
     * {@link GVRAndroidResource.CancelableCallback#stillWanted(GVRAndroidResource)
     * stillWanted()} does. The flip side, of course, is that it <em>is</em> a
     * bit more expensive: methods like
     * {@link GVRMaterial#setMainTexture(Future)} use an extra thread from the
     * thread pool to wait for the blocking {@link Future#get()} call. For
     * modest numbers of loads, this overhead is acceptable - but thread
     * creation is not free, and if your {@link GVRScript#onInit(GVRContext)
     * onInit()} method fires of dozens of future loads, you may well see an
     * impact.
     * 
     * @param resource
     *            Basically, a stream containing a texture file. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @return A {@link Future} that you can pass to methods like
     *         {@link GVRShaders#setMainTexture(Future)}
     * 
     * @since 1.6.7
     * 
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public Future<GVRTexture> loadFutureTexture(GVRAndroidResource resource) {
        return loadFutureTexture(resource, DEFAULT_PRIORITY);
    }

    /**
     * Simple, high-level method to load a texture asynchronously, for use with
     * {@link GVRShaders#setMainTexture(Future)} and
     * {@link GVRShaders#setTexture(String, Future)}.
     * 
     * This method uses a default render quality:
     * {@link #loadFutureTexture(GVRAndroidResource, int, int)} to specify
     * render quality.
     * 
     * <p>
     * This method is significantly easier to use than
     * {@link #loadTexture(GVRAndroidResource.TextureCallback, GVRAndroidResource, int)
     * : you don't have to implement a callback; you don't have to pay attention
     * to the low-level details of
     *{@linkplain GVRSceneObject#attachRenderData(GVRRenderData) attaching} a
     * {@link GVRRenderData} to your scene object. What's more, you don't even
     * lose any functionality: {@link Future#cancel(boolean)} lets you cancel a
     * 'stale' request, just like
     * {@link GVRAndroidResource.CancelableCallback#stillWanted(GVRAndroidResource)
     * stillWanted()} does. The flip side, of course, is that it <em>is</em> a
     * bit more expensive: methods like
     * {@link GVRMaterial#setMainTexture(Future)} use an extra thread from the
     * thread pool to wait for the blocking {@link Future#get()} call. For
     * modest numbers of loads, this overhead is acceptable - but thread
     * creation is not free, and if your {@link GVRScript#onInit(GVRContext)
     * onInit()} method fires of dozens of future loads, you may well see an
     * impact.
     * 
     * @param resource
     *            Basically, a stream containing a texture file. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>. Also, please note priorities only apply to
     *            uncompressed textures (standard Android bitmap files, which
     *            can take hundreds of milliseconds to load): compressed
     *            textures load so quickly that they are not run through the
     *            request scheduler.
     * @return A {@link Future} that you can pass to methods like
     *         {@link GVRShaders#setMainTexture(Future)}
     * 
     * @since 1.6.7
     * 
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public Future<GVRTexture> loadFutureTexture(GVRAndroidResource resource,
            int priority) {
        return loadFutureTexture(resource, priority,
                GVRCompressedTexture.BALANCED);
    }

    /**
     * Simple, high-level method to load a texture asynchronously, for use with
     * {@link GVRShaders#setMainTexture(Future)} and
     * {@link GVRShaders#setTexture(String, Future)}.
     * 
     * 
     * <p>
     * This method is significantly easier to use than
     * {@link #loadTexture(GVRAndroidResource.TextureCallback, GVRAndroidResource, int, int)
     * : you don't have to implement a callback; you don't have to pay attention
     * to the low-level details of
     *{@linkplain GVRSceneObject#attachRenderData(GVRRenderData) attaching} a
     * {@link GVRRenderData} to your scene object. What's more, you don't even
     * lose any functionality: {@link Future#cancel(boolean)} lets you cancel a
     * 'stale' request, just like
     * {@link GVRAndroidResource.CancelableCallback#stillWanted(GVRAndroidResource)
     * stillWanted()} does. The flip side, of course, is that it <em>is</em> a
     * bit more expensive: methods like
     * {@link GVRMaterial#setMainTexture(Future)} use an extra thread from the
     * thread pool to wait for the blocking {@link Future#get()} call. For
     * modest numbers of loads, this overhead is acceptable - but thread
     * creation is not free, and if your {@link GVRScript#onInit(GVRContext)
     * onInit()} method fires of dozens of future loads, you may well see an
     * impact.
     * 
     * @param resource
     *            Basically, a stream containing a texture file. The
     *            {@link GVRAndroidResource} class has six constructors to
     *            handle a wide variety of Android resource types. Taking a
     *            {@code GVRAndroidResource} here eliminates six overloads.
     * @param priority
     *            This request's priority. Please see the notes on asynchronous
     *            priorities in the <a href="package-summary.html#async">package
     *            description</a>. Also, please note priorities only apply to
     *            uncompressed textures (standard Android bitmap files, which
     *            can take hundreds of milliseconds to load): compressed
     *            textures load so quickly that they are not run through the
     *            request scheduler.
     * @param quality
     *            The compressed texture {@link GVRCompressedTexture#mQuality
     *            quality} parameter: should be one of
     *            {@link GVRCompressedTexture#SPEED},
     *            {@link GVRCompressedTexture#BALANCED}, or
     *            {@link GVRCompressedTexture#QUALITY}, but other values are
     *            'clamped' to one of the recognized values. Please note that
     *            this (currently) only applies to compressed textures; normal
     *            {@linkplain GVRBitmapTexture bitmapped textures} don't take a
     *            quality parameter.
     * @return A {@link Future} that you can pass to methods like
     *         {@link GVRShaders#setMainTexture(Future)}
     * 
     * @since 1.6.7
     * 
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public Future<GVRTexture> loadFutureTexture(GVRAndroidResource resource,
            int priority, int quality) {
        return GVRAsynchronousResourceLoader.loadFutureTexture(this,
                sTextureCache, resource, priority, quality);
    }

    /**
     * Simple, high-level method to load a cube map texture asynchronously, for
     * use with {@link GVRShaders#setMainTexture(Future)} and
     * {@link GVRShaders#setTexture(String, Future)}.
     * 
     * @param resource
     *            A steam containing a zip file which contains six bitmaps. The
     *            six bitmaps correspond to +x, -x, +y, -y, +z, and -z faces of
     *            the cube map texture respectively. The default names of the
     *            six images are "posx.png", "negx.png", "posy.png", "negx.png",
     *            "posz.png", and "negz.png", which can be changed by calling
     *            {@link GVRCubemapTexture#setFaceNames(String[])}.
     * @return A {@link Future} that you can pass to methods like
     *         {@link GVRShaders#setMainTexture(Future)}
     * 
     * @since 1.6.9
     * 
     * @throws IllegalArgumentException
     *             If you 'abuse' request consolidation by passing the same
     *             {@link GVRAndroidResource} descriptor to multiple load calls.
     *             <p>
     *             It's fairly common for multiple scene objects to use the same
     *             texture or the same mesh. Thus, if you try to load, say,
     *             {@code R.raw.whatever} while you already have a pending
     *             request for {@code R.raw.whatever}, it will only be loaded
     *             once; the same resource will be used to satisfy both (all)
     *             requests. This "consolidation" uses
     *             {@link GVRAndroidResource#equals(Object)}, <em>not</em>
     *             {@code ==} (aka "reference equality"): The problem with using
     *             the same resource descriptor is that if requests can't be
     *             consolidated (because the later one(s) came in after the
     *             earlier one(s) had already completed) the resource will be
     *             reloaded ... but the original descriptor will have been
     *             closed.
     */
    public Future<GVRTexture> loadFutureCubemapTexture(
            GVRAndroidResource resource) {
        return GVRAsynchronousResourceLoader.loadFutureCubemapTexture(this,
                sTextureCache, resource, DEFAULT_PRIORITY,
                GVRCubemapTexture.faceIndexMap);
    }

    /**
     * Get the current {@link GVRScene}, which contains the scene graph (a
     * hierarchy of {@linkplain GVRSceneObject scene objects}) and the
     * {@linkplain GVRCameraRig camera rig}
     * 
     * @return A {@link GVRScene} instance, containing scene and camera
     *         information
     */
    public abstract GVRScene getMainScene();

    /** Set the current {@link GVRScene} */
    public abstract void setMainScene(GVRScene scene);

    /**
     * Returns a {@link GVRScene} that you can populate before passing to
     * {@link #setMainScene(GVRScene)}.
     * 
     * Implementation maintains a single element buffer, initialized to
     * {@code null}. When this method is called, creates a new scene if the
     * buffer is {@code null}, then returns the buffered scene. If this buffered
     * scene is passed to {@link #setMainScene(GVRScene)}, the buffer is reset
     * to {@code null}.
     * 
     * <p>
     * One use of this is to build your scene graph while the splash screen is
     * visible. If you have called {@linkplain #getNextMainScene()} (so that the
     * next-main-scene buffer is non-{@code null} when the splash screen is
     * closed) GVRF will automatically switch to the 'pending' main-scene; if
     * the buffer is {@code null}, GVRF will simply remove the splash screen
     * from the main scene object.
     * 
     * @since 1.6.4
     */
    public GVRScene getNextMainScene() {
        return getNextMainScene(null);
    }

    /**
     * Returns a {@link GVRScene} that you can populate before passing to
     * {@link #setMainScene(GVRScene)}.
     * 
     * Implementation maintains a single element buffer, initialized to
     * {@code null}. When this method is called, creates a new scene if the
     * buffer is {@code null}, then returns the buffered scene. If this buffered
     * scene is passed to {@link #setMainScene(GVRScene)}, the buffer is reset
     * to {@code null}.
     * 
     * <p>
     * One use of this is to build your scene graph while the splash screen is
     * visible. If you have called {@linkplain #getNextMainScene()} (so that the
     * next-main-scene buffer is non-{@code null} when the splash screen is
     * closed) GVRF will automatically switch to the 'pending' main-scene; if
     * the buffer is {@code null}, GVRF will simply remove the splash screen
     * from the main scene object.
     * 
     * @param onSwitchMainScene
     *            Optional (may be {@code null}) {@code Runnable}, called when
     *            this {@link GVRScene} becomes the new main scene, whether
     *            {@linkplain #setMainScene(GVRScene) explicitly} or implicitly
     *            (as, for example, when the splash screen closes). This
     *            callback lets apps do things like start animations when their
     *            scene becomes visible, instead of in
     *            {@link GVRScript#onInit(GVRContext) onInit()} when the scene
     *            objects may be hidden by the splash screen.
     * 
     * @since 1.6.4
     */
    public abstract GVRScene getNextMainScene(Runnable onSwitchMainScene);

    /**
     * Is the key pressed?
     * 
     * @param keyCode
     *            An Android {@linkplain KeyEvent#KEYCODE_0 key code}
     */
    public abstract boolean isKeyDown(int keyCode);

    /**
     * The interval between this frame and the previous frame, in seconds: a
     * rough gauge of Frames Per Second.
     */
    public abstract float getFrameTime();

    /**
     * Enqueues a callback to be run in the GL thread.
     * 
     * This is how you take data generated on a background thread (or the main
     * (GUI) thread) and pass it to the coprocessor, using calls that must be
     * made from the GL thread (aka the "GL context"). The callback queue is
     * processed before any registered
     * {@linkplain #registerDrawFrameListener(GVRDrawFrameListener) frame
     * listeners}.
     * 
     * @param runnable
     *            A bit of code that must run on the GL thread
     */
    public abstract void runOnGlThread(Runnable runnable);

    /**
     * Subscribes a {@link GVRDrawFrameListener}.
     * 
     * Each frame listener is called, once per frame, after any pending
     * {@linkplain #runOnGlThread(Runnable) GL callbacks} and before
     * {@link GVRScript#onStep()}.
     * 
     * @param frameListener
     *            A callback that will fire once per frame, until it is
     *            {@linkplain #unregisterDrawFrameListener(GVRDrawFrameListener)
     *            unregistered}
     */
    public abstract void registerDrawFrameListener(
            GVRDrawFrameListener frameListener);

    /**
     * Remove a previously-subscribed {@link GVRDrawFrameListener}.
     * 
     * @param frameListener
     *            An instance of a {@link GVRDrawFrameListener} implementation.
     *            Unsubscribing a listener which is not actually subscribed will
     *            not throw an exception.
     */
    public abstract void unregisterDrawFrameListener(
            GVRDrawFrameListener frameListener);

    /**
     * The {@linkplain GVRMaterialShaderManager object shader manager}
     * singleton.
     * 
     * Use the shader manager to define custom GL object shaders, which are used
     * to render a scene object's surface.
     * 
     * @return The {@linkplain GVRMaterialShaderManager shader manager}
     *         singleton.
     */
    public GVRMaterialShaderManager getMaterialShaderManager() {
        return getRenderBundle().getMaterialShaderManager();
    }

    /**
     * The {@linkplain GVRPostEffectShaderManager scene shader manager}
     * singleton.
     * 
     * Use the shader manager to define custom GL scene shaders, which can be
     * inserted into the rendering pipeline to apply image processing effects to
     * the rendered scene graph. In classic GL programming, this is often
     * referred to as a "post effect."
     * 
     * @return The {@linkplain GVRPostEffectShaderManager post effect shader
     *         manager} singleton.
     */
    public GVRPostEffectShaderManager getPostEffectShaderManager() {
        return getRenderBundle().getPostEffectShaderManager();
    }

    /**
     * The {@linkplain GVRAnimationEngine animation engine} singleton.
     * 
     * Use the animation engine to start and stop {@linkplain GVRAnimation
     * animations}.
     * 
     * @return The {@linkplain GVRAnimationEngine animation engine} singleton.
     */
    public GVRAnimationEngine getAnimationEngine() {
        return GVRAnimationEngine.getInstance(this);
    }

    /**
     * The {@linkplain GVRPeriodicEngine periodic engine} singleton.
     * 
     * Use the periodic engine to schedule {@linkplain Runnable runnables} to
     * run on the GL thread at a future time.
     * 
     * @return The {@linkplain GVRPeriodicEngine periodic engine} singleton.
     */
    public GVRPeriodicEngine getPeriodicEngine() {
        return GVRPeriodicEngine.getInstance(this);
    }

    /**
     * Register a method that is called every time GVRF creates a new
     * {@link GVRContext}.
     * 
     * Android apps aren't mapped 1:1 to Linux processes; the system may keep a
     * process loaded even after normal complete shutdown, and call Android
     * lifecycle methods to reinitialize it. This causes problems for (in
     * particular) lazy-created singletons that are tied to a particular
     * {@code GVRContext}. This method lets you register a handler that will be
     * called on restart, which can reset your {@code static} variables to the
     * compiled-in start state.
     * 
     * <p>
     * For example,
     * 
     * <pre>
     * 
     * static YourSingletonClass sInstance;
     * static {
     *     GVRContext.addResetOnRestartHandler(new Runnable() {
     * 
     *         &#064;Override
     *         public void run() {
     *             sInstance = null;
     *         }
     *     });
     * }
     * 
     * </pre>
     * 
     * <p>
     * GVRF will force an Android garbage collection after running any handlers,
     * which will free any remaining native objects from the previous run.
     * 
     * @param handler
     *            Callback to run on restart.
     */
    public synchronized static void addResetOnRestartHandler(Runnable handler) {
        sHandlers.add(handler);
    }

    protected synchronized static void resetOnRestart() {
        for (Runnable handler : sHandlers) {
            Log.d(TAG, "Running on-restart handler %s", handler);
            handler.run();
        }

        // We've probably just nulled-out a bunch of references, but many GVRF
        // apps do relatively little Java memory allocation, so it may actually
        // be a longish while before the recyclable references go stale.
        System.gc();

        // We do NOT want to clear sHandlers - the static initializers won't be
        // run again, even if the new run does recreate singletons.
    }

    private static final List<Runnable> sHandlers = new ArrayList<Runnable>();

    abstract GVRRenderBundle getRenderBundle();

    /**
     * Capture a 2D screenshot from the position in the middle of left eye and
     * right eye.
     * 
     * The screenshot capture is done asynchronously -- the function does not
     * return the result immediately. Instead, it registers a callback function
     * and pass the result (when it is available) to the callback function. The
     * callback will happen on a background thread: It will probably not be the
     * same thread that calls this method, and it will not be either the GUI or
     * the GL thread.
     * 
     * Users should not start a {@code captureScreenCenter} until previous
     * {@code captureScreenCenter} callback has returned. Starting a new
     * {@code captureScreenCenter} before the previous
     * {@code captureScreenCenter} callback returned may cause out of memory
     * error.
     * 
     * @param callback
     *            Callback function to process the capture result. It may not be
     *            {@code null}.
     */
    public abstract void captureScreenCenter(GVRScreenshotCallback callback);

    /**
     * Capture a 2D screenshot from the position of left eye.
     * 
     * The screenshot capture is done asynchronously -- the function does not
     * return the result immediately. Instead, it registers a callback function
     * and pass the result (when it is available) to the callback function. The
     * callback will happen on a background thread: It will probably not be the
     * same thread that calls this method, and it will not be either the GUI or
     * the GL thread.
     * 
     * Users should not start a {@code captureScreenLeft} until previous
     * {@code captureScreenLeft} callback has returned. Starting a new
     * {@code captureScreenLeft} before the previous {@code captureScreenLeft}
     * callback returned may cause out of memory error.
     * 
     * @param callback
     *            Callback function to process the capture result. It may not be
     *            {@code null}.
     */
    public abstract void captureScreenLeft(GVRScreenshotCallback callback);

    /**
     * Capture a 2D screenshot from the position of right eye.
     * 
     * The screenshot capture is done asynchronously -- the function does not
     * return the result immediately. Instead, it registers a callback function
     * and pass the result (when it is available) to the callback function. The
     * callback will happen on a background thread: It will probably not be the
     * same thread that calls this method, and it will not be either the GUI or
     * the GL thread.
     * 
     * Users should not start a {@code captureScreenRight} until previous
     * {@code captureScreenRight} callback has returned. Starting a new
     * {@code captureScreenRight} before the previous {@code captureScreenRight}
     * callback returned may cause out of memory error.
     * 
     * @param callback
     *            Callback function to process the capture result. It may not be
     *            {@code null}.
     */
    public abstract void captureScreenRight(GVRScreenshotCallback callback);

    /**
     * Capture a 3D screenshot from the position of left eye. The 3D screenshot
     * is composed of six images from six directions (i.e. +x, -x, +y, -y, +z,
     * and -z).
     * 
     * The screenshot capture is done asynchronously -- the function does not
     * return the result immediately. Instead, it registers a callback function
     * and pass the result (when it is available) to the callback function. The
     * callback will happen on a background thread: It will probably not be the
     * same thread that calls this method, and it will not be either the GUI or
     * the GL thread.
     * 
     * Users should not start a {@code captureScreen3D} until previous
     * {@code captureScreen3D} callback has returned. Starting a new
     * {@code captureScreen3D} before the previous {@code captureScreen3D}
     * callback returned may cause out of memory error.
     * 
     * @param callback
     *            Callback function to process the capture result. It may not be
     *            {@code null}.
     * 
     * @since 1.6.8
     */
    public abstract void captureScreen3D(GVRScreenshot3DCallback callback);
}
