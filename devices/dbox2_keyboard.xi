<!DOCTYPE commandset PUBLIC "-//bengt-martensson.de//devices//en"
 "../dtds/devices.dtd">
<commandset type="ir" deviceno="7" toggle="no">
  <!-- Keyboard -->
  <!-- DecodeIR says protocol="nokia12" with varying device numbers, but -->
  <!-- with same devicenumber and command number for different keys.-->
  <!-- So it is obviously wrong.-->

  <!-- Upper row -->
  <command cmdref="key_esc" />
  <command cmdref="key_f1"/>
  <command cmdref="key_f2"/>
  <command cmdref="key_f3"/>
  <command cmdref="key_f4"/>
  <command cmdref="key_f5"/>
  <command cmdref="key_f6"/>
  <command cmdref="key_f7"/>
  <command cmdref="key_f8"/>
  <command cmdref="key_f9"/>
  <command cmdref="key_f10"/>
  <command cmdref="key_numlock"/>
  <command cmdref="key_sysrq"/>
  <command cmdref="key_scrolllock"/>
  <command cmdref="key_pause"/>
  <command cmdref="key_insert"/>
  <command cmdref="key_delete"/>

  <!-- second row -->
  <command cmdref="key_1"/>
  <command cmdref="key_2"/>
  <command cmdref="key_3"/>
  <command cmdref="key_4"/>
  <command cmdref="key_5"/>
  <command cmdref="key_6"/>
  <command cmdref="key_7"/>
  <command cmdref="key_8"/>
  <command cmdref="key_9"/>
  <command cmdref="key_0"/>
  <command cmdref="key_minus"/>
  <command cmdref="key_equal"/>
  <command cmdref="key_backspace"/>
  <command cmdref="key_home"/>

  <!-- third row -->
  <command cmdref="key_tab"/>
  <command cmdref="key_q"/>
  <command cmdref="key_w"/>
  <command cmdref="key_e"/>
  <command cmdref="key_t"/>
  <command cmdref="key_y"/>
  <command cmdref="key_u"/>
  <command cmdref="key_i"/>
  <command cmdref="key_o"/>
  <command cmdref="key_p"/>
  <command cmdref="key_leftbrace"/>
  <command cmdref="key_rightbrace"/>
  <command cmdref="key_backslash"/>
  <command cmdref="key_pageup"/>

  <!--forth row -->
  <command cmdref="key_capslock"/>
  <command cmdref="key_a"/>
  <command cmdref="key_s"/>
  <command cmdref="key_d"/>
  <command cmdref="key_f"/>
  <command cmdref="key_g"/>
  <command cmdref="key_h"/>
  <command cmdref="key_j"/>
  <command cmdref="key_k"/>
  <command cmdref="key_l"/>
  <command cmdref="key_semicolon"/>
  <command cmdref="key_apostrophe"/>
  <command cmdref="key_enter"/>
  <command cmdref="key_pagedown"/>

  <!-- fifth row -->
  <command cmdref="key_leftshift"/>
  <command cmdref="key_z"/>
  <command cmdref="key_x"/>
  <command cmdref="key_c"/>
  <command cmdref="key_v"/>
  <command cmdref="key_b"/>
  <command cmdref="key_n"/>
  <command cmdref="key_m"/>
  <command cmdref="key_comma"/>
  <command cmdref="key_dot"/>
  <command cmdref="key_slash"/>
  <command cmdref="key_rightshift"/>
  <command cmdref="key_up"/>
  <command cmdref="key_end"/>

  <!-- sixth row -->
  <command cmdref="key_leftctrl"/>
  <command cmdref="key_leftmeta"/>
  <command cmdref="key_leftalt"/>
  <command cmdref="key_space"/>
  <command cmdref="key_102nd"/>
  <command cmdref="key_grave"/>
  <command cmdref="key_rightalt"/>
  <command cmdref="key_rightmeta"/>
  <command cmdref="key_left"/>
  <command cmdref="key_down"/>
  <command cmdref="key_right"/>

  <!-- other -->
  <command cmdref="key_btnleft" cmdno="8"/>
<!--
0000 006F 0000 0010 000F 000B 0006 0017 0006 000B 0006 0011 0006 001E 0006 001E 0006 001E 0006 17A2 000F 000B 0006 0017 0006 000B 0006 0011 0006 001E 0006 001E 0006 001E 0006 1775
-->
  <command cmdref="key_btnright"/>
</commandset>

