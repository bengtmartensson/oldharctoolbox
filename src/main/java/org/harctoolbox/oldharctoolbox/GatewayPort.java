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

package org.harctoolbox.oldharctoolbox;

import org.harctoolbox.harchardware.misc.Ethers;

/**
 *
 * @author bengt
 */
public final class GatewayPort {
   private final String id;
   private final String gateway;
   private final int connectorno;
   private final CommandType_t connectortype;
   private final String hostname;
   private final int portnumber;
   private final String mac;
   private final boolean wol;
   private final int timeout;

   public GatewayPort(String id, String gateway, int connectorno,
		       CommandType_t connectortype, String hostname,
		       int portnumber, String mac, boolean wol, int timeout) {
       this.id = id;
       this.gateway = gateway;
       this.connectorno = connectorno;
       this.connectortype = connectortype;
       this.hostname = hostname;
       this.portnumber = portnumber;
       this.mac = ((mac == null) || mac.isEmpty()) ? Ethers.getEtherAddress(hostname) : mac;
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

   public CommandType_t get_connectortype() {
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
