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

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.dom.DOMSource

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite

import net.sf.saxon.s9api._
import net.sf.saxon.Configuration.LicenseFeature._

import com.fasterxml.jackson.databind.ObjectMapper

import com.rackspace.com.papi.components.checker.util.XMLParserPool._

@RunWith(classOf[JUnitRunner])
class AttributeMapperSuite extends AttributeMapperBase {

  val testDir = new File("src/test/resources/tests/mapping-tests")
  val tests : List[File] = testDir.listFiles.toList

  def getMapsFromTest (test : File) : List[File] = (new File(test,"maps")).listFiles().toList.filter(f => {
    f.toString.endsWith("xml") || f.toString.endsWith("json")})
  def getAssertsFromTest (test : File) : List[File] = (new File(test,"asserts")).listFiles().toList.filter(f => {f.toString.endsWith("xml")})

  type MapperTest = (File /* map */, File /* assertion */, String /* validation engine */) => Source /* Resulting assertion */

  def runTests(description : String, mapperTest : MapperTest) : Unit = {
      tests.foreach( t => {
        val maps : List[File] = getMapsFromTest(t)
        val asserts : List[File] = getAssertsFromTest(t)

        maps.foreach ( map => {
          asserts.foreach ( assertFile => {
            val asserterExec = getAsserterExec(new StreamSource(assertFile))
            validators.foreach (v => {
              test (s"$description ($map on $assertFile validated with $v)") {
                var docBuilder : javax.xml.parsers.DocumentBuilder = null
                try {
                  docBuilder = borrowParser
                  val outDoc = docBuilder.newDocument
                  val domDest = new DOMDestination(outDoc)
                  val newAssertion = mapperTest(map, assertFile, v)
                  val asserter = AttributeMapper.getXsltTransformer(asserterExec)
                  asserter.setSource(newAssertion)
                  asserter.setDestination(domDest)
                  asserter.transform()

                  assert(outDoc)
                } finally {
                  if (docBuilder != null) returnParser(docBuilder)
                }
              }
            })
          })
        })
      })
  }

  runTests("Stream Source and XDM Dest", (map : File, assertFile : File, v : String) => {
    println (s"Running $map on $assertFile") // scalastyle:ignore
    val dest = new XdmDestination
    AttributeMapper.convertAssertion (new StreamSource(map), new StreamSource(assertFile), dest, true,
                                      map.toString.endsWith("json"), true, v)
    dest.getXdmNode.asSource
  })

  runTests("DOM/JSON Node Source and DOM Dest", (map : File, assertFile : File, v : String) => {
    println (s"Running $map on $assertFile") // scalastyle:ignore
    var docBuilder : javax.xml.parsers.DocumentBuilder = null
    try {
      docBuilder = borrowParser
      val isJSON = map.toString.endsWith("json")
      val policyExec : XsltExecutable = {
        if (isJSON) {
          val om = new ObjectMapper()
          AttributeMapper.generateXSLExec (om.readTree(map), true, v)
        } else {
          AttributeMapper.generateXSLExec (docBuilder.parse(map), true, v)
        }
      }

      val resultDoc = AttributeMapper.convertAssertion (policyExec, docBuilder.parse(assertFile))
      new DOMSource(resultDoc)
    } finally {
      if (docBuilder != null) returnParser(docBuilder)
    }
  })
}
