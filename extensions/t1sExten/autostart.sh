#!/bin/bash
if [[ "$(last -x | tac | grep -B1 boot | tail -2 | grep shutdown)" == "" ]]; then
  echo "['$(date)'] Last shutdown: Uncontrolled (not safe). Possible power failure."
  sudo reboot
else
  echo "['$(date)'] Last shutdown: Controlled (safe)"
  sudo modprobe dahdi_echocan_mg2
  sudo dahdi_span_assignments auto
  sudo dahdi_genconf -vv
  sudo dahdi_scan
  sudo dahdi_cfg -s
  sudo dahdi_cfg -vv
  sudo /home/t1pbx/kevin/pcio/x.sh &
  sudo /home/t1pbx/kevin/pbxSetExe/x.sh
fi
