# -*- coding: iso-8859-1 -*-
import sys
import time
import subprocess
import string
import socket
import org.harctoolbox.oldharctoolbox.ProtocolDataBase
import org.harctoolbox.oldharctoolbox.Home
import org.harctoolbox.oldharctoolbox.command_t
import org.harctoolbox.oldharctoolbox.CommandType_t
import org.harctoolbox.oldharctoolbox.MediaType

__doc__ = "Macros for cat confusing"

srcdevice = ""
zone2_srcdevice = ""
zone2_status = ""
zone3_srcdevice = ""
zone3_status = ""
bedroom_srcdevice = ""
hometheatre_outputdevice = ""
dbox_channel_list = {}
dbox_current_programs = {}
default_max_trys = 40
default_tuxbox='coolstream'

def get_trace():
    return _jython_trace

def set_trace(arg=True):
    global _jython_trace
    _jython_trace = arg
    return True

def _trace(function, arg, message):
    global _jython_trace
    if _jython_trace:
        print '[' + function + '(' + arg + ')] ' + message;

# Issue a hint to a remote panel that it is to jump to a particular page,
# if possible and wanted
def paneljump(panelname):
    print "paneljump to " + panelname + " not yet implemented."

def devices_command(devices, command, count = 1):
    """To all entries in DEVICES (a list of strings), the command COMMAND is sent."""
    return map(lambda d: hm.do_command(d, command, count), devices)

def devices_command_delay(devices, command, delay):
    """To all entries in DEVICES (a list of strings), the command COMMAND is sent."""
    for d in devices:
        hm.do_command(d, command)
        time.sleep(delay/1000.0)

def devicegroup_command(devicegroup, command, count = 1):
    devs=get_device_group(devicegroup)
    if devs==None:
        print "\adevicegroup \"" + devicegroup + "\" has no members"
        return False
    else:
        return devices_command(devs, command, count)

def devicegroup_command_delay(devicegroup, command, delay):
    devs=get_device_group(devicegroup)
    if devs==None:
        print "\adevicegroup \"" + devicegroup + "\" has no members"
        return False
    else:
        return devices_command_delay(devs, command, delay)

def get_alias(device):
    return str(hm.expand_alias(device))

def get_device_group(devicegroup):
    return hm.get_devices(devicegroup)

def get_devices():
    """Returns all devices in the home."""
    return hm.get_devices()

def print_device_group(devicegroup):
    devs = get_device_group(devicegroup)
    for d in devs:
        print str(d)

def print_protocols():
    prt = org.harctoolbox.oldharctoolbox.Protocol.get_protocols()
    for p in prt:
        print str(p)

def ping_all():
    not_pingables = []
    ons = []
    offs = []
    for d in get_devices():
        if hm.has_command(d, org.harctoolbox.oldharctoolbox.commandtype_t.any, org.harctoolbox.oldharctoolbox.command_t.ping):
            if device_command(d, 'ping'):
                ons.append(d)
            else:
                offs.append(d)
        else:
            not_pingables.append(d)

    #print 'Not pingable:',  not_pingables
    print "Alive:", ons
    print "Not alive:", offs
    return str([ ons, offs ])

def ping():
    return "alive and well"

#def use_analog_video(device):
#    return device in ['sat', 'dbox', 'vcr', 'vr1100']

#def video_connection_type(device):
#    return "yuv" if use_analog_video(device) else "hdmi"
def video_connection_type(device):
    return "hdmi"

def hometheatre_unused_sources_off():
    global srcdevice
    set_srcdevice()
    srcs = hm.get_devices("Hometheatre sources")
    map(lambda dev: hm.get_canonical_name(dev) == srcdevice or assure_off(dev), srcs)
    return ""

def devices_assure_on(devices):
    for d in devices:
        assure_on(d)
    return ""

def devicegroup_assure_on(devicegroup):
    devices_assure_on(get_device_group(devicegroup))

def wait_delay(device, deltype, default):
    del_ms=hm.get_delay(device, deltype)
    if del_ms == -1:
        del_ms=default
    _trace('wait_delay', deltype + '=' + str(default), "Waiting for " + str(del_ms) + " ms")
    time.sleep(del_ms/1000.0)

def assure_on(device, wait=False, max_trys=default_max_trys):
    """Assure that the device is turned on; if second argument true, waits for it to become ready."""

    # make it safe to call with null arg
    if not device:
        return False
    assure_on(hm.get_powered_through(device), wait)
    stat=is_on(device)
    if stat and stat != "unknown":
        # Device is on, nothing to do.
        _trace('assure_on', device, "Already on")
        return True
    
    elif stat == False or stat is None:
        # Device is off, is_on is working; turn on, possibly wait
        if hm.has_command(device, org.harctoolbox.oldharctoolbox.commandtype_t.ip, org.harctoolbox.oldharctoolbox.command_t.wol):
            device_command(device, org.harctoolbox.oldharctoolbox.command_t.wol)
        elif hm.has_command(device, org.harctoolbox.oldharctoolbox.commandtype_t.any, org.harctoolbox.oldharctoolbox.command_t.power_on):
            _trace('assure_on', device, "discret power on present, using that")
            hm.do_command(device, org.harctoolbox.oldharctoolbox.command_t.power_on, 1)
        else:
            _trace('assure_on', device, "discret power on not present, but known state (off)")
            device_command(device, org.harctoolbox.oldharctoolbox.command_t.power_toggle)

        if wait:
            i = 0;
            while i < max_trys and not is_on(device):
                i = i + 1
                _trace('assure_on', device, "waiting for device to come on [" + str(i) + "]")
                device_command(device, org.harctoolbox.oldharctoolbox.command_t.wol) or device_command(device, org.harctoolbox.oldharctoolbox.command_t.power_on)
                time.sleep(1)

            success = is_on(device)
            _trace('assure_on', device, ('succeded' if success else
                                         'giving up after ' + str(max_trys) + ' attempts'))
            return success

        return True

    else:
        # Unknown state, use discrete command if present.
        _trace('assure_on', device, "unknown state...")
        if hm.has_command(device, org.harctoolbox.oldharctoolbox.commandtype_t.ip, org.harctoolbox.oldharctoolbox.command_t.wol):
            device_command(device, org.harctoolbox.oldharctoolbox.command_t.wol)
            if wait:
                wait_delay(device, "from_standby", 1000)
            return True
        elif hm.has_command(device, org.harctoolbox.oldharctoolbox.commandtype_t.any, org.harctoolbox.oldharctoolbox.command_t.power_on):
            _trace('assure_on', device, "discret power on present, using that")
            hm.do_command(device, org.harctoolbox.oldharctoolbox.command_t.power_on, 1)
            if wait:
                wait_delay(device, "from_standby", 1000)
            return True
        else:
            _trace('assure_on', device, "no discret power, unknown state, giving up")
            return False

