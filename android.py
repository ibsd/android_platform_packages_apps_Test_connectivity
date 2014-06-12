# python3.4
# Copyright (C) 2009 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

"""
JSON RPC interface to android scripting engine.

Example:
    a = Android()
    a.makeToast("poptart")
    a.close()

Also supports serializing data objects (class instances). Register them with the android client object.


class Mydata:
    def __init__(self, value):
        self.value = value

a = Android()
a.register(Mydata)

data = Mydata(2)
a.makeCall(data)
a.close()

This feature requires support on the SL4A side for the Jackson ObjectMapper.

http://jackson.codehaus.org/1.7.3/javadoc/org/codehaus/jackson/map/ObjectMapper.html

Where you also need a POJO on the server side. Otherwise this feature cannot be used.
"""

__author__ = 'Keith Dart <dart@google.com>'
__oldauthor__ = 'Damon Kohler <damonkohler@gmail.com>'

import os
import sys
import socket
import logging
from json.decoder import JSONDecoder
from json.encoder import JSONEncoder


HOST = os.environ.get('AP_HOST', None)
PORT = os.environ.get('AP_PORT', 9999)

LAUNCH_CMD=("adb shell am start -a com.googlecode.android_scripting.action.LAUNCH_SERVER "
        "-n com.googlecode.android_scripting/.activity.ScriptingLayerServiceLauncher "
        "--ei com.googlecode.android_scripting.extra.USE_SERVICE_PORT {}")

class SL4AException(Exception):
    pass

class SL4AAPIError(SL4AException):
    """Raised when remote API reports an error."""

class SL4AProtocolError(SL4AException):
    """Raised when there is some error in exchanging data with server on device."""


class AndroidJSONDecoder(JSONDecoder):
    def __init__(self):
        super().__init__(object_hook=self._decode_object, parse_float=None, parse_int=None,
                parse_constant=None, strict=True, object_pairs_hook=None)
        self._regs = {}

    def _decode_object(self, d):
        if "@class" in d:
            cname = d.pop("@class")
            cls = _load_class(cname)
            o = cls.__new__(cls)
            while d:
                k, v = d.popitem()
                setattr(o, k, v)
            return o
        else:
            return d

    def register(self, cls, decoder=None):
        self._regs[cls] = decoder

    def decode(self, s):
        return super().decode(str(s, encoding="utf8"))


def _load_class(path):
    modname, clsname = path.rsplit(".", 1)
    try:
        mod = sys.modules[modname]
    except KeyError:
        pass
    __import__(modname)
    mod = sys.modules[modname]
    return getattr(mod, clsname)


class AndroidJSONEncoder(JSONEncoder):
    def __init__(self):
        super().__init__ (skipkeys=False, ensure_ascii=False,
                check_circular=True, allow_nan=True, sort_keys=False,
                indent=None, separators=None, default=None)
        self._regs = {}

    def default(self, o):
        try:
            encoder = self._regs[type(o)]
        except KeyError:
            return super().default(o)
        if encoder is not None:
            return encoder(o)
        d = o.__dict__.copy()
        t = type(o)
        d["@class"] = "{}.{}".format(t.__module__, t.__name__)
        return d

    def encode(self, o):
        s = super().encode(o)
        return s.encode("utf8")+b'\n'

    def register(self, cls, encoder=None):
        self._regs[cls] = encoder


def IDCounter():
    i = 0
    while True:
        yield i
        i += 1

class Android(object):
    """Client interface to SL4A server running on Android.

    Supports generic calling to SL4A APIs by getting an API method as an attribute.

    Example:
        a = Android()
        a.makeToast("poptart")
    """
    COUNTER = IDCounter()

    def __init__(self, cmd='initiate', uid=-1, port=PORT, addr=HOST):
        self.client = None # prevent close errors on connect failure
        self.uid = None
        self._encoder = AndroidJSONEncoder()
        self._decoder = AndroidJSONDecoder()
        conn = socket.create_connection((addr, port))
        self.client = conn.makefile(mode="brw")
        handshake = {'cmd':cmd, 'uid':uid}
        self.client.write(self._encoder.encode(handshake))
        self.client.flush()
        resp = self.client.readline(16384)
        if not resp:
            raise SL4AProtocolError("No response from handshake.")
        result = self._decoder.decode(resp)
        if result['status']:
          self.uid = result['uid']
        else:
          self.uid = -1

    def close(self):
        if self.client is not None:
            c = self.client
            self.client = None
            try:
                # TODO gracefully terminate session when supported on server side.
                # e.g.: c.write(self._encoder.encode({"cmd":"terminate", "uid":self.uid}))
                c.close()
            except (ValueError, OSError):
                logging.warn("Closing Android interface that was already closed.")

    def __del__(self):
        self.close()

    def register(self, cls, encoder=None, decoder=None):
        """Register a class object that will support serialization to and
        from sl4a server.

        Different encoder and decoder callables may be supplied. If not
        supplied, generic ones will be used.

        A factory function can register objects as server side POJOs are developed.
        """
        self._encoder.register(cls, encoder)
        self._decoder.register(cls, decoder)

    def _rpc(self, method, *args):
        apiid = next(Android.COUNTER)
        data = {'id': apiid,
                    'method': method,
                    'params': args}
        self.client.write(self._encoder.encode(data))
        self.client.flush()
        response = self.client.readline(16384)
        if not response:
            raise SL4AProtocolError("No response from server.")
        result = self._decoder.decode(response)
        if result['error']:
            raise SL4AAPIError(result['error'])
        if result['id'] != apiid:
            raise SL4AProtocolError("Mismatched API id")
        return result['result']

    def __getattr__(self, name):
        def rpc_call(*args):
            return self._rpc(name, *args)
        return rpc_call


def start_forwarding(port, localport=PORT):
    os.system("adb forward tcp:{} tcp:{}".format(localport, port))


def kill_adb_server():
    os.system("adb kill-server")


def start_adb_server():
    os.system("adb start-server")


def start_sl4a(port=8080):
    start_adb_server()
    os.system(LAUNCH_CMD.format(port))


def android(argv):
    import getopt

    def _usage():
        print("""Usage: android [-p <remoteport>] [-l <localport>] [-u <uid>] <apicall>|start [<apiargs>...]
            Example: android.py -p 8080 makeToast hello""")

    localport = PORT
    port = 8080
    uid = -1
    cmd = "initiate"
    try:
        opts, args = getopt.getopt(argv[1:], "l:p:u:", ["localport=", "port=", "uid="])
    except getopt.GetoptError:
        _usage()
        return
    for opt, optarg in opts:
        if opt in ("-p", "--port"):
            try:
                port = int(optarg)
            except ValueError:
                _usage()
                return
        elif opt in ("-u", "--uid"):
            try:
                uid = int(optarg)
            except ValueError:
                _usage()
                return
            cmd = "continue"
        elif opt in ("-l", "--localport"):
            try:
                localport = int(optarg)
            except ValueError:
                _usage()
                return

    if not args:
        _usage()
        return

    if args[0] == "start":
        kill_adb_server()
        start_adb_server()
        start_sl4a(port)
        return

    if cmd == "initiate":
        start_forwarding(port, localport)

    a = Android(cmd=cmd, uid=uid, port=localport)
    print ("UID:", a.uid)
    rpc = getattr(a, args[0])
    rpc(*args[1:])
    a.close()


# Run as top-level script for handy test utility
if __name__ == "__main__":
    android(sys.argv)
