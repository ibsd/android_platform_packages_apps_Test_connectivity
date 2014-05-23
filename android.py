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


class SL4AException(Exception):
    pass

class SL4AAPIError(SL4AException):
    pass

class SL4AProtocolError(SL4AException):
    pass


def IDCounter():
    i = 0
    while True:
        yield i
        i += 1

class Android(object):
    COUNTER = IDCounter()

    def __init__(self, cmd='initiate', uid=-1, port=PORT, addr=HOST):
        conn = socket.create_connection((addr, port))
        self.uid = None
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

    def _rpc(self, method, *args):
        apiid = next(Android.COUNTER)
        data = {'id': apiid,
                    'method': method,
                    'params': args}
        request = json.dumps(data)
        self.client.write(request.encode("utf8")+b'\n')
        self.client.flush()
        response = self.client.readline()
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


def android(argv):
    import getopt

    def _usage():
        print("""Usage: android -p <port> [-l <localport>] [-u <uid>] <apicall> [<apiargs>...]
            Example: android.py -p 56241 makeToast hello""")

    localport = PORT
    port = 0
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
        if opt in ("-u", "--uid"):
            try:
                uid = int(optarg)
            except ValueError:
                _usage()
                return
            cmd = "continue"

    if not args:
        _usage()
        return

    if not port:
        _usage()
        return

    if cmd == "initiate":
        start_forwarding(port, localport)

    a = Android(cmd=cmd, uid=uid, port=localport)
    print ("UID:", a.uid)
    rpc = getattr(a, args[0])
    rpc(*args[1:])


# Run as top-level script for handy test utility
if __name__ == "__main__":
    import sys
    android(sys.argv)