def assure_off_devices(devices):
    for d in devices:
        assure_off(d)

def assure_off(device):
    """Assure that the device is turned off."""
    stat=is_on(device)
    if stat and stat != "unknown":
        if hm.has_command(device, org.harctoolbox.oldharctoolbox.commandtype_t.any, org.harctoolbox.oldharctoolbox.command_t.power_off):
            _trace('assure_off', device, 'discret power off present, using that')
            hm.do_command(device, org.harctoolbox.oldharctoolbox.command_t.power_off, 1)
        else:
            _trace('assure_off', device, 'discret power on not found, but known state (on)')
            device_command(device, org.harctoolbox.oldharctoolbox.command_t.power_toggle)

        return True
    elif stat == False or stat is None:
        _trace('assure_off', device, 'Already off')
        return True
    else:
        _trace('assure_off', device, 'unknown state...')
        if hm.has_command(device, org.harctoolbox.oldharctoolbox.commandtype_t.any, org.harctoolbox.oldharctoolbox.command_t.power_off):
            _trace('assure_off', device, 'discret power off present, using that')
            hm.do_command(device, org.harctoolbox.oldharctoolbox.command_t.power_off, 1)
        else:
            _trace('assure_off', device, 'no discret power, unknown state, giving up')
        return False

def is_on(device):
    dev=hm.get_device(device)
    if hm.has_command(device, org.harctoolbox.oldharctoolbox.commandtype_t.ip, org.harctoolbox.oldharctoolbox.command_t.ping) and dev.get_pingable_on() and not dev.get_pingable_standby():
        return hm.do_command(device, org.harctoolbox.oldharctoolbox.command_t.ping, 1) == ""
    elif hm.has_command(device, org.harctoolbox.oldharctoolbox.commandtype_t.any, org.harctoolbox.oldharctoolbox.command_t.get_power):
        response = device_command(device, org.harctoolbox.oldharctoolbox.command_t.get_power)
        return "unknown" if (response is None) else (device_command(device, org.harctoolbox.oldharctoolbox.command_t.get_power).find("ON") != -1)
    elif hm.has_command(device, org.harctoolbox.oldharctoolbox.commandtype_t.any, org.harctoolbox.oldharctoolbox.command_t.get_status_power):
        response = device_command(device, org.harctoolbox.oldharctoolbox.command_t.get_status_power)
        return response == '1' or response == '2' or response == '3'
    else:
        return "unknown"

def print_devices():
    for d in hm.get_devices():
        print str(d)

def is_valid_command(command):
    return org.harctoolbox.oldharctoolbox.command_t.is_valid(command)

# Python does not distinguish between Whizzo butter and a dead crab, sorry,
# between "" and false; while Harc does. Fix.
def device_command(device, command):
    res = hm.do_command(device, command, 1)
    return res == '' or res


#def device_command_n(device, command, n):
#    res = hm.do_command(device, command, n)
#    return res == "" or res

def device_command_n(device, command, arg1, arg2=None):
    if arg2 is None:
        res = hm.do_command(device, command, arg1)
    else:
        res = hm.do_command(device, command, arg1, arg2)
    return res == "" or res

def set_verbose(true_false=True):
    org.harctoolbox.oldharctoolbox.Main.getPreferences().setVerbose(true_false)
    return True

def set_verbose_on():
    return set_verbose(True)

def set_verbose_off():
    return set_verbose(False)

def set_debug(arg):
    org.harctoolbox.oldharctoolbox.Main.getPreferences().setDebug(arg)

# Turn on tracing and verboseness on my development machine, not for deployment
_jython_trace = socket.gethostname() == 'epsilon'
#set_verbose(_jython_trace)

def louder():
    """Turn up the volume slightly."""
    device_command_n("amp", "volume_up", 10)

def quieter():
    """Turn down the volume slightly."""
    device_command_n("amp", "volume_down", 10)

def denon_set_volume(db):
    return hm.do_command("amp", "set_volume", '%02d' % (80+db))

def set_volume(db):
    return denon_set_volume(db)

def set_volume_zone(db, zone):
    return hm.do_command("amp", "set_zone" + str(zone) + "_volume", '%02d' % (80+db))

# TODO: str(...) presently necessary, fix somewhere else?
def denon_get_volume():
    response=str(hm.do_command("amp", "get_volume"))[2:]
    x=int(response)
    return "--" if x == 99 else str((x if len(response) == 2 else x/10.0) - 80.0)

def get_volume():
    return denon_get_volume()

# Note: there is no command for getting volume for zone2/3.

def volume_loud():
    """Somewhat loud (-20 dB)"""
    denon_set_volume(-20)

def volume_normal():
    """-27 dB"""
    denon_set_volume(-27)

def volume_room():
    """-35 dB"""
    denon_set_volume(-35)

def volume_quiet():
    """-42 dB"""
    denon_set_volume(-42)

def volume_feeble():
    """Very quiet (-50 dB)"""
    denon_set_volume(-50)

def volume_faint():
    """Barely audible (-65 dB)"""
    denon_set_volume(-65)

