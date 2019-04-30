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
_jython_trace = False

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
