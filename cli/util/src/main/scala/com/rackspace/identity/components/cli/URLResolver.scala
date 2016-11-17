/***
 *   Copyright 2016 Rackspace US, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.rackspace.identity.components.cli

import java.io.File
import java.net.URI

object URLResolver {
  def toAbsoluteSystemId(systemId : String) : URI = {
    toAbsoluteSystemId(systemId, (new File(System.getProperty("user.dir")).toURI().toString))
  }

  def toAbsoluteSystemId(systemId : String, base : String) : URI = {
    val inURI = new URI(systemId)
    if (!inURI.isAbsolute()) {
      (new URI(base)).resolve(systemId)
    } else {
      inURI
    }
  }
}
