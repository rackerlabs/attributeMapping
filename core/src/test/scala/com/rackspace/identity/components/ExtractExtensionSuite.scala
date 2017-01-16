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

import java.io.ByteArrayOutputStream
import java.io.File

import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathException

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.dom.DOMSource

import com.rackspace.com.papi.components.checker.util.XMLParserPool
import com.rackspace.com.papi.components.checker.util.ImmutableNamespaceContext
import com.rackspace.com.papi.components.checker.util.XPathExpressionPool._


import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite

import net.sf.saxon.s9api._
import net.sf.saxon.Configuration.LicenseFeature._

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

import org.scalatest.exceptions.TestFailedException

import org.w3c.dom.Document

@RunWith(classOf[JUnitRunner])
class ExtractExtensionSuite extends AttributeMapperBase with XPathAssertions {
  val testDir = new File("src/test/resources/tests/extract-extn-tests")
  val tests : List[File] = testDir.listFiles.toList.filter(f => {
    f.toString.endsWith("xml")
  })

  val authXMLSample   = new File("src/test/resources/samples/AuthenticateResponseSAML.xml")
  val authJSONSample  = new File("src/test/resources/samples/AuthenticateResponseSAML.json")

  type ExtractTestXML = (File /* assertion */, String /* validation engine */) => Source /* Resulting extensions */

  def runTestsXML(description : String, extractTest : ExtractTestXML) : Unit = {
      tests.foreach( assertFile => {
        val asserterExec = getAsserterExec(new StreamSource(assertFile))
        validators.foreach (v => {
          test (s"$description ($assertFile validated with $v)") {
            val bout = new ByteArrayOutputStream
            val newExt = extractTest(assertFile, v)
            val asserter = AttributeMapper.getXsltTransformer(asserterExec)
            asserter.setSource(newExt)
            asserter.setDestination(AttributeMapper.processor.newSerializer(bout))
            asserter.transform()

            assert(bout.toString.contains("mapping:success"))
          }
        })
      })
  }


  type ExtractTestJSON = (File /* assertion */, String /* validation engine */) => String /* Resulting JSON as string */

  def runTestsJSON(description : String, extractTest : ExtractTestJSON) : Unit = {
      tests.foreach( assertFile => {
        val asserterExec = getAsserterJsonExec(new StreamSource(assertFile))
        validators.foreach (v => {
          test (s"$description ($assertFile validated with $v)") {
            val bout = new ByteArrayOutputStream
            val newExt = extractTest(assertFile, v)
            val asserter = AttributeMapper.getXQueryEvaluator(asserterExec, Map[QName,XdmValue](new QName("__JSON__") ->
                                                                                                new XdmAtomicValue(newExt)))
            asserter.setDestination(AttributeMapper.processor.newSerializer(bout))
            asserter.run()

            assert(bout.toString.contains("mapping:success"))
          }
        })
      })
  }

  //
  // Register namespaces for xpath asserts
  //

  register("ks","http://docs.openstack.org/identity/api/v2.0")
  register("rax-auth","http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0")

  runTestsXML("XML Extended Attributes", (assertFile : File, v : String) => {
    println (s"Getting extended attributes in XML from $assertFile") // scalastyle:ignore
    val dest = new XdmDestination
    AttributeMapper.extractExtendedAttributes (new StreamSource(assertFile), dest, false,
                                              true, v)
    dest.getXdmNode.asSource
  })

