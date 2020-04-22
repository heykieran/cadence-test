rm -rf classes/*
clj -A:cadence -e "(compile 'cdnce.hello.workflow) (compile 'cdnce.ucase.workflow) (compile 'cdnce.greet.activities) (compile 'cdnce.greet.workflow)"