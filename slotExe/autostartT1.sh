#!/bin/bash
sudo dahdi_span_assignments auto
sleep 1
sudo /home/t1pbx/kevin/pcio/x.sh &
sudo /home/t1pbx/kevin/pbxSetExe/x.sh
