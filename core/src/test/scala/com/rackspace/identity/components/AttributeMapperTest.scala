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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import javax.xml.transform.stream.StreamSource

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}

@RunWith(classOf[JUnitRunner])
class AttributeMapperTest extends FunSuite with Matchers {

  import AttributeMapperTest._

  val yamlMapper = new ObjectMapper(new YAMLFactory())

  test("policy2YAML should translate a JSON policy to YAML") {
    val jsonPolicy = new StreamSource(new ByteArrayInputStream(VALID_JSON_POLICY.getBytes))
    val yamlPolicy = new ByteArrayOutputStream()

    AttributeMapper.policy2YAML(jsonPolicy, yamlPolicy, validate = false, XSDEngine.AUTO.toString)

    val outputYaml = yamlMapper.readTree(yamlPolicy.toByteArray)
    val expectedYaml = yamlMapper.readTree(VALID_YAML_POLICY)

    outputYaml shouldEqual expectedYaml
  }

  test("policy2YAML should translate an invalid JSON policy to YAML") {
    val jsonPolicy = new StreamSource(new ByteArrayInputStream(INVALID_JSON_POLICY.getBytes))
    val yamlPolicy = new ByteArrayOutputStream()

    AttributeMapper.policy2YAML(jsonPolicy, yamlPolicy, validate = false, XSDEngine.AUTO.toString)

    val outputYaml = yamlMapper.readTree(yamlPolicy.toByteArray)
    val expectedYaml = yamlMapper.readTree(INVALID_YAML_POLICY)

    outputYaml shouldEqual expectedYaml
  }

  test("policy2YAML should fail to validate an invalid policy") {
    val jsonPolicy = new StreamSource(new ByteArrayInputStream(INVALID_JSON_POLICY.getBytes))
    val yamlPolicy = new ByteArrayOutputStream()

    a[Exception] should be thrownBy AttributeMapper.policy2YAML(jsonPolicy, yamlPolicy, validate = true, XSDEngine.XERCES.toString)
  }
}

object AttributeMapperTest {
  final val VALID_JSON_POLICY: String =
    """
      |{
      |  "mapping": {
      |    "description": "Default mapping policy",
      |    "rules": [
      |      {
      |        "local": {
      |          "user": {
      |            "domain": "{D}",
      |            "email": "{D}",
      |            "expire": "{D}",
      |            "name": "{D}",
      |            "roles": "{D}"
      |          }
      |        }
      |      }
      |    ],
      |    "version": "RAX-1"
      |  }
      |}
    """.stripMargin

  final val VALID_YAML_POLICY: String =
    """
      |---
      |mapping:
      |  description: Default mapping policy
      |  rules:
      |  - local:
      |      user:
      |        domain: "{D}"
      |        email: "{D}"
      |        expire: "{D}"
      |        name: "{D}"
      |        roles: "{D}"
      |  version: RAX-1
    """.stripMargin

  final val INVALID_JSON_POLICY: String =
    """
      |{
      |  "mapping": {
      |    "description": "Default mapping policy",
      |    "rules": [
      |      {
      |        "local": {
      |          "user": {
      |            "domain": "{D}",
      |            "email": "{D}",
      |            "expire": "{D}",
      |            "name": "{D}",
      |            "roles": "{D}"
      |          }
      |        }
      |      }
      |    ],
      |    "version": "RAX-OVER-9000"
      |  }
      |}
    """.stripMargin

  final val INVALID_YAML_POLICY: String =
    """
      |---
      |mapping:
      |  description: Default mapping policy
      |  rules:
      |  - local:
      |      user:
      |        domain: "{D}"
      |        email: "{D}"
      |        expire: "{D}"
      |        name: "{D}"
      |        roles: "{D}"
      |  version: RAX-OVER-9000
    """.stripMargin
}
