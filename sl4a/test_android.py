#!/usr/bin/python3.4
# vim:ts=4:sw=4:softtabstop=4:smarttab:expandtab

#   Copyright 2014- The Android Open Source Project
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.

"""
Unit tests for sl4a client module.
"""


import unittest

import android

class TestClass:
    def __init__(self, v):
        self._v = v

class AndroidTests(unittest.TestCase):

    def test_object_codec(self):
        i = TestClass(1)
        encoder = android.AndroidJSONEncoder()
        encoder.register(TestClass)
        j = encoder.encode(i)
        decoder = android.AndroidJSONDecoder()
        o = decoder.decode(j)
        self.assertTrue(type(o) is type(i))



if __name__ == "__main__":
    unittest.main()
