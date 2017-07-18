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

import java.io.{ByteArrayOutputStream, File}
import java.net.URI
import javax.xml.transform.stream.StreamSource

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class YAMLConversionSuite extends FunSuite {
  val testDir = new File("src/test/resources/tests/mapping-tests")
  val tests : List[File] = testDir.listFiles.toList.filter(f=>f.isDirectory)

  def toJson(f : File) : File = {
    val newURI = f.toURI.toString.replaceAll(".yaml$",".json")
    new File (new URI(newURI))
  }

  def existsJSON(f : File) : Boolean = {
    val xf : File = toJson(f)
    xf.exists
  }

  def getYAMLMapsFromTest(test : File) : List[File] = new File(test, "maps").listFiles().toList.filter(f => {
    f.toString.endsWith("yaml") && existsJSON(f)})

  tests.foreach ( t => {
    getYAMLMapsFromTest(t).foreach (f => {
      val jf = toJson(f)
      test (s"yaml conversion for $jf") {
        println (s"Converting $jf to yaml") // scalastyle:ignore
        val om = new ObjectMapper(new YAMLFactory())
        val bout_val = new ByteArrayOutputStream
        val bout = new ByteArrayOutputStream

        AttributeMapper.policy2YAML(new StreamSource(jf), bout_val, validate = true, XSDEngine.AUTO.toString)
        AttributeMapper.policy2YAML(new StreamSource(jf), bout, validate = false, XSDEngine.AUTO.toString)

        val convTree_val  = om.readTree(bout_val.toByteArray)
        val convTree = om.readTree(bout.toByteArray)
        val readTree = om.readTree(f)

        //
        //  JSON Node does a deep comparison.  Some examples are
        //  normalized (with default values filled in) and some are
        //  not.  That's okay, we compare normalized and
        //  non-normalized versions, if any matches we are good.
        //
        assert(readTree.equals(convTree) || readTree.equals(convTree_val))
      }
    })
  })
}
