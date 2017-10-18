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

import com.rackspace.identity.components.AttributeMapper.{MAPPING_NS_PREFIX, MAPPING_NS_URI, XQUERY_VERSION}
import com.saxonica.expr.HofParserExtension
import com.saxonica.functions.hof.UserFunctionReference
import net.sf.saxon.expr.instruct.UserFunctionParameter
import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.expr.{Expression, FunctionCall, StaticContext}
import net.sf.saxon.functions.FunctionLibraryList
import net.sf.saxon.lib.NamespaceConstant._
import net.sf.saxon.om.StructuredQName
import net.sf.saxon.query.{XQueryFunction, XQueryFunctionLibrary}
import net.sf.saxon.s9api._
import net.sf.saxon.sxpath.{IndependentContext, XPathStaticContext}
import net.sf.saxon.trans.XPathException
import net.sf.saxon.tree.NamespaceNode
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
    Array(
      SequenceType.makeSequenceType(ItemType.ANY_NODE, OccurrenceIndicator.ONE),
      SequenceType.makeSequenceType(ItemType.STRING, OccurrenceIndicator.ONE)
    )
  }

  override def getName: QName = {
    new QName(MAPPING_NS_URI, LOCAL_NAME)
  }

  override def getResultType: SequenceType = {
    SequenceType.makeSequenceType(ItemType.BOOLEAN, OccurrenceIndicator.ONE)
  }

  override def call(arguments: Array[XdmValue]): XdmValue = {
    val rootNode = arguments(0).asInstanceOf[XdmNode]
    val path = arguments(1).asInstanceOf[XdmAtomicValue].getStringValue
    val nsmap = COMMON_NS_MAP ++ extractNamespaces(rootNode)
    val expr = parseXPath(path, getXPathContext(nsmap))

    ExpressionProcessor(expr, {
      case functionCall: FunctionCall =>
        val functionName = functionCall.getFunctionName
        if (!EXCEPTION_FUNCTION_MAP.exists(_ == functionName.getURI -> functionName.getLocalPart) &&
          !LEGAL_FUNCTION_NAMESPACE_SET.contains(functionName.getURI) ||
          ILLEGAL_SYSTEM_FUNCTION_SET.exists(functionCall.isCallOnSystemFunction)) {
          throw new XPathException(s"${functionName.getLocalPart} function in the ${functionName.getURI} namespace is not allowed in a policy path")
        }
      case _: UserFunctionReference =>
        throw new XPathException("Inline functions are not allowed in a policy path")
      case _ => // Intentional no-op
    })

    new XdmAtomicValue(true)
  }

  //
  // Extracts all of the namespaces defined in the provided node.
  //
  private def extractNamespaces(node: XdmNode): Map[String, String] = {
    node.axisIterator(Axis.NAMESPACE).asScala
      .map(_.getUnderlyingValue)
      .map(_.asInstanceOf[NamespaceNode])
      .map(ns => ns.getDisplayName -> ns.getStringValue)
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
    xpathParser.setParserExtension(new HofParserExtension)
    xpathParser.setLanguage(XPathParser.XPATH, XQUERY_VERSION)
    xpathParser.parse(expression, 0, 0, context)
  }
}

object ValidateXPathFunction {

  private final val LOCAL_NAME = "validate-xpath"
  private final val AVAILABLE_ENVIRONMENT_VARIABLES_FUNCTION_NAME = "available-environment-variables"
  private final val APPLY_FUNCTION_NAME = "apply"
  private final val COLLECTION_FUNCTION_NAME = "collection"
  private final val DOC_FUNCTION_NAME = "doc"
  private final val DOC_AVAILABLE_FUNCTION_NAME = "doc-available"
  private final val DOCUMENT_FUNCTION_NAME = "document"
  private final val ENVIRONMENT_VARIABLE_FUNCTION_NAME = "environment-variable"
  private final val FUNCTION_LOOKUP_FUNCTION_NAME = "function-lookup"
  private final val LOAD_XQUERY_MODULE_FUNCTION_NAME = "load-xquery-module"
  private final val TRANSFORM_FUNCTION_NAME = "transform"
  private final val UNPARSED_TEXT_AVAILABLE_FUNCTION_NAME = "unparsed-text-available"
  private final val UNPARSED_TEXT_FUNCTION_NAME = "unparsed-text"
  private final val UNPARSED_TEXT_LINES_FUNCTION_NAME = "unparsed-text-lines"
  private final val URI_COLLECTION_FUNCTION_NAME = "uri-collection"
  private final val ILLEGAL_SYSTEM_FUNCTION_SET = Set(
    AVAILABLE_ENVIRONMENT_VARIABLES_FUNCTION_NAME,
    COLLECTION_FUNCTION_NAME,
    DOC_FUNCTION_NAME,
    DOC_AVAILABLE_FUNCTION_NAME,
    DOCUMENT_FUNCTION_NAME,
    ENVIRONMENT_VARIABLE_FUNCTION_NAME,
    FUNCTION_LOOKUP_FUNCTION_NAME,
    LOAD_XQUERY_MODULE_FUNCTION_NAME,
    TRANSFORM_FUNCTION_NAME,
    UNPARSED_TEXT_AVAILABLE_FUNCTION_NAME,
    UNPARSED_TEXT_FUNCTION_NAME,
    UNPARSED_TEXT_LINES_FUNCTION_NAME,
    URI_COLLECTION_FUNCTION_NAME
  )
  private final val LEGAL_FUNCTION_NAMESPACE_SET = Set(
    FN,
    MATH,
    MAP_FUNCTIONS,
    ARRAY_FUNCTIONS,
    MAPPING_NS_URI
  )
  private final val EXCEPTION_FUNCTION_MAP = Map(
    // Enables dynamic function application
    SAXON -> APPLY_FUNCTION_NAME
  )
  private final val COMMON_NS_MAP = Map(
    MAPPING_NS_PREFIX -> MAPPING_NS_URI,
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
