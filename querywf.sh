docker run --network=host --rm ubercadence/cli:master --do test-domain workflow query --query_type "$1" --workflow_id "$2" 