def volume_standard():
    """"Standard" volume, whatever that means"""
    volume_room()


def desk_amp_on():
    """Turn on the desk amplifier"""
    device_command("desk_amp", "power_on")

def desk_amp_off():
    """Turn off the desk amplifier"""
    device_command("desk_amp", "power_off")

def bedroom_amp_on():
    """Turn on the bedroom amplifier"""
    device_command("bedroom_amp", "power_on")

def bedroom_amp_off():
    """Turn off the bedroom amplifier"""
    device_command("bedroom_amp", "power_off")

def volume_mute_on():
    """Master mute on"""
    device_command("amp", "mute_on")

def volume_mute_off():
    """Master mute off"""
    device_command("amp", "mute_off")

def get_mute():
    return device_command("amp", "get_mute") == "MUON"

#   <macros name="Complex-AV-macros">
#    """Actions involving several components"""

def assure_tuxbox_ready(device, max_trys=default_max_trys):
    i = 0
    while i < max_trys and not device_command(device, 'get_time'):
        i = i + 1
        _trace('assure_tuxbox_ready', device, 'Waiting for a tuxbox ' + device + ' to get ready [' + str(i) + ']')
        device_command(device, 'power_on')
    _trace('assure_tuxbox_ready', device, 'succeded' if i < max_trys else ('failed after ' + str(max_trys) + ' attempts'))
    return "" if i <= max_trys else False

def start_dbox_tv(dev=default_tuxbox):
    assure_on('amp')
    assure_on(dev, True)
    assure_on('tv', True)
    assure_tuxbox_ready(dev)
    assure_on('tv', True)
    watch_dbox(dev)

def pause_dvd_watch_dbox(player='dvd'):
    if player == 'dvd':
        oppo_pause_on()
    watch_dbox()

def start_dvd_tv():
    assure_on('amp')
    assure_on('dvd', True)
    assure_on('tv', True)
    watch_dvd()

def start():
    """Start up the system"""
    assure_on('amp')

def shutdown_main():
    """Shuts down the system"""
    assure_off_devices(["projector", "dvd", "dbox", "coolstream", "hddvd", "oplay", "tv"])
    #poweroff_popcorn()
    # TODO: shutdown ferb (eventghost)
    time.sleep(10) # Give the disk players a chance
    assure_off("amp")

def shutdown_desk():
    assure_off('desk_amp')

def shutdown_bedroom():
    assure_off('bedroom_amp')

def shutdown(zone='all'):
    if zone in ['1', 'zone1', 'main', 'all']:
        shutdown_main()
    if zone in ['2', 'zone2', 'bedroom', 'all']:
        shutdown_bedroom()
    if zone in ['3', 'zone3', 'desk', 'all']:
        shutdown_desk()

def denon_restart():
    assure_off_devices(["dvd", "uhd-bluray"])
    assure_off("amp")
    time.sleep(10)
    assure_on("amp")

################################################################
# Make sure to keep srcdevice "canonical", i.e. to use the name given as 
# the "canonical" attribute.
def gplay():
    if not srcdevice:
        set_srcdevice()

    if srcdevice in ['sat', 'tv']:
        gunmute()
    elif hm.do_command(srcdevice, 'play') != None:
        pass
    elif hm.do_command(srcdevice, 'play_pause') != None:
        pass
    else:
        print 'Do not know how to play on device ' + srcdevice
        return False
    return True
    
gplay_pause_state=True

def gplay_pause():
    global gplay_pause_state
    if not srcdevice:
        set_srcdevice()

    if srcdevice in ['sat', 'tv', 'cable']:
        return gmute_toggle()
    elif hm.do_command(srcdevice, 'play_pause') != None:
        return true
    elif srcdevice == 'dvd':
        return hm.do_command(srcdevice, ('pause_toggle' if oppo_isplay() else 'play'))
    else:
        gplay_pause_state = not gplay_pause_state
        return hm.do_command(srcdevice, 'pause_toggle' if gplay_pause_state else 'play')
    
def gmute():
    hm.do_command("amp", "mute_on")

def gunmute():
    hm.do_command("amp", "mute_off")

def gmute_toggle():
    hm.do_command("amp", "mute_toggle")

def gpause_on():
    global srcdevice
    if not srcdevice:
        set_srcdevice()
    if srcdevice=="sat":
        gmute()
    elif srcdevice=="dvd":
        oppo_pause_on()
    elif hm.do_command(srcdevice, 'pause_on') != None:
        pass
    elif hm.do_command(srcdevice, 'pause_toggle') != None:
        pass
    elif hm.do_command(srcdevice, 'stop') != None:
        pass
    elif hm.do_command(srcdevice, 'play_pause') != None:
        pass
    else:
        _trace('gpause_on', "", 'Do not know how to pause ' + srcdevice + ' muting')
        gmute();
        return False
    return True

def gpause_off():
    global srcdevice
    set_srcdevice()
    if srcdevice=="sat" or srcdevice=="cable":
        gunmute()
    elif srcdevice=="dvd":
        oppo_pause_off()
    elif hm.do_command(srcdevice, 'pause_off') != None:
        pass
    elif hm.do_command(srcdevice, 'pause_toggle') != None:
        pass
    elif hm.do_command(srcdevice, 'play') != None:
        pass
    elif hm.do_command(srcdevice, 'play_pause') != None:
        pass
    else:
        _trace('gpause_off', '', 'Do not know how to unpause ' + srcdevice)
        return False
    return True

def gcommand(command):
    global srcdevice
    if not srcdevice:
        set_srcdevice()
    return hm.do_command(srcdevice, command) != None

def gopen_close():
    return gcommand('open_close')

def gup():
    return gcommand('up')

def gdown():
    return gcommand('down')

def gleft():
    return gcommand('left')

def gright():
    return gcommand('right')

def gok():
    return gcommand('ok')

def gfast_forward():
    return gcommand('fast_forward')

