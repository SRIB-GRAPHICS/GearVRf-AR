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
 * Utility functions about time.
 ***************************************************************************/

#ifndef GVR_TIME_H_
#define GVR_TIME_H_

#include "time.h"

namespace gvr {

static long long getCurrentTime() {
    timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts); // Works on Linux
    long long time = static_cast<long long>(ts.tv_sec)
            * static_cast<long long>(1000000000)
            + static_cast<long long>(ts.tv_nsec);
    return time;
}

}

#endif
