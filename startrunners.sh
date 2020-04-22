trap "exit" INT TERM ERR
trap "kill 0" EXIT

clj -A:cadence -m cdnce.cdn-activity-runner &
clj -A:cadence -m cdnce.cdn-core &

wait
