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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, InputStream, OutputStream, Writer}
import java.net.URI
import javax.xml.parsers.DocumentBuilder
import javax.xml.transform.Source
import javax.xml.transform.ErrorListener
import javax.xml.transform.TransformerException
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException

import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.github.fge.jsonschema.report.{ListReportProvider, LogLevel}
import com.github.fge.jsonschema.exceptions.ProcessingException

import com.rackspace.cloud.api.wadl.util.LogErrorListener
import com.rackspace.com.papi.components.checker.util.XMLParserPool
import net.sf.saxon.Configuration.LicenseFeature._
import net.sf.saxon.s9api._
import net.sf.saxon.serialize.MessageWarner
import net.sf.saxon.lib.FeatureKeys
import org.w3c.dom.Document

import com.typesafe.scalalogging.slf4j.LazyLogging

import org.xml.sax.ErrorHandler
import org.xml.sax.SAXParseException

import scala.util.Try
import scala.collection.JavaConversions._

import collection.JavaConverters._

object XSDEngine extends Enumeration {
  val AUTO = Value("auto")
  val SAXON = Value("saxon")
  val XERCES = Value("xerces")
}

object PolicyFormat extends Enumeration {
  val XML = Value("xml")
  val JSON = Value("json")
  val YAML = Value("yaml")

  def fromPath(policyPath: String): PolicyFormat.Value = {
    Try {
      val policyExtension = policyPath.substring(policyPath.lastIndexOf('.') + 1).toLowerCase
      withName(policyExtension)
    }.getOrElse(YAML)
  }
}

//
//  Exceptions
//


//
// Generic Attribute Mapper Exception, should anything else go wrong.
//
class AttributeMapperException(message: String, cause : Throwable) extends Exception(message, cause) {
  def this(message : String) {
    this(message, null)
  }
}


//
//  An attribute mapper execption where the cause of the error is
//  known. (ie it's not a catch for an unknown Exception).  It should
//  always be safe to reveal to the end user the message of a
//  known exception.
//
abstract class KnownAttributeMapperException(message : String, cause : Throwable) extends AttributeMapperException(message, cause) {
  def this(message : String) {
    this(message, null)
  }
}

//
//  Thrown if the policy format is not supported.
//
class UnsupportedPolicyFormatException(message: String) extends KnownAttributeMapperException(message)

//
//  Thrown if the policy is malformed in some way: bad XML, JSON,
//  YAML.  The cause will likely come from the XML, JSON, YAML parser.
//
class ParseException(message : String, cause : Throwable) extends KnownAttributeMapperException(message, cause)

//
//  Thrown if there's a validation error the cause will likely be the
//  exception from the XSD impl.
//
class ValidationException(message : String, cause : Throwable) extends KnownAttributeMapperException(message, cause)

//
//  The policy itself is valid according to schema, but there's an
//  issue with an XPath in the policy.
//
class XPathException(message : String) extends ValidationException(message, null)

//
//  A Generic Validation Error Handler : Allows us to sort out the
//  differences of handling validation errors between Xerces and Saxon
//
private class GenericValidationErrorHandler[T <: Exception] extends LazyLogging {
  def fatalError(te : T) : Unit = {
    throw new ValidationException (te.getMessage, te)
  }

  def error(te : T) : Unit = {
    //
    // All errors are fatal!
    //
    fatalError(te)
  }

  def warning(te : T) : Unit = {
    logger.warn (te.getMessage, te)
  }
}

//
//  Saxon error listener...
//
private object ValidationErrorListener extends GenericValidationErrorHandler[TransformerException] with ErrorListener {}

//
//  Xerces error handler...
//
private object ValidationErrorHandler extends GenericValidationErrorHandler[SAXParseException] with ErrorHandler {}

import com.rackspace.identity.components.XSDEngine._
import com.rackspace.identity.components.PolicyFormat._

object AttributeMapper {

  //
  // The version of XQuery (and, by extension, XPath) to use.
  //
  final val XQUERY_VERSION = 31
  final val XQUERY_VERSION_STRING = "3.1"

  //
  // The namespace components for the "mapping" namespace.
  //
  final val MAPPING_NS_PREFIX = "mapping"
  final val MAPPING_NS_URI = "http://docs.rackspace.com/identity/api/ext/MappingRules"

