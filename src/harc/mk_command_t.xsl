<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
    Document   : mk_command_t.xsl
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
    <xsl:template match="/">
// This file has been automatically generated from commandnames.xml.
// Do not edit.
// Do not check in version management.

package harc;

public enum command_t {
 
<xsl:apply-templates select="//command"/>
       invalid;
       
       public static command_t parse(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return invalid;
            }
        }

        public static boolean is_valid(String name) {
            try {
                valueOf(name);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        public static void main(String[] args) {
            System.out.println(command_t.parse(args[0]));
        }
}
    </xsl:template>

<xsl:template match="command"><xsl:text>        </xsl:text><xsl:value-of select="@id"/>,
</xsl:template>

</xsl:stylesheet>
