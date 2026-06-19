#!/usr/bin/env bash
#
# Instant simulator smoke run.
#
# Everything here is pre-decompressed and uses paths relative to this folder,
# so there is no setup: it launches the real CloudSim-Storage simulator on the
# prepared Al_w3 initial-population case straight away. Requires `java` on PATH.
#
# The simulator is locale-sensitive (its output parser expects a dot decimal
# separator) and the full-size case (2048 VMs / 512 hosts) needs more than the
# default heap, hence the pinned -Duser.* and -Xmx2g below.
set -euo pipefail
cd "$(dirname "$0")"

java -Xmx2g -Duser.language=en -Duser.country=US \
  -jar ../cloudsimStorage.jar --standalone Al_w3/meta.mtc

echo "----- result (Al_w3) -----"
grep -E "total Energy consumption|Total simulation time" Al_w3/output.tc
