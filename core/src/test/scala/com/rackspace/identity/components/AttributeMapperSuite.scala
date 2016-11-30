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

  tests.foreach( t => {
    val maps : List[File] = (new File(t,"maps")).listFiles().toList.filter(f => {f.toString.endsWith("xml") || f.toString.endsWith("json")})
    val asserts : List[File] = (new File(t,"asserts")).listFiles().toList.filter(f => {f.toString.endsWith("xml")})

    maps.foreach ( map => {
      asserts.foreach ( assert => {
        val dest = new XdmDestination
        test (s"Testing $map on $assert") {
          AttributeMapper.convertAssertion (new StreamSource(map), new StreamSource(assert), dest, true,
                                            map.toString.endsWith("json"), true, "auto")
        }
      })
    })
  })

  test("true") {
    assert (testDir.isDirectory)
  }
}