  //
  //  THe namespaces components for the "map" namespace.
  //
  final val MAP_NS_PREFIX = "map"
  final val MAP_NS_URI = "http://www.w3.org/2005/xpath-functions/map"

  def newProcessor : Processor = {
    val p = new Processor(true)
    val dynLoader = p.getUnderlyingConfiguration.getDynamicLoader
    dynLoader.setClassLoader(getClass.getClassLoader)
    p
  }

  val processor = {
    val p = newProcessor

    //
    // Suppress the XSLT warning.  We have stylesheets that extract
    // things in namespaces outside of the namespace of the root
    // document (in SAML).
    //
    p.setConfigurationProperty(FeatureKeys.SUPPRESS_XSLT_NAMESPACE_CHECK, true)

    //
    //  Register the XPath parser as an extension function.
    //
    p.registerExtensionFunction(new XPath31Parse.SaxonDefinition_XPath())

    p
  }

  val compiler = processor.newXsltCompiler
  val xqueryCompiler = {
    val c = processor.newXQueryCompiler
    c
  }
  val xpathCompiler = {
    val c = processor.newXPathCompiler()
    c.setLanguageVersion(XQUERY_VERSION_STRING)
    c.declareNamespace(MAPPING_NS_PREFIX, MAPPING_NS_URI)
    c.declareNamespace(MAP_NS_PREFIX, MAP_NS_URI)
    c
  }

  private val mapperXsltExec = compiler.compile(new StreamSource(getClass.getResource("/xsl/mapping.xsl").toString))
  private val xpathValXsltExec = compiler.compile(new StreamSource(getClass.getResource("/xsl/xpath-31-validate.xsl").toString))
  private val getErrorsXPathExec = xpathCompiler.compile(
    """
       (:
           Taking a very simple approach for now. We are returning the first
           error only as a map with the error attributes.
        :)
       let $firstError := /mapping:errors/mapping:error[1]
           return if ($firstError) then map:merge(
             for $attr in $firstError/@* return
                map:entry(local-name($attr), string($attr))
           ) else ()
    """)
  private lazy val mapper2JSONExec = xqueryCompiler.compile(getClass.getResourceAsStream("/xq/mapping2JSON.xq"))
  private lazy val mapper2XMLExec = xqueryCompiler.compile(getClass.getResourceAsStream("/xq/mapping2XML.xq"))
  private lazy val mappingXSDSource = new StreamSource(getClass.getResource("/xsd/mapping.xsd").toString)

  private lazy val extractExtExec = compiler.compile(new StreamSource(getClass.getResource("/xsl/extract-ext.xsl").toString))
  private lazy val joinExtExec = compiler.compile(new StreamSource(getClass.getResource("/xsl/join-ext.xsl").toString))
  private lazy val ext2JSONExec = xqueryCompiler.compile(getClass.getResourceAsStream("/xq/ext2JSON.xq"))

  private lazy val extAttribsXSDSource = new StreamSource(getClass.getResource("/xsd/extAttribs.xsd").toString)

  private val transformerFactory = new net.sf.saxon.TransformerFactoryImpl
  private def idTransform = {
    val idt = transformerFactory.newTransformer()
    idt.setErrorListener (new LogErrorListener)
    idt
  }

  private lazy val schemaFactory = {
    val sf = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1", "org.apache.xerces.jaxp.validation.XMLSchema11Factory",
                              this.getClass.getClassLoader)
    //
    //  Enable CTA full XPath2.0 checking in XSD 1.1
    //
    sf.setFeature ("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
    sf
  }

  private def getSchemaManager(xsdSource : Source) : SchemaManager = {
    val sm = processor.getSchemaManager

    sm.setXsdVersion("1.1")
    sm.setErrorListener (new LogErrorListener)
    sm.load(xsdSource)
    sm
  }

  //
  //  Xerces Schemas for validation
  //
  private lazy val mappingSchema = schemaFactory.newSchema(mappingXSDSource)
  private lazy val extAttribsSchema = schemaFactory.newSchema(extAttribsXSDSource)

  //
  //  Saxon Schemas for validation
  //
  private lazy val mappingSchemaManager = getSchemaManager(mappingXSDSource)
  private lazy val extAttribsSchemaManager = getSchemaManager(extAttribsXSDSource)

