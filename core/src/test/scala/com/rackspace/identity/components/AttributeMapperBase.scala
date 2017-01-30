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
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource

import org.scalatest.FunSuite

import net.sf.saxon.s9api._
import net.sf.saxon.Configuration.LicenseFeature._

class AttributeMapperBase extends FunSuite {
  val testerXSLExec = AttributeMapper.compiler.compile (new StreamSource(new File("src/test/resources/xsl/mapping-tests.xsl")))
  val testerJsonXSLExec = AttributeMapper.compiler.compile (new StreamSource(new File("src/test/resources/xsl/mapping-tests-json.xsl")))

  val validators : List[String] = {
    if (!AttributeMapper.processor.getUnderlyingConfiguration.isLicensedFeature(SCHEMA_VALIDATION)) {
      println("------------------------------------------------") // scalastyle:ignore
      println("NO SAXON LICENSE DETECTED - SKIPPING SAXON TESTS") // scalastyle:ignore
      println("------------------------------------------------") // scalastyle:ignore
      List[String]("xerces")
    } else {
      List[String]("xerces", "saxon")
    }
  }

  def getAsserterExec (assertSource : Source) : XsltExecutable = {
    val asserterXSL = new XdmDestination

    val asserterTrans = AttributeMapper.getXsltTransformer (testerXSLExec)
    asserterTrans.setSource (assertSource)
    asserterTrans.setDestination(asserterXSL)
    asserterTrans.transform()

    AttributeMapper.compiler.compile(asserterXSL.getXdmNode.asSource)
  }

  def getAsserterJsonExec (assertSource : Source) : XQueryExecutable = {
    val bout = new ByteArrayOutputStream
    val asserterXQuery = AttributeMapper.processor.newSerializer(bout)

    val asserterJsonTrans = AttributeMapper.getXsltTransformer (testerJsonXSLExec)
    asserterJsonTrans.setSource(assertSource)
    asserterJsonTrans.setDestination(asserterXQuery)
    asserterJsonTrans.transform()

    AttributeMapper.xqueryCompiler.compile (new ByteArrayInputStream(bout.toByteArray))
  }
}
