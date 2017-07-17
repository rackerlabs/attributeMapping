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
import java.io.ByteArrayOutputStream
import java.net.URI

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource

import net.sf.saxon.s9api.Destination

import com.fasterxml.jackson.databind.ObjectMapper

import org.junit.runner.RunWith

import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class JSONConversionSuite extends FunSuite {
  val testDir = new File("src/test/resources/tests/mapping-tests")
  val tests : List[File] = testDir.listFiles.toList.filter(f=>f.isDirectory)


  def toXML(f : File) : File = {
    val newURI = f.toURI.toString.replaceAll(".json$",".xml");
    new File (new URI(newURI));
  }

  def existsXML(f : File) : Boolean = {
    val xf : File = toXML(f)
    xf.exists
  }

  def getJSONMapsFromTest (test : File) : List[File] = new File(test, "maps").listFiles().toList.filter(f => {
    f.toString.endsWith("json") && existsXML(f)})

  tests.foreach ( t => {
    getJSONMapsFromTest(t).foreach (f => {
      val xf = toXML(f)
      test (s"json conversion for $xf") {
        println (s"Converting $xf to json") // scalastyle:ignore
        val om = new ObjectMapper()
        val bout_val = new ByteArrayOutputStream
        val bout = new ByteArrayOutputStream

        val dest_val = AttributeMapper.processor.newSerializer(bout_val)
        val dest = AttributeMapper.processor.newSerializer(bout)

        AttributeMapper.policy2JSON(new StreamSource(xf), dest_val, true, "auto")
        AttributeMapper.policy2JSON(new StreamSource(xf), dest, false, "auto")

        val convTree_val  = om.readTree(bout_val.toByteArray())
        val convTree = om.readTree(bout.toByteArray())
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