  //
  //  Given XSLTExec and an optional set of XSLT parameters, creates an XsltTransformer
  //
  def getXsltTransformer (xsltExec : XsltExecutable, params : Map[QName, XdmValue]=Map[QName, XdmValue]()) : XsltTransformer = {
    val t = xsltExec.load
    t.setErrorListener (new LogErrorListener)
    t.getUnderlyingController.setMessageEmitter(new MessageWarner)
    for {(param, value) <- params} {
      t.setParameter(param, value)
    }
    t
  }

  def getXQueryEvaluator (xqueryExec : XQueryExecutable, params : Map[QName, XdmValue]=Map[QName, XdmValue]()) : XQueryEvaluator = {
    val e = xqueryExec.load
    e.setErrorListener (new LogErrorListener)
    for {(param, value) <- params} {
      e.setExternalVariable (param, value)
    }
    e
  }

  def useSaxon(engineStr : String) : Boolean = {
    val engine = XSDEngine.withName(engineStr)
    ((engine == AUTO &&
      processor.getUnderlyingConfiguration.isLicensedFeature(SCHEMA_VALIDATION))
     || engine == SAXON)
  }


  private lazy val jsonSchemaFactory = JsonSchemaFactory.newBuilder.setReportProvider(new ListReportProvider(LogLevel.WARNING, LogLevel.ERROR)).freeze
  private lazy val mappingJSONSchema = {
    val om = new ObjectMapper
    val grammar = om.readValue(getClass.getResource("/jsonSchema/mapping.json"), classOf[JsonNode])
    jsonSchemaFactory.getJsonSchema(grammar)
  }

  //
  //  All of the logic for wrapping exceptions into Attribute Mapping
  //  Exceptions here.
  //
  private def handleExceptions[T](tryFun :  => T) : T = {
    try {
      tryFun
    } catch {
      //
      //  Pass attribute mapper exceptions through...
      //
      case ae : AttributeMapperException => throw ae

      //
      //  Parse Exceptions
      //
      case sae : SaxonApiException if (sae.getMessage.contains("ParseException")) =>
        throw new ParseException(sae.getMessage, sae)
      case spe : SAXParseException =>
        throw new ParseException(spe.getMessage, spe)
      case mye : MarkedYAMLException =>
        throw new ParseException(mye.getMessage, mye)
      case jpe : JsonParseException =>
        throw new ParseException(jpe.getMessage, jpe)

      //
      // Validation Exceptions
      //
      case pe : ProcessingException =>
        throw new ValidationException (processingException2Message(pe), pe)

      //
      //  No idea, man.
      //
      case t : Throwable => throw new AttributeMapperException("Error while processing mapping policy", t)
    }
  }

  private def getJsonSchemaMessagesFromReports (reports : JsonNode, current : Option[String]) : List[String] = {
    val allReports : Iterator[JsonNode] = reports.elements.flatMap(a => a.elements.asInstanceOf[java.util.Iterator[JsonNode]])
    val allReportMessages : List[String] = allReports.flatMap(r => getJsonSchemaMessages(r)).toList

    current match {
      case Some(msg) => msg :: allReportMessages
      case None => allReportMessages
    }
  }

  private def getJsonSchemaMessages (report : JsonNode) : List[String] = {
    val currentMessage : Option[JsonNode] =  Some(report.findValue("message"))
    val reports : Option[JsonNode] = Some(report.findValue("reports"))
    val pointer : Option[JsonNode] = Some(report.findValue("instance")).map(_.findValue("pointer"))

    val pointerString : String = pointer match {
      case Some(pointerNode : JsonNode) => " in " + pointerNode.asText
      case _ => ""
    }

    val withCurrentMessage : Option[String] = currentMessage match {
      case Some(msgNode : JsonNode) => Some(msgNode.asText + pointerString)
      case _ => None
    }

    reports match {
      case Some(reportNode : JsonNode) => getJsonSchemaMessagesFromReports (reportNode, withCurrentMessage)
      case _ => withCurrentMessage match {
        case Some(msg) => List(msg)
        case None => Nil
      }
    }
  }

