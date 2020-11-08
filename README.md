# Percival

> As you'll read in a moment, it's about "persistent task".
> I couldn't find a smart name for that, so I kinda went
> phonetically.

This library is supposed to be used when you have long running
tasks that depend on each other. Thanks to Percival you can
define tasks that are run once and their results (as well as
run mmetadata) are stored in a database.

> Right now it's hardcoded to use MongoDB.

This can be particularly useful in scientific research
in IT, ML, etc. You can write down your experiment plan,
process the results and then decide what to do next. At that
point you extend your code with next steps and rerun the app.

Initial part will be recognized as persisted and won't be 
executed. It's results will be available to new experiments,
e.g. to derive parameters.

Besides, if you need to shut down the app, you can 
salvage results from previous run in next run.