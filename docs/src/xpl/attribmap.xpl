<?xml version="1.0" encoding="UTF-8"?>
<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
    xmlns:cx="http://xmlcalabash.com/ns/extensions"
    xmlns:c="http://www.w3.org/ns/xproc-step" version="1.0">
    <p:import href="recursive-dir-list.xpl"/>
    <cx:recursive-directory-list path="../site/sphinx" include-filter="^.*\.rst$"/>
    <p:for-each>
        <p:iteration-source select="//c:directory"/>
        <p:variable name="base" select="substring-after(/c:directory/@xml:base,'file:')"/>
        <p:for-each>
            <p:iteration-source select="//c:file"/>
            <p:variable name="fullName" select="concat($base,/c:file/@name)"/>
            <p:variable name="resultName" select="concat('../../target/site/sphinx/',substring-after($fullName,'src/site/sphinx/'))"/>
            <p:exec command="rst2xml-2.7.py" result-is-xml="true" source-is-xml="false" arg-separator=",">
                <p:input port="source"><p:empty/></p:input>
                <p:with-option name="args" select="concat('--no-doctype,--no-file-insertion,',$fullName)"/>
            </p:exec>
            <p:filter select="c:result/element()"/>
            <p:xslt version="2.0">
                <p:input port="stylesheet">
                    <p:document href="../xsl/add-directives.xsl"/>
                </p:input>
                <p:input port="parameters">
                    <p:empty/>
                </p:input>
            </p:xslt>
            <p:xslt version="2.0">
                <p:input port="stylesheet">
                    <p:document href="../xsl/handle-saml-and-map.xsl"/>
                </p:input>
                <p:input port="parameters">
                    <p:empty/>
                </p:input>
            </p:xslt>
            <p:xslt version="2.0">
                <p:input port="stylesheet">
                    <p:document href="../xsl/handle-attribmap.xsl"/>
                </p:input>
                <p:input port="parameters">
                    <p:empty/>
                </p:input>
            </p:xslt>
            <p:xslt version="2.0">
                <p:input port="stylesheet">
                    <p:document href="../xsl/xml2rst-wrapper.xsl"/>
                </p:input>
                <p:input port="parameters">
                    <p:empty/>
                </p:input>
            </p:xslt>
            <p:store method="text">
                <p:with-option name="href" select="$resultName"/>
            </p:store>
        </p:for-each>
    </p:for-each>
</p:declare-step>
