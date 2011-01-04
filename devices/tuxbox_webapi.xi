<!DOCTYPE commandset PUBLIC "-//bengt-martensson.de//devices//en"
 "../dtds/devices.dtd">
    <commandset type="web_api" prefix="control/" name="Tuxbox Web API commands"
      delay_between_reps="100">
      <!-- 1. Channellist -->
      <command cmdref="get_channellist" transmit="channellist" response_lines="-1">
	<returnvalues>
	  <!-- returntype="texttable" returncontent="List of channels"-->
	  <syntax>
	    <returntype type="string"/>
	  </syntax>
	<!--returnformat>channel_id program_name</returnformat-->
	</returnvalues>
      </command>

      <!-- 2. EPG Request -->
      <command cmdref="get_current_programs" transmit="epg" response_lines="-1">
	<!-- returntype="texttable" returncontent="List of running TV or radio programs">
	<returnformat>channel_id EVENTID titel_of_current_program</returnformat-->
      </command>
      <command cmdref="get_programs_channel" transmit="epg?$1" response_lines="-1">
<!-- returntype="texttable" returncontent="List of programs on selected channel"-->
	<argument name="channel_id" semantic="Code for channel"/>
	<!--returnformat>EVENTID starttime DURATION TITLE INFO1 INFO2</returnformat-->
      </command>
      <command cmdref="get_programs_event" transmit="epg?id=$1" response_lines="-1">
	<!-- returntype="texttable" returncontent=""-->
	<argument name="event_id" semantic=""/>
      </command>
      <command cmdref="get_programs_eventid" transmit="epg?eventid=$1" response_lines="-1">
	<!-- returntype="texttable" returncontent=""-->
	<argument name="event_id" semantic=""/>
      </command>
      <command cmdref="get_current_programs" transmit="epg?ext" response_lines="-1">
	<!-- returntype="texttable" returncontent="List of running TV or radio programs">
	<returnformat>channel_id starttime duration EVENTID title_of_current_program</returnformat-->
      </command>

      <!-- 3. Shutting down -->
      <command cmdref="power_off" transmit="shutdown"
	expected_response="ok" response_lines="1"/>

      <!-- 4. Standby Mode -->
      <command cmdref="get_standby" transmit="standby" response_lines="1"/>
      <!-- returntype="on_off"/-->
      <command cmdref="standby_on" transmit="standby?on" response_lines="1"
	expected_response="ok"/>
      <command cmdref="standby_off" transmit="standby?off"
	response_lines="1" expected_response="ok"/>

      <!-- 5. Volume control -->
      <command cmdref="get_volume" transmit="volume" response_lines="1">
	<!-- returntype="percent"/-->
      </command>
      <command cmdref="set_volume" transmit="volume?$1"
	response_lines="1" expected_response="ok">
	<argument name="new_volume" semantic="Desired volume 0,5,10,15,...,95,100"/>
      </command>
      <command cmdref="get_mute" transmit="volume?status" response_lines="1"/>
      <!-- returntype="0_1" returncontent="0 unmuted, 1 muted"/-->
      <command cmdref="mute_on" transmit="volume?mute" response_lines="1" expected_response="ok"/>
      <command cmdref="mute_off" transmit="volume?unmute"
	response_lines="1" expected_response="ok"/>

      <!-- 6. Set program -->
      <command cmdref="get_program" transmit="zapto" response_lines="1"/>
      <!-- returntype="integer" returncontent="channel_id"/-->
      <command cmdref="set_program_by_id" transmit="zapto?$1" response_lines="1" expected_response="ok">
	<!-- returntype="hexadecimal_integer"-->
	<argument name="channel_id"/>
      </command>
      <command cmdref="get_program_pids" transmit="zapto?getpids"
	returntype="texttable" returncontent="pid numbers"  response_lines="-1"/>
      <command cmdref="get_program_allpids" transmit="zapto?getallpids"
	returntype="texttable" returncontent="pid numbers and descriptions"  response_lines="-1"/>
      <command cmdref="get_subchannels" transmit="zapto?getallsubchannels"
	returntype="texttable" returncontent="subchannel numbers and
	descriptions"  response_lines="-1"/>
      <command cmdref="stop" transmit="zapto?stopplayback" response_lines="1" expected_response="ok"/>
      <command cmdref="play" transmit="zapto?startplayback"  response_lines="1" expected_response="ok"/>
      <command cmdref="get_playback" transmit="zapto?statusplayback"
	returntype="0_1"  response_lines="1"/>
      <command cmdref="stop_sectionsd" transmit="zapto?stopsectionsd"  response_lines="1" expected_response="ok"/>
      <command cmdref="start_sectionsd" transmit="zapto?startsectionsd"  response_lines="1" expected_response="ok"/>
      <command cmdref="get_sectionsd" transmit="zapto?statussectionsd"
	returntype="0_1"  response_lines="1"/>
      <command cmdref="set_program_by_name"
	transmit="zapto?name=$1" expected_response="ok" response_lines="1">
	<argument name="channel_name"/>
      </command>

      <!-- 7. Radio/TV, Record Mode -->
      <command cmdref="tv_mode" transmit="setmode?tv"  response_lines="1" expected_response="ok"/>
      <command cmdref="radio_mode" transmit="setmode?radio" response_lines="1" expected_response="ok"/>
      <command cmdref="get_recordmode" transmit="setmode?status"
	returntype="on_off"  response_lines="1"/>
      <command cmdref="record_on" transmit="setmode?record=start"  response_lines="1" expected_response="ok"/>
      <command cmdref="record_off" transmit="setmode?record=stop"  response_lines="1" expected_response="ok"/>

      <!-- 8. Radio/TV Mode Inquiry -->
      <command cmdref="get_mode" transmit="getmode"
	returntype="tv_radio"  response_lines="1"/>

      <!-- 9. Date inquiry -->
      <command cmdref="get_date" transmit="getdate" returntype="dd.mm.yyyy"  response_lines="1"/>
      <!-- 10. Time inquiry -->
      <command cmdref="get_time" transmit="gettime" returntype="hh:mm:ss" response_lines="1"/>
      <command cmdref="get_rawtime" transmit="gettime?rawtime" returntype="decimal_integer" response_lines="1"/>

      <!-- 11. General information -->
      <!-- info  (not implemented, trivial) -->
      <command cmdref="get_info" transmit="info?streaminfo"
	returntype="texttable"  response_lines="-1"/>
      <command cmdref="get_version" transmit="info?version"
	returntype="texttable"  response_lines="-1"/><!-- /.version -->
      <command cmdref="get_settings" transmit="info?settings" returntype="texttable" response_lines="-1"/>
      <!-- info?httpdversion -->
      <!-- 12. Current channel inquiry -->
      <!-- "Diese Funktion sollte NICHT mehr verwendet werden und ist durch
      folgenden Aufruf ersetzt:
      http://dbox/control/zapto" -->

      <!-- 13. services.xml request -->
      <!-- 14. bouquets.xml request -->
      <!-- 15. Bouquetlist inquiry -->
      <command cmdref="get_bouquets" transmit="getbouquets"
	returntype="texttable"  response_lines="-1"/>
      <!-- 16. Bouquet inquiry -->
      <command cmdref="get_bouquetsender"
	transmit="getbouquet?bouquet=$1&amp;mode=TV"
	returntype="texttable"  response_lines="-1">
	<argument name="bouquet_nr"/>
      </command>
      <command cmdref="get_bouquetsender_radio"
	transmit="getbouquet?bouquet=$1&amp;mode=radio"
	returntype="texttable"  response_lines="-1">
	<argument name="bouquet_nr"/>
      </command>
      <!-- 17. Open popup window -->
      <command cmdref="set_popup_window" transmit="message?popup=$1" response_lines="1" expected_response="ok">
	<argument name="text"/>
      </command>
      <command cmdref="set_message_window" transmit="message?nmsg=$1" response_lines="1" expected_response="ok">
	<argument name="text"/>
      </command>
      <!-- 18. Timerd Interface -->
      <!-- 19. LCD Interface -->
      <!-- 20. Shellscript execution -->
      <command cmdref="execute_script" transmit="cgi-bin/exec?$1"  response_lines="1" expected_response="ok">
	<argument name="script" semantic="Name of script, to which '.sh' will be appended."/>
      </command>
      <!-- 21. System-/Driver functions -->

      <!-- 21. Remote control lock/unlock -->

      <!-- 23. Reboot -->
      <command cmdref="reboot" transmit="reboot" response_lines="1" expected_response="ok"/>

      <!-- 24. Request some settings -->

      <!-- 25. Return the /.versions-File -->

      <!-- 26. Plugin execute -->
      <command cmdref="execute_plugin" transmit="startplugin?name=$1" response_lines="1" expected_response="ok">
	<argument name="pluginname"/>
      </command>
      <!-- 27. Support for yweb -->

      <!-- 28. Aspektratio inquiry -->
      <command cmdref="get_aspectratio" transmit="aspectratio"
	returntype="4:3_16:9"  response_lines="1"/>
      <!-- 29. Videoformat set/get -->
      <command cmdref="get_videoformat" transmit="videoformat"
	returntype="dbox_videoformat"  response_lines="1"/>
      <command cmdref="aspectratio_automatic" transmit="videoformat?automatic"  response_lines="1" expected_response="ok"/>
      <command cmdref="aspectratio_4_3_LB" transmit="videoformat?4:3-LB" response_lines="1" expected_response="ok"/>
      <command cmdref="aspectratio_4_3_PS" transmit="videoformat?4:3-PS" response_lines="1" expected_response="ok"/>
      <command cmdref="aspectratio_16_9" transmit="videoformat?16:9" response_lines="1" expected_response="ok"/>
      <!-- 30. Videooutput set/get -->
      <command cmdref="get_videooutput" transmit="videooutput"
	returntype="dbox_videooutput" response_lines="1"/>
      <command cmdref="out_cvbs" transmit="videooutput?cvbs"  response_lines="1" expected_response="ok"/>
      <command cmdref="out_s_video" transmit="videooutput?s-video" response_lines="1" expected_response="ok"/>
      <command cmdref="out_rgb" transmit="videooutput?rgb" response_lines="1" expected_response="ok"/>
      <command cmdref="out_yuv_vbs" transmit="videooutput?yuv-vbs" response_lines="1" expected_response="ok"/>
      <command cmdref="out_yuv_cvbs" transmit="videooutput?yuv-cvbs" response_lines="1" expected_response="ok"/>

      <!-- 31. VCR-output set/get -->
      <command cmdref="get_vcroutput" transmit="videooutput" returntype="dbox_videooutput" response_lines="1"/>
      <command cmdref="out_vcr_cvbs" transmit="vcroutput?cvbs" response_lines="1" expected_response="ok"/>
      <command cmdref="out_vcr_s_video" transmit="vcroutput?s-video" response_lines="1" expected_response="ok"/>

      <!-- 31. Scart-mode set/get -->
      <command cmdref="get_scartmode" transmit="scartmode" returntype="on_off" response_lines="1"/>
      <command cmdref="scartmode_on" transmit="scartmode?on" response_lines="1" expected_response="ok"/>
      <command cmdref="scartmode_off" transmit="scartmode?off" response_lines="1" expected_response="ok"/>

      <!-- 33. Emulation of keys of the remote control -->
      <command cmdref="send_key" transmit="rcem?KEY_$1" response_lines="1" expected_response="ok">
	<argument name="name"/><!-- Uppercase!! -->
      </command>

      <command cmdref="cmd_0" transmit="rcem?KEY_0" expected_response="ok"/>
      <command cmdref="cmd_1" transmit="rcem?KEY_1" expected_response="ok"/>
      <command cmdref="cmd_2" transmit="rcem?KEY_2"/>
      <command cmdref="cmd_3" transmit="rcem?KEY_3" expected_response="ok"/>
      <command cmdref="cmd_4" transmit="rcem?KEY_4" expected_response="ok"/>
      <command cmdref="cmd_5" transmit="rcem?KEY_5" expected_response="ok"/>
      <command cmdref="cmd_6" transmit="rcem?KEY_6" expected_response="ok"/>
      <command cmdref="cmd_7" transmit="rcem?KEY_7" expected_response="ok"/>
      <command cmdref="cmd_8" transmit="rcem?KEY_8" expected_response="ok"/>
      <command cmdref="cmd_9" transmit="rcem?KEY_9" expected_response="ok"/>
      <command cmdref="right" transmit="rcem?KEY_RIGHT" expected_response="ok"/>
      <command cmdref="left" transmit="rcem?KEY_LEFT" expected_response="ok"/>
      <command cmdref="up" transmit="rcem?KEY_UP" expected_response="ok"/>
      <command cmdref="down" transmit="rcem?KEY_DOWN" expected_response="ok"/>
      <command cmdref="ok" transmit="rcem?KEY_OK" expected_response="ok"/>
      <command cmdref="mute_toggle" transmit="rcem?KEY_MUTE" expected_response="ok"/>
      <command cmdref="power_toggle" transmit="rcem?KEY_POWER" expected_response="ok"/>
      <command cmdref="green" transmit="rcem?KEY_GREEN" expected_response="ok"/>
      <command cmdref="yellow" transmit="rcem?KEY_YELLOW" expected_response="ok"/>
      <command cmdref="red" transmit="rcem?KEY_RED" expected_response="ok"/>
      <command cmdref="blue" transmit="rcem?KEY_BLUE" expected_response="ok"/>
      <command cmdref="volume_up" transmit="rcem?KEY_VOLUMEUP" expected_response="ok"/>
      <command cmdref="volume_down" transmit="rcem?KEY_VOLUMEDOWN" expected_response="ok"/>
      <command cmdref="info" name="?" transmit="rcem?KEY_HELP" expected_response="ok"/>
      <command cmdref="setup" name="dBox" transmit="rcem?KEY_SETUP" expected_response="ok"/>
      <command cmdref="home" transmit="rcem?KEY_HOME" expected_response="ok"/>
      <command cmdref="page_down" transmit="rcem?KEY_PAGEDOWN" expected_response="ok"/>
      <command cmdref="page_up" transmit="rcem?KEY_PAGEUP" expected_response="ok"/>
      <command cmdref="topleft" transmit="rcem?KEY_TOPLEFT" expected_response="ok"/>
      <command cmdref="topright" transmit="rcem?KEY_TOPRIGHT" expected_response="ok"/>
      <command cmdref="bottomleft" transmit="rcem?KEY_BOTTOMLEFT" expected_response="ok"/>
      <command cmdref="bottomright" transmit="rcem?KEY_BOTTOMRIGHT" expected_response="ok"/>

      <!-- 34. Bouquet-Editor: set attribute -->

      <!-- 35. Bouquet-Editor: save bouquet-liste -->
      
      <!-- 36. Bouquet-Editor: reorder bouquet -->

      <!-- 37. Bouquet-Editor: delete bouquet -->

      <!-- 38. Bouquet-Editor: add bouquet -->

      <!-- 39. Bouquet-Editor: rename bouquet -->

      <!-- 40. Bouquet-Editor: alter channels -->

      <!-- 41. Bouquet-Editor: refresh bouquet channels -->

      <!-- 42. Create/change URL for Live-Streaming (VLC) -->
    </commandset>
