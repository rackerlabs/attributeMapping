<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:rstd="http://www.rackspace.com/docs/rstd"
    exclude-result-prefixes="xs"
    version="2.0">

    <xsl:output  indent="no"></xsl:output>
    <!-- Copy Everything -->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="document">
        <xsl:copy>
            <xsl:namespace name="rstd" select="'http://www.rackspace.com/docs/rstd'"/>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Capture an directive system message and
         turn it into a directive -->
    <xsl:template match="system_message[@level=3 and @type='ERROR' and contains(paragraph,'Unknown directive')]">
        <xsl:variable name="directiveText" select="literal_block" as="xs:string"/>
        <rstd:directive line="{@line}" source="{@source}">
            <xsl:call-template name="parseDirective">
                <xsl:with-param name="directiveText" select="$directiveText"/>
            </xsl:call-template>
            <rstd:raw xml:space="preserve"><xsl:value-of select="$directiveText"/></rstd:raw>
        </rstd:directive>
    </xsl:template>

    <xsl:template name="parseDirective">
        <xsl:param name="directiveText" as="xs:string"/>
        <xsl:analyze-string select="$directiveText" regex="^\.\.\s(([A-Z]|[a-z]|[0-9]|-|_)*)::\s(.*)$" flags="s">
            <xsl:matching-substring>
                <xsl:attribute name="type" select="regex-group(1)"/>
                <xsl:call-template name="parseDirectiveArgs">
                    <xsl:with-param name="argText" select="regex-group(3)"/>
                </xsl:call-template>
            </xsl:matching-substring>
            <xsl:non-matching-substring>
                <xsl:attribute name="type">UNKNOWN</xsl:attribute>
                <xsl:message>[WARNING] Unable to parse directive: <xsl:value-of select="$directiveText"/></xsl:message>
            </xsl:non-matching-substring>
        </xsl:analyze-string>
    </xsl:template>

    <xsl:template name="parseDirectiveArgs">
        <xsl:param name="argText" as="xs:string"/>
        <xsl:analyze-string select="$argText" regex="^\s*:(([A-Z]|[a-z]|[0-9]|-|_)*):(.*)$" flags="m">
            <xsl:matching-substring>
                <rstd:field name="{regex-group(1)}"><xsl:value-of select="normalize-space(regex-group(3))"/></rstd:field>
            </xsl:matching-substring>
            <xsl:non-matching-substring>
                <xsl:if test="normalize-space(.) != ''">
                    <rstd:content><xsl:value-of select="."/></rstd:content>
                </xsl:if>
            </xsl:non-matching-substring>
        </xsl:analyze-string>
    </xsl:template>
</xsl:stylesheet>
