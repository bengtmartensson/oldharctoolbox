!declaration

!define \protocol_parms(*protocolno, *deviceno, *subdevice)
((*protocolno === "00 e8") & *deviceno || " null " || *deviceno) |
if(*subdevice, (*deviceno || " " || *subdevice),  *deviceno)
  ;

!define \jp1_devicetype(*type)
((*type === "receiver") & "Amp") |
((*type === "misc_audio") & "Misc Audio") |
((*type === "tape") & "Tape") |
((*type === "ld") & "Laserdisc") |
((*type === "projector") & "TV") |
((*type === "ha") & "Home Auto") |
*type.sed("s/_/ /").uppercase()
 ;


!transformation

!begin []
source.child
;

device []
!replace "Description=" ||  self.@name || "\n",
  "DeviceType=" || \jp1_devicetype(self.@type) || "\n"
;

remote []
!replace "Remote.name=" || self.@name || "\n";

setupcode []
!replace "SetupCode=" || self.@code || "\n";

protocol []
!replace "Protocol=" || self.@number || "\n",
  "Protocol.name=" || self.@name || "\n",
  "ProtocolParms=" || \protocol_parms(self.@number, self.@deviceno, self.@subdevice)|| "\n"
  ;

notes []
!replace "Notes=" || self.r.data || "\n";

function []
!replace "Function." || self.@index || ".name=" || self.@name || "\n",
"Function." || self.@index || ".hex=" || self.@hex || "\n"
//"Function." || self.@index || ".hex=" || self.@obc || "\n"
;

button []
!replace "Button." || self.@keycode || "=" || (self.@unshifted|"null") || "|" || (self.@shifted|"null") || "|" || (self.@xshifted|"null") || "\n"
;

!default []
!replace self.gid || "=" || self.data || "\n"
;
