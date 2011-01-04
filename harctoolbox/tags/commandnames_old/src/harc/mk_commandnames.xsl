<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
    Document   : mk_ir_commands.xsl
    Created on : February 2, 2009, 1:53 PM
    Author     : bengt
    Description:
        Purpose of transformation follows.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text"/>

    <!-- TODO customize transformation rules 
         syntax recommendation http://www.w3.org/TR/xslt 
    -->
    <xsl:variable name="cnt" select="42"/>
    <xsl:template match="/">
// This file has been automatically generated from commandnames.xml.
// Do not edit.
// Do not check in version management.

package harc;

public interface commandnames {
    public final static int cmd_invalid = -1;
    <xsl:apply-templates select="//command"/>
    
    public final static String command_name_table[] = {
<xsl:for-each select="//command">        "<xsl:value-of select="@id"/>",
</xsl:for-each>
     };
}
     
    </xsl:template>

<xsl:template match="command">
    public final static int <xsl:value-of select="@id"/> = <xsl:value-of select="position()-1"/>;</xsl:template>

</xsl:stylesheet>
