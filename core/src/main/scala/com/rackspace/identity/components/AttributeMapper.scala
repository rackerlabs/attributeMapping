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
package com.rackspace.identity.compenents

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource

import com.rackspace.cloud.api.wadl.util.LogErrorListener
import com.rackspace.cloud.api.wadl.util.XSLErrorDispatcher

import net.sf.saxon.serialize.MessageWarner

import net.sf.saxon.s9api.QName
import net.sf.saxon.s9api.Processor
import net.sf.saxon.s9api.Destination
import net.sf.saxon.s9api.XdmDestination
import net.sf.saxon.s9api.XdmAtomicValue
import net.sf.saxon.s9api.XsltTransformer
import net.sf.saxon.s9api.XsltExecutable
import net.sf.saxon.s9api.XdmValue

object AttributeMapper {
  private val processor = new Processor(true)
  private val compiler = processor.newXsltCompiler

  private val mapperXsltExec = compiler.compile(new StreamSource(getClass.getResource("/xsl/mapping.xsl").toString))

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

  def generateXSL (policy : Source, xsl : Destination) : Unit = {
    val mappingTrans = getXsltTransformer(mapperXsltExec)
    mappingTrans.setSource(policy)
    mappingTrans.setDestination(xsl)
    mappingTrans.transform
  }

  def convertAssertion (policy : Source, assertion : Source, dest : Destination, outputSAML : Boolean) : Unit = {
    val outXSL = new XdmDestination
    
    //
    //  Genereate the XSLT.
    //
    generateXSL (policy, outXSL)
 
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
