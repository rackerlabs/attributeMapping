/**
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
package com.rackspace.identity.components

import java.io.File
import javax.xml.transform.stream.StreamSource

import net.sf.saxon.s9api.SaxonApiException
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ValidatePolicySuite extends AttributeMapperBase {

  val testDir = new File("src/test/resources/tests/validate-policy-tests")

  val validPolicies = new File(testDir, "valid").listFiles.toList.filter { f =>
    f.getName.endsWith("xml") || f.getName.endsWith("json")
  }
  val invalidPolicies = new File(testDir, "invalid").listFiles.toList.filter { f =>
    f.getName.endsWith("xml") || f.getName.endsWith("json")
  }

  validators foreach { validator =>
    validPolicies foreach { policy =>
      test(s"A Valid Policy Validates ($policy validated with $validator)") {
        val source = new StreamSource(policy)
        AttributeMapper.validatePolicy(source, validator)
      }
    }

    invalidPolicies foreach { policy =>
      test(s"An Invalid Policy Fails Validation ($policy validated with $validator)") {
        val source = new StreamSource(policy)
        intercept[SaxonApiException] {
          AttributeMapper.validatePolicy(source, validator)
        }
      }
    }
  }
}
