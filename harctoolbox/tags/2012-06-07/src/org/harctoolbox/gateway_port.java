/*
Copyright (C) 2009-2011 Bengt Martensson.

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
*/

package org.harctoolbox;

/**
 *
 * @author bengt
 */
public class gateway_port {
   private String id;
   private String gateway;
   private int connectorno;
   private commandtype_t connectortype;
   private String hostname;
   private int portnumber;
   private String mac;
   private boolean wol;
   private int timeout;

   public gateway_port(String id, String gateway, int connectorno,
		       commandtype_t connectortype, String hostname,
		       int portnumber, String mac, boolean wol, int timeout) {
       this.id = id;
       this.gateway = gateway;
       this.connectorno = connectorno;
       this.connectortype = connectortype;
       this.hostname = hostname;
       this.portnumber = portnumber;
       this.mac = ((mac == null) || mac.isEmpty()) ? ethers.get_mac(hostname) : mac;
       this.wol = wol;
       this.timeout = timeout;
   }

   public String get_id() {
       return id;
   }

   public String get_gateway() {
       return gateway;
   }

   public String get_hostname() {
       return hostname;
   }

   public commandtype_t get_connectortype() {
       return connectortype;
   }

   public int get_connectorno() {
       return connectorno;
   }

   public int get_portnumber() {
       return portnumber;
   }

   public String get_mac() {
       return mac;
   }

   public int get_timeout() {
       return timeout;
   }
}