  //
  //  Some simple spot assertions to make sure we didn't muck with the
  //  auth response after we modified it.
  //
  def accessXMLAssertions(doc : Document) : Unit = {
    assert(doc, "/ks:access")

    // spot check token

    assert(doc, "/ks:access/ks:token[@id='aaaaa-bbbbb-ccccc-dddd']")
    assert(doc, "/ks:access/ks:token/ks:tenant[@id='12345']")
    assert(doc, "/ks:access/ks:token/rax-auth:authenticatedBy/rax-auth:credential = 'FEDERATED'")

    // spot check user

    assert(doc, "/ks:access/ks:user[@id='161418']")
    assert(doc, "/ks:access/ks:user[@id='161418']/ks:roles/ks:role[1]/@id = '3'")
    assert(doc, "/ks:access/ks:user[@id='161418']/ks:roles/ks:role[1]/@name = 'identity:default'")
    assert(doc, "/ks:access/ks:user[@id='161418']/ks:roles/ks:role[2]/@id = '208'")
    assert(doc, "/ks:access/ks:user[@id='161418']/ks:roles/ks:role[2]/@name = 'nova:admin'")

    // spot check catalog
    assert(doc, """/ks:access/ks:serviceCatalog/ks:service[@type='rax:database']/ks:endpoint[@region='DFW']/@publicURL
                 = 'https://dfw.databases.api.rackspacecloud.com/v1.0/12345' """)

    assert(doc, """/ks:access/ks:serviceCatalog/ks:service[@type='rax:monitor']/ks:endpoint/@publicURL
                 = 'https://monitoring.api.rackspacecloud.com/v1.0/12345' """)

    assert(doc, """/ks:access/ks:serviceCatalog/ks:service[@type='compute' and @name='cloudServers']/ks:endpoint/@publicURL
                 = 'https://servers.api.rackspacecloud.com/v1.0/12345' """)


    assert(doc, """/ks:access/ks:serviceCatalog/ks:service[@type='compute' and @name='cloudServersOpenStack']/ks:endpoint[@region='DFW']/@publicURL
                 = 'https://dfw.servers.api.rackspacecloud.com/v2/12345' """)

  }


  //
  //  Some simple spot assertions to make sure we didn't muck with the
  //  auth response after we modified it.
  //
  def accessJSONAssertions(node : JsonNode) : Unit = {
    assert (node, "exists($_?access)")

    //
    // spot check token
    //
    assert (node, "$_?access?token?id = 'aaaaa-bbbbb-ccccc-dddd'")
    assert (node, "$_?access?token?tenant?id = '12345'")
    assert (node, "$_?access?token?('RAX-AUTH:authenticatedBy')?*[1] = 'FEDERATED'")

    // spot check user
    assert (node, "$_?access?user?id = '161418'")
    assert (node, "$_?access?user?roles?1?id = '3'")
    assert (node, "$_?access?user?roles?1?name = 'identity:default'")
    assert (node, "$_?access?user?roles?2?id = '208'")
    assert (node, "$_?access?user?roles?2?name = 'nova:admin'")

    // spot check catalog
    assert (node, """$_?access?serviceCatalog?*[?type='rax:database']?endpoints?*[?region='DFW']?publicURL =
      'https://dfw.databases.api.rackspacecloud.com/v1.0/12345'""")
    assert (node, """$_?access?serviceCatalog?*[?type='rax:monitor']?endpoints?1?publicURL =
      'https://monitoring.api.rackspacecloud.com/v1.0/12345'""")
    assert (node, """$_?access?serviceCatalog?*[?type='compute' and ?name='cloudServers']?endpoints?1?publicURL =
      'https://servers.api.rackspacecloud.com/v1.0/12345'""")
    assert (node, """$_?access?serviceCatalog?*[?type='compute' and ?name='cloudServersOpenStack']?endpoints?*[?region='DFW']?publicURL =
      'https://dfw.servers.api.rackspacecloud.com/v2/12345'""")
  }

  def shouldBeEmptyExtensions(assert : Source) : Boolean = {
    val nsContext = ImmutableNamespaceContext(Map[String,String]())
    val xpathString = "//processing-instruction()[name() ='noExt']"
    val XPATH_VERSION = 31
    var exp : XPathExpression = null
    try {
      exp = borrowExpression(xpathString,nsContext, XPATH_VERSION)
      exp.evaluate (assert, XPathConstants.BOOLEAN).asInstanceOf[Boolean]
    } catch {
      case xpe : XPathException => throw new TestFailedException (s"Error in XPath $xpathString", xpe, 4) // scalastyle:ignore
      case tf : TestFailedException => throw tf
      case unknown : Throwable => throw new TestFailedException(s"Unknown error in XPath $xpathString", 4) // scalastyle:ignore
    } finally {
      if (exp != null) returnExpression (xpathString, nsContext, XPATH_VERSION, exp)
    }
  }

