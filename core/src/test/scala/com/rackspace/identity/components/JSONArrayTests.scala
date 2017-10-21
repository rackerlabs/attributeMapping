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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode

import javax.xml.transform.stream.StreamSource

import org.junit.runner.RunWith

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JSONArrayTests extends FunSuite {
  val targetSAMLAssertion = new File ("src/test/resources/tests/mapping-tests/defaults/asserts/sample_assert.xml");

  def doTest (desc : String, policy : String, asserts : JsonNode => Unit) : Unit = {
    test (desc) {
      val policyIn = new StreamSource(new ByteArrayInputStream(policy.getBytes))
      val assertIn = new StreamSource(targetSAMLAssertion)
      val destStream = new ByteArrayOutputStream
      val destOut = AttributeMapper.processor.newSerializer(destStream)
      val om = new ObjectMapper()

      AttributeMapper.convertAssertion(policyIn, PolicyFormat.YAML, assertIn,
        destOut, false, true, "auto")

      val tree = om.readTree(destStream.toByteArray)
      println (new String(destStream.toByteArray)) // scalastyle:ignore
      asserts(tree)
    }
  }

  doTest("An extended user attribute with an array and multiple items should return correctly",
    """---
mapping:
  version: RAX-1
  rules:
  - local:
      user:
        domain: "{D}"
        name: "{D}"
        email: "{D}"
        roles: "{D}"
        expire: "{D}"
        ext:
         value:
         - Sample1
         - Sample2
         multiValue: true
""", (tree) => {
      assert(tree.get("local").get("user").get("ext").get("value").get(0).asText == "Sample1")
      assert(tree.get("local").get("user").get("ext").get("value").get(1).asText == "Sample2")
    })

  doTest("An extended user attribute with an array and multiple items should return correctly (no multivalue)",
    """---
mapping:
  version: RAX-1
  rules:
  - local:
      user:
        domain: "{D}"
        name: "{D}"
        email: "{D}"
        roles: "{D}"
        expire: "{D}"
        ext:
         - Sample1
         - Sample2
""", (tree) => {
      assert(tree.get("local").get("user").get("ext").get("value").get(0).asText == "Sample1")
      assert(tree.get("local").get("user").get("ext").get("value").get(1).asText == "Sample2")
    })

  doTest("An extended user attribute with an array and single item should return correctly",
    """---
mapping:
  version: RAX-1
  rules:
  - local:
      user:
        domain: "{D}"
        name: "{D}"
        email: "{D}"
        roles: "{D}"
        expire: "{D}"
        ext:
         value:
         - Sample1
         multiValue: true
""", (tree) => {
      assert(tree.get("local").get("user").get("ext").get("value").get(0).asText == "Sample1")
    })

    doTest("An extended user attribute with an array and single item should return correctly (no multi-value)",
    """---
mapping:
  version: RAX-1
  rules:
  - local:
      user:
        domain: "{D}"
        name: "{D}"
        email: "{D}"
        roles: "{D}"
        expire: "{D}"
        ext:
         - Sample1
""", (tree) => {
      assert(tree.get("local").get("user").get("ext").get("value").get(0).asText == "Sample1")
    })


  doTest("An extended attribute with an array and multiple items should return correctly",
    """---
mapping:
  version: RAX-1
  rules:
  - local:
      user:
        domain: "{D}"
        name: "{D}"
        email: "{D}"
        roles: "{D}"
        expire: "{D}"
      ext:
        ext2:
         value:
         - Sample1
         - Sample2
         multiValue: true
""", (tree) => {
      assert(tree.get("local").get("ext").get("ext2").get("value").get(0).asText == "Sample1")
      assert(tree.get("local").get("ext").get("ext2").get("value").get(1).asText == "Sample2")
    })


    doTest("An extended attribute with an array and multiple items should return correctly (no multi-value)",
    """---
mapping:
  version: RAX-1
  rules:
  - local:
      user:
        domain: "{D}"
        name: "{D}"
        email: "{D}"
        roles: "{D}"
        expire: "{D}"
      ext:
        ext2:
         - Sample1
         - Sample2
""", (tree) => {
      assert(tree.get("local").get("ext").get("ext2").get("value").get(0).asText == "Sample1")
      assert(tree.get("local").get("ext").get("ext2").get("value").get(1).asText == "Sample2")
    })

    doTest("An extended attribute with an array and single item should return correctly",
    """---
mapping:
  version: RAX-1
  rules:
  - local:
      user:
        domain: "{D}"
        name: "{D}"
        email: "{D}"
        roles: "{D}"
        expire: "{D}"
      ext:
        ext2:
         value:
         - Sample1
         multiValue: true
""", (tree) => {
      assert(tree.get("local").get("ext").get("ext2").get("value").get(0).asText == "Sample1")
    })


    doTest("An extended attribute with an array and single item should return correctly (no multi-value)",
    """---
mapping:
  version: RAX-1
  rules:
  - local:
      user:
        domain: "{D}"
        name: "{D}"
        email: "{D}"
        roles: "{D}"
        expire: "{D}"
      ext:
        ext2:
         - Sample1
""", (tree) => {
      assert(tree.get("local").get("ext").get("ext2").get("value").get(0).asText == "Sample1")
    })
}
