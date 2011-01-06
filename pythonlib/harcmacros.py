# -*- coding: iso-8859-1 -*-
import sys
import time
import subprocess
import string
import socket
import org.harctoolbox.protocol
import org.harctoolbox.home
import org.harctoolbox.command_t
import org.harctoolbox.commandtype_t
import org.harctoolbox.mediatype

__doc__ = "Macros for cat confusing"

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
