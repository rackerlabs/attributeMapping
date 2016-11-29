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

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.sax.SAXResult
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


object XSDEngine extends Enumeration {
  val Auto = Value("auto")
  val Saxon = Value("saxon")
  val Xerces = Value("xerces")
}

import XSDEngine._

object AttributeMapper {
  val processor = new Processor(true)
  private val compiler = processor.newXsltCompiler
  private val xqueryCompiler = {
    val c = processor.newXQueryCompiler
    c.setLanguageVersion("3.1")
    c
  }

  private val mapperXsltExec = compiler.compile(new StreamSource(getClass.getResource("/xsl/mapping.xsl").toString))
  private lazy val mapper2JSONExec = xqueryCompiler.compile(getClass.getResourceAsStream("/xq/mapping2JSON.xq"))

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
  private def getXsltTransformer (xsltExec : XsltExecutable, params : Map[QName, XdmValue]=Map[QName, XdmValue]()) : XsltTransformer = {
    val t = xsltExec.load
    t.setErrorListener (new LogErrorListener)
    t.getUnderlyingController.setMessageEmitter(new MessageWarner)
    for ((param, value) <- params) {
      t.setParameter(param, value)
    }
    t
  }

  private def getXQueryEvaluator (xqueryExec : XQueryExecutable) : XQueryEvaluator = {
    val e = xqueryExec.load
    e.setErrorListener (new LogErrorListener)
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

  def generateXSL (policy : Source, xsl : Destination, validate : Boolean, xsdEngine : String) : Unit = {
    val policySrc = {
      if (validate) {
        validatePolicy(policy, xsdEngine)
      } else {
        policy
      }
    }

    val mappingTrans = getXsltTransformer(mapperXsltExec)
    mappingTrans.setSource(policySrc)
    mappingTrans.setDestination(xsl)
    mappingTrans.transform
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

  def convertAssertion (policy : Source, assertion : Source, dest : Destination, outputSAML : Boolean, validate : Boolean, xsdEngine : String) : Unit = {
    val outXSL = new XdmDestination

    //
    //  Genereate the XSLT.
    //
    generateXSL (policy, outXSL, validate, xsdEngine)

    //
    // Comple the resulting XSL
    //
    val mapExec = compiler.compile(outXSL.getXdmNode.asSource)

    //
    //  Run the generate XSL on the assertion
    //
    val mapTrans = getXsltTransformer (mapExec, Map(new QName("outputSAML") -> new XdmAtomicValue(outputSAML)))
    mapTrans.setSource(assertion)
    mapTrans.setDestination(dest)
    mapTrans.transform
  }
}