  //
  //  Validates an auth result with a possible auth extension and
  //  returns the extension (if one exists) or the original if one
  //  doesn't.
  //
  def validateAuthExtensions (authExt : Document, assertion : Source) : Source = {
    var docBuilder : javax.xml.parsers.DocumentBuilder = null
    var outDoc2 : Document = null
    try {
      docBuilder = XMLParserPool.borrowParser
      outDoc2 = docBuilder.newDocument
    } finally {
      if (docBuilder != null) XMLParserPool.returnParser(docBuilder)
    }

    //
    //  Asserts on destination
    //
    accessXMLAssertions(authExt)

    //
    // Empty assert
    //
    if (shouldBeEmptyExtensions(assertion)) {
      assert(authExt,"empty(/ks:access/rax-auth:extendedAttributes)")
    } else {
      assert(authExt,"not(empty(/ks:access/rax-auth:extendedAttributes))")
      assert(authExt,"count(/ks:access/rax-auth:extendedAttributes) = 1")
    }

    //
    //  Extract and return extensions
    //
    val extnNode = authExt.getElementsByTagNameNS("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0",
                                                 "extendedAttributes").item(0)
    if (extnNode != null) {
      val newExtn = outDoc2.importNode (extnNode, true)
      outDoc2.appendChild(newExtn)
      new DOMSource(outDoc2)
    } else {
      new DOMSource(authExt)
    }
  }

  //
  //  Validates an auth result with a possible auth extension and
  //  returns the extension (if one exists) or the original if one
  //  doesn't.
  //
  def validateAuthExtensions (authExt : JsonNode, assertion : Source) : String = {
    val om = new ObjectMapper

    //
    //  Asserts on destination
    //
    accessJSONAssertions(authExt)

    //
    //  Empty assert
    //
    if (shouldBeEmptyExtensions(assertion)) {
      assert(authExt,"empty($_?access?('RAX-AUTH:extendedAttributes'))")
    } else {
      assert(authExt,"exists($_?access?('RAX-AUTH:extendedAttributes'))")
    }

    om.writeValueAsString(authExt.get("access"))
  }

  runTestsXML("XML Extended Attributes -- built into request (combine call)", (assertFile : File, v : String) => {
    println (s"Adding extended attributes in XML from $assertFile") // scalastyle:ignore
    var docBuilder : javax.xml.parsers.DocumentBuilder = null
    try {
      docBuilder = XMLParserPool.borrowParser
      val outDoc = docBuilder.newDocument
      val accessDest = new DOMDestination(outDoc)
      AttributeMapper.addExtendedAttributes (new StreamSource(authXMLSample), new StreamSource(assertFile),
                                             accessDest, false, true, v)

      validateAuthExtensions (outDoc, new StreamSource(assertFile))
    } finally {
      if (docBuilder != null) XMLParserPool.returnParser(docBuilder)
    }
  })


  runTestsXML("XML Extended Attributes -- built into request (doc assert, doc result)", (assertFile : File, v : String) => {
    println (s"Adding extended attributes in XML from $assertFile") // scalastyle:ignore
    var docBuilder : javax.xml.parsers.DocumentBuilder = null
    try {
      docBuilder = XMLParserPool.borrowParser
      val assert = docBuilder.parse(assertFile)
      val outDoc = AttributeMapper.addExtendedAttributes (new StreamSource(authXMLSample), assert,
                                                          true, v)

      validateAuthExtensions (outDoc, new DOMSource(assert))
    } finally {
      if (docBuilder != null) XMLParserPool.returnParser(docBuilder)
    }
  })

  runTestsXML("XML Extended Attributes -- built into request (doc auth, doc assert, doc result)", (assertFile : File, v : String) => {
    println (s"Adding extended attributes in XML from $assertFile") // scalastyle:ignore
    var docBuilder : javax.xml.parsers.DocumentBuilder = null
    try {
      docBuilder = XMLParserPool.borrowParser
      val assert = docBuilder.parse(assertFile)
      val authResp = docBuilder.parse(authXMLSample)

      val outDoc = AttributeMapper.addExtendedAttributes (authResp, assert, true, v)

      validateAuthExtensions (outDoc, new DOMSource(assert))
    } finally {
      if (docBuilder != null) XMLParserPool.returnParser(docBuilder)
    }
  })


