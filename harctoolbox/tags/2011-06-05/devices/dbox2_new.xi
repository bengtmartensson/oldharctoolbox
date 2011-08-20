<!DOCTYPE commandset PUBLIC "-//bengt-martensson.de//devices//en"
 "../dtds/devices.dtd">
<commandset type="ir" protocol="nokia" deviceno="13" subdevice="64" toggle="no" name="dBox new protocol" remotename="dbox2_new" pseudo_power_on="ok">
<!-- Bits 6-7 of the command no makes up sort of a 4-state toggle
 (on a Sagem remote) (these appears to be ignored however),
 bit 5 is always 0, but also 1 works (masked away). -->
  <command cmdref="cmd_0" 	wakeup="yes" cmdno="0x00"/>
  <command cmdref="cmd_1"     	wakeup="yes" cmdno="0x01"/>
  <command cmdref="cmd_2"     	wakeup="yes" cmdno="0x02"/>
  <command cmdref="cmd_3"     	wakeup="yes" cmdno="0x03"/>
  <command cmdref="cmd_4"     	wakeup="yes" cmdno="0x04"/>
  <command cmdref="cmd_5"     	wakeup="yes" cmdno="0x05"/>
  <command cmdref="cmd_6"     	wakeup="yes" cmdno="0x06"/>
  <command cmdref="cmd_7"     	wakeup="yes" cmdno="0x07"/>
  <command cmdref="cmd_8"     	wakeup="yes" cmdno="0x08"/>
  <command cmdref="cmd_9"     	wakeup="yes" cmdno="0x09"/>
  <command cmdref="right"     	wakeup="no"  cmdno="0x0a"/>
  <command cmdref="left"      	wakeup="no"  cmdno="0x0b"/>
  <command cmdref="up"        	wakeup="no"  cmdno="0x0c"/>
  <command cmdref="down"      	wakeup="no"  cmdno="0x0d"/>
  <command cmdref="ok"        	wakeup="yes" cmdno="0x0e"/>
  <command cmdref="mute_toggle"	wakeup="no"  cmdno="0x0f"/>
  <command cmdref="power_toggle" wakeup="yes" cmdno="0x10"/>
  <command cmdref="green"     	wakeup="no"  cmdno="0x11"/>
  <command cmdref="yellow"    	wakeup="no"  cmdno="0x12"/>
  <command cmdref="red"       	wakeup="no"  cmdno="0x13"/>
  <command cmdref="blue"      	wakeup="no"  cmdno="0x14"/>
  <command cmdref="volume_up" 	wakeup="no"  cmdno="0x15"/>
  <command cmdref="volume_down" wakeup="no"  cmdno="0x16"/>
  <command cmdref="info"      	wakeup="no"  cmdno="0x17"/>
  <command cmdref="setup"     	wakeup="yes" cmdno="0x18"/>
  <command cmdref="home"      	wakeup="no"  cmdno="0x1f"/>

  <!-- The "double arrows" (?) on very old remotes -->
  <command cmdref="page_down" 	wakeup="no"  cmdno="0x19"/>
  <command cmdref="page_up"   	wakeup="no"  cmdno="0x1a"/>

  <!-- Not present on most remotes. -->
  <command cmdref="topleft"   	wakeup="no"  cmdno="0x1b"/>
  <command cmdref="topright"  	wakeup="no"  cmdno="0x1c"/>
  <command cmdref="bottomleft" 	wakeup="no"  cmdno="0x1d"/>
  <command cmdref="bottomright" wakeup="no"  cmdno="0x1e"/>
</commandset>
