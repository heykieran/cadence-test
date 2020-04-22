docker run --network=host --rm ubercadence/cli:master --do test-domain workflow signal --name "$1" --workflow_id "$2" --input \"$3\"