def grewind():
    return gcommand('rewind')

def gnext():
    return gcommand("next") or gcommand("channel_up") or gcommand("up")

def gprevious():
    return gcommand("previous") or gcommand("channel_down") or gcommand("down")

def ghas_command(cmd):
    if not srcdevice:
        set_srcdevice()
    return hm.has_command(srcdevice, cmd)

def gother_commands():
    if not srcdevice:
        set_srcdevice()
    return other_commands(srcdevice)

################################################################
def other_commands(device):
    # Heuristic: Return only IR commands
    return netremote_string(hm.get_commands(device, org.harctoolbox.oldharctoolbox.commandtype_t.ir))

def netremote_string(cmds):
    return '\n'.join(cmds)

######################################## "mode-change-macros"

def projector_mode():
    """Turn on projector, and use it"""
    device_command("screen", "power_on")
    # TODO: replace by assure_on('projector')
    device_command("projector", "power_on")
    #setup_dbox_projector() # FIXME
    assure_off("tv")

def tv_mode(raise_screen=False):
    """Turn on TV, and use it"""
    global srcdevice
    if raise_screen:
        device_command("screen", "power_off")
    assure_on("tv", False)
    assure_off("projector")
    setup_dbox_tv() # FIXME
    assure_on("tv", True)
    set_srcdevice()
    hm.select('tv', 'amp', org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, video_connection_type(srcdevice))
    return ""

#def tv_mode_screen_up():
#    """Turn on TV, and use it. Screen up"""
#    device_command("screen", "power_off")
#    return tv_mode()

#         <macros name="Bedroom-commands():
# def bedroom_speakers_a():
#     """Turn on bedroom speaker pair A exclusively"""
#     device_command("yamaha_rxv1400", "spk_a_on")
#     device_command("yamaha_rxv1400", "spk_b_off")

# def bedroom_speakers_b():
#     """Turn on bedroom speaker pair B exclusively"""
#     device_command("yamaha_rxv1400", "spk_b_on")
#     device_command("yamaha_rxv1400", "spk_a_off")

# def bedroom_select_device(device):
#     global bedroom_srcdevice
#     assure_on("yamaha_rxv1400")
#     assure_on(device, True)
#     hm.select('yamaha_rxv1400', device, org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, None)
#     return ""

#     <macros name="AV-output-macros">
#         <macros name="tv-macros():

def tv_unless_projector(wait=False):
    if not projector_ison():
        assure_on("tv", wait)

def projector_ison():
    """Returns true if the projector is on"""
    global hometheatre_outputdevice
    stat=device_command("projector", "get_status_power")
    result = stat=='1' or stat=='2' or stat=='3'
    hometheatre_outputdevice = "projector" if result else "tv"
    return result

def listen_tuner():
    global srcdevice
    """Listen to the tuner"""
    device_command("amp", "in_tuner")# toggles fm/am, grrr
    device_command("amp", "fm")
    srcdevice="tuner"
    paneljump("tuner")
    return ""

def watch_tuner():
    return listen_tuner()

def listen_net_usb():
    global srcdevice
    """Listen to net/usb"""
    device_command("amp", "in_net_usb")
    srcdevice="net_usb"
    paneljump("net_usb")
    return ""

def watch_net_usb():
    return listen_net_usb()

def listen_htpc():
    return listen_device('htpc')

def look_htpc():
    global srcdevice
    assure_on("tv")
    assure_on("htpc", True)
    #hm.select('tv', 'htpc', org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, org.harctoolbox.oldharctoolbox.ConnectionType.hdmi)
    #hm.select('tv', 'amp', org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, org.harctoolbox.oldharctoolbox.ConnectionType.hdmi)
    srcdevice = hm.get_canonical_name("htpc")
    paneljump("htpc")

def watch_htpc():
    listen_htpc()
    return look_htpc()

def listen_device(device):
    """Listen to DEV."""
    global srcdevice
    if device == "tuner":
        return listen_tuner()
    if device == "net_usb":
        return listen_net_usb()

    assure_on("amp", True)
    assure_on(device, True)
    hm.select('amp', device, org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_only, None) or hm.select('amp', device, org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, None)
    srcdevice = hm.get_canonical_name(device)
    paneljump(device)
    return ""

def watch_device(device):
    """Watch to DEV on TV or projector."""
    global srcdevice
    if device in ["tuner"]:
        return watch_tuner()
    elif device in ["net_usb"]:
        watch_net_usb()
    elif device in ["sat", "dbox", "coolstream"]:
        return watch_dbox(device)
    elif device in ["tv"]:
        watch_tv()
    elif not hm.has_device(device):
        print "No such device:", device
        return False
    
    tv_unless_projector(False)
    assure_on("amp", True)
    assure_on(device, True)
    hm.select('amp', device, org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, None)
    tv_unless_projector(True)
    hm.select('tv', 'amp', org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, org.harctoolbox.oldharctoolbox.ConnectionType.hdmi)
    srcdevice = hm.get_canonical_name(device)
    paneljump(device)
    return ""

def look_device(device):
    """Watch to DEV on TV or projector."""
    global srcdevice
    tv_unless_projector(False)
    assure_on("amp", True)
    assure_on(device, True)
    hm.select('amp', device, org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.video_only, None)
    hm.select('tv', 'amp', org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, org.harctoolbox.oldharctoolbox.ConnectionType.hdmi)
    srcdevice = hm.get_canonical_name(device)

def look_dvd(device):
    """Watch to DVD on TV, without sound."""
    return look_device(device)

def watch_device_play(device):
    watch_dvd(device)
    device_command(device, "play")

def watch_dvd():
    return watch_device("dvd")

def look_dvd():
    return look_device("dvd")

def listen_dvd():
    return listen_device("dvd")

def watch_hddvd():
    return watch_device("hddvd")

def look_hddvd():
    return look_device("hddvd")

def listen_hddvd():
    return listen_device("hddvd")

