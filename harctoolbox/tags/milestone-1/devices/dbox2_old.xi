<!DOCTYPE commandset PUBLIC "-//bengt-martensson.de//devices//en"
 "../dtds/devices.dtd">
<commandset type="ir" protocol="nrc17" deviceno="12" subdevice="5" toggle="no" name="dBox old protocol" remotename="dbox2_old" pseudo_power_on="home">
  <!-- called NRC17,  dev 92 Misc (7)by decodeIR -->
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
  <command cmdref="right" 	wakeup="no"  cmdno="0x2e"/>
  <command cmdref="left"  	wakeup="no"  cmdno="0x2f"/>
  <command cmdref="up"    	wakeup="no"  cmdno="0x0e"/>
  <command cmdref="down"  	wakeup="no"  cmdno="0x0f"/>
  <command cmdref="ok"    	wakeup="yes" cmdno="0x30"/>
  <command cmdref="mute_toggle" wakeup="no" cmdno="0x28"/>
  <command cmdref="power_toggle" wakeup="yes" cmdno="0x0c"/>
  <command cmdref="green" 	wakeup="no"  cmdno="0x55"/>
  <command cmdref="yellow" 	wakeup="no"  cmdno="0x52"/>
  <command cmdref="red"   	wakeup="no"  cmdno="0x2d"/>
  <command cmdref="blue"  	wakeup="no"  cmdno="0x3b"/>
  <command cmdref="volume_up" 	wakeup="no"  cmdno="0x16"/>
  <command cmdref="volume_down" wakeup="no"  cmdno="0x17"/>
  <command cmdref="info"  	wakeup="no"  cmdno="0x82"/>
  <command cmdref="setup" 	wakeup="no"  cmdno="0x27"/>
  <command cmdref="home"  	wakeup="yes" cmdno="0x20"/>

  <!-- The "double arrows" (?) on very old remotes -->
  <command cmdref="page_down" 	wakeup="yes" cmdno="0x53"/>
  <command cmdref="page_up" 	wakeup="yes" cmdno="0x54"/>

  <!-- To use these (other than the first one) modify
  dbox2_fp_rc.c, line 71-74. -->
  <command cmdref="topleft" 	wakeup="yes" cmdno="0xff"/>
  <command cmdref="topright" 	wakeup="yes" cmdno="0xfb"/>
  <command cmdref="bottomleft" 	wakeup="yes" cmdno="0xfd"/>
  <command cmdref="bottomright" wakeup="yes" cmdno="0xfc"/>
</commandset>
