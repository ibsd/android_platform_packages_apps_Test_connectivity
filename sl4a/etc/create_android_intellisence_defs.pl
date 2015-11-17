#!/usr/bin/perl
## Copyright (C) 2014 Google Inc.
##
## Licensed under the Apache License, Version 2.0 (the "License"); you may not
## use this file except in compliance with the License. You may obtain a copy of
## the License at
##
## http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
## WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
## License for the specific language governing permissions and limitations under
## the License.
#
#
=head
This script parses each individual java file
and finds each method that is prefixed by the
line @Rpc. If the method does not have it
then the method will be skipped. The results
are written in defs.txt. When udpating of
android intellisense in acts is required,
run this scrpt then copy the results into
the android_intellisense.py file.
=cut

use strict;
use warnings;
use Data::Dumper;
use File::Find;
$|++;

my $path = '../';
my $cmd = 'file';
my @def_list;
finddepth (\&wanted,$path);
open(FILE, ">defs.txt");
foreach (@def_list) {
    print FILE $_ . "\n";
}
close(FILE);

sub wanted {
    return unless -f; #-d for dir ops or comment out for both
    if ($_ =~ m/\.java/) {
        parse_file_for_rpc($_);
    }
}

sub parse_file_for_rpc {
    my $file = shift;
    open (FILE, $file);
    my @lines = <FILE>;
    close(FILE);
    for(my $i = 0; $i < scalar(@lines); $i++) {
        my $line = $lines[$i];
        my $sig = "";
        if ($line =~ m/\@Rpc\(/) {
            my $done = 0;
            until($done == 1) {
                $i++;
                $line = $lines[$i];
                $line =~ s/^\s+//;
                $line =~ s/\s+$//;
                if ($line =~ m/^public/) {
                    $sig = $line;
                    $done =1;
                }
            }
        }
        if ($sig !~ m/public/) {
            next;
        }
        my $split_sig = (split(/\(/, $sig))[0];
        my @words = split(/\s/, $split_sig);
        my $return_type = $words[1];
        my $method_name = $words[$#words];
        $method_name =~ s/\(.*$//g;
        $method_name =~ s/\s//g;
        my $done = 0;
        my $unparsed_params = "";
        until ($done == 1) {
            if ($line =~ m/\{/) {
                $unparsed_params = $unparsed_params . $line;
                $done = 1;
            } else {
                $unparsed_params = $unparsed_params . $line;
                $i++;
                $line = $lines[$i];
            }
        }
        my $info = (split(/$method_name/,$unparsed_params))[1];
        $info =~ tr/ //s;
        $info =~ s/\n//g;
        my @tokens = split(/\s/, $info);
        my $optional_flag = 0;
        my $params = "";
        my $var_flag = 0;
        my @var_list = qw(
        String Integer Boolean boolean int String[] JSONArray
        Object List<String> int[] Boolean[] Short[] Character[]
        Long[] Float[] Serializable Integer[] Byte Byte[] Double Float
        );
        foreach (@tokens) {
            if ($var_flag ==1) {
                $var_flag = 0;
                $params = $params . $_ ;
            }
            elsif ($_ =~ /RpcOptional/) {
                $optional_flag = 1;
            }
            elsif ($_ ~~ @var_list) {
                if ($optional_flag == 1) {
                    $params = $params . "Optional_";
                    $optional_flag = 0;
                }
                $params = $params . $_ . "_";
                $var_flag = 1;
            }
        }
        $params=~s/\)//g;
        $params=~s/\[\]/Array/g;
        $params=~s/List</List/g;
        $params=~s/>//g;
        my $def_name = "    def $method_name (self, $params): pass";
        push(@def_list, $def_name);
    }
}

