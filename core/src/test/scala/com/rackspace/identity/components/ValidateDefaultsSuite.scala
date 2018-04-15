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

import javax.xml.validation.Schema

import javax.xml.transform.stream.StreamSource

import net.sf.saxon.s9api._

import org.scalatest.FunSuite
import org.scalatest.exceptions.TestFailedException

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import org.w3c.dom.Document

import javax.xml.parsers.DocumentBuilder

import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathConstants

import com.rackspace.com.papi.components.checker.util.XMLParserPool._
import com.rackspace.com.papi.components.checker.util.XPathExpressionPool._

import AttributeMapperBase._

@RunWith(classOf[JUnitRunner])
class ValidateDefaultsSuite extends AttributeMapperBase {

  private lazy val defaultsSchema = schemaFactory.newSchema(new StreamSource(getClass.getResource("/xsd/defaults.xsd").toString))

  private val mapperXsltExec = AttributeMapper.compiler.compile(new StreamSource(getClass.getResource("/xsl/mapping.xsl").toString))

  //
  //  This test fails if defaults.xml is not valid according to the
  //  schema.  We want a build failure if it's misconfigured.
  //
  test("Attribute defaults should validate against the schema!") {
    defaultsSchema.newValidator.validate(new StreamSource(getClass.getResource("/xsl/defaults.xml").toString))
  }

  test("If there's a namespace in attribute defaults, it should be reflected in the XSL") {
    val defaultMap = new File("src/test/resources/tests/mapping-tests/defaults/maps/defaults.xml")
    val attributeDefaults = new File("src/test/resources/defaults-ns.xml")

    var docBuilder : DocumentBuilder = null
    var outDoc : Document = null
    try {
      docBuilder = borrowParser
      outDoc = docBuilder.newDocument
    } finally {
      if (docBuilder != null) returnParser(docBuilder)
    }

    val xslDest = new DOMDestination(outDoc)

    val mapperTrans = AttributeMapper.getXsltTransformer (mapperXsltExec, Map[QName, XdmValue]
      (new QName("defaults-config")->new XdmAtomicValue(attributeDefaults.toURI.toString())))

    mapperTrans.setSource(new StreamSource(defaultMap))
    mapperTrans.setDestination(xslDest)
    mapperTrans.transform()

    val fooExpressionStr = "namespace-uri-for-prefix('foo',/element()[1])"
    val xExpressionStr = "namespace-uri-for-prefix('x',/element()[1])"

    var fooExpression : XPathExpression = null
    var xExpression : XPathExpression = null

    try {
      fooExpression = borrowExpression(fooExpressionStr, NS_CONTEXT, XPATH_VERSION)
      xExpression = borrowExpression(xExpressionStr, NS_CONTEXT, XPATH_VERSION)

      assert(fooExpression.evaluate(outDoc, XPathConstants.STRING).asInstanceOf[String] == "bar")
      assert(xExpression.evaluate(outDoc, XPathConstants.STRING).asInstanceOf[String] == "yz")
    }  finally {
      if (fooExpression != null) returnExpression(fooExpressionStr, NS_CONTEXT, XPATH_VERSION, fooExpression)
      if (xExpression != null) returnExpression(xExpressionStr, NS_CONTEXT, XPATH_VERSION, xExpression)
    }
  }
}
