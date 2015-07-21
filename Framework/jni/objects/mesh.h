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
 * The mesh for rendering.
 ***************************************************************************/

#ifndef MESH_H_
#define MESH_H_

#include <map>
#include <memory>
#include <vector>
#include <string>

#include "GLES3/gl3.h"
#include "glm/glm.hpp"
#include "gl/gl_buffer.h"

#include "objects/hybrid_object.h"
#include "objects/material.h"

#include "engine/memory/gl_delete.h"

namespace gvr {
class Mesh: public HybridObject {
public:
    Mesh() :
            vertices_(), normals_(), tex_coords_(), triangles_(), float_vectors_(), vec2_vectors_(), vec3_vectors_(), vec4_vectors_(), vertexLoc_(
                    -1), normalLoc_(-1), texCoordLoc_(-1), have_bounding_box_(false), have_bounding_sphere_(false) {
    }

    ~Mesh() {
        cleanUp();
    }

    void cleanUp() {
        std::vector<glm::vec3> vertices;
        vertices.swap(vertices_);
        std::vector<glm::vec3> normals;
        normals.swap(normals_);
        std::vector<glm::vec2> tex_coords;
        tex_coords.swap(tex_coords_);
        std::vector<unsigned short> triangles;
        triangles.swap(triangles_);

        deleteVaos();
    }

    void deleteVaos() {
        for (auto iterator = vaoID_map_.begin(); iterator != vaoID_map_.end();
                iterator++) {
            gl_delete.queueVertexArray(iterator->second);
        }
        vaoID_map_.clear();

        for (auto iterator = triangle_vboID_map_.begin();
                iterator != triangle_vboID_map_.end(); iterator++) {
            gl_delete.queueBuffer(iterator->second);
        }
        triangle_vboID_map_.clear();

        for (auto iterator = vert_vboID_map_.begin();
                iterator != vert_vboID_map_.end(); iterator++) {
            gl_delete.queueBuffer(iterator->second);
        }
        vert_vboID_map_.clear();

        for (auto iterator = norm_vboID_map_.begin();
                iterator != norm_vboID_map_.end(); iterator++) {
            gl_delete.queueBuffer(iterator->second);
        }
        norm_vboID_map_.clear();

        for (auto iterator = tex_vboID_map_.begin();
                iterator != tex_vboID_map_.end(); iterator++) {
            gl_delete.queueBuffer(iterator->second);
        }
        tex_vboID_map_.clear();
        have_bounding_box_ = false;
        have_bounding_sphere_ = false;
    }

    std::vector<glm::vec3>& vertices() {
        return vertices_;
    }

    const std::vector<glm::vec3>& vertices() const {
        return vertices_;
    }

    void set_vertices(const std::vector<glm::vec3>& vertices) {
        vertices_ = vertices;
        getBoundingSphereInfo(); // calculate bounding sphere
    }

    void set_vertices(std::vector<glm::vec3>&& vertices) {
        vertices_ = std::move(vertices);
        getBoundingSphereInfo(); // calculate bounding sphere
    }

    std::vector<glm::vec3>& normals() {
        return normals_;
    }

    const std::vector<glm::vec3>& normals() const {
        return normals_;
    }

    void set_normals(const std::vector<glm::vec3>& normals) {
        normals_ = normals;
    }

    void set_normals(std::vector<glm::vec3>&& normals) {
        normals_ = std::move(normals);
    }

    std::vector<glm::vec2>& tex_coords() {
        return tex_coords_;
    }

    const std::vector<glm::vec2>& tex_coords() const {
        return tex_coords_;
    }

    void set_tex_coords(const std::vector<glm::vec2>& tex_coords) {
        tex_coords_ = tex_coords;
        vao_dirty_ = true;
    }

    void set_tex_coords(std::vector<glm::vec2>&& tex_coords) {
        tex_coords_ = std::move(tex_coords);
        vao_dirty_ = true;
    }

    std::vector<unsigned short>& triangles() {
        return triangles_;
    }

    const std::vector<unsigned short>& triangles() const {
        return triangles_;
    }

    void set_triangles(const std::vector<unsigned short>& triangles) {
        triangles_ = triangles;
    }

    void set_triangles(std::vector<unsigned short>&& triangles) {
        triangles_ = std::move(triangles);
    }

    std::vector<float>& getFloatVector(std::string key) {
        auto it = float_vectors_.find(key);
        if (it != float_vectors_.end()) {
            return it->second;
        } else {
            std::string error = "Mesh::getFloatVector() : " + key
                    + " not found";
            throw error;
        }
    }

    const std::vector<float>& getFloatVector(std::string key) const {
        auto it = float_vectors_.find(key);
        if (it != float_vectors_.end()) {
            return it->second;
        } else {
            std::string error = "Mesh::getFloatVector() : " + key
                    + " not found";
            throw error;
        }
    }

    void setFloatVector(std::string key, const std::vector<float>& vector) {
        float_vectors_[key] = vector;
    }

    std::vector<glm::vec2>& getVec2Vector(std::string key) {
        auto it = vec2_vectors_.find(key);
        if (it != vec2_vectors_.end()) {
            return it->second;
        } else {
            std::string error = "Mesh::getVec2Vector() : " + key + " not found";
            throw error;
        }
    }

