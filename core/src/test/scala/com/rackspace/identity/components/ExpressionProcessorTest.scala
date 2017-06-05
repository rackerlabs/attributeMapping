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

import net.sf.saxon.expr.parser.XPathParser
import net.sf.saxon.sxpath.IndependentContext
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ExpressionProcessorTest extends FunSuite {

  test("Processes the expression and sub-expressions") {
    val ic = new IndependentContext()
    ic.setXPathLanguageLevel(AttributeMapper.XQUERY_VERSION)

    val exp = new XPathParser().parse("/root", 0, 0, ic)
    var count = 0

    ExpressionProcessor.apply(exp, e => {
      count += 1
    })

    assert(count == 4)
  }
}
