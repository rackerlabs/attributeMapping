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

import scala.collection.mutable.Map
import scala.collection.mutable.HashMap

import javax.xml.transform.Source
import javax.xml.namespace.QName

import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathException

import org.scalatest.exceptions.TestFailedException

import com.fasterxml.jackson.databind.JsonNode

import com.rackspace.com.papi.components.checker.util.ImmutableNamespaceContext
import com.rackspace.com.papi.components.checker.util.XPathExpressionPool._
import com.rackspace.com.papi.components.checker.util.XMLParserPool._
import com.rackspace.com.papi.components.checker.util.JSONConverter._
import com.rackspace.com.papi.components.checker.util.VarXPathExpression

import org.w3c.dom.Document

object XPathAssertions {
  val XPATH_VERSION = 31

  private val inDoc = {
    val parser = borrowParser
    try {
      parser.newDocument
    } finally {
      returnParser(parser)
    }
  }
}

import XPathAssertions._

trait XPathAssertions {
  private val nsMap : Map[String, String] = new HashMap[String, String]()

  def register (prefix : String, uri : String) : Unit = {
    nsMap += (prefix -> uri)
  }

  def assert (src : Document, xpathString : String) : Unit = {
    val nsContext = ImmutableNamespaceContext(nsMap)
    var exp : XPathExpression = null
    try {
      exp = borrowExpression(xpathString, nsContext, XPATH_VERSION)
      if (!exp.evaluate(src, XPathConstants.BOOLEAN).asInstanceOf[Boolean]) {
        throw new TestFailedException (s"XPath expression does not evaluate to true(): $xpathString", 4) // scalastyle:ignore
      }
    } catch {
      case xpe : XPathException => throw new TestFailedException (s"Error in XPath $xpathString", xpe, 4) // scalastyle:ignore
      case tf : TestFailedException => throw tf
      case unknown : Throwable => throw new TestFailedException(s"Unknown error in XPath $xpathString", 4) // scalastyle:ignore
    } finally {
      if (exp != null) returnExpression (xpathString, nsContext, XPATH_VERSION, exp)
    }
  }

  def assert (jsonSrc : JsonNode, xpathString : String) : Unit = {
    val nsContext = ImmutableNamespaceContext(nsMap)
    var exp : VarXPathExpression = null
    val vars = scala.collection.immutable.Map(new QName("_") -> convert(jsonSrc))

    try {
      exp = borrowExpression(xpathString, nsContext, XPATH_VERSION).asInstanceOf[VarXPathExpression]
      if (!exp.evaluate(inDoc, XPathConstants.BOOLEAN, vars).asInstanceOf[Boolean]) {
        throw new TestFailedException (s"XPath expression does not evaluate to true(): $xpathString", 4) // scalastyle:ignore
      }
    } catch {
      case xpe : XPathException => throw new TestFailedException (s"Error in XPath $xpathString", xpe, 4) // scalastyle:ignore
      case tf : TestFailedException => throw tf
      case unknown : Throwable => throw new TestFailedException(s"Unknown error in XPath $xpathString", 4) // scalastyle:ignore
    } finally {
      if (exp != null) returnExpression (xpathString, nsContext, XPATH_VERSION, exp)
    }
  }
}
