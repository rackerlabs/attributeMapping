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

import net.sf.saxon.trans.XPathException
import net.sf.saxon.s9api.XdmDestination
import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ValidatePolicySuite extends AttributeMapperBase with Matchers {

  val testDir: File = new File("src/test/resources/tests/validate-policy-tests")
  val validTestDir: File = new File(testDir, "valid")
  val invalidTestDir: File = new File(testDir, "invalid")

  val validPolicies: Array[File] = validTestDir.listFiles
  val blacklistedPolicies: Array[File] = new File(invalidTestDir, "blacklisted").listFiles
  val blacklistedInlineFunctionPolicies: Array[File] = new File(invalidTestDir, "blacklisted-inline-function").listFiles
  val unsupportedPolicies: Array[File] = new File(invalidTestDir, "unsupported").listFiles

  //
  // "Higher-order functions" are entierly blocked by the parser. There is no distinction between "safe" and
  // "unsafe" (i.e., recursive higher-order function). When such a distinction is made "safe" higher-order functions
  // (i.e., those duplicated in the "valid/hof" directory) should be
  // removed from the invalid test category.
  //
  val blacklistedHofPolicies: Array[File] = new File(invalidTestDir, "blacklisted-hof").listFiles

  def filterXmlFiles(files: Array[File]): Array[File] = {
    files.filter(f => f.getName.endsWith("xml"))
  }

  def filterJsonFiles(files: Array[File]): Array[File] = {
    files.filter(f => f.getName.endsWith("json"))
  }

  validators foreach { validator =>
    test(s"An Invalid Policy Format Fails Compilation with an exception (with $validator)") {
      val source = new StreamSource()
      val e = intercept[UnsupportedPolicyFormatException] {
        AttributeMapper.generateXSL(source, null, new XdmDestination, validate = true, validator)
      }
      val m = e.getMessage
      m should include("is not supported")
    }

    filterXmlFiles(validPolicies) foreach { policy =>
      test(s"A Valid Policy Validates ($policy validated with $validator)") {
        val source = new StreamSource(policy)
        AttributeMapper.validatePolicy(source, validator)
      }
    }

    filterJsonFiles(validPolicies) foreach { policy =>
      test(s"A Valid Policy Validates ($policy validated with $validator)") {
        val source = new StreamSource(policy)
        val json = AttributeMapper.parseJsonNode(source)
        AttributeMapper.validatePolicy(json, validator)
      }
    }

    Seq(
      blacklistedPolicies -> "(?s).*The function .* is not allowed!.*",
      blacklistedHofPolicies -> "(?s).*Inline Functions are not allowed!.*",
      blacklistedInlineFunctionPolicies -> "(?s).*Inline Functions are not allowed!.*",
      unsupportedPolicies -> "(?s).*The function .* is not allowed!.*"
    ) foreach { case (policies, causeMessage) =>
      filterXmlFiles(policies) foreach { policy =>
        test(s"An Invalid Policy Fails Validation ($policy validated with $validator)") {
          val source = new StreamSource(policy)
          val e = intercept[XPathException] {
            AttributeMapper.validatePolicy(source, validator)
          }
          val m = e.getMessage
         m should fullyMatch regex causeMessage
        }

        test(s"An Invalid Policy Fails XSL Compilation ($policy compiled with $validator)") {
          val source = new StreamSource(policy)
          val e = intercept[XPathException] {
            AttributeMapper.generateXSL(source, PolicyFormat.XML, new XdmDestination, validate = true, validator)
          }
          val m = e.getMessage
          m should fullyMatch regex causeMessage
        }

        test(s"An Invalid Policy Fails XSLExec Compilation ($policy compiled with $validator)") {
          val source = new StreamSource(policy)
          val e = intercept[XPathException] {
            AttributeMapper.generateXSLExec(source, PolicyFormat.XML, validate = true, validator)
          }
          val m = e.getMessage
          m should fullyMatch regex causeMessage
        }
      }

      filterJsonFiles(policies) foreach { policy =>
        test(s"An Invalid Policy Fails Validation ($policy validated with $validator)") {
          val source = new StreamSource(policy)
          val json = AttributeMapper.parseJsonNode(source)
          val e = intercept[XPathException] {
            AttributeMapper.validatePolicy(json, validator)
          }
          val m = e.getMessage
          m should fullyMatch regex causeMessage
        }

        test(s"An Invalid Policy Fails XSL Compilation ($policy compiled with $validator)") {
          val source = new StreamSource(policy)
          val json = AttributeMapper.parseJsonNode(source)
          val e = intercept[XPathException] {
            AttributeMapper.generateXSL(json, new XdmDestination, validate = true, validator)
          }
          val m = e.getMessage
          m should fullyMatch regex causeMessage
        }

        test(s"An Invalid Policy Fails XSLExec Compilation ($policy compiled with $validator)") {
          val source = new StreamSource(policy)
          val json = AttributeMapper.parseJsonNode(source)
          val e = intercept[XPathException] {
            AttributeMapper.generateXSLExec(json, validate = true, validator)
          }
          val m = e.getMessage
          m should fullyMatch regex causeMessage
        }
      }
    }
  }
}
