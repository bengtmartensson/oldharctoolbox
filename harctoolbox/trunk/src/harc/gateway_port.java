package harc;

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
       this.mac = mac;
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
