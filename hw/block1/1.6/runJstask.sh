java --source=11 $1 &
PID="$!" && sleep $3
jstack -l $PID > $2
kill $PID
