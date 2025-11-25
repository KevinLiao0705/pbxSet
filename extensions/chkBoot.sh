if [[ "$(last -x | tac | grep -B1 boot | tail -2 | grep shutdown)" == "" ]]; then
  echo "['$(date)'] Last shutdown: Uncontrolled (not safe). Possible power failure."
else
  echo "['$(date)'] Last shutdown: Controlled (safe)"
fi