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

import java.io.ByteArrayOutputStream
import java.io.File

import javax.xml.transform.stream.StreamSource

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite

import net.sf.saxon.s9api._

@RunWith(classOf[JUnitRunner])
class AttributeMapperSuite extends FunSuite {
  val testDir = new File("src/test/resources/tests")
  val tests : List[File] = testDir.listFiles.toList

  val testerXSLExec = AttributeMapper.compiler.compile (new StreamSource(new File("src/test/resources/xsl/mapping-tests.xsl")))

  tests.foreach( t => {
    val maps : List[File] = (new File(t,"maps")).listFiles().toList.filter(f => {f.toString.endsWith("xml") || f.toString.endsWith("json")})
    val asserts : List[File] = (new File(t,"asserts")).listFiles().toList.filter(f => {f.toString.endsWith("xml")})

    maps.foreach ( map => {
      asserts.foreach ( assertFile => {
        val dest = new XdmDestination
        val asserterXSL = new XdmDestination

        val asserterTrans = AttributeMapper.getXsltTransformer (testerXSLExec)
        asserterTrans.setSource (new StreamSource(assertFile))
        asserterTrans.setDestination(asserterXSL)
        asserterTrans.transform

        val asserterExec = AttributeMapper.compiler.compile(asserterXSL.getXdmNode.asSource)

        test (s"Testing $map on $assertFile") {
          val bout = new ByteArrayOutputStream
          println (s"Running $map on $assertFile")
          AttributeMapper.convertAssertion (new StreamSource(map), new StreamSource(assertFile), dest, true,
                                            map.toString.endsWith("json"), true, "auto")
          println ("Testing assertions")
          val asserter = AttributeMapper.getXsltTransformer(asserterExec)
          asserter.setSource(dest.getXdmNode.asSource)
          asserter.setDestination(AttributeMapper.processor.newSerializer(bout))
          asserter.transform

          assert(bout.toString.contains("mapping:success"))
        }
      })
    })
  })
}
