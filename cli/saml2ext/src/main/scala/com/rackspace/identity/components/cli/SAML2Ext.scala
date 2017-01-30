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

import java.io.File
import java.io.PrintStream
import java.io.InputStream

import java.net.URI

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource

import org.clapper.argot.ArgotConverters._
import org.clapper.argot.{ArgotParser, ArgotUsageException}

import com.martiansoftware.nailgun.NGContext

import net.sf.saxon.s9api.Serializer
import net.sf.saxon.s9api.Destination

import com.rackspace.com.papi.components.checker.util.URLResolver

import com.rackspace.identity.components.AttributeMapper

object SAML2Ext {
  val title = getClass.getPackage.getImplementationTitle
  val version = getClass.getPackage.getImplementationVersion

  def parseArgs(args: Array[String], // scalastyle:ignore
                base: String,        // This method is longer than 50 lines due to locally defined methods.
                in: InputStream,
                out: PrintStream,
                err: PrintStream): Option[(Source, Destination, Boolean, String, Boolean)] = {

    val parser = new ArgotParser("saml2ext", preUsage=Some(s"$title v$version"))

    val saml = parser.parameter[String]("samlResult",
                                        "A SAMLResult that may contain extended attributes",
                                        false)

    val output = parser.parameter[String]("output",
                                          "Output file. If not specified, stdout will be used.",
                                          true)

    val dontValidate = parser.flag[Boolean](List("D", "dont-validate"),
                                                 "Disable Validation (Validation will be enabled by default)")

    val xsdEngine = parser.option[String](List("x", "xsd-engine"), "xsd-engine",
                                                  "XSD Engine to use. Valid values are auto, saxon, xerces (default is auto)")

    val useJSON = parser.flag[Boolean] (List("j", "json"),
                                        "Output in JSON format")

    val help = parser.flag[Boolean] (List("h", "help"),
                                     "Display usage.")

    val printVersion = parser.flag[Boolean] (List("version"),
                                             "Display version.")


    def samlSource : Source = new StreamSource(URLResolver.toAbsoluteSystemId(saml.value.get, base))
    def destination : Destination = {
      val ser  = {
        if (output.value.isEmpty) {
          AttributeMapper.processor.newSerializer(out)
        } else {
          AttributeMapper.processor.newSerializer(new File(new URI(URLResolver.toAbsoluteSystemId(output.value.get, base))))
        }
      }
      ser.setOutputProperty(Serializer.Property.INDENT, "yes")
      ser
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
        Some((samlSource, destination, !dontValidate.value.getOrElse(false),
              xsdEngine.value.getOrElse("auto"),useJSON.value.getOrElse(false)))
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
    parseArgs (args, getBaseFromWorkingDir(System.getProperty("user.dir")),
               System.in, System.out, System.err) match {
      case Some((saml : Source,  dest : Destination, validate : Boolean, xsdEngine : String, asJSON : Boolean)) =>
        AttributeMapper.extractExtendedAttributes (saml,  dest, asJSON, validate, xsdEngine)
      case None => /* Bad args, Ignore */
    }
  }

  //
  // Nailgun run...
  //
  def nailMain(context : NGContext): Unit = {
    parseArgs (context.getArgs, getBaseFromWorkingDir(context.getWorkingDirectory),
               context.in, context.out, context.err) match {
      case Some((saml : Source,  dest : Destination, validate : Boolean, xsdEngine : String, asJSON : Boolean)) =>
        AttributeMapper.extractExtendedAttributes (saml,  dest, asJSON, validate, xsdEngine)
      case None => /* Bad args, Ignore */
    }
  }
}
