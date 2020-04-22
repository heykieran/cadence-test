docker run --network=host --rm ubercadence/cli:master --do test-domain workflow start --tasklist HelloWorldTaskList --workflow_type $1 --workflow_id $2 --execution_timeout 3600 --input \"$3\"
