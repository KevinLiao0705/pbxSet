#!/bin/bash
sudo dahdi_hardware
sudo modprobe wctdm24xxp
sudo modprobe dahdi_echocan_mg2
sudo dahdi_span_assignments auto
#sudo dahdi_genconf -vv
sudo dahdi_scan
sudo asterisk -rx "dahdi restart"
#sudo /home/fxopbx/kevin/pcio/x.sh &
#sudo /home/fxopbx/kevin/pbxSetExe/x.sh