    const std::vector<glm::vec2>& getVec2Vector(std::string key) const {
        auto it = vec2_vectors_.find(key);
        if (it != vec2_vectors_.end()) {
            return it->second;
        } else {
            std::string error = "Mesh::getVec2Vector() : " + key + " not found";
            throw error;
        }
    }

    void setVec2Vector(std::string key, const std::vector<glm::vec2>& vector) {
        vec2_vectors_[key] = vector;
    }

    std::vector<glm::vec3>& getVec3Vector(std::string key) {
        auto it = vec3_vectors_.find(key);
        if (it != vec3_vectors_.end()) {
            return it->second;
        } else {
            std::string error = "Mesh::getVec3Vector() : " + key + " not found";
            throw error;
        }
    }

    const std::vector<glm::vec3>& getVec3Vector(std::string key) const {
        auto it = vec3_vectors_.find(key);
        if (it != vec3_vectors_.end()) {
            return it->second;
        } else {
            std::string error = "Mesh::getVec3Vector() : " + key + " not found";
            throw error;
        }
    }

    void setVec3Vector(std::string key, const std::vector<glm::vec3>& vector) {
        vec3_vectors_[key] = vector;
    }

    std::vector<glm::vec4>& getVec4Vector(std::string key) {
        auto it = vec4_vectors_.find(key);
        if (it != vec4_vectors_.end()) {
            return it->second;
        } else {
            std::string error = "Mesh::getVec4Vector() : " + key + " not found";
            throw error;
        }
    }

    const std::vector<glm::vec4>& getVec4Vector(std::string key) const {
        auto it = vec4_vectors_.find(key);
        if (it != vec4_vectors_.end()) {
            return it->second;
        } else {
            std::string error = "Mesh::getVec4Vector() : " + key + " not found";
            throw error;
        }
    }

    void setVec4Vector(std::string key, const std::vector<glm::vec4>& vector) {
        vec4_vectors_[key] = vector;
    }

    Mesh* getBoundingBox();
    const float* getBoundingBoxInfo(); // Xmin, Ymin, Zmin and Xmax, Ymax, Zmax
    void getTransformedBoundingBoxInfo(glm::mat4 *M,
            float *transformed_bounding_box); //Get Bounding box info transformed by matrix
    const float *getBoundingSphereInfo(); // Get bounding sphere based on the bounding box

    // /////////////////////////////////////////////////
    //  code for vertex attribute location

    void setVertexLoc(GLuint loc) {
        vertexLoc_ = loc;
    }

    const GLuint getVertexLoc() const {
        return vertexLoc_;
    }

    void setNormalLoc(GLuint loc) {
        normalLoc_ = loc;
    }

    const GLuint getNormalLoc() const {
        return normalLoc_;
    }

    void setTexCoordLoc(GLuint loc) {
        texCoordLoc_ = loc;
    }

    const GLuint getTexCoordLoc() const {
        return texCoordLoc_;
    }

    void setVertexAttribLocF(GLuint location, std::string key) {
        attribute_float_keys_[location] = key;
    }

    void setVertexAttribLocV2(GLuint location, std::string key) {
        attribute_vec2_keys_[location] = key;
    }

    void setVertexAttribLocV3(GLuint location, std::string key) {
        attribute_vec3_keys_[location] = key;
    }

    void setVertexAttribLocV4(GLuint location, std::string key) {
        attribute_vec4_keys_[location] = key;
    }

    // generate VAO
    void generateVAO(Material::ShaderType key);

    const GLuint getVAOId(Material::ShaderType key) const {
        auto iterator = vaoID_map_.find(key);
        return iterator != vaoID_map_.end() ? iterator->second : 0;
    }

    GLuint getNumTriangles() {
        return numTriangles_;
    }

private:
    Mesh(const Mesh& mesh);
    Mesh(Mesh&& mesh);
    Mesh& operator=(const Mesh& mesh);
    Mesh& operator=(Mesh&& mesh);

private:
    std::vector<glm::vec3> vertices_;
    std::vector<glm::vec3> normals_;
    std::vector<glm::vec2> tex_coords_;
    std::map<std::string, std::vector<float>> float_vectors_;
    std::map<std::string, std::vector<glm::vec2>> vec2_vectors_;
    std::map<std::string, std::vector<glm::vec3>> vec3_vectors_;
    std::map<std::string, std::vector<glm::vec4>> vec4_vectors_;
    std::vector<unsigned short> triangles_;

    // add location slot map
    std::map<int, std::string> attribute_float_keys_;
    std::map<int, std::string> attribute_vec2_keys_;
    std::map<int, std::string> attribute_vec3_keys_;
    std::map<int, std::string> attribute_vec4_keys_;

    // add vertex array object and VBO
    std::map<Material::ShaderType, GLuint> vaoID_map_;
    std::map<Material::ShaderType, GLuint> triangle_vboID_map_;
    std::map<Material::ShaderType, GLuint> vert_vboID_map_;
    std::map<Material::ShaderType, GLuint> norm_vboID_map_;
    std::map<Material::ShaderType, GLuint> tex_vboID_map_;

    // attribute locations
    GLuint vertexLoc_;
    GLuint normalLoc_;
    GLuint texCoordLoc_;

    // triangle information
    GLuint numTriangles_;

    // bounding box info
    bool have_bounding_box_;
    float bounding_box_info_[6];

    // bounding sphere info
    bool have_bounding_sphere_;
    float bounding_sphere_info_[4]; // [0-2] center x,y,z; [3] radius

    bool vao_dirty_;
};
}
#endif
