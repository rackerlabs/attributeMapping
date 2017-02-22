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

import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathConstants

import org.scalatest.FunSuite
import org.scalatest.exceptions.TestFailedException

import org.w3c.dom.Document

import net.sf.saxon.s9api._
import net.sf.saxon.Configuration.LicenseFeature._

import com.rackspace.com.papi.components.checker.util.ImmutableNamespaceContext
import com.rackspace.com.papi.components.checker.util.XPathExpressionPool._

object AttributeMapperBase {
  //
  //  XPath namespaces and expressions
  //
  val NS_CONTEXT = ImmutableNamespaceContext(Map[String,String]("mapping"->"http://docs.rackspace.com/identity/api/ext/MappingRules"))
  val XPATH_VERSION = 31

  val TEST_SUCCESS_EXP   = "/mapping:success"
  val TEST_ASSERT_EXP    = "/mapping:fail/@assertion"
  val TEST_MESSAGE_EXP   = "normalize-space(/mapping:fail/mapping:message)"
  val TEST_CONT_EXP       = """if (/mapping:fail/mapping:onXML) then serialize(/mapping:fail/mapping:onXML/element())
                               else /mapping:fail/mapping:onJSON"""
}

import AttributeMapperBase._

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

  def assert (doc : Document) : Unit = {
    var successExpression : XPathExpression = null
    var assertExpression : XPathExpression = null
    var messageExpression : XPathExpression = null
    var contExpression : XPathExpression = null
    try {
      successExpression = borrowExpression(TEST_SUCCESS_EXP, NS_CONTEXT, XPATH_VERSION)
      if (!successExpression.evaluate(doc, XPathConstants.BOOLEAN).asInstanceOf[Boolean]) {
        assertExpression = borrowExpression(TEST_ASSERT_EXP, NS_CONTEXT, XPATH_VERSION)
        messageExpression = borrowExpression(TEST_MESSAGE_EXP, NS_CONTEXT, XPATH_VERSION)
        contExpression = borrowExpression(TEST_CONT_EXP, NS_CONTEXT, XPATH_VERSION)

        val assertion = assertExpression.evaluate(doc,XPathConstants.STRING).asInstanceOf[String]
        val message = messageExpression.evaluate(doc,XPathConstants.STRING).asInstanceOf[String]
        val cont = contExpression.evaluate(doc,XPathConstants.STRING).asInstanceOf[String]

        val fullMessage = s"TEST FAILED!\n- ASSERTION: $assertion\n- MESSAGE: $message\n- ON CONTENT: $cont\n"
        print(fullMessage)
        throw new TestFailedException (fullMessage, 4) // scalastyle:ignore
      }
    } finally {
      if (successExpression != null) returnExpression(TEST_SUCCESS_EXP, NS_CONTEXT, XPATH_VERSION, successExpression)
      if (assertExpression != null) returnExpression(TEST_ASSERT_EXP, NS_CONTEXT, XPATH_VERSION, assertExpression)
      if (messageExpression != null) returnExpression(TEST_MESSAGE_EXP, NS_CONTEXT, XPATH_VERSION, messageExpression)
      if (contExpression != null) returnExpression(TEST_CONT_EXP, NS_CONTEXT, XPATH_VERSION, contExpression)
    }
  }
}
