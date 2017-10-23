<?xml version="1.0" encoding="UTF-8"?>
<!--
   extract-ext.xsl

   This stylesheet is responsible for extracting extension attributes
   from a SAML Response and converting it to a
   RAX-AUTH:extendedAttributes element.

   Extended attributes always follow the following rules:

   1. They are located in the first assertion of the SAML Response.
   2. They contain an attribute name with the following pattern
   "group/name" where "group" is the name of the extended attribute
   name and "name" is the name of the attribute.
   3. They may contain a boolean attribute mapping:multiValue that
   denotes that they are multiValue attributes eventhough they contain
   a single value - this helps in JSON conversion.

   Copyright 2017 Rackspace US, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
    xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
    xmlns:attribEX="http://openrepose.org/attribExtractor"
    xmlns:RAX-AUTH="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    exclude-result-prefixes="xs saml2p saml2 attribEX"
    version="2.0">

    <xsl:output indent="yes"/>

    <xsl:template match="/">
        <xsl:variable name="groups" as="node()">
            <attribEX:groups>
                <xsl:apply-templates/>
            </attribEX:groups>
        </xsl:variable>
        <RAX-AUTH:extendedAttributes xmlns="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0">
            <xsl:for-each-group select="$groups/attribEX:group" group-by="@name">
                <group name="{current-group()[1]/@name}">
                    <xsl:for-each-group select="current-group()//attribEX:variable" group-by="@name">
                        <attribute name="{@name}">
                            <!--
                                Multivalue is only set if it is true.
                            -->
                            <xsl:if test="current-group()//@multiValue">
                                <xsl:attribute name="multiValue">true</xsl:attribute>
                            </xsl:if>
                            <xsl:for-each select="current-group()//attribEX:value">
                                <value><xsl:value-of select="."/></value>
                            </xsl:for-each>
                        </attribute>
                    </xsl:for-each-group>
                </group>
            </xsl:for-each-group>
        </RAX-AUTH:extendedAttributes>
    </xsl:template>

    <xsl:template match="/saml2p:Response/saml2:Assertion[1]/saml2:AttributeStatement/saml2:Attribute[count(tokenize(@Name,'/')) = 2]">
        <xsl:variable name="tokens" as="xs:string*" select="tokenize(@Name,'/')"/>
        <xsl:variable name="multiValue" as="xs:boolean"
                      select="if (exists (@mapping:multiValue)) then xs:boolean(@mapping:multiValue) else false()"/>
        <attribEX:group name="{$tokens[1]}">
            <attribEX:variable name="{$tokens[2]}">
                <!--
                    Only set multiValue if it's a multiValue variable.
                -->
                <xsl:if test="$multiValue">
                    <xsl:attribute name="multiValue">true</xsl:attribute>
                </xsl:if>
                <xsl:for-each select="saml2:AttributeValue">
                    <attribEX:value><xsl:value-of select="."/></attribEX:value>
                </xsl:for-each>
            </attribEX:variable>
        </attribEX:group>
    </xsl:template>
    <xsl:template match="text()"/>
</xsl:stylesheet>
