#!/bin/bash
#
# Copyright (c) 2010, Cormac Flanagan (University of California, Santa Cruz)
#                     and Stephen Freund (Williams College) 
#
# All rights reserved.  
# 
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
# 
#    * Redistributions of source code must retain the above copyright
#      notice, this list of conditions and the following disclaimer.
#
#    * Redistributions in binary form must reproduce the above
#      copyright notice, this list of conditions and the following
#      disclaimer in the documentation and/or other materials provided
#      with the distribution.
#
#    * Neither the names of the University of California, Santa Cruz
#      and Williams College nor the names of its contributors may be
#      used to endorse or promote products derived from this software
#      without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


#
# Generates the FastTrack version with long Epochs from
# the original code.  Also generates the LongVectorClock
# class from the original VectorClock class.
#

mkdir -p auto/tools/fasttrack_long
cp src/tools/fasttrack/*.java auto/tools/fasttrack_long/
sed -i.bak  -e "s/int\/\*epoch\*\//long\/*epoch*\//g" auto/tools/fasttrack_long/*.java
sed -i.bak  -e "s/VectorClock/LongVectorClock/g" auto/tools/fasttrack_long/*.java
sed -i.bak  -e "s/Epoch/LongEpoch/g" auto/tools/fasttrack_long/*.java
sed -i.bak  -e "s/package tools\.fasttrack/package tools.fasttrack_long/g" auto/tools/fasttrack_long/*.java
sed -i.bak  -e "s/\"FT2\"/\"FT2L\"/g" auto/tools/fasttrack_long/FastTrackTool.java

for i in `ls auto/tools/fasttrack_long/*.java`; do
    mv $i tmp.java
    echo "// AUTO-GENERATED --- DO NOT EDIT DIRECTLY " > $i
    cat tmp.java >> $i
done

rm auto/tools/fasttrack_long/*.bak

mkdir -p auto/tools/util
echo "// AUTO-GENERATED --- DO NOT EDIT DIRECTLY " > auto/tools/util/LongVectorClock.java
cat src/tools/util/VectorClock.java >> auto/tools/util/LongVectorClock.java
sed -i.bak  -e "s/int\/\*epoch\*\//long\/*epoch*\//g" auto/tools/util/LongVectorClock.java
sed -i.bak  -e "s/VectorClock/LongVectorClock/g" auto/tools/util/LongVectorClock.java
sed -i.bak  -e "s/Epoch/LongEpoch/g" auto/tools/util/LongVectorClock.java
rm auto/tools/util/*.bak

echo "// AUTO-GENERATED --- DO NOT EDIT DIRECTLY " > auto/tools/util/LongEpoch.java
cat src/tools/util/Epoch.java >> auto/tools/util/LongEpoch.java
sed -i.bak  -e "s/int\/\*epoch\*\//long\/*epoch*\//g" auto/tools/util/LongEpoch.java
sed -i.bak  -e "s/Integer\/\*epoch\*\//Long\/*epoch*\//g" auto/tools/util/LongEpoch.java
sed -i.bak  -e "s/Epoch/LongEpoch/g" auto/tools/util/LongEpoch.java
rm auto/tools/util/*.bak
