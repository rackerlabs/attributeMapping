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

import javax.xml.validation.Schema

import javax.xml.transform.stream.StreamSource

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ValidateXPathRulesSuite extends AttributeMapperBase {
  private lazy val xpath31RulesSchema = schemaFactory.newSchema (new StreamSource(getClass.getResource("/xsd/xpath-31-rules.xsd").toString))


  //
  //  The test simply checks the grammar of the
  //  /xsl/xpath-31-rules.xml config file.  Because Xerces does not
  //  implement the XPath function in-scope-prefixes() that is needed
  //  for validation, we don't run the test if a Saxon license is not
  //  detected.
  //
  //  This *should* be okay since we don't release unless we run tests
  //  with a Saxon license.
  //
  test ("XPath 3.1 Rules should validate against schema!") {
    if (validators.contains("saxon")) {
      xpath31RulesSchema.newValidator.validate(new StreamSource(getClass.getResource("/xsl/xpath-31-rules.xml").toString))
    } else {
      println("----------------------------------------------------------------") // scalastyle:ignore
      println("NO SAXON LICENSE DETECTED - xpath-31-rules.xml not verified!!!!!") // scalastyle:ignore
      println("----------------------------------------------------------------") // scalastyle:ignore
    }
  }
}