  private def processingException2Message (pe : ProcessingException) : String = {
    val errorAsJSON = pe.getProcessingMessage.asJson
    Some(errorAsJSON.findValue("message")) match {
      case Some(msgNode : JsonNode) if (msgNode.isTextual) => getJsonSchemaMessages(errorAsJSON).mkString("\n")
      case _ => pe.getProcessingMessage.toString
    }
  }

  def validate(src : Source, engineStr : String, schema : => Schema, schemaManager : => SchemaManager) : Source = handleExceptions[Source] {
    val docBuilder = processor.newDocumentBuilder
    val bch = docBuilder.newBuildingContentHandler

    if (useSaxon(engineStr)) {
      Console.err.println("Saxon validation") // scalastyle:ignore
      val svalidator = schemaManager.newSchemaValidator
      svalidator.setErrorListener (ValidationErrorListener)
      svalidator.setDestination(new SAXDestination(bch))
      svalidator.validate(src)
    } else {
      Console.err.println("Xerces validation") // scalastyle:ignore
      val schemaHandler = schema.newValidatorHandler
      schemaHandler.setContentHandler(bch)
      schemaHandler.setErrorHandler(ValidationErrorHandler)
      //
      //  Xerces does not honor the ValidationErrorHandler when
      //  throwing errors due to assertions failing. If we get an
      //  exception from Saxon and the cause is a SAXParseException we
      //  assume it's coming from Xerces and we pass it on the
      //  handler ourselves.
      //
      try {
        idTransform.transform(src, new SAXResult(schemaHandler))
      } catch {
        case xpe : net.sf.saxon.trans.XPathException if Option(xpe.getCause).exists(_.isInstanceOf[SAXParseException]) =>
          ValidationErrorHandler.fatalError(xpe.getCause.asInstanceOf[SAXParseException])
      }
    }
    bch.getDocumentNode.asSource
  }

  def validatePolicy (policy : Source, engineStr : String) : Source = handleExceptions[Source] {
    val docBuilder = processor.newDocumentBuilder
    val xdmPolicy = docBuilder.build(policy)
    val outXPathErrors = new XdmDestination

    //
    //  Validate policy against schema first.
    //
    val goodPolicy = validate(xdmPolicy.asSource, engineStr, mappingSchema, mappingSchemaManager)

    //
    //  Then check XPath rules.
    //
    val validateXPathTrans = getXsltTransformer(xpathValXsltExec)
    validateXPathTrans.setSource(goodPolicy)
    validateXPathTrans.setDestination(outXPathErrors)
    validateXPathTrans.transform()

    val errorSelector = getErrorsXPathExec.load
    errorSelector.setContextItem(outXPathErrors.getXdmNode)
    val error = errorSelector.evaluateSingle

    //
    //  If we have an error throw an XPathException!
    //
    if (error != null) {
      val errorMap = error.asMap
      val msg      = errorMap.get(new XdmAtomicValue("msg"))
      val inXPath  = errorMap.get(new XdmAtomicValue("inXPath"))

      throw new XPathException(s"$msg in XPath: $inXPath")
    }

    goodPolicy
  }

  def validatePolicy (policy : JsonNode, engineStr : String) : JsonNode = handleExceptions[JsonNode] {
    //
    //  Validate then return JSON node.
    //
    val bout = new ByteArrayOutputStream
    val dest = processor.newSerializer(bout)
    policy2JSON(validatePolicyToXML(policy, engineStr), dest, false, engineStr)

    val bin = new ByteArrayInputStream(bout.toByteArray)
    parseJsonNode(new StreamSource(bin))
  }

  def validatePolicyToXML (policy : JsonNode, engineStr : String) : Source = handleExceptions[Source] {
    //
    //  Validate as JSON
    //
    mappingJSONSchema.validate(policy)

    //
    //  Convert to XML and Validate as XML. This produces new XML with
    //  defaults filled in.
    //
    val outXMLPolicy = new XdmDestination
    policy2XML(policy, outXMLPolicy)
    validatePolicy(outXMLPolicy.getXdmNode.asSource, engineStr)
  }

