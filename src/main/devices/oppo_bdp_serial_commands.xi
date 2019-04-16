<!DOCTYPE commandset PUBLIC "-//bengt-martensson.de//devices//en"
 "../dtds/devices.dtd">
  <commandset type="serial" ifattribute="rs232" protocol="rs232_9600_8n1"
    prefix="#" suffix="\r">
    <commandgroup description="Command without arguments or return values.">
      <command transmit="POW" cmdref="power_toggle" wakeup="yes"/>
      <command transmit="SRC" cmdref="source"/>
      <command transmit="EJT" cmdref="open_close"/>
      <command transmit="PON" cmdref="power_on" wakeup="yes"/>
      <command transmit="POF" cmdref="power_off"/>
      <command transmit="SYS" cmdref="pal_ntsc"/>
      <command transmit="DIM" cmdref="dimmer"/>
      <command transmit="PUR" cmdref="audio_only" name="Pure Audio"/>
      <command transmit="VUP" cmdref="volume_up"/>
      <command transmit="VDN" cmdref="volume_down"/>
      <command transmit="MUT" cmdref="mute_toggle"/>
      <command transmit="NU1" cmdref="cmd_1"/>
      <command transmit="NU2" cmdref="cmd_2"/>
      <command transmit="NU3" cmdref="cmd_3"/>
      <command transmit="NU4" cmdref="cmd_4"/>
      <command transmit="NU5" cmdref="cmd_5"/>
      <command transmit="NU6" cmdref="cmd_6"/>
      <command transmit="NU7" cmdref="cmd_7"/>
      <command transmit="NU8" cmdref="cmd_8"/>
      <command transmit="NU9" cmdref="cmd_9"/>
      <command transmit="NU0" cmdref="cmd_0"/>
      <command transmit="CLR" cmdref="clear"/>
      <command transmit="GOT" cmdref="cmd_goto"/>
      <command transmit="HOM" cmdref="home"/>
      <command transmit="PUP" cmdref="page_up"/>
      <command transmit="PDN" cmdref="page_down"/>
      <command transmit="OSD" cmdref="osd" name="Display"/>
      <command transmit="TTL" cmdref="title" name="Top menu"/>
      <command transmit="MNU" cmdref="menu"/>
      <command transmit="NUP" cmdref="up"/>
      <command transmit="NLT" cmdref="left"/>
      <command transmit="NRT" cmdref="right"/>
      <command transmit="NDN" cmdref="down"/>
      <command transmit="SEL" cmdref="ok"/>
      <command transmit="SET" cmdref="setup"/>
      <command transmit="RET" cmdref="cmd_return"/>
      <command transmit="RED" cmdref="red"/>
      <command transmit="GRN" cmdref="green"/>
      <command transmit="BLU" cmdref="blue"/>
      <command transmit="YLW" cmdref="yellow"/>
      <command transmit="STP" cmdref="stop"/>
      <command transmit="PLA" cmdref="play"/>
      <command transmit="PAU" cmdref="pause"/>
      <command transmit="PRE" cmdref="previous"/>
      <command transmit="REV" cmdref="rewind"/>
      <command transmit="FWD" cmdref="fast_forward"/>
      <command transmit="NXT" cmdref="next"/>
      <command transmit="AUD" cmdref="audio"/>
      <command transmit="SUB" cmdref="subtitle"/>
      <command transmit="ANG" cmdref="angle"/>
      <command transmit="ZOM" cmdref="zoom"/>
      <command transmit="SAP" cmdref="SAP"/>
      <command transmit="ATB" cmdref="a_b"/>
      <command transmit="RPT" cmdref="repeat"/>
      <command transmit="PIP" cmdref="PIP"/>
      <command transmit="HDM" cmdref="resolution"/>
      <command transmit="SUH" cmdref="subtitle_shift"/>
      <command transmit="NOP" cmdref="nop"/>
      
      <command transmit="DPL" cmdref="direct_play"/>
      <command transmit="RST" cmdref="reset"/>

      <!--command transmit="BMK" cmdref="bookmark"/>
      <command transmit="SFD" cmdref="soundfield" type="cyclic"/>
      <command transmit="EQR" cmdref="equalizing"/>
      <command transmit="CAP" cmdref="img_logo_capture"/>
      <command transmit="BRW" cmdref="info"/>
      <command transmit="N10" cmdref="plus10"/>
      <command transmit="PLP" cmdref="play_pause"/-->

      <!--command transmit="KBD" cmdref="keyboard"/--><!-- ??? -->
      <!--command transmit="SLW" cmdref="slow"/-->
    </commandgroup>

    <commandgroup description="Commands without parameters, but with returnvalue.">
      <command transmit="QVM" cmdref="get_verbosity" remark="Query verbosity mode" response_lines="1">
	<returnvalues>
