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
 * Renders a bounding box for occlusion query.
 ***************************************************************************/

#include "bounding_box_shader.h"

#include "gl/gl_program.h"
#include "objects/material.h"
#include "objects/mesh.h"
#include "objects/components/render_data.h"
#include "objects/textures/texture.h"
#include "util/gvr_gl.h"

namespace gvr {
static const char VERTEX_SHADER[] = //
        "precision highp  float;\n"
                "attribute vec3 a_position;\n"
                "uniform mat4 u_mvp;\n"
                "void main() {\n"
                "gl_Position = u_mvp * vec4(a_position, 1.0);\n"
                "}\n";

static const char FRAGMENT_SHADER[] = //
        "precision mediump  float;\n"
                "void main() {\n"
                "gl_FragColor =  vec4(0.0, 1.0, 0.0, 0.0);\n"
                "}\n";

BoundingBoxShader::BoundingBoxShader() :
        program_(0), a_position_(0), u_mvp_(0) {
    program_ = new GLProgram(VERTEX_SHADER, FRAGMENT_SHADER);
    a_position_ = glGetAttribLocation(program_->id(), "a_position");
    u_mvp_ = glGetUniformLocation(program_->id(), "u_mvp");
}

BoundingBoxShader::~BoundingBoxShader() {
    if (program_ != 0) {
        recycle();
    }
}

void BoundingBoxShader::recycle() {
    delete program_;
    program_ = 0;
}

void BoundingBoxShader::render(const glm::mat4& mvp_matrix,
        RenderData* render_data, Material* material) {
    Mesh* mesh = render_data->mesh();

#if _GVRF_USE_GLES3_
    mesh->setVertexLoc(a_position_);
    mesh->generateVAO(material->shader_type());

    glUseProgram(program_->id());
    glUniformMatrix4fv(u_mvp_, 1, GL_FALSE, glm::value_ptr(mvp_matrix));

    glBindVertexArray(mesh->getVAOId(material->shader_type()));
    glDrawElements(GL_TRIANGLES, mesh->triangles().size(), GL_UNSIGNED_SHORT,
            0);
    glBindVertexArray(0);

#else
    glUseProgram(program_->id());
    glVertexAttribPointer(a_position_, 3, GL_FLOAT, GL_FALSE, 0,
            mesh->vertices().data());
    glEnableVertexAttribArray(a_position_);

    glUniformMatrix4fv(u_mvp_, 1, GL_FALSE, glm::value_ptr(mvp_matrix));
    glDrawElements(GL_TRIANGLES, mesh->triangles().size(), GL_UNSIGNED_SHORT,
            mesh->triangles().data());
#endif

    checkGlError("BoundingBoxShader::render");
}

}
;
