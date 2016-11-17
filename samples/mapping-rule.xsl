<?xml version="1.0" encoding="UTF-8"?>
<!--
            -                                                       -
            -  THIS IS A GENERATED TRANSFORM  DON'T EDIT BY HAND    -
            -                                                       -
        -->
<xsl:transform xmlns="http://docs.rackspace.com/identity/api/ext/MappingRules"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:saml2="urn:oasis:names:tc:SAML:2.0:assertion"
    xmlns:mapping="http://docs.rackspace.com/identity/api/ext/MappingRules"
    version="2.0">
    <xsl:param name="outputSAML" as="xs:boolean" select="true()"/>
    <xsl:output method="xml" encoding="UTF-8" indent="yes"/>
    <xsl:variable name="locals" as="node()*">
        <xsl:call-template name="d1e4"/>
    </xsl:variable>
    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="$outputSAML">
                <xsl:apply-templates/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="mapping:outLocal"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="d1e4">
        <local>
            <user>
                <xsl:attribute name="name">
                    <xsl:value-of select="/saml2:Assertion/saml2:Subject/saml2:NameID"/>
                </xsl:attribute>
                <xsl:attribute name="email">
                    <xsl:value-of select="/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@Name='email']/saml2:AttributeValue[1]"/>
                </xsl:attribute>
                <xsl:attribute name="expire">
                    <xsl:value-of select="/saml2:Assertion/saml2:Subject/saml2:SubjectConfirmation/saml2:SubjectConfirmationData/@NotOnOrAfter"/>
                </xsl:attribute>
            </user>
            <domain>
                <xsl:attribute name="id">
                    <xsl:value-of select="/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@Name='domain']/saml2:AttributeValue[1]"/>
                </xsl:attribute>
            </domain>
            <role>
                <xsl:attribute name="names">
                    <xsl:value-of select="/saml2:Assertion/saml2:AttributeStatement/saml2:Attribute[@Name='roles']/saml2:AttributeValue"
                        separator=" "/>
                </xsl:attribute>
            </role>
        </local>
    </xsl:template>
    <xsl:template match="node() | @*">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="saml2:Subject">
        <xsl:copy>
            <saml2:NameID Format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified">
                <xsl:value-of select="$locals/mapping:local/mapping:user/@name[1]"/>
            </saml2:NameID>
            <saml2:SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
                <saml2:SubjectConfirmationData>
                    <xsl:attribute name="NotOnOrAfter">
                        <xsl:value-of select="$locals/mapping:local/mapping:user/@expire[1]"/>
                    </xsl:attribute>
                </saml2:SubjectConfirmationData>
            </saml2:SubjectConfirmation>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="saml2:AttributeStatement">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <saml2:Attribute Name="domain">
                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
                    <xsl:value-of select="$locals/mapping:local/mapping:domain/@id[1]"/>
                </saml2:AttributeValue>
            </saml2:Attribute>
            <saml2:Attribute Name="email">
                <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
                    <xsl:value-of select="$locals/mapping:local/mapping:user/@email[1]"/>
                </saml2:AttributeValue>
            </saml2:Attribute>
            <saml2:Attribute Name="roles">
                <xsl:variable name="allRolesJoin"
                    as="xs:string"
                    select="string-join($locals/mapping:local/mapping:role/@names,' ')"/>
                <xsl:variable name="allRoles"
                    as="xs:string*"
                    select="tokenize($allRolesJoin,' ')"/>
                <xsl:for-each select="allRoles">
                    <saml2:AttributeValue xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="xs:string">
                        <xsl:value-of select="."/>
                    </saml2:AttributeValue>
                </xsl:for-each>
            </saml2:Attribute>
            <xsl:apply-templates select="node()"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="saml2:Attribute[@Name=('domain','email','roles')]"/>
    <xsl:template name="mapping:outLocal"/>
</xsl:transform>