def assure_dvd_open(device):
    """Makes up for the lack of a discrete open command"""
    if hm.has_command(device, org.harctoolbox.oldharctoolbox.commandtype_t.any, org.harctoolbox.oldharctoolbox.command_t.get_status):
        answ=str(device_command(device, "get_status"))
        if not answ.find("OPEN") > 0:
            device_command(device, "stop")
            time.sleep(5)
            device_command(device, "open_close")
    else:
         device_command(device, "play")
         time.sleep(5)
         device_command(device, "stop")
         time.sleep(5)
         device_command(device, "open_close")

def assure_oppo_closed(device):
    answ=str(device_command(device, "get_status"))
    print answ
    if answ.find("OPEN") > 0:
        device_command(device, "stop")
        time.sleep(5)
        device_command(device, "open_close")

def switch_dvd(device, max_trys = default_max_trys):
    assure_on(device, True)
    time.sleep(1)
    device_command(device, "stop")
    time.sleep(1)
    assure_dvd_open(device)
    answ=""
    if hm.get_deviceclass(device) == 'oppo_bdp83':
        _trace('switch_dvd', device, 'Using smart method')
        org.harctoolbox.harchardware.globalcache.setSerialTimeout(100000)
        device_command_n(device, 'set_verbosity', '2')
        ok=False
        i = 0
        while i < max_trys and not ok:
            i = i + 1
            answ=device_command(device, 'listen1') # None if timeout
            if answ is None:
                ok=True
                _trace('switch_dvd', device, 'Timeout')
            else:
                ok=(answ in ["@UPL CLOS", "@UPL LOAD"])
                _trace('switch_dvd', device, 'Got answer: "' + answ + ('" quitting loop' if ok else '" continuing'))
        org.harctoolbox.harchardware.globalcache.setSerialTimeout(2000)
        device_command_n(device, 'set_verbosity', '0')
        if answ is None:
            device_command(device, "open_close")
    else:
        _trace('switch_dvd', device, 'Using timeout 60 seconds')
        time.sleep(60)
        device_command(device, "open_close")

def remove_dvd(device = 'dvd'):
    switch_dvd(device)
    assure_off(device)

def play_another_dvd(device='dvd'):
    switch_dvd(device)
    device_command(device, "play")
    watch_device(device)

def toshiba_get_version():
    assure_on("toshiba", True)
    device_command("toshiba", "display")
    device_command("toshiba", "cmd_1")
    device_command("toshiba", "cmd_9")
    device_command("toshiba", "cmd_5")
    device_command("toshiba", "display")

def toshiba_rce_1():
    assure_on("toshiba", True)
    #<delay duration="50000")
    device_command("toshiba", "stop")
    time.sleep(5000/1000)
    device_command("toshiba", "open_close")
    time.sleep(5000/1000)
    device_command("toshiba", "title_search")
    time.sleep(2000/1000)
    device_command("toshiba", "cmd_1")
    time.sleep(2000/1000)
    device_command("toshiba", "cmd_3")
    time.sleep(2000/1000)
    device_command("toshiba", "cmd_9")
    time.sleep(2000/1000)
    device_command("toshiba", "cmd_1")
    time.sleep(2000/1000)
    device_command("toshiba", "title_search")

def hb(device):
    device_command(device, "ok")
    wait_delay(device, "intra-command", 2000)
    wait_delay(device, "intra-command", 2000)
    wait_delay(device, "intra-command", 2000)
    device_command(device, "left")
    wait_delay(device, "intra-command", 2000)
    device_command(device, "left")
    wait_delay(device, "intra-command", 2000)
    device_command(device, "ok")
    wait_delay(device, "intra-command", 2000)
    device_command(device, "right")
    wait_delay(device, "intra-command", 2000)
    device_command(device, "right")
    wait_delay(device, "intra-command", 2000)
    device_command(device, "ok")
    wait_delay(device, "intra-command", 2000)
    device_command(device, "left")
    wait_delay(device, "intra-command", 2000)
    device_command(device, "ok")
    wait_delay(device, "intra-command", 2000)
    device_command(device, "right")
    wait_delay(device, "intra-command", 2000)
    device_command(device, "ok")
    wait_delay(device, "intra-command", 2000)
    device_command(device, "right")
    wait_delay(device, "intra-command", 2000)
    device_command(device, "ok")
    wait_delay(device, "intra-command", 2000)
    device_command(device, "ok")

def setup_dbox_tv_or_projector(dev=default_tuxbox):
    """Setup the dbox depending upon projector or TV is used"""
    return setup_dbox_projector(dev) if projector_ison() else setup_dbox_tv(dev)

def setup_dbox_tv(dev=default_tuxbox):
    """Use to encapsulate"""
    return # device_command(dev, "aspectratio_automatic")

def setup_dbox_projector(dev=default_tuxbox):
    """Use to encapsulate"""
    return # device_command(dev, "aspectratio_16_9")


def listen_dbox(dev=default_tuxbox):
    global srcdevice
    """Sets up the system for dBox viewing on TV (just sound)."""
    assure_on(dev, False)
    assure_on('amp', True)
    hm.select('amp', dev, org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_only, None)
    assure_on(dev, True)
    srcdevice = "sat"
    paneljump("sat")

def look_dbox(dev=default_tuxbox):
    """Sets up the system for dBox viewing on TV, without sound."""
    global srcdevice
    assure_on(dev, False)
    assure_on('amp', True)
    tv_unless_projector(True)
    hm.select('amp', dev, org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.video_only, None)
    hm.select('tv', 'amp', org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, "hdmi")
    assure_on(dev, True)
    device_command(dev, "standby_off")
    setup_dbox_tv_or_projector(dev)
    srcdevice = "sat"
    paneljump("sat")


