import sys
import inspect
import harcmacros
import harc.harcutils

sys.ps1 = '[[>>> '
sys.ps2 = '..... '

harcmacros.hm=hm

def cmd(dev, command):
   harcmacros.device_command(dev, command)

def version():
   print str(harc.harcutils.version_string)

def help():
    print "Useful commands are, e.g., <TODO>"

def license():
    print str(harc.harcutils.license_string)

def _harcfuncs_n_args(n):
   """Return all functions taking exactly n arguments."""
   return map(lambda x: x[0], \
                 inspect.getmembers(harcmacros, \
                                       lambda o: inspect.isfunction(o) \
                                       and len(inspect.getargspec(o)[0])==n))

def _harcfuncs_gt2_args():
   """Return all functions taking more than 2 arguments."""
   return map(lambda x: x[0], \
                 inspect.getmembers(harcmacros, \
                                       lambda o: inspect.isfunction(o) \
                                       and len(inspect.getargspec(o)[0])>2))

def _harcfuncs_device():
   """Return all functions having its first argument called device."""
   return map(lambda x: x[0], \
                 inspect.getmembers(harcmacros, \
                                       lambda o: inspect.isfunction(o) \
                                       and not inspect.getargspec(o)[0] == []
                                       and inspect.getargspec(o)[0][0]=='device'))

def _stdcommands():
   """Returns some stuff that does  not belong to harcmacros, but that the completer should know about."""
   return [ 'help()', 'license()', 'version()', 'quit()', 'reload(harcmacros)', 'print' ]

#print "Welcome to Harc with Jython"
