<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xslout="http://www.rackspace.com/repose/wadl/checker/Transform"
    xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    xmlns:saml2p="urn:oasis:names:tc:SAML:2.0:protocol"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    version="2.0">

    <xsl:param name="base" as="xs:anyURI"/>

    <xsl:namespace-alias stylesheet-prefix="xslout" result-prefix="xsl"/>

    <xsl:output method="xml" indent="yes" />

    <xsl:template match="/">
        <xsl:comment>
            -                                                       -
            -  THIS IS A GENERATED TRANSFORM  DON'T EDIT BY HAND    -
            -                                                       -
        </xsl:comment>
        <xslout:transform version="2.0" xmlns="http://docs.rackspace.com/identity/api/ext/MappingRules">
            <xsl:copy-of select="/saml2p:Response/namespace::*"/>
            <xslout:output method="xml" indent="yes"/>
            <xslout:variable name="assert"  select="/" as="node()"/>
            <xslout:template match="/">
                <xslout:choose>
                   <xsl:apply-templates />
                    <xslout:otherwise><mapping:success/></xslout:otherwise>
                </xslout:choose>
            </xslout:template>
            <xslout:function name="mapping:get-attributes" as="xs:string*">
                <xslout:param name="name" as="xs:string"/>
                <xslout:sequence
                    select="$assert//saml2:Assertion[1]/saml2:AttributeStatement/saml2:Attribute[@Name=$name]/saml2:AttributeValue"/>
            </xslout:function>
            <xslout:function name="mapping:get-attribute" as="xs:string">
                <xslout:param name="name" as="xs:string"/>
                <xslout:sequence select="mapping:get-attributes($name)[1]"/>
            </xslout:function>
            <xslout:function name="mapping:get-expire" as="xs:dateTime">
                <xslout:sequence select="xs:dateTime($assert//saml2:Assertion[1]/saml2:Subject/saml2:SubjectConfirmation/saml2:SubjectConfirmationData/@NotOnOrAfter)"/>
            </xslout:function>
        </xslout:transform>
    </xsl:template>

    <xsl:template match="processing-instruction()[name()='assert']">
        <xsl:call-template name="failAssert">
            <xsl:with-param name="testAssertion" select="string(.)"/>
            <xsl:with-param name="message" select="preceding-sibling::comment()[1]"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="processing-instruction()[name()='include']">
        <xsl:variable name="includeURL" as="xs:string" select="normalize-space(string(.))"/>
        <xsl:variable name="urlParts" as="xs:string*"  select="tokenize($includeURL,'#')"/>
        <xsl:variable name="assertURL" as="xs:anyURI" select="resolve-uri($urlParts[1], $base)"/>
        <xsl:variable name="groupName" as="xs:string?" select="$urlParts[2]"/>

        <xsl:variable name="commonAsserts" as="node()" select="doc($assertURL)"/>
        <xsl:choose>
            <xsl:when test="not(empty($groupName))">
                <xsl:choose>
                    <xsl:when test="$commonAsserts/mapping:common-assertions/mapping:assert-group[@name=$groupName]">
                        <xsl:apply-templates select="$commonAsserts/mapping:common-assertions/mapping:assert-group[@name=$groupName]/mapping:assert"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xslout:when test="true()">
                            <mapping:fail assertion="">
                                <mapping:message>
                                    Error in test: the assertion group '<xsl:value-of select="$groupName"/>' is not found in
                                    <xsl:value-of select="$assertURL"/>
                                </mapping:message>
                                <mapping:onXML>
                                    <mapping:not-found/>
                                </mapping:onXML>
                            </mapping:fail>
                        </xslout:when>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="$commonAsserts//mapping:assert"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="mapping:assert">
         <xsl:call-template name="failAssert">
            <xsl:with-param name="testAssertion" select="@test"/>
            <xsl:with-param name="message" select="normalize-space(.)"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="failAssert">
        <xsl:param name="testAssertion" as="xs:string"/>
        <xsl:param name="message" as="xs:string?"/>

        <xslout:when test="not({$testAssertion})">
            <mapping:fail assertion="{$testAssertion}">
                <mapping:message>
                <xsl:if test="not(empty($message))">
                    <xsl:value-of select="$message"/>
                </xsl:if>
                </mapping:message>
                <mapping:onXML>
                    <xslout:copy-of select="$assert"/>
                </mapping:onXML>
            </mapping:fail>
        </xslout:when>
    </xsl:template>

    <xsl:template match="text()"/>
</xsl:stylesheet>