  def generateXSL (policy : Source, policyFormat : PolicyFormat.Value, xsl : Destination,
    validate : Boolean, xsdEngine : String) : Unit = handleExceptions[Unit] {
    val policySrc = policyFormat match {
      case PolicyFormat.YAML =>
        if (validate) {
          validatePolicyToXML(parseYamlNode(policy.asInstanceOf[StreamSource]), xsdEngine)
        } else {
          val outPolicyXML = new XdmDestination
          policy2XML(parseYamlNode(policy.asInstanceOf[StreamSource]), outPolicyXML)
          outPolicyXML.getXdmNode.asSource
        }
      case PolicyFormat.JSON =>
        if (validate) {
          validatePolicyToXML(parseJsonNode(policy.asInstanceOf[StreamSource]), xsdEngine)
        } else {
          val outPolicyXML = new XdmDestination
          policy2XML(policy.asInstanceOf[StreamSource], outPolicyXML)
          outPolicyXML.getXdmNode.asSource
        }
      case PolicyFormat.XML =>
        if (validate) {
          validatePolicy(policy, xsdEngine)
        } else {
          policy
        }
      case _ =>
        throw new UnsupportedPolicyFormatException(s"The Policy Format $policyFormat is not supported at this time.")
    }

    val mappingTrans = getXsltTransformer(mapperXsltExec)
    mappingTrans.setSource(policySrc)
    mappingTrans.setDestination(xsl)
    mappingTrans.transform()
  }

  def generateXSL (policy : JsonNode, xsl : Destination, validate : Boolean,
    xsdEngine : String) : Unit = handleExceptions[Unit] {

    val policySrc = {
      if (validate) {
        validatePolicyToXML(policy, xsdEngine)
      } else {
        val outPolicyXML = new XdmDestination
        policy2XML(policy, outPolicyXML)
        outPolicyXML.getXdmNode.asSource
      }
    }

    generateXSL(policySrc, PolicyFormat.XML, xsl, false, xsdEngine)
  }

  def generateXSLExec (policy : Source, policyFormat : PolicyFormat.Value,
    validate : Boolean, xsdEngine : String) : XsltExecutable = handleExceptions[XsltExecutable] {
    val outXSL = new XdmDestination

    generateXSL (policy, policyFormat, outXSL, validate, xsdEngine)
    compiler.compile(outXSL.getXdmNode.asSource)
  }

  def generateXSLExec (policy : JsonNode, validate : Boolean,
    xsdEngine : String) : XsltExecutable = handleExceptions[XsltExecutable] {
    val outXSL = new XdmDestination

    generateXSL (policy, outXSL, validate, xsdEngine)
    compiler.compile(outXSL.getXdmNode.asSource)
  }

  def generateXSLExec (policy : Document, validate : Boolean,
    xsdEngine : String) : XsltExecutable = handleExceptions[XsltExecutable] {
    generateXSLExec (new DOMSource(policy), PolicyFormat.XML, validate, xsdEngine)
  }

  def parseJsonNode (source : StreamSource) : JsonNode = handleExceptions[JsonNode] {
    val om = new ObjectMapper()

    if (source.getInputStream != null) {
      om.readTree(source.getInputStream)
    } else if (source.getReader != null) {
      om.readTree(source.getReader)
    } else {
      om.readTree(new File(new URI(source.getSystemId)))
    }
  }

  def parseYamlNode (source : StreamSource) : JsonNode = handleExceptions[JsonNode] {
    val om = new ObjectMapper(new YAMLFactory())

    if (source.getInputStream != null) {
      om.readTree(source.getInputStream)
    } else if (source.getReader != null) {
      om.readTree(source.getReader)
    } else {
      om.readTree(new File(new URI(source.getSystemId)))
    }
  }

  def policy2JSON(policyXML : Source, policyJSON : Destination, validate : Boolean,
    xsdEngine : String) : Unit = handleExceptions[Unit] {
    val policySrc = {
      if (validate) {
        validatePolicy(policyXML, xsdEngine)
      } else {
        policyXML
      }
    }

    val evaluator = getXQueryEvaluator(mapper2JSONExec)
    evaluator.setSource(policySrc)
    evaluator.setDestination(policyJSON)
    evaluator.run()
  }

  def policy2XML(policyJSON : StreamSource, policyXML : Destination) : Unit = handleExceptions[Unit] {
    policy2XML(parseJsonNode(policyJSON), policyXML)
  }

