# Writing Cadence Workflows in Clojure

This repository is a quick overview of how to write workflows and activities for the Cadence workflow orchestration system
using Clojure. It assumes that you're somewhat familiar with Clojure as many details are elided.

## Installation & Setup of Cadence

Install Docker (if you don't already have it installed) and then setup Cadence using the instructions found 
in the [Cadence Quick Start Guide][cadence-quick-start].

This will install a number of docker images using `docker-compose`. These provide the services that Cadence
needs to run locally (Cassandra, web-server, etc.).

Start all the docker instances required by Cadence by issuing the `docker-compose up` command.

When the instances have started and initialized you should continue to follow the Quick Start instructions 
and create and register a domain with your local Cadence service. For the purposes of this demo the domain is 
assumed to be named `test-domain` (i.e the same as in Cadence's Quick Start with Java Guide).

## Using Clojure with Cadence

In order to use Cadence from Clojure you'll need Clojure installed and you'll need the Cadence libraries 
and some logging libraries on you class path. You can achieve this by adding an alias called `:cadence` to 
your `deps.edn` file. This is the alias used by the bash scripts during the compilation and exercise phases.

```
:cadence 
  {:extra-paths ["resources" "classes"]
   :extra-deps 
   {com.uber.cadence/cadence-client {:mvn/version "2.7.1"}
    commons-configuration/commons-configuration {:mvn/version "1.9"}
    ch.qos.logback/logback-classic {:mvn/version "1.2.3"}}}
```

Also, don't forget to create an appropriate `logback.xml` file on your class path also. Given the alias
defined above you can place this file in the `resources` directory.

## Exercising Cadence

Make sure Cadence is running (`docker-compose up`).

### Compile the classes we'll need.

From the root of your project (assuming `cdnce`) run the compile script

    ./compile.sh

This will clean out the `classes` folder (which needs to exist; you may need to create it), and 
will compile the required workflow and activity classes.

### Start the Cadence Workers

Now from a terminal start the Workflow Worker and the Activity Worker (which will run in separate processes)
using the supplied script

    ./startrunners.sh

### Starting A Simple Workflow

Start a simple workflow (`IHello::sayHello`) named __WF1__, passing "Fred" as the argument. This is a _straight-line_ workflow,
the simplest possible implementation - it simply outputs a message to the log and the exits. Messages from the log should
be visible in the terminal window where you started the runners.
    
    ./startwf.sh IHello::sayHello WF1 Fred

You can run this workflow again, using a different name, and observe the result in the runners' terminal window. (You must 
supply a different workflow instance name (__WF1A__) or Cadence will complain that the worflow instance already exist.)

    ./startwf.sh IHello::sayHello WF1A Barney

### Starting A More Complex Workflow

Now, we can exercise a more complex workflow. 

This workflow (`IGreet`) is an example of a _long-lived_ workflow: it starts, outputs a message
to the log, and then sits in a waiting state until it receives a signal or a query. 

The workflow uses an activity (`IGreetActivities`) and a child workflow (`IUcase`) during its operations. 

`IGreetActivity` implements two trivial methods `sayNow` and `sayAfterDelay`. The first method outputs immediately 
to `System/out` a message containing the parameter passed to the method, the second outputs a slightly different 
message after a delay of 5 seconds. 

The only purpose of the activity is to demonstrate a workflow's ability to call asynchronously (using `sayAfterDelay`) a 
_slow_ external process/function without stalling the workflow, which is for all effective purposes _single-threaded_.

The child workflow (`IUcase`) is a trivially simple workflow that converts to upper case whatever string is passed to it. 
It's used to demonstrates a workflow's ability to call another workflow (in this case, it is called synchronously).

We can start an instance of the `IGreet` workflow with the instance name of **WF2** as follows:

    ./startwf.sh IGreet::greet WF2 Fred

This workflow _loops_ internally, echoing messages to the log and responding to signals and queries. 

The signal defined in the interface is
`IGreet::updateGreeting`. When this signal is received, the workflow updates an internal variable (`greeting`) and if
it is different than its current value the workflow transitions from its waiting state, increments an internal counter 
(`count`) and outputs a message with the new greeting to the log. 

If the new greeting parameter passed with the signal is equal to the old greeting then no log 
message is emitted. 

If the signal's string parameter is "Bye", the workflow exits. 

You can send a signal to the running workflow instance changing the greeting to "Howdy" as follows:

    ./signalwf.sh IGreet::updateGreeting WF2 Howdy 

You should see a message emitted to the log. If we send another signal with the same parameter value,

    ./signalwf.sh IGreet::updateGreeting WF2 Howdy 

you won't see the message.

### Two Implementations

Internally, `IGreet::greet` is implemented in two different ways. 

* The first implementation uses a watcher (on the state atom) and a queue
to signal when greeting parameter may have changed. The workflow exits its _low-demand_ waiting state when
it receives a message on the queue indicating that the greeting's value has changed.

* The second implementation, which adheres more closely in intent to the original Java code in the Quick Start 
Guide, uses `ThreadLocal` variables. (Ironically, this inverts the mutable/immutable relationships by storing
the instance variables in an atom, and the local variable in ThreadLocal variables - almost the reverse of Java.)

The execution _mode_ to be used by a workflow instance is determined when the instance is started. If the
parameter with which it is started does **not** begin with an underscore character then the watcher/queue method is 
used, if it does then the ThreadLocal method is used.  

### Continuing the Exploration

You can start another instance of the workflow called **WF3** passing the argument "_Fred". This will ensure that the
ThreadLocal implementation is used by the instance.

    ./startwf.sh IGreet::greet WF3 _Fred

Send a signal to the running instance of the workflow (named **WF2**) with the parameter "Hi"

    ./signalwf.sh IGreet::updateGreeting WF2 Hi

You should see this echoed to the log.

Send another signal to the running **WF3** workflow with the parameter "HowdyLocal"

    ./signalwf.sh IGreet::updateGreeting WF3 HowdyLocal

The workflow also responds to queries. The only query (there can be many) defined in the interface is 
`IGreet::getCount`. It simply returns the number of unique greetings the workflow instance has seen since it was 
started.

To interrogate both active workflow instances, we can send two queries

    ./querywf.sh IGreet::getCount WF2
    ./querywf.sh IGreet::getCount WF3

The results are returned immediately at the command line.

Now, shutdown both **WF2** and **WF3** by sending the "Bye" signal.

    ./signalwf.sh IGreet::updateGreeting WF2 Bye
    ./signalwf.sh IGreet::updateGreeting WF3 Bye

You can now stop the Workflow and Activity runners by typing Ctrl+C in the terminal window where you started 
them.

### Using Cadence without the CLI

Up to this point you've started, signalled and queried workflow instances by using Cadence's CLI, using a docker
container. However, it's also helpful to know how to do the same thing using Clojure code.

There is also in the repository a Clojure namespace that runs many of the same commands (`cdnce.cdn-exercice`). It 
can be run by issuing 

    clj -A:cadence -m cdnce.cdn-exercise <workflow instance name>
    
at the command line e.g.

    clj -A:cadence -m cdnce.cdn-exercise WF5

## Important Implementation Notes

### Threads

Workflow instances are effectively single-threaded so you need to be careful about using certain Clojure
constructs within workflows. Basically, don't use anything that utilizes threads apart from the thread of
execution. Doing so will cause Cadence to throw an exception regarding thread identity. I had this issue
while trying to use Clojure channels instead of standard queues in workflow methods. 

### Annotations

There are some interesting items to note in the code. Cadence allows annotations on workflow
methods to include some required timeout parameters e.g. `scheduleToCloseTimeoutSeconds`. However,
if these are included in the Clojure code's `definterface` method as one might expect, like

```
(definterface IGreetActivities
  ^{ActivityMethod true {:scheduleToCloseTimeoutSeconds 100}}
  ;; IGreetActivities::say
  (^void sayNow [^String message])
  ;; IGreetActivities::sayAfterDelay
  (^Boolean sayAfterDelay [^String message]))
```

The value is converted 100 to a `long` and Cadence will throw an exception as it expects an `int`. The workaround is to
use Cadence's `ActivityOptions.Builder` when creating the `Workflow.ActivityStub` when using `Workflow/newActivityStub`
when the activity is _attached_ to the workflow.

### Method References 

Also, Cadence relies quite a bit on method references, so the Clojure code needs to `reify` the appropriate 
interface rather than relying on the method reference syntactic sugar.

So, what in Java might look like 

    Async.function(IGreet::sayAfterDelay, converted_greeting);;

becomes in Clojure 

```
(Async/function
  (reify com.uber.cadence.workflow.Functions$Func1
    (apply [_ t1]
    (.sayAfterDelay activity-stub-for-IGreet t1)))
  converted-greeting)
```

## License

Copyright © 2020 Kieran Owens

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[cadence-quick-start]: https://cadenceworkflow.io/docs/06_javaclient/01_quick_start