<description>
OK 0
OK 1
OK 2
OK 3</description>
	</returnvalues>
      </command>
      
      <command cmdref="get_power" transmit="QPW" remark="Query power on or off status" response_lines="1">
	<returnvalues>
	  <description>
	@OK ON
	@OK OFF
	  </description>
	</returnvalues>
      </command>
      <command cmdref="get_version" transmit="QVR" remark="Query firmware version" response_lines="1">
	<returnvalues>
	  <description>@OK BDP83-48-1224</description>
	</returnvalues>
      </command>
      <command cmdref="get_volume" transmit="QVL" remark="Query volume setting"
	response_lines="1">
	<returnvalues>
	  <description>@OK XX (XX = 00 - 100, 0 is mute)</description>
	</returnvalues>
      </command>
      <command cmdref="get_resolution" transmit="QHD" remark="Query output resolution" response_lines="1">
	<returnvalues>
	  <description>
	OK 480P
	OK AUTO
          </description>
	</returnvalues>
      </command>

     <command cmdref="get_status" transmit="QPL" remark="Query playback status" response_lines="1">
	<returnvalues>
	  <description>
OK NO DISC
OK LOADING
OK OPEN
OK CLOSE
OK PLAY
OK PAUSE
OK STOP
OK STEP
OK FREV
OK FFWD
OK SFWD
OK SREV
OK SETUP
OK HOME MENU
OK MEDIA CENTER
          </description>
	</returnvalues>
      </command>

      <command cmdref="get_title" transmit="QTK" remark="Query title playing" response_lines="1">
	<returnvalues>
	  <description>
	XX/YY
	Current title number / Total
	title number
<!-- (No longer) broken on CD -->
          </description>
	</returnvalues>
      </command>
      <command cmdref="get_chapter" transmit="QCH" remark="Query chapter playing"
	response_lines="1">
	<returnvalues>
	  <description>XX/YY
	Current chapter number / Total chapter number
</description>
	</returnvalues>
      </command>
      <command cmdref="get_elapsed_time" transmit="QTE" remark="Query title elapsed time" response_lines="1">
	<returnvalues>
	  <description>H:MM:SS</description>
	</returnvalues>
      </command>
      <command cmdref="get_remaining_time" transmit="QTR" remark="Query title remaining time"
	response_lines="1">
	<returnvalues>
	  <description>H:MM:SS</description>
	</returnvalues>

      </command>
      <command cmdref="get_chapter_elapsed_time" transmit="QCE" remark="Query chapter elapsed time"
	response_lines="1">
	<returnvalues>
	  <description>H:MM:SS</description>
	</returnvalues>
      </command>
      <command cmdref="get_chapter_remaining_time" transmit="QCR" remark="Query chapter remaining time" response_lines="1">
	<returnvalues>
	  <description>H:MM:SS</description>
	</returnvalues>
      </command>
      <command cmdref="get_total_elapsed_time" transmit="QEL" remark="Query total elapsed time"
	response_lines="1">
	<returnvalues>
	  <description>H:MM:SS</description>
	</returnvalues>
      </command>
      <command cmdref="get_total_remaining_time" transmit="QRE" remark="Query
	total remaining time" response_lines="1">
	<returnvalues>
	  <description>H:MM:SS</description>
	</returnvalues>
      </command>
      <command cmdref="get_disc_type" transmit="QDT" remark="Query disc type" response_lines="1">
	<returnvalues>
	  <description>
OK BD-MV
OK DVD-VIDEO
OK DVD-AUDIO
OK SACD
OK CDDA
OK HDCD
OK DATA-DISC
          </description>
	</returnvalues>
      </command>
      <command cmdref="get_audio_type" transmit="QAT" remark="Query audio type" response_lines="1">
	<returnvalues>
	  <description>
OK DD 1/1
OK DD 1/5 English
OK DTS 2/5 English
OK LPCM
OK DTS-HD 1/4 English
          </description>
	</returnvalues>
      </command>
      <command cmdref="get_subtitle_type" transmit="QST" remark="Query subtitle type" response_lines="1">
	<returnvalues>
	  <description>
OK OFF
OK 1/1 English
          </description>
	</returnvalues>
      </command>

      <command cmdref="get_subtitle_shift" transmit="QSH" remark="Query subtitle type" response_lines="1">
	<returnvalues>
	  <description>
OK -5
(valid returns are -5 .. 00 .. 05)
          </description>
	</returnvalues>
      </command>

      <command cmdref="get_osd_position" transmit="QOP" remark="Query OSD position" response_lines="1">
	<returnvalues>
	  <description>
OK 0
(valid returns are 0 .. 5)
          </description>
	</returnvalues>
      </command>

      <command cmdref="get_repeat" transmit="QRP" remark="Query repeat mode" response_lines="1">
	<returnvalues>
	  <description>
