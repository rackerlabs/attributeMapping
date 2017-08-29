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

import net.sf.saxon.s9api.{SaxonApiException, XdmDestination}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ValidatePolicySuite extends AttributeMapperBase {

  val testDir = new File("src/test/resources/tests/validate-policy-tests")

  val xmlValidPolicies = new File(testDir, "valid").listFiles.toList.filter { f =>
    f.getName.endsWith("xml")
  }
  val xmlInvalidPolicies = new File(testDir, "invalid").listFiles.toList.filter { f =>
    f.getName.endsWith("xml")
  }

  val jsonValidPolicies = new File(testDir, "valid").listFiles.toList.filter { f =>
    f.getName.endsWith("json")
  }
  val jsonInvalidPolicies = new File(testDir, "invalid").listFiles.toList.filter { f =>
    f.getName.endsWith("json")
  }

  validators foreach { validator =>
    xmlValidPolicies foreach { policy =>
      test(s"A Valid Policy Validates ($policy validated with $validator)") {
        val source = new StreamSource(policy)
        AttributeMapper.validatePolicy(source, validator)
      }
    }

    xmlInvalidPolicies foreach { policy =>
      test(s"An Invalid Policy Fails Validation ($policy validated with $validator)") {
        val source = new StreamSource(policy)
        val e = intercept[SaxonApiException] {
          AttributeMapper.validatePolicy(source, validator)
        }
        val m = e.getCause.getMessage
        assert(
          m.contains("is not allowed in a policy path") ||
            m.contains("is not available with this host-language/version/license") ||
            m.matches("Cannot find a \\d-argument function .*"),
          "A function in a policy path is illegal")
      }

      test(s"An Invalid Policy Fails XSL Compilation ($policy compiled with $validator)") {
        val source = new StreamSource(policy)
        val e = intercept[SaxonApiException] {
          AttributeMapper.generateXSL(source, PolicyFormat.XML, new XdmDestination, validate = true, validator)
        }
        val m = e.getCause.getMessage
        assert(
          m.contains("is not allowed in a policy path") ||
            m.contains("is not available with this host-language/version/license") ||
            m.matches("Cannot find a \\d-argument function .*"),
          "A function in a policy path is illegal")
      }

      test(s"An Invalid Policy Fails XSLExec Compilation ($policy compiled with $validator)") {
        val source = new StreamSource(policy)
        val e = intercept[SaxonApiException] {
          AttributeMapper.generateXSLExec(source, PolicyFormat.XML, validate = true, validator)
        }
        val m = e.getCause.getMessage
        assert(
          m.contains("is not allowed in a policy path") ||
            m.contains("is not available with this host-language/version/license") ||
            m.matches("Cannot find a \\d-argument function .*"),
          "A function in a policy path is illegal")
      }
    }

    test(s"An Invalid Policy Format Fails Compilation with an exception (with $validator)") {
      val source = new StreamSource()
      val e = intercept[UnsupportedPolicyFormatException] {
        AttributeMapper.generateXSL(source, null, new XdmDestination, validate = true, validator)
      }
      assert(e.getMessage.contains("is not supported"))
    }

    jsonValidPolicies foreach { policy =>
      test(s"A Valid Policy Validates ($policy validated with $validator)") {
        val source = new StreamSource(policy)
        val json = AttributeMapper.parseJsonNode(source)
        AttributeMapper.validatePolicy(json, validator)
      }
    }

    jsonInvalidPolicies foreach { policy =>
      test(s"An Invalid Policy Fails Validation ($policy validated with $validator)") {
        val source = new StreamSource(policy)
        val json = AttributeMapper.parseJsonNode(source)
        val e = intercept[SaxonApiException] {
          AttributeMapper.validatePolicy(json, validator)
        }
        val m = e.getCause.getMessage
        assert(
          m.contains("is not allowed in a policy path") ||
            m.contains("is not available with this host-language/version/license") ||
            m.matches("Cannot find a \\d-argument function .*"),
          "A function in a policy path is illegal")
      }

      test(s"An Invalid Policy Fails XSL Compilation ($policy compiled with $validator)") {
        val source = new StreamSource(policy)
        val json = AttributeMapper.parseJsonNode(source)
        val e = intercept[SaxonApiException] {
          AttributeMapper.generateXSL(json, new XdmDestination, validate = true, validator)
        }
        val m = e.getCause.getMessage
        assert(
          m.contains("is not allowed in a policy path") ||
            m.contains("is not available with this host-language/version/license") ||
            m.matches("Cannot find a \\d-argument function .*"),
          "A function in a policy path is illegal")
      }

      test(s"An Invalid Policy Fails XSLExec Compilation ($policy compiled with $validator)") {
        val source = new StreamSource(policy)
        val json = AttributeMapper.parseJsonNode(source)
        val e = intercept[SaxonApiException] {
          AttributeMapper.generateXSLExec(json, validate = true, validator)
        }
        val m = e.getCause.getMessage
        assert(
          m.contains("is not allowed in a policy path") ||
            m.contains("is not available with this host-language/version/license") ||
            m.matches("Cannot find a \\d-argument function .*"),
          "A function in a policy path is illegal")
      }
    }
  }
}