def watch_dbox(dev=default_tuxbox):
    """Sets up the system for dBox viewing on TV or projector."""
    global srcdevice
    assure_on(dev, False)
    assure_on('amp', True)
    tv_unless_projector(True)
    hm.select('amp', dev, org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, None)
    hm.select('tv', 'amp', org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, org.harctoolbox.oldharctoolbox.ConnectionType.any)
    assure_on(dev, True)
    assure_tuxbox_ready(dev)
    device_command(dev, "standby_off")
    setup_dbox_tv_or_projector(dev)
    srcdevice = "sat"
    paneljump("sat")
    return ""

def watch_cable():
    """Sets up the system for cable viewing on TV or projector."""
    watch_device('cable')

def look_cable():
    """Sets up the system for cable viewing on TV or projector."""
    look_device('cable')

def listen_cable():
    """Sets up the system for cable viewing on TV or projector."""
    listen_device('cable')

#def set_cable_prog(seq):
#    """Turn cable receiver to PROGNAME"""
#    watch_cable()
#    send_numsequence('cable', seq, True)

#def ard_hd():
#    set_cable_prog(395)

#def zdf_hd():
#    set_cable_prog(396)

#def arte_hd():
#    set_cable_prog(397)

def set_prog(progname, dev=default_tuxbox):
    """Turn dBox to PROGNAME"""
    ## If listening to something else, and looking to dbox, do not issue commands
    #set_srcdevice()
    #if ~ ((srcdevice != 'sat') and (str(device_command('amp', 'get_videoinput'))=='SVSAT')):
    #    watch_dbox()
    return str(hm.do_command(dev, "set_program_by_name", progname))=="ok"

def watch_tv():
    """Turn on TV (not using projector)"""
    global srcdevice
    hm.select("amp", "tv", org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_only, None)
    srcdevice="tv"
    device_command("tv", "tv_mode")
    paneljump("tv")


def look_tv():
    global srcdevice
    """Turn on TV, leaving audio alone"""
    device_command("tv", "tv_mode")
    srcdevice="tv"
    paneljump("tv")

def listen_tv():
    watch_tv()

def desk_listen_device(device):
    assure_on('yamaha_rxv596')
    return hm.select('yamaha_rxv596', device, org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, None)

def desk_listen_mac():
    desk_listen_device('mac')

def desk_listen_delta():
    desk_listen_device('epsilon')

def desk_listen_zone1():
    desk_listen_device('amp')
    device_command('denon', 'zone3_on') 


def rtl_f1():
    """Display the RTL Formula 1 teletext pages"""
    assure_on("tv", True)
    device_command("tv", "cmd_4")
    device_command("tv", "ok")
    wait_delay("tv", "input_switch", 1234)
    wait_delay("tv", "channel_switch", 1234)
    device_command("tv", "teletext_toggle")
    wait_delay("tv", "enter_teletext", 1234)
    device_command("tv", "cmd_2")
    wait_delay("tv", "intra_command", 500)
    device_command("tv", "cmd_5")
    wait_delay("tv", "intra_command", 500)
    device_command("tv", "cmd_0")
    paneljump("tv")
    
def enter_pin(device='humax'):
    send_numsequence(device, hm.get_pin(device))

def send_numsequence(device, num, append_ok=False):
    seq = str(num)
    for i in range(len(seq)):
        device_command(device, 'cmd_' + seq[i])
        #if i < len(seq) - 1:
        wait_delay(device, "intra_command", 250)
    if append_ok:
        device_command(device, 'ok')

def watch_vcr():
    """Watch VCR"""
    global srcdevice
    hm.select("amp", "vcr", org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, None)
    hm.select("tv", "amp", org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, org.harctoolbox.oldharctoolbox.ConnectionType.yuv)
    srcdevice="vcr"

def look_vcr():
    """Watch VCR"""
    global srcdevice
    hm.select("amp", "vcr", org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.video_only, None)
    hm.select("tv", "amp", org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, org.harctoolbox.oldharctoolbox.ConnectionType.yuv)
    srcdevice="vcr"
    paneljump("vcr")

def listen_vcr():
    """Watch VCR"""
    global srcdevice
    hm.select("amp", "vcr", org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_video, None)
    srcdevice="vcr"
    paneljump("vcr")


#def vcr_off():
#    """Discretely Turns on VCR (very bad implementation)"""
#    device_command("vr1100", "standby_toggle")
#    #time.sleep(1)
#    device_command("vr1100", "power_toggle")

#def vcr_on():
#    """Turns on VCR"""
#    device_command("vr1100", command="standby_toggle")
#    #time.sleep(1)
#    device_command("vr1100", command="standby_toggle")

#         <macros name="Phonomacros():
def listen_phono():
    """Listen to vinyl"""
    global srcdevice
    hm.select("amp", "phono", org.harctoolbox.oldharctoolbox.commandtype_t.any, None, org.harctoolbox.oldharctoolbox.MediaType.audio_only, None)
    srcdevice="phono"
    paneljump("phono")

#         <macros name="Popcorn-hour-macros():
def poweroff_popcorn():
    """Shuts down the Popcorn hour"""
    device_command("popcorn", "power_toggle")
    device_command("popcorn", "delete")

def on(device):
    device_command(device, 'power_on')

def off(device):
    device_command(device, 'power_off')

def power(device):
    device_command(device, 'power_toggle')

def sunrise():
    """Macro called at sunrise."""
    #devicegroup_command("blinds", "power_off")
    return devicegroup_command("blinds", "power_off")

def sunset():
    """Macro called at sunset."""
    #devicegroup_command("blinds", "power_on")
    return devicegroup_command("blinds", "power_on")

#     <macros name="Powermacros():
    """Macros for turning on light, lowering blinds, etc"""

#         <macros name="Lightning():
    """Macros for dealing with light"""

def front_left_right_on():
    """Turn on halogen light front, left, and right"""
    return devicegroup_command("front left right halogen lights", 'power_on')

def front_left_right_off():
    """Turn off halogen light front, left, and right"""
    return devicegroup_command("front left right halogen lights", 'power_off')

