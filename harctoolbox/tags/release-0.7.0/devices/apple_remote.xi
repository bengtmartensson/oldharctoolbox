<!DOCTYPE commandset PUBLIC "-//bengt-martensson.de//devices//en"
 "../dtds/devices.dtd">
<!-- As far as I am aware of, there are 256 different "remotes", -->
<!-- distinguished by the PairID number -->

<!-- See also http://support.apple.com/kb/HT1619 for "pairing" a remote with -->
<!-- a Mac. -->
  <commandset type="ir" protocol="apple" additional_parameters="PairID=212" remotename="apple_remote" original_remote="">
    <command cmdref="play_pause"	name="play/pause"	cmdno="2"/>
    <command cmdref="previous"					cmdno="4"/>
    <command cmdref="next"					cmdno="3"/>
    <command cmdref="menu"					cmdno="1"/>
    <command cmdref="up"					cmdno="5"/>
    <command cmdref="down"					cmdno="6"/>
  </commandset>
