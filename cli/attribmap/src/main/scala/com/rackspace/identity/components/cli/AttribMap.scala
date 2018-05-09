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
package com.rackspace.identity.components.cli

import java.io.{File, InputStream, PrintStream}
import java.net.URI
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource

import com.martiansoftware.nailgun.NGContext
import com.rackspace.com.papi.components.checker.util.URLResolver
import com.rackspace.identity.components.{AttributeMapper, PolicyFormat, KnownAttributeMapperException}
import net.sf.saxon.s9api.Destination
import org.clapper.argot.ArgotConverters._
import org.clapper.argot.{ArgotParser, ArgotUsageException}

object AttribMap {
  val title = getClass.getPackage.getImplementationTitle
  val version = getClass.getPackage.getImplementationVersion

  def parseArgs(args: Array[String], // scalastyle:ignore
                base: String,        // This method is longer than 50 lines due to locally defined methods.
                in: InputStream,
                out: PrintStream,
                err: PrintStream): Option[(Source, PolicyFormat.Value, Source, Destination, Boolean, Boolean, String)] = {

    val parser = new ArgotParser("attribmap", preUsage=Some(s"$title v$version"))

    val policy = parser.parameter[String]("policy",
                                          "Attribute mapping policy",
                                          false)

    val assertion = parser.parameter[String]("assertion",
                                             "The assertion to translate based on policy",
                                             false)

    val output = parser.parameter[String]("output",
                                          "Output file. If not specified, stdout will be used.",
                                          true)

    val dontValidate = parser.flag[Boolean](List("D", "dont-validate"),
                                                 "Disable Validation (Validation will be enabled by default)")

    val xsdEngine = parser.option[String](List("x", "xsd-engine"), "xsd-engine",
                                                  "XSD Engine to use. Valid values are auto, saxon, xerces (default is auto)")

    val help = parser.flag[Boolean] (List("h", "help"),
                                     "Display usage.")

    val useSAML = parser.flag[Boolean] (List("s", "saml"),
                                        "Output in SAML format")

    val printVersion = parser.flag[Boolean] (List("version"),
                                             "Display version.")

    def policySource : Source = new StreamSource(URLResolver.toAbsoluteSystemId(policy.value.get, base))
    def assertionSource : Source = new StreamSource(URLResolver.toAbsoluteSystemId(assertion.value.get, base))
    def destination : Destination = {
      if (output.value.isEmpty) {
        AttributeMapper.processor.newSerializer(out)
      } else {
        AttributeMapper.processor.newSerializer(new File(new URI(URLResolver.toAbsoluteSystemId(output.value.get, base))))
      }
    }
    try {
      parser.parse(args)

      if (help.value.getOrElse(false)) {
        parser.usage() // throws ArgotUsageException
      }

      if (printVersion.value.getOrElse(false)) {
        err.println(s"$title v$version") // scalastyle:ignore
        None
      } else {
        Some((policySource,
              PolicyFormat.fromPath(policy.value.get),
              assertionSource,
              destination,
              useSAML.value.getOrElse(false),
              !dontValidate.value.getOrElse(false),
              xsdEngine.value.getOrElse("auto")))
      }
    } catch {
      case e: ArgotUsageException => err.println(e.message) // scalastyle:ignore
                                     None
      case iae : IllegalArgumentException => err.println(iae.getMessage) // scalastyle:ignore
                                             None
    }
  }

  private def getBaseFromWorkingDir (workingDir : String) : String = {
    (new File(workingDir)).toURI().toString
  }

  //
  // Local run...
  //
  def main(args : Array[String]): Unit = {
    try {
      parseArgs (args, getBaseFromWorkingDir(System.getProperty("user.dir")),
        System.in, System.out, System.err) match {
        case Some((policy : Source, policyFormat : PolicyFormat.Value, assertion : Source, dest : Destination,
                  useSAML : Boolean, validate : Boolean, xsdEngine : String)) =>
          AttributeMapper.convertAssertion (policy, policyFormat, assertion, dest, useSAML, validate, xsdEngine)
        case None => /* Bad args, Ignore */
      }
    } catch {
      case e : KnownAttributeMapperException => System.err.println(e.getMessage) // scalastyle:ignore
    }
  }

  //
  //  Nailgun run...
  //
  def nailMain(context : NGContext): Unit = {
    try {
      parseArgs (context.getArgs, getBaseFromWorkingDir(context.getWorkingDirectory),
        context.in, context.out, context.err) match {
        case Some((policy : Source, policyFormat : PolicyFormat.Value, assertion : Source, dest : Destination,
          useSAML : Boolean, validate : Boolean, xsdEngine : String)) =>
          AttributeMapper.convertAssertion (policy, policyFormat, assertion, dest, useSAML, validate, xsdEngine)
        case None => /* Bad args, Ignore */
      }
    } catch {
      case e : KnownAttributeMapperException => context.err.println(e.getMessage) // scalastyle:ignore
    }
  }
}