  runTestsXML("XML Extended Attributes -- built into request (xml source call)", (assertFile : File, v : String) => {
    println (s"Adding extended attributes in XML from $assertFile") // scalastyle:ignore
    var docBuilder : javax.xml.parsers.DocumentBuilder = null
    try {
      docBuilder = XMLParserPool.borrowParser
      val outDoc = docBuilder.newDocument

      val accessDest = new DOMDestination(outDoc)
      AttributeMapper.addExtendedAttributes (new StreamSource(authXMLSample), new StreamSource(assertFile),
                                             accessDest, true, v)

      validateAuthExtensions (outDoc, new StreamSource(assertFile))
    } finally {
      if (docBuilder != null) XMLParserPool.returnParser(docBuilder)
    }
  })


  runTestsJSON("JSON Extended Attributes", (assertFile : File, v : String) => {
    println (s"Getting extended attributes in JSON from $assertFile") // scalastyle:ignore
    val bout = new ByteArrayOutputStream
    val dest = AttributeMapper.processor.newSerializer(bout)

    AttributeMapper.extractExtendedAttributes(new StreamSource(assertFile), dest, true,
                                              true, v)
    bout.toString("UTF-8")
  })

  runTestsJSON("JSON Extended Attributes (JsonNode)", (assertFile : File, v : String) => {
    println (s"Getting extended attributes in JSON from $assertFile") // scalastyle:ignore
    val om = new ObjectMapper
    val node = AttributeMapper.extractExtendedAttributes(new StreamSource(assertFile), true, v)

    om.writeValueAsString(node)
  })

  runTestsJSON("JSON Extended Attributes -- built into request (combine call)", (assertFile : File, v : String) => {
    println (s"Getting extended attributes in JSON from $assertFile") // scalastyle:ignore
    val om = new ObjectMapper
    val bout = new ByteArrayOutputStream
    val dest = AttributeMapper.processor.newSerializer(bout)

    AttributeMapper.addExtendedAttributes(new StreamSource (authJSONSample), new StreamSource(assertFile), dest,
                                          true, true, v)
    val node = om.readTree (bout.toString("UTF-8"))

    validateAuthExtensions(node, new StreamSource(assertFile))
  })

  runTestsJSON("JSON Extended Attributes -- built into request (authResp as JsonNode, return JsonNode)", (assertFile : File, v : String) => {
    println (s"Getting extended attributes in JSON from $assertFile") // scalastyle:ignore
    val om = new ObjectMapper

    val node = AttributeMapper.addExtendedAttributes(om.readTree (authJSONSample), new StreamSource(assertFile),
                                                     true, v)

    validateAuthExtensions(node, new StreamSource(assertFile))
  })

  runTestsJSON("JSON Extended Attributes -- built into request (authResp as streamSource, return JsonNode)", (assertFile : File, v : String) => {
    println (s"Getting extended attributes in JSON from $assertFile") // scalastyle:ignore

    val node = AttributeMapper.addExtendedAttributes(new StreamSource (authJSONSample), new StreamSource(assertFile),
                                                     true, v)

    validateAuthExtensions(node, new StreamSource(assertFile))
  })

  runTestsJSON("JSON Extended Attributes -- built into request (authResp as JsonNode, assert as Doc, return JsonNode)",
               (assertFile : File, v : String) => {
    println (s"Getting extended attributes in JSON from $assertFile") // scalastyle:ignore
    var docBuilder : javax.xml.parsers.DocumentBuilder = null
    try {
      docBuilder = XMLParserPool.borrowParser
      val om = new ObjectMapper
      val assert = docBuilder.parse(assertFile)
      val node = AttributeMapper.addExtendedAttributes(om.readTree (authJSONSample), assert, true, v)

      validateAuthExtensions(node, new StreamSource(assertFile))
    } finally {
      if (docBuilder != null) XMLParserPool.returnParser(docBuilder)
    }
  })
}
