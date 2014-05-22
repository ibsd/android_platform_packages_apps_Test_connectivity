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

__author__ = 'Damon Kohler <damonkohler@gmail.com>'

import collections
import json
import os
import socket
import sys

PORT = os.environ.get('AP_PORT')
HOST = os.environ.get('AP_HOST')
HANDSHAKE = os.environ.get('AP_HANDSHAKE')
Result = collections.namedtuple('Result', 'id,result,error')


class Android(object):

  def __init__(self, cmd='initiate', uid=-1, port=None, addr=None):
    self.PORT = os.environ.get('AP_PORT')
    if port is not None:
      self.PORT = port
    if addr is None:
      addr = HOST, self.PORT
    self.conn = socket.create_connection(addr)
    self.client = self.conn.makefile()
    self.id = 0
    handshake = {'cmd':cmd,'uid':uid}
    self.client.write(json.dumps(handshake)+'\n')
    self.client.flush()
    resp = self.client.readline()
    #print(resp)
    result = json.loads(resp)
    self.uid = uid
    if result['status']:
      self.uid = result['uid']
    #self._authenticate(HANDSHAKE)

  def _rpc(self, method, *args):
    data = {'id': self.id,
            'method': method,
            'params': args}
    request = json.dumps(data)
    #print(request)
    self.client.write(request+'\n')
    self.client.flush()
    response = self.client.readline()
    self.id += 1
    print("Response: "+str(response))
    result = json.loads(response)
    if result['error'] is not None:
      print(result['error'])
    # namedtuple doesn't work with unicode keys.
    return Result(id=result['id'], result=result['result'],
                  error=result['error'], )

  def __getattr__(self, name):
    def rpc_call(*args):
      #print(args)
      return self._rpc(name, *args)
    return rpc_call
