<!DOCTYPE commandset PUBLIC "-//bengt-martensson.de//devices//en"
 "../dtds/devices.dtd">
<!-- As far as I am aware of, there are 256 different "remotes", -->
<!-- distinguished by our device number -->

<!-- See also http://support.apple.com/kb/HT1619 for "pairing" a remote with -->
<!-- a Mac. -->
  <commandset type="ir" protocol="apple" deviceno="212" remotename="apple_remote" original_remote="">
    <command cmdref="play_pause"	name="play/pause"	cmdno="4"/>
    <command cmdref="previous"					cmdno="8"/>
    <command cmdref="next"					cmdno="7"/>
    <command cmdref="menu"					cmdno="2"/>
    <command cmdref="up"					cmdno="11"/>
    <command cmdref="down"					cmdno="13"/>
  </commandset>
