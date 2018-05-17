<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
<!ENTITY CR "&#x0A;">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:rstd="http://www.rackspace.com/docs/rstd"
    xmlns:data="a"
    xmlns:u="u"
    exclude-result-prefixes="xs data rstd"
    version="2.0">
    <xsl:import href="xml2rst-noexslt.xsl"/>

    <xsl:output method="xml"/>

    <!--
        The noexslt version used document('') to access this lookup table.
        This caused a lot of warnings when running in SAXON, so we copy the
        lookup table to a global variable and override the approprate template.
     -->
    <xsl:variable name="lookup" as="node()">
        <!--
             Indent a block if it's a child of...
         -->
        <data:lookup>
            <node name="address" indent="10"/>
            <node name="author" indent="9"/>
            <node name="authors" indent="10"/>
            <node name="contact" indent="10"/>
            <node name="copyright" indent="12"/>
            <node name="date" indent="7"/>
            <node name="organization" indent="15"/>
            <node name="revision" indent="11"/>
            <node name="status" indent="9"/>
            <node name="version" indent="10"/>
            <!-- This is only for `bullet_list/list_item';
         `enumerated_list/list_item' is handled special -->
            <node name="list_item" indent="2"/>
            <node name="definition_list_item" indent="4"/>
            <node name="field_body" indent="4"/>
            <node name="option_list_item" indent="4"/>
            <!-- This is also the indentation if block_quote comes as one of the
         special directives -->
            <node name="block_quote" indent="4"/>
            <node name="literal_block" indent="4"/>
            <node name="attribution" indent="3"/>
            <node name="line" indent="2"/>
        </data:lookup>
    </xsl:variable>

    <!-- Wrap the text in an element -->
    <xsl:template match="/">
        <wapper>
            <xsl:apply-templates/>
        </wapper>
    </xsl:template>

    <!-- Do indent according to ancestor -->
    <xsl:template
        name="u:indent">
        <!-- In some cases the ancestors to indent for need to be determined
         by the calling template -->
        <xsl:param
            name="ancestors"
            select="ancestor::*"/>
        <xsl:for-each
            select="$ancestors">
            <xsl:variable
                name="this"
                select="name()"/>
            <xsl:choose>
                <xsl:when
                    test="contains($directives, concat('*', $this, '*'))">
                    <xsl:call-template
                        name="u:repeat">
                        <!-- TODO Indentation of lines after some directives must be
	              indented to align with the directive instead of a
	              fixed indentation; however, this is rather complicated
	              since identation for parameters should be fixed -->
                        <xsl:with-param
                            name="length"
                            select="3"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:when
                    test="$this = 'list_item' and parent::enumerated_list">
                    <!-- Enumerated list items base their indentation on the
               numeration -->
                    <xsl:variable
                        name="enumerator">
                        <xsl:call-template
                            name="u:outputEnumerator"/>
                    </xsl:variable>
                    <xsl:call-template
                        name="u:repeat">
                        <xsl:with-param
                            name="length"
                            select="string-length($enumerator)"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template
                        name="u:repeat">
                        <xsl:with-param
                            name="length"
                            select="$lookup//node[@name=$this]/@indent"/>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <!-- Handle Directives -->

    <xsl:template match="rstd:directive[rstd:raw]">
        <xsl:text>&CR;</xsl:text>
        <xsl:value-of select="rstd:raw"/>
        <xsl:text>&CR;</xsl:text>
    </xsl:template>

    <xsl:template match="rstd:directive[not(rstd:raw)]">
        <xsl:text>&CR;</xsl:text>
        <xsl:text>.. </xsl:text><xsl:value-of select="@type"/>
        <xsl:text>:: </xsl:text>
        <xsl:apply-templates />
        <xsl:text>&CR;&CR;</xsl:text>
    </xsl:template>

    <xsl:template match="rstd:field">
        <xsl:text>&CR;   :</xsl:text><xsl:value-of select="@name"/><xsl:text>: </xsl:text>
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="rstd:content">
        <xsl:value-of select="."/>
    </xsl:template>

    <!-- Re-add disabled directives -->
    <xsl:template match="system_message[@level=2 and @type='WARNING'
                         and contains(paragraph,'directive disabled')]">
        <xsl:variable name="directiveText" as="xs:string" select="literal_block"/>
        <xsl:text>&CR;</xsl:text>
        <xsl:value-of select="$directiveText"/>
        <xsl:text>&CR;</xsl:text>
    </xsl:template>
</xsl:stylesheet>