  def policy2XML(node : JsonNode, policyXML : Destination) : Unit = handleExceptions[Unit] {
    val om = new ObjectMapper()

    val evaluator = getXQueryEvaluator(mapper2XMLExec, Map[QName, XdmValue](new QName("__JSON__") -> new XdmAtomicValue(om.writeValueAsString(node))))
    evaluator.setDestination(policyXML)
    evaluator.run()
  }

  def policy2YAML(policyJSON : StreamSource, policyYAML : OutputStream, validate : Boolean, xsdEngine : String) : Unit = handleExceptions[Unit] {
    val yamlMapper = new ObjectMapper(new YAMLFactory())
    val policyJson = parseJsonNode(policyJSON)

    val validatedPolicy = {
      if (validate) {
        validatePolicy(policyJson, xsdEngine)
      } else {
        policyJson
      }
    }

    policyYAML.write(yamlMapper.writeValueAsBytes(validatedPolicy))
  }

  def convertAssertion (policy : Source, policyFormat : PolicyFormat.Value, assertion : Source, dest : Destination,
                        outputSAML : Boolean, validate : Boolean, xsdEngine : String,
                         params : Map[String,String]=Map[String,String]()) : Unit = handleExceptions[Unit] {
    //
    // Generate the XSLTExec
    //
    val mapExec = generateXSLExec (policy, policyFormat, validate, xsdEngine)

    //
    //  Run the generate XSL on the assertion
    //
    convertAssertion(mapExec, assertion, dest, outputSAML, !XML.equals(policyFormat), params)
  }

  def convertAssertion (policyExec : XsltExecutable, assertion : Source, dest : Destination, outputSAML : Boolean, toJSON : Boolean,
                        params : Map[String,String]) : Unit = handleExceptions[Unit] {
    val assertionDest = {
      if (toJSON && !outputSAML) {
        new XdmDestination
      } else {
        dest
      }
    }

    //
    //  Run the generate XSL on the assertion
    //
    val mapTrans = getXsltTransformer (policyExec, Map(new QName("outputSAML") -> new XdmAtomicValue(outputSAML),
                                                       new QName("params") -> XdmMap.makeMap(params.asJava)))
    mapTrans.setSource(assertion)
    mapTrans.setDestination(assertionDest)
    mapTrans.transform()

    if (toJSON && !outputSAML) {
      policy2JSON(assertionDest.asInstanceOf[XdmDestination].getXdmNode.asSource, dest, false, "Xerces")
    }
  }

  def convertAssertion (policyExec : XsltExecutable, assertion : Source,
    params : Map[String,String]) : Document = handleExceptions[Document] {
    var docBuilder : DocumentBuilder = null
    var outDoc : Document = null
    try {
      docBuilder = XMLParserPool.borrowParser
      outDoc = docBuilder.newDocument
    } finally {
      if (docBuilder != null) XMLParserPool.returnParser(docBuilder)
    }

    val dest = new DOMDestination(outDoc)
    convertAssertion (policyExec, assertion, dest, true, false, params)
    outDoc
  }

  def convertAssertion (policyExec : XsltExecutable, assertion : Document,
    params : Map[String,String]) : Document = handleExceptions[Document] {
    convertAssertion (policyExec, new DOMSource(assertion), params)
  }

  def validateExtAttributes (extAttribs : Source, engineStr : String) : Source = handleExceptions[Source] {
    validate (extAttribs, engineStr, extAttribsSchema, extAttribsSchemaManager)
  }

  def extractExtendedAttributes (assertion : Source, extendedAttribs : Destination, asJSON : Boolean,
    validate : Boolean, xsdEngine : String) : Unit = handleExceptions[Unit] {
    val xdmDest = new XdmDestination

    val dest : Destination = {
      if (asJSON) {
        xdmDest
      } else if (validate) {
        new TeeDestination (extendedAttribs, xdmDest)
      } else {
        extendedAttribs
      }
    }

    val extractTrans = getXsltTransformer(extractExtExec)
    extractTrans.setSource(assertion)
    extractTrans.setDestination(dest)
    extractTrans.transform()

    if (validate) {
      validateExtAttributes (xdmDest.getXdmNode.asSource,  xsdEngine)
    }

    if (asJSON) {
      val evaluator = getXQueryEvaluator(ext2JSONExec)
      evaluator.setSource(xdmDest.getXdmNode.asSource)
      evaluator.setDestination(extendedAttribs)
      evaluator.run()
    }
  }