OK 00 Off
(OK followed by a repeat
mode code and text:
00 Off
01 Repeat One
02 Repeat Chapt.
03 Repeat All
04 Repeat Title
05 Shuffle
06 Random)
          </description>
	</returnvalues>
      </command>

      <command cmdref="get_zoom" transmit="QZM" remark="Query zoom mode" response_lines="1">
	<returnvalues>
	  <description>
OK 00 Off
(OK followed by a zoom
mode code and text:
00 Off
01 Stretch
02 Full
03 Underscan
04 1.2
05 1.3
06 1.5
07 2
08 3
09 4
10 1/2
11 1/3
12 1/4)
          </description>
	</returnvalues>
      </command>

    </commandgroup>

    <commandgroup description="Commands with a parameter.">
      <command cmdref="set_verbosity" transmit="SVM $1" remark="Set verbosity."
	response_lines="1">
	<argument name="code">
	  <description>
0 – Set Verbose Mode to off
1 – Commands are echoed back in
the response
2 – Enable unsolicited status update.
Only major status changes are
reported.
3 – Enable detailed status update.
When content is playing, the player
sends out playback time update
every second.
          </description>
	</argument>
      </command>

      <command cmdref="set_resolution" transmit="SHD $1" remark="Set output resolution." response_lines="1">
	<argument name="resolution">

	  <description>
SDI
SDP
720P
1080I
1080P
SRC
AUTO

Set HDMI output resolution.
SDI – Standard definition interlaced
(480i/576i)
SDP – Standard definition
progressive (480p/576p)
SRC – Source Direct
          </description>
	</argument>
      </command>

      <command cmdref="set_tv_system" transmit="SPN $1" remark="Set TV system
	(NTSC, PAL, AUTO)." response_lines="1">
	<argument name="tv_system">
	  <description>The parameter can be one of the followings:
	NTSC
	PAL
	AUTO
	  </description>
	</argument>
      </command>

      <command cmdref="set_zoom" transmit="SZM $1" remark="Set aspect
 (zoom) ratio." response_lines="1">
	<argument name="zoom_ratio">
	  <description>
1
AR
FS
US
1.2
1.3
1.5
2
1/2
3
4
1/3
1/4
	  </description>
	</argument>
      </command>

      <command cmdref="set_volume" transmit="SVL $1" remark="Set volume." response_lines="1">
	<argument name="volume">
	  <description>	The volume parameter can be one of the followings:
	MUTE
	00-100</description>
	</argument>
      </command>

      <command cmdref="set_repeat" transmit="SRP $1" remark="Set repeat mode." response_lines="1">
	<argument name="volume">
	  <description>
CH
TT
ALL
OFF
SHF
RND</description>
	</argument>
      </command>

      <!--command cmdref="set_forward_speed" transmit="SFF $1" remark="Fast
	forward" response_lines="1">
	<argument name="speed">
	  <description>The speed parameter can be one of the followings:
	1X 	
	2X 	
	4X 	
	8X 	
	16X 	
	32X
</description>
	</argument>
      </command-->
      <!--command cmdref="set_reverse_speed" transmit="SRV $1" remark="Fast reverse">
	<argument name="speed">
	<description>The speed parameter can be one of the followings:
	1X 	
	2X 	
	4X 	
	8X 	
	16X 	
	32X</description>
	</argument>
      </command-->

      <command cmdref="set_position" transmit="SRH $1" remark="Search to
	position." response_lines="1">
	<argument name="position">
	  <description>
	The position parameter can be one of the followings:
	TXX CYYY Search to Chapter YYY of Title XX
	TXX H:MM:SS Search to elapsed time H:MM:SS of Title XX
	CXXX H:MM:SS Search to elapsed time H:MM:SS of Chapter XXX
	TXX Search to Title XX
	CXXX Search to Chapter XXX
	H:MM:SS Search to total elapsed time H:MM:SS
	The disc may prohibit some of the search function.</description>
	</argument>
      </command>
      <command cmdref="set_regioncode" ifattribute="bluraychip_rs232"
	transmit="POF SR:$1" response_lines="2" remark="Only works in standby">
	<!-- presently not implemented -->
	<!-- expected_response="@OK OFF\n@OK REG:$1" -->
	<argument name="regioncode">

	  <description>Format: xy, where x is the Bluray zone (A, B, C), and y
the DVD region (0,1,...,9).</description>
	</argument>
	<returnvalues>
	</returnvalues>
      </command>
    </commandgroup>

    <commandgroup>
      <command cmdref="listen1" response_lines="1"/>
      <command cmdref="listen2" response_lines="2"/>
      <command cmdref="listen3" response_lines="3"/>
      <command cmdref="listen" response_lines="-1"/>
    </commandgroup>
  </commandset>
