<!DOCTYPE commandset PUBLIC "-//bengt-martensson.de//devices//en"
 "../dtds/devices.dtd">
  <commandset type="ir" protocol="nec1" deviceno="128" subdevice="255"
    remotename="coolstream" pseudo_power_on="power_toggle">
    <command cmdref="power_toggle"	name="standby"	cmdno="10" wakeup="yes"/>
    <command cmdref="mute_toggle" 	name="mute"	cmdno="13"/>
    <command cmdref="tv_radio"		name="tv"	cmdno="29"/>
    <!--command cmdref="previous"		name="prev"	cmdno="95"/-->
    <command cmdref="aspectratio"	name="pic mode"	cmdno="95"/>
    <!--command cmdref="next"				cmdno="90"/-->
    <command cmdref="aspectratio_for_4_3"				cmdno="90"/>
    <command cmdref="resolution"	name="mode"	cmdno="64"/>
    <command cmdref="satellites"			cmdno="4"/>
    <command cmdref="sleep"				cmdno="46"/><!-- presently NOP-->
    <command cmdref="audio"				cmdno="73"/> <!-- audioplayer -->
    <command cmdref="cmd_0x0f"				cmdno="15"/> <!-- help -->
    <command cmdref="cmd_0x2f"				cmdno="47"/> <!-- w -->
    <command cmdref="cmd_0x3f"				cmdno="63"/> <!-- sub -->
    <command cmdref="cmd_0x3e"				cmdno="62"/> <!-- recall -->
    <command cmdref="cmd_0x3c"				cmdno="60"/> <!-- pos -->
    <command cmdref="cmd_1"				cmdno="17"/>
    <command cmdref="cmd_2"				cmdno="18"/>
    <command cmdref="cmd_3"				cmdno="19"/>
    <command cmdref="cmd_4"				cmdno="20"/>
    <command cmdref="cmd_5"				cmdno="21"/>
    <command cmdref="cmd_6"				cmdno="22"/>
    <command cmdref="cmd_7"				cmdno="23"/>
    <command cmdref="cmd_8"				cmdno="24"/>
    <command cmdref="cmd_9"				cmdno="25"/>
    <command cmdref="cmd_0"				cmdno="16"/>
    <command cmdref="teletext"				cmdno="6"/>
    <command cmdref="favorites"				cmdno="27"/>
    <command cmdref="volume_up"				cmdno="14"/>
    <command cmdref="volume_down"			cmdno="72"/>
    <command cmdref="page_up"				cmdno="76"/>
    <command cmdref="page_down"				cmdno="7"/>

    <command cmdref="up"				cmdno="0"/>
    <command cmdref="left"				cmdno="3"/>
    <command cmdref="right"				cmdno="2"/>
    <command cmdref="down"				cmdno="1"/>
    <command cmdref="ok"				cmdno="31"/>

    <command cmdref="epg"				cmdno="30"/>
    <command cmdref="info"				cmdno="11"/>
    <command cmdref="setup"				cmdno="26"/> <!-- menu -->
    <command cmdref="home" name="exit"			cmdno="28"/>
    <command cmdref="red"				cmdno="5"/>
    <command cmdref="green"				cmdno="9"/>
    <command cmdref="yellow"				cmdno="66"/>
    <command cmdref="blue"				cmdno="67"/>
    <command cmdref="rewind"				cmdno="68"/>
    <command cmdref="play"				cmdno="70"/>
    <command cmdref="fast_forward"			cmdno="69"/>
    <command cmdref="stop"				cmdno="71"/>
    <command cmdref="record_toggle"			cmdno="89"/>
    <command cmdref="pause_toggle"			cmdno="65"/>
    <command cmdref="games_mode"			cmdno="12"/> <!-- m/s -->
    <command cmdref="timeshift"				cmdno="94"/>
  </commandset>
