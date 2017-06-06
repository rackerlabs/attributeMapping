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

import com.rackspace.identity.components.AttributeMapper.XQUERY_VERSION
import net.sf.saxon.expr.instruct.UserFunctionParameter
import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.expr.{Expression, StaticContext}
import net.sf.saxon.functions.FunctionLibraryList
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.query.{XQueryFunction, XQueryFunctionLibrary}
import net.sf.saxon.s9api._
import net.sf.saxon.sxpath.{IndependentContext, XPathStaticContext}
import net.sf.saxon.trans.XPathException
import net.sf.saxon.{Configuration, value}

import scala.collection.JavaConverters._

//
// An extension function for XQuery which validates
// XPath expressions within a document.
//
class ValidateXPathFunction(conf: Configuration) extends ExtensionFunction {

  import ValidateXPathFunction._

  private val xqlib = new XQueryFunctionLibrary(conf)

  //
  // Declare all of the supported extension functions.
  //
  FUNCTIONS foreach { function =>
    val fun = new XQueryFunction
    fun.setFunctionName(new StructuredQName(MAPPING_NS_PREFIX, MAPPING_NS_URI, function.name))
    fun.setResultType(function.retType)

    function.argTypes foreach { argType =>
      val param = new UserFunctionParameter
      param.setVariableQName(new StructuredQName(MAPPING_NS_PREFIX, MAPPING_NS_URI, "arg"))
      param.setRequiredType(argType)
      fun.addArgument(param)
    }

    xqlib.declareFunction(fun)
  }

  override def getArgumentTypes: Array[SequenceType] = {
    Array(SequenceType.makeSequenceType(ItemType.ANY_NODE, OccurrenceIndicator.ONE))
  }

  override def getName: QName = {
    new QName(MAPPING_NS_URI, LOCAL_NAME)
  }

  override def getResultType: SequenceType = {
    SequenceType.makeSequenceType(ItemType.ANY_NODE, OccurrenceIndicator.ONE)
  }

  override def call(arguments: Array[XdmValue]): XdmValue = {
    val remoteNode: XdmNode = arguments(0).asInstanceOf[XdmNode]
    val path = extractPathAttrib(remoteNode)
    val nsmap = COMMON_NS_MAP ++ extractNamespaceAttribs(remoteNode)
    val exp: Expression = parseXPath(path, getXPathContext(nsmap))

    ExpressionProcessor(exp, e => {
      if (e.isCallOnSystemFunction(DOC_FUNCTION_NAME) || e.isCallOnSystemFunction(DOC_AVAILABLE_FUNCTION_NAME)) {
        throw new XPathException(s"`$DOC_FUNCTION_NAME` and `$DOC_AVAILABLE_FUNCTION_NAME` are not allowed in a policy path")
      }
    })

    remoteNode
  }

  //
  // Extracts the value of the path attribute from a node.
  //
  private def extractPathAttrib(remoteNode: XdmNode): String = {
    remoteNode.axisIterator(Axis.ATTRIBUTE).asScala
      .map(_.asInstanceOf[XdmNode])
      .filter(_.getNodeName.getLocalName == "path")
      .map(_.getStringValue)
      .next()
  }

  //
  // Extracts the values of the prefix and uri attributes from every child node of a node.
  //
  private def extractNamespaceAttribs(remoteNode: XdmNode): Map[String, String] = {
    remoteNode.axisIterator(Axis.CHILD).asScala
      .map(_.asInstanceOf[XdmNode])
      .map(_.axisIterator(Axis.ATTRIBUTE).asScala)
      .map(attribs => {
        val attribNodes = attribs.map(_.asInstanceOf[XdmNode])
        val prefix = attribNodes.filter(_.getNodeName.getLocalName == "prefix").map(_.getStringValue).next()
        val uri = attribNodes.filter(_.getNodeName.getLocalName == "uri").map(_.getStringValue).next()
        prefix -> uri
      })
      .toMap
  }

  //
  // Constructs a context to be used when parsing or evaluating XPath expressions.
  //
  private def getXPathContext(namespaces: Map[String, String]): XPathStaticContext = {
    val ic = new IndependentContext

    ic.setXPathLanguageLevel(XQUERY_VERSION)

    namespaces.filterKeys(p => (p != "xmlns") && (p != "xml"))
      .foreach({ case (prefix, uri) => ic.declareNamespace(prefix, uri) })

    ic.getFunctionLibrary.asInstanceOf[FunctionLibraryList].addFunctionLibrary(xqlib)
    ic
  }

  //
  // Parses an XPath expression.
  //
  private def parseXPath(expression: String, context: StaticContext): Expression = {
    val xpathParser = new XPathParser
    xpathParser.setLanguage(XPathParser.XPATH, XQUERY_VERSION)
    xpathParser.parse(expression, 0, 0, context)
  }
}

object ValidateXPathFunction {

  private final val MAPPING_NS_PREFIX = "mapping"
  private final val MAPPING_NS_URI = "http://docs.rackspace.com/identity/api/ext/MappingRules"
  private final val LOCAL_NAME = "validate-xpath"
  private final val DOC_FUNCTION_NAME = "doc"
  private final val DOC_AVAILABLE_FUNCTION_NAME = "doc-available"
  private final val COMMON_NS_MAP = Map(
    "mapping" -> MAPPING_NS_URI,
    "saml2" -> "urn:oasis:names:tc:SAML:2.0:assertion",
    "saml2p" -> "urn:oasis:names:tc:SAML:2.0:protocol",
    "xs" -> "http://www.w3.org/2001/XMLSchema",
    "xsi" -> "http://www.w3.org/2001/XMLSchema-instance"
  )

  private final val FUNCTIONS = List(
    XQFunction("get-attribute", List(value.SequenceType.SINGLE_STRING), value.SequenceType.SINGLE_STRING),
    XQFunction("get-attributes", List(value.SequenceType.SINGLE_STRING), value.SequenceType.STRING_SEQUENCE)
  )

  private case class XQFunction(name: String, argTypes: List[value.SequenceType], retType: value.SequenceType)

}