def front_left_right_toggle():
    """Toggle halogen light front, left, and right"""
    return devicegroup_command("front left right halogen lights", 'power_toggle')

def front_left_right_shelf_toggle():
    """Toggle halogen light front, left, and right"""
    return devicegroup_command("front halogen lights", 'power_toggle')

def floorlamps_on():
    """Turn on halogen light front, left, and right"""
    return devicegroup_command("floor lamps", 'power_on')

def floorlamps_off():
    """Turn off halogen light front, left, and right"""
    return devicegroup_command("floor lamps", 'power_off')

def floorlamps_toggle():
    """Toggle halogen light front, left, and right"""
    return devicegroup_command("floor lamps", 'power_toggle')

#    """Macros for raising and lowering blinds"""
def all_down():
    """Lowers all blinds, except bedroom."""
    return devicegroup_command("blinds except bedroom", "power_on")

def all_up():
    """Lowers all blinds, except bedroom."""
    return devicegroup_command("blinds except bedroom", "power_off")

def all_down_smart():
    """...also inhibits the relay signal."""
    all_down()
    return all_down()

def all_up_smart():
    """...also inhibits the relay signal."""
    all_up()
    return all_up()

def bedroom_slowly_dark(level=50, dimtime=240):
    """Set light level to specified value, then dim down in specified time."""
    device_command_n('bedroom_light', 'set_power', str(level))
    time.sleep(1)
    return device_command_n('bedroom_light', 'dim_off_time', str(dimtime))

def column_on():
    """Turn on column (light and air)"""
    return devicegroup_command("column", "power_on")

def column_off():
    """Turn off column (light and air)"""
    return devicegroup_command("column", "power_off")

def column_toggle():
    """Toggle column (light and air)"""
    return devicegroup_command("column", "power_toggle")

def decoration_on():
    """Turn on funny decoration elements"""
    return devicegroup_command("decoration", "power_on")

def decoration_off():
    """Turn on funny decoration elements"""
    return devicegroup_command("decoration", "power_off")

#         <macros name="misc-power">
fundelay=2000

# FIXME (reimplement appending lists)
def flashy_on():
    """Turn on lots of fun stuff"""
    devicegroup_command('front halogen lights', 'power_on')
    return devicegroup_command("decoration", "power_on")

def flashy_off():
    """Turn on lots of fun stuff"""
    devicegroup_command('front halogen lights', 'power_off')
    return devicegroup_command("decoration", "power_off")

def flashy_slow():
    """Turn on lots of fun stuff, slowly"""
    devicegroup_command_delay('front halogen lights', 'power_on', fundelay)
    return devicegroup_command_delay("decoration", "power_on", fundelay)

def lights_on():
    """Turns off all lights"""
    devicegroup_command("lights", "power_on")

def lights_off():
    """Turns off all lights"""
    devicegroup_command("lights", "power_off")

def fans_on():
    """Turns off all fans"""
    devicegroup_command("fans", "power_on")

def fans_off():
    """Turns off all fans"""
    devicegroup_command("fans", "power_off")

def dark():
    """Lights off, blinds down"""
    all_down()
    lights_off()

def door_is_open():
    return str(device_command('balcony_door', 'get_state')) != 'on'

def door_open():
    """Opens the balcony door, unless already open."""
    if not door_is_open():
        return str(device_command('balcony_door', 'open_close'))
    return True

def door_close():
    """Closes the balcony door, unless already closed."""
    if door_is_open():
        return str(device_command('balcony_door', 'open_close'))
    return True

def door_open_close():
    return str(device_command('balcony_door', 'open_close'))

#     <macros name="scenarios">

def movie(darkening=True):
    """Prepares for movie"""
    projector_mode()
    if darkening:
        all_down()
    lights_off()
    decoration_off()
    x_display_standby()
    device_command("sub", "power_on")

def dvd_movie(darkening):
    """DVD movie"""
    movie(darkening)
    assure_on("dvd", False)
    watch_dvd()

def dbox_movie(darkening):
    """dBox movie"""
    assure_on("dbox", False)
    movie(darkening)
    watch_dbox()

def bedtime():
    """Turns everything off, lights for going to bed"""
    devices_command(set(get_device_group('lights'))- set(['bathroom_light', 'bedroom_light']), 'power_off')
    #lights_off()
    device_command("bathroom_light", "power_on")
    bedroom_slowly_dark(60, 300)
    shutdown('all')
    decoration_off()
    fans_off()

def leaving_home():
    """Turns everything off, lights in hallway"""
    lights_off()
    device_command("hall_light", "power_on")
    shutdown('all')
    decoration_off()
    fans_off()
    if door_is_open():
        door_close()
        print "The balcony door needs to be shut properly" # Not too clever really...
    
def kitchen_mediabridge():
    listen_htpc()
    kitchen_main_zone()

def zone2_on_mainzone():
    assure_on('bedroom_amp', True)
    assure_on('amp', True)
    bedroom_select_device('amp')
    device_command('amp', 'zone2_on')

def kitchen_main_zone():
    zone2_on_mainzone()
    bedroom_speakers_b()

def oppo_ispaused():
    return str(device_command("oppo", "get_status")).find("PAUSE") > 0

def oppo_isplay():
    return str(device_command("oppo", "get_status")).find("PLAY") > 0

def oppo_pause_on():
    if not oppo_ispaused():
        device_command("oppo", "pause_toggle")

def oppo_pause_off():
    device_command("oppo", "play")

def oppo_play_pause():
    return hm.do_command('oppo', ('pause_toggle' if oppo_isplay() else 'play'))

def oppo_bluraychip_set_region(device='oppo', zone="B", region=0):
    assure_off(device)
    z = zone + str(region)
    _trace('oppo_bluraychip_set_region', device + zone + str(region), z)
    device_command_n(device, 'set_regioncode', z)

def get_srcdevice(update=False):
    global srcdevice
    if not srcdevice or update:
        set_srcdevice()
    return srcdevice