  def extractExtendedAttributes (assertion : Source, validate : Boolean, xsdEngine : String) : JsonNode = handleExceptions[JsonNode] {
    val om = new ObjectMapper
    val bout = new ByteArrayOutputStream
    val dest = processor.newSerializer (bout)

    extractExtendedAttributes(assertion, dest, true, validate, xsdEngine)
    om.readTree (bout.toByteArray)
  }

  def addExtendedAttributes (authResp : Source, assertion : Source, newAuthResp : Destination,
                             isJSON : Boolean, validate : Boolean, xsdEngine : String) : Unit = handleExceptions[Unit] {
    if (!isJSON) {
      addExtendedAttributes (authResp, assertion, newAuthResp, validate, xsdEngine)
    } else {
      val ow = (new ObjectMapper).writer(SerializationFeature.INDENT_OUTPUT)
      val jsonOut = addExtendedAttributes (authResp.asInstanceOf[StreamSource], assertion, validate, xsdEngine)
      newAuthResp.asInstanceOf[Serializer].getOutputDestination match {
        case f : File => ow.writeValue (f, jsonOut)
        case o : OutputStream => ow.writeValue (o, jsonOut)
        case w : Writer => ow.writeValue (w, jsonOut)
      }
    }
  }

  def addExtendedAttributes (authResp : Source, assertion : Source, newAuthResp : Destination,
                             validate : Boolean, xsdEngine : String) : Unit = handleExceptions[Unit] {
    val extDest = new XdmDestination
    extractExtendedAttributes (assertion, extDest, false, validate, xsdEngine)

    val joinExtTrans = getXsltTransformer(joinExtExec, Map[QName, XdmValue](new QName("extAttributes")->extDest.getXdmNode))
    joinExtTrans.setSource(authResp)
    joinExtTrans.setDestination(newAuthResp)
    joinExtTrans.transform()
  }

  def addExtendedAttributes (authResp : Source, assertion : Document,
    validate : Boolean, xsdEngine : String) : Document = handleExceptions[Document] {
    var docBuilder : DocumentBuilder = null
    var outDoc : Document = null
    try {
      docBuilder = XMLParserPool.borrowParser
      outDoc = docBuilder.newDocument
    } finally {
      if (docBuilder != null) XMLParserPool.returnParser(docBuilder)
    }

    val dest = new DOMDestination(outDoc)
    addExtendedAttributes (authResp, new DOMSource(assertion), dest,
                           validate, xsdEngine)
    outDoc
  }

  def addExtendedAttributes (authResp : Document, assertion : Document,
    validate : Boolean, xsdEngine : String) : Document = handleExceptions[Document] {
    addExtendedAttributes (new DOMSource(authResp), assertion, validate, xsdEngine)
  }

  def addExtendedAttributes (authResp : JsonNode, assertion : Source,
    validate : Boolean, xsdEngine : String) : JsonNode = handleExceptions[JsonNode] {
    val extAttribs = extractExtendedAttributes (assertion, validate, xsdEngine)
    val retResp : ObjectNode  = authResp.deepCopy[ObjectNode]
    if (extAttribs.get("RAX-AUTH:extendedAttributes").size != 0) {
      val accessObj = retResp.get("access").asInstanceOf[ObjectNode]
      accessObj.set("RAX-AUTH:extendedAttributes",extAttribs.get("RAX-AUTH:extendedAttributes"))
    }
    retResp
  }

  def addExtendedAttributes (authResp : StreamSource, assertion : Source,
    validate : Boolean, xsdEngine : String) : JsonNode = handleExceptions[JsonNode] {
    addExtendedAttributes (parseJsonNode(authResp), assertion, validate, xsdEngine)
  }

  def addExtendedAttributes (authResp : JsonNode, assertion : Document,
    validate : Boolean, xsdEngine : String) : JsonNode = handleExceptions[JsonNode] {
    addExtendedAttributes (authResp, new DOMSource(assertion), validate, xsdEngine)
  }
}
