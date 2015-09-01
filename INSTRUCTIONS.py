"""
So, here is how I see the multi-agent system working:

=================
Problem 1:
Flowcharts that don’t produce any output

There is a multi-agent system (basically lots of the same software running 
concurrently on their own thread) of 5 agents.

Each agent initially has access to 3 flowchart nodes, one of which is
of type “Retriever”, such as Guardian articles, Twitter, Churchill
Speeches, etc.  Each set of nodes is different.

At the start, each agent randomly starts to generates flowcharts. This
is done using the Java software that John has written, or the web
service, if possible.  Each agent records to file some details about
the flowcharts which are successful (i.e., produce at least one text
line of output), and records to an ArrayList the flowcharts which are
unsuccessful. This continues indefinitely, and you might want to put a
sleep command in, to slow the process down a bit (i.e., to give the
dynamic nodes like Twitter chance to change their output).

At random time intervals (different for each agent), it broadcasts one
of its node to the whole agency, so that the other agents can now use
that node in its random generation of flowcharts. In addition to
further random generation, each agent looks through the ArrayList of
failed flowcharts and tries to substitute one node in the flowchart
for the new node. Each old node is tried, and any instances of
"failure to success" are recorded, so that we can inspect them for
potentially serendipity.

Once each agent has broadcast all of its nodes, and all agents have
tried to fix all their broken flowcharts with the broadcast nodes, the
process stops.

=================
Problem 2:
Flowcharts which produce too much output

As above, but the broken flowcharts are those which produce > 100
lines, and fixing the flowchart means appending the broadcast node to
the end of the broken flowchart. This can be done with the existing
web service.
=================

Please let me know if there is anything unclear about this setup. Once
this is working, we can study the results and move onto more
sophisticated models.  I have a feeling that Twitter, etc., will
change too slowly, so we could artificially increase this. At this
stage, we’re only trying to engineer a system which does things that
we can point to and say: serendipity happened there. As a bonus, this
is a simulation of how the FloWr framework will work in the future
(with people replacing the agents), but this shouldn’t concern us: we
just need to get a working system up and running.
"""