# FIXME
# ugly, rewrite with a table
def set_srcdevice():
    global srcdevice
    ans = str(device_command('amp', 'get_input'))
    print ans
    if ans == "SIBD":
        srcdevice="dvd"
    elif ans == "SIDVD":
        srcdevice="hddvd"
    elif ans == "SICD":
        srcdevice="tv"
    elif ans == "SIPHONO":
        srcdevice="phono"
    elif ans == "SIDOCK":
        srcdevice="ipod_dock"
    elif ans == "SISAT/CBL":
        srcdevice="sat"
    elif ans == "SITV":
        srcdevice="player"
    elif ans == "SIGAME":
        srcdevice="htpc"
    elif ans == "SIDVR":
        srcdevice="vcr"
    elif ans == "SIV.AUX":
        srcdevice="v.aux"
    elif ans == "SINET/USB":
        srcdevice="net_usb"
    elif ans == "SITUNER":
        srcdevice="tuner"
    else:
        srcdevice="unknown"

def break_on():
    """Break, some guidance light"""
    gpause_on()
    device_command("kitchen_light", "power_on")

def break_off():
    """Break, some guidance light"""
    gpause_off()
    device_command("kitchen_light", "power_off")

break_state = False

def break_toggle():
    global break_state
    if break_state:
        break_off()
    else:
        break_on()
    break_state = not break_state

def daylight():
    """(Daylight) All blinds up, all lights out"""
    device_command("screen", "power_off")
    all_up_smart()
    lights_off()

def good_morning():
    device_command("screen", "power_off")
    all_up_smart()
    lights_off()
    device_command("bathroom_light", "power_on")
    shutdown('all')

def early_evening():
    """Turn on decent lights"""
    return front_left_right_on()

def evening():
    """Blinds down, appropriate lights"""
    front_left_right_on()
    return all_down_smart()

def x_display_standby():
    return "ok" if subprocess.call(['xset', 'dpms', 'force', 'standby'])==0 else None

def x_display_on():
    return "ok" if subprocess.call(['xset', 'dpms', 'force', 'on'])==0 else None

def execute(device, av, moviemode, blinds, playmode):
    if device=='player':
        device='oplay'

    assure_on(device, False)

    if moviemode:
        movie(blinds)

    if av == "audio_video":
        watch_device(device)
    elif av == "audio_only":
        listen_device(device)
    elif av == "video_only":
        look_device(device)
    else:
        return false

    if playmode:
        device_command(device, "play")

def setup_dbox_channel_list(dev=default_tuxbox):
    global dbox_channel_list
    cl=device_command(dev, 'get_channellist')
    if cl is None:
        return False
    p = string.split(cl, '\n')
    for ent in p:
        [key, prog] = string.split(ent, " ", 1)
        ind = int(key, 16)
        dbox_channel_list[ind] = prog.encode('latin_1', 'replace') # FIXME

    return True

def get_dbox_channelname(dev=default_tuxbox):
    global dbox_channel_list
    if len(dbox_channel_list) == 0:
        setup_dbox_channel_list()
    response = device_command(dev, 'get_program')
    if response is None:
        return False
    id=int(response,16)
    return dbox_channel_list[id]

def get_dbox_current_programs(dev=default_tuxbox):
    global dbox_channel_list
    global dbox_current_programs
    if len(dbox_channel_list) == 0:
        setup_dbox_channel_list()
    str=device_command(dev, 'get_current_programs')
    if str is None:
        return False
    lst=string.split(str,'\n')
    for ent in lst:
        [progid, eventid, text] = string.split(ent, " ", 2)
        index=int(progid, 16)
        dbox_current_programs[dbox_channel_list[index]]=text

    return dbox_current_programs

def get_dbox_current_program(dev=default_tuxbox):
    id=int(device_command(dev, 'get_program'),16)
    str=device_command(dev, 'get_current_programs')
    if str is None:
        return False
    lst=string.split(str,'\n')
    for ent in lst:
        [progid, eventid, text] = string.split(ent, " ", 2)
        index=int(progid, 16)
        if index==id:
            return text#,eventid
    
def get_dbox_current_program_epg(dev=default_tuxbox):
    id=int(device_command(dev, 'get_program'),16)
    str=device_command(dev, 'get_current_programs')
    lst=string.split(str,'\n')
    for ent in lst:
        [progid, eventid, text] = string.split(ent, " ", 2)
        index=int(progid, 16)
        if index==id:
            break;

    text = device_command_n(dev, 'get_programs_eventid', eventid)
    epg = text.encode('latin_1', 'replace')
    ##print "eventid=" + eventid
    ##print "epf=" + epg
    #print "txt=" + text
    return epg

def oplay_show_version():
    assure_on('oplay')
    device_command('oplay', 'home')
    device_command('oplay', 'stop')
    device_command('oplay', 'play_pause')

def test_none():
    return None

def test_false():
    return False

def test_true():
    return True

def test_emptystring():
    return ""

def test_foobar():
    return "foobar"

def test_wait(duration=10):
    time.sleep(duration)
    return "wait is over!"

def irtrans(sw):
    if sw == 25:
        return door_open_close()
    elif sw == 27:
        return start_dbox_tv()
    elif sw == 28:
        return bedtime()
    elif sw == 29:
        return leaving_home()
    elif sw == 30:
        return movie()
    elif sw == 31:
        return tv_mode(True)
    elif sw == 32:
        return all_up()
    else: 
        return device_command_n('t10_dev', 'power_toggle', str(sw))

def pana_command(cmd):
    device_command('panasonic_dvd_s75', cmd)
    time.sleep(2)

def panasonic_codefree():
    pana_command('ok')
    pana_command('cmd_6')
    pana_command('two_digit')
    pana_command('rewind')
    pana_command('cmd_0xf2')
    pana_command('audio')
    pana_command('cmd_8')
    pana_command('cmd_1')
    pana_command('subtitle')
    pana_command('cmd_4')
    return True
