<?xml version="1.0" encoding="ISO-8859-1"?>

<!--
Copyright (C) 2009 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
-->

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

/**
  * All commands known in the system.
  */

public enum command_t {
 
<xsl:apply-templates select="//command"/>
       invalid;
       
       /**
        * Safe version of valueOf(String).
        *
        * @param name the name of the enum constant to be returned.
        * @return On success matching command, otherwise command_t.invalid.
        */
        public static command_t parse(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException e) {
                return invalid;
            }
        }

       /**
        * Determines whether the argument is a valid command name.
        *
        * @param name The name to be checked
        * @return true if and only if the argument is a valid command.
        */
        public static boolean is_valid(String name) {
            try {
                valueOf(name);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

       /**
        * For debugging only: try to parse args[0].
        */
        public static void main(String[] args) {
            System.out.println(command_t.parse(args[0]));
        }
}
    </xsl:template>

<xsl:template match="command"><xsl:text>        </xsl:text><xsl:value-of select="@id"/>,
</xsl:template>

</xsl:stylesheet>
