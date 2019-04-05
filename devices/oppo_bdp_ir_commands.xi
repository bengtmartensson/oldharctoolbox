<!DOCTYPE commandgroup PUBLIC "-//bengt-martensson.de//devices//en"
 "../dtds/devices.dtd">
    <commandgroup id="oppo_bdp_ir_commands">
      <command cmdref="power_toggle"	cmdno="26"/>
      <command cmdref="open_close"	cmdno="27"/>
      <command cmdref="setup"		cmdno="0"/>
      <command cmdref="power_on"		cmdno="90"/>
      <command cmdref="power_off"		cmdno="91"/>
      <command cmdref="source"		cmdno="17"/> <!-- Selects GUI Menu -->
      
      <command cmdref="play"		cmdno="86"/>
      <command cmdref="pause"   	cmdno="20"/>
      <command cmdref="stop"		cmdno="82" remark="one stop allows resuming with play, two irrecovably stops"/>
      <command cmdref="rewind"		cmdno="85" remark="set speed backwards"/>
      <command cmdref="fast_forward"	cmdno="81" remark="set speed forwards"/>
      <command cmdref="next"		cmdno="80"/>
      <command cmdref="previous"		cmdno="84"/>
      <!--command cmdref="slow"		cmdno="78" remark="ignored on CDs"/-->
      <command cmdref="a_b"		cmdno="77"/>
      <command cmdref="repeat"		cmdno="76"/>

      <command cmdref="title" name="Top Menu" cmdno="72"/>
      <command cmdref="menu" name="Pop-up Menu" cmdno="64"/>
      <command cmdref="cmd_goto"		cmdno="74"/>
      <command cmdref="cmd_return"	cmdno="66"/>
      <command cmdref="cmd_0"		cmdno="4"/>
      <command cmdref="cmd_1"		cmdno="11"/>
      <command cmdref="cmd_2"		cmdno="7"/>
      <command cmdref="cmd_3"		cmdno="3"/>
      <command cmdref="cmd_4"		cmdno="10"/>
      <command cmdref="cmd_5"		cmdno="6"/>
      <command cmdref="cmd_6"		cmdno="2"/>
      <command cmdref="cmd_7"		cmdno="9"/>
      <command cmdref="cmd_8"		cmdno="5"/>
      <command cmdref="cmd_9"		cmdno="1"/>
      <!--command cmdref="plus10"		cmdno="8"/-->
      <command cmdref="cancel"		cmdno="70"/>

      <command cmdref="up"		cmdno="24"/>
      <command cmdref="down"		cmdno="16"/>
      <command cmdref="left"		cmdno="87"/>
      <command cmdref="right"		cmdno="79"/>
      <command cmdref="ok" name="Enter"	cmdno="83"/>

      <command cmdref="red"		cmdno="28"/>
      <command cmdref="green"		cmdno="29"/>
      <command cmdref="blue"		cmdno="31"/>
      <command cmdref="yellow"		cmdno="30"/>

      <command cmdref="subtitle"		cmdno="75"/>
      <command cmdref="audio"		cmdno="71"/>
      <command cmdref="angle"		cmdno="73"/>
      <command cmdref="zoom"		cmdno="69"/>
      <command cmdref="SAP"		cmdno="18"/>
      <command cmdref="PIP"		cmdno="12"/>

      <command cmdref="osd" name="Display" cmdno="68"/>
      <command cmdref="home"		cmdno="92"/>
      <command cmdref="dimmer"		cmdno="95"/>
      <command cmdref="light_on" name="Light" cmdno="25"/> <!-- Function? -->
      <command cmdref="page_up"		cmdno="94"/>
      <command cmdref="page_down"		cmdno="93"/>
      <!--command cmdref="info"		cmdno="13" remark="disk to go to?"/-->
      <!--command cmdref="bookmark"		cmdno="88"/-->
      <!--command cmdref="img_logo_capture"	cmdno="89"/-->

      <command cmdref="volume_up"		cmdno="19"/>
      <command cmdref="volume_down"	cmdno="23"/>
      <command cmdref="mute_toggle"	cmdno="67"/>
      <command cmdref="audio_only" name="Pure Audio" cmdno="65"/>

      <command cmdref="pal_ntsc"		cmdno="22"/>
      <command cmdref="resolution"	cmdno="21" name="hdmi"/>
    </commandgroup>
