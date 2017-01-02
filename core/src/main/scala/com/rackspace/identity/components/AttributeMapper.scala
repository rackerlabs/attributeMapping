/***
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

import java.net.URI
import java.io.File

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.sax.SAXResult
import javax.xml.transform.dom.DOMSource
import javax.xml.validation.SchemaFactory

import com.rackspace.cloud.api.wadl.util.LogErrorListener
import com.rackspace.cloud.api.wadl.util.XSLErrorDispatcher

import net.sf.saxon.serialize.MessageWarner
import net.sf.saxon.Configuration.LicenseFeature._

import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.Destination
import net.sf.saxon.s9api.XdmDestination
import net.sf.saxon.s9api.SAXDestination
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XsltTransformer
import net.sf.saxon.s9api.XsltExecutable
import net.sf.saxon.s9api.XQueryExecutable
import net.sf.saxon.s9api.XQueryEvaluator
import net.sf.saxon.s9api.XdmValue

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode

import org.w3c.dom.Document

object XSDEngine extends Enumeration {
  val Auto = Value("auto")
  val Saxon = Value("saxon")
  val Xerces = Value("xerces")
}

import XSDEngine._

object AttributeMapper {
  val processor = new Processor(true)
  val compiler = processor.newXsltCompiler
  private val xqueryCompiler = {
    val c = processor.newXQueryCompiler
    c.setLanguageVersion("3.1")
    c
  }

  private val mapperXsltExec = compiler.compile(new StreamSource(getClass.getResource("/xsl/mapping.xsl").toString))
  private lazy val mapper2JSONExec = xqueryCompiler.compile(getClass.getResourceAsStream("/xq/mapping2JSON.xq"))
  private lazy val mapper2XMLExec = xqueryCompiler.compile(getClass.getResourceAsStream("/xq/mapping2XML.xq"))

  private lazy val mappingXSDSource = new StreamSource(getClass.getResource("/xsd/mapping.xsd").toString)

  private val transformerFactory = new net.sf.saxon.TransformerFactoryImpl
  private def idTransform = {
    val idt = transformerFactory.newTransformer()
    idt.setErrorListener (new LogErrorListener)
    idt
  }


  //
  //  Xerces Schema for validation
  //
  private lazy val mappingSchema = {
    val schemaFactory = SchemaFactory.newInstance("http://www.w3.org/XML/XMLSchema/v1.1", "org.apache.xerces.jaxp.validation.XMLSchema11Factory",
                                                  this.getClass.getClassLoader)

    //
    //  Enable CTA full XPath2.0 checking in XSD 1.1
    //
    schemaFactory.setFeature ("http://apache.org/xml/features/validation/cta-full-xpath-checking", true)
    schemaFactory.newSchema(mappingXSDSource)
  }

  //
  //  Saxon Schema for validation
  //
  private lazy val mappingSchemaManager = {
    val sm = processor.getSchemaManager

    sm.setXsdVersion("1.1")
    sm.setErrorListener (new LogErrorListener)
    sm.load(mappingXSDSource)
    sm
  }

  //
  //  Given XSLTExec and an optional set of XSLT parameters, creates an XsltTransformer
  //
  def getXsltTransformer (xsltExec : XsltExecutable, params : Map[QName, XdmValue]=Map[QName, XdmValue]()) : XsltTransformer = {
    val t = xsltExec.load
    t.setErrorListener (new LogErrorListener)
    t.getUnderlyingController.setMessageEmitter(new MessageWarner)
    for ((param, value) <- params) {
      t.setParameter(param, value)
    }
    t
  }

  private def getXQueryEvaluator (xqueryExec : XQueryExecutable, params : Map[QName, XdmValue]=Map[QName, XdmValue]()) : XQueryEvaluator = {
    val e = xqueryExec.load
    e.setErrorListener (new LogErrorListener)
    for ((param, value) <- params) {
      e.setExternalVariable (param, value)
    }
    e
  }

  def validatePolicy (policy : Source, engineStr : String) : Source ={
    val docBuilder = processor.newDocumentBuilder
    val engine = XSDEngine.withName(engineStr)
    val saxonEdition = processor.getUnderlyingConfiguration.isLicensedFeature(SCHEMA_VALIDATION)
    val useSaxon : Boolean = ((engine == Auto && saxonEdition)  || engine == Saxon);
    val bch = docBuilder.newBuildingContentHandler

    if (useSaxon) {
      Console.err.println("Using Saxon for validation...")
      val svalidator = mappingSchemaManager.newSchemaValidator
      svalidator.setDestination(new SAXDestination(bch))
      svalidator.validate(policy)
    } else {
      Console.err.println("Using Xerces for validation...")
      val schemaHandler = mappingSchema.newValidatorHandler
      schemaHandler.setContentHandler(bch)
      idTransform.transform(policy, new SAXResult(schemaHandler))
    }
    bch.getDocumentNode.asSource
  }

  def generateXSL (policy : Source, xsl : Destination, isJSON : Boolean, validate : Boolean, xsdEngine : String) : Unit = {
    val policySourceConv1 = {
      if (isJSON) {
        val outPolicyXML = new XdmDestination
        // TODO: Fail nicely if we get something other than a stream source.
        policy2XML(policy.asInstanceOf[StreamSource], outPolicyXML)
        outPolicyXML.getXdmNode.asSource
      } else {
        policy
      }
    }

    val policySrc = {
      if (validate) {
        validatePolicy(policySourceConv1, xsdEngine)
      } else {
        policySourceConv1
      }
    }

    val mappingTrans = getXsltTransformer(mapperXsltExec)
    mappingTrans.setSource(policySrc)
    mappingTrans.setDestination(xsl)
    mappingTrans.transform
  }

  def generateXSL (policy : JsonNode, xsl : Destination, validate : Boolean, xsdEngine : String) : Unit = {
    val outPolicyXML = new XdmDestination
    policy2XML(policy, outPolicyXML)

    generateXSL(outPolicyXML.getXdmNode.asSource, xsl, false, validate, xsdEngine)
  }

  def generateXSLExec (policy : Source, isJSON : Boolean, validate : Boolean, xsdEngine : String) : XsltExecutable = {
    val outXSL = new XdmDestination

    generateXSL (policy, outXSL, isJSON, validate, xsdEngine)
    compiler.compile(outXSL.getXdmNode.asSource)
  }

  def generateXSLExec (policy : JsonNode, validate : Boolean, xsdEngine : String) : XsltExecutable = {
    val outXSL = new XdmDestination

    generateXSL (policy, outXSL, validate, xsdEngine)
    compiler.compile(outXSL.getXdmNode.asSource)
  }

  def generateXSLExec (policy : Document, validate : Boolean, xsdEngine : String) : XsltExecutable = {
    generateXSLExec (new DOMSource(policy), false, validate, xsdEngine)
  }

  def policy2JSON(policyXML : Source, policyJSON : Destination, validate : Boolean, xsdEngine : String) : Unit = {
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
    evaluator.run
  }

  def policy2XML(policyJSON : StreamSource, policyXML : Destination) : Unit = {
    val om = new ObjectMapper()
    val node = {
      if (policyJSON.getInputStream != null) om.readTree(policyJSON.getInputStream) else
        if (policyJSON.getReader != null) om.readTree(policyJSON.getReader) else
          om.readTree(new File(new URI(policyJSON.getSystemId)))
    }

    policy2XML(node, policyXML)
  }

  def policy2XML(node : JsonNode, policyXML : Destination) : Unit = {
    val om = new ObjectMapper()

    val evaluator = getXQueryEvaluator(mapper2XMLExec, Map[QName, XdmValue](new QName("__JSON__") -> new XdmAtomicValue(om.writeValueAsString(node))))
    evaluator.setDestination(policyXML)
    evaluator.run
  }

  def convertAssertion (policy : Source, assertion : Source, dest : Destination, outputSAML : Boolean, isJSON : Boolean,
                        validate : Boolean, xsdEngine : String) : Unit = {
    //
    // Generate the XSLTExec
    //
    val mapExec = generateXSLExec (policy, isJSON, validate, xsdEngine)

    //
    //  Run the generate XSL on the assertion
    //
    convertAssertion(mapExec, assertion, dest, outputSAML, isJSON)
  }

  def convertAssertion (policyExec : XsltExecutable, assertion : Source, dest : Destination, outputSAML : Boolean, toJSON : Boolean) : Unit = {
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
    val mapTrans = getXsltTransformer (policyExec, Map(new QName("outputSAML") -> new XdmAtomicValue(outputSAML)))
    mapTrans.setSource(assertion)
    mapTrans.setDestination(assertionDest)
    mapTrans.transform

    if (toJSON && !outputSAML) {
      policy2JSON(assertionDest.asInstanceOf[XdmDestination].getXdmNode.asSource, dest, false, "Xerces")
    }
  }
}
