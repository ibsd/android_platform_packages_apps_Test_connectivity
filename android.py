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
"""

__author__ = 'Keith Dart <dart@google.com>'
__oldauthor__ = 'Damon Kohler <damonkohler@gmail.com>'

import os
import json
import socket


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


def IDCounter():
    i = 0
    while True:
        yield i
        i += 1

class Android(object):
    COUNTER = IDCounter()

    def __init__(self, cmd='initiate', uid=-1, port=PORT, addr=HOST):
        self.client = None # prevent close errors on connect failure
        self.uid = None
        conn = socket.create_connection((addr, port))
        self.client = conn.makefile(mode="brw")
        handshake = {'cmd':cmd, 'uid':uid}
        self.client.write(json.dumps(handshake).encode("utf8")+b'\n')
        self.client.flush()
        resp = self.client.readline()
        if not resp:
            raise SL4AProtocolError("No response from handshake.")
        result = json.loads(str(resp, encoding="utf8"))
        if result['status']:
          self.uid = result['uid']
        else:
          self.uid = -1

    def close(self):
        if self.client is not None:
            self.client.close()
            self.client = None

    def __del__(self):
        self.close()

    def _rpc(self, method, *args):
        apiid = next(Android.COUNTER)
        data = {'id': apiid,
                    'method': method,
                    'params': args}
        request = json.dumps(data)
        self.client.write(request.encode("utf8")+b'\n')
        self.client.flush()
        response = self.client.readline()
        if not response:
            raise SL4AProtocolError("No response from server.")
        result = json.loads(str(response, encoding="utf8"))
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
    import sys
    android(sys.argv)

