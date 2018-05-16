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

import javax.xml.transform.Source
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamSource

import com.fasterxml.jackson.databind.ObjectMapper
import com.rackspace.com.papi.components.checker.util.XMLParserPool._
import net.sf.saxon.s9api._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.xml._

import scala.reflect.ClassTag

@RunWith(classOf[JUnitRunner])
class AttributeMapperExceptionSuite extends AttributeMapperBase {

  lazy val errorMessageSchema = schemaFactory.newSchema (new StreamSource(getClass.getResource("/xsd/error-message.xsd").toString))

  val testDir = new File("src/test/resources/tests/bad-maps")

  def getMapsFromTest (test : File) : List[File] = new File(test, "maps").listFiles().toList.filter(f => {
    f.toString.endsWith("xml") || f.toString.endsWith("json") || f.toString.endsWith("yaml")})
  def getAssertsFromTest (test : File) : List[File] = new File(test, "asserts").listFiles().toList.filter(f => {f.toString.endsWith("xml")})

  type MapperTest = (File /* map */, File /* assertion */, String /* validation engine */,
                    Seq[String] /* Error Strings */) => Unit /* Were're testing exceptional cases so no result */

  def getErrorMessageStrings (map : File) : Seq[String] = {
    val messageFileName = map.toString + ".errors"
    //
    //  Validate the error message file...
    //
    errorMessageSchema.newValidator.validate(new StreamSource(messageFileName))

    //
    //  Return a list of error message strings
    //
    val messageFile = XML.load(messageFileName)
    (messageFile \\ "contains").map(_.text)
  }

  def runTests(subTestName : String, description : String, mapperTest : MapperTest) : Unit = {
    val subTestDir = new File(testDir, subTestName)
    val tests : List[File] = subTestDir.listFiles.toList.filter(f=>f.isDirectory)
      tests.foreach( t => {
        val maps : List[File] = getMapsFromTest(t)
        val asserts : List[File] = getAssertsFromTest(t)

        maps.foreach ( map => {
          asserts.foreach ( assertFile => {
            validators.foreach (v => {
              test (s"$description ($map on $assertFile validated with $v)") {
                  mapperTest(map, assertFile, v, getErrorMessageStrings(map))
              }
            })
          })
        })
      })
  }

  def mapStreamSourceXDMDest(map : File, assertFile : File, validator : String ) : Unit = {
    val mapFormat = map.getName.substring(map.getName.lastIndexOf('.') + 1).toLowerCase
    val dest = new XdmDestination
    AttributeMapper.convertAssertion (
      new StreamSource(map),
      PolicyFormat.withName(mapFormat),
      new StreamSource(assertFile),
      dest,
      true,
      true,
      validator, Map("domain"->"foo:999-882"))
  }

  def mapNodeSourceNodeDest(map : File, assertFile : File, validator : String) : Unit = {
    var docBuilder : javax.xml.parsers.DocumentBuilder = null
    try {
      docBuilder = borrowParser
      val mapFormat = map.getName.substring(map.getName.lastIndexOf('.') + 1).toLowerCase
      val policyExec : XsltExecutable = PolicyFormat.withName(mapFormat) match {
        case PolicyFormat.JSON =>
          val om = new ObjectMapper()
          val jsonPolicy = om.readTree(map)
          //
          //  We double validate to make sure validation call works
          //
          AttributeMapper.generateXSLExec (AttributeMapper.validatePolicy(jsonPolicy, validator), true, validator)
        case PolicyFormat.YAML =>
          val jsonPolicy = AttributeMapper.parseYamlNode(new StreamSource(map))
          //
          //  We double validate to make sure validation call works
          //
          AttributeMapper.generateXSLExec (AttributeMapper.validatePolicy(jsonPolicy, validator), true, validator)
        case PolicyFormat.XML =>
          AttributeMapper.generateXSLExec (docBuilder.parse(map), true, validator)
      }

          val resultDoc = AttributeMapper.convertAssertion (policyExec, docBuilder.parse(assertFile), Map("domain"->"foo:999-882"))
    } finally {
      if (docBuilder != null) returnParser(docBuilder)
    }
  }

  def doTest[T <: Exception](subTestName : String, description : String,
    runner : (File, File, String) => Unit)(implicit m : Manifest[T]) : Unit = {
    runTests(subTestName, description,
      (map : File, assertFile : File, v : String, errorMessageStrings : Seq[String]) => {
        print (s"Running $map or $assertFile") // scalastyle:ignore
        val e = intercept[T] {
          runner(map, assertFile, v)
        }
        val msg = e.getMessage
        println (s"...$msg") // scalastyle:ignore
        errorMessageStrings.foreach ( s =>
          assert(msg.contains(s))
        )
      })
  }

  doTest[ParseException]("malformed",
    "Stream Source and XDM Dest Malformed Data should fail with ParseException",
    mapStreamSourceXDMDest)

  doTest[ValidationException]("invalid",
    "Stream Source and XDM Dest Invalid policy should fail with ValidationException",
    mapStreamSourceXDMDest)

  doTest[ValidationException]("invalid",
    "Node Source and Node Dest Invalid policy should fail with ValidationException",
    mapNodeSourceNodeDest)

  doTest[XPathException]("invalid-xpath",
    "Stream Source and XDM Dest Invalid xpath in policy should fail with XPathException",
    mapStreamSourceXDMDest)

  doTest[XPathException]("invalid-xpath",
    "Node Source and Node Dest Invalid xpath in policy should fail with XPathException",
    mapNodeSourceNodeDest)

}
