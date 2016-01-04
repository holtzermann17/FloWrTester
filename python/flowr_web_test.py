# -*- coding: utf-8; -*-

# This file uses the functions defined in flowr_web.py to test
# creation, running, deleting flow charts.  

# One hopefully straightforward thing to do is create and run a
# flowchart defined in a standard script (like the ones stored for the
# Desktop version of FloWr).

# The next iteration after will have to reason about how to swap one
# kind of node for another one, as described in Simon's specification,
# see INSTRUCTIONS.py.

import flowr_web
import json
import copy

from random import Random

from pprint import pprint

# We set up some global variables to keep track of information

global_vars = {}
global_vars['user_charts'] = {}
global_vars['node_store'] = {}

# this should be a dictionary, of dictionaries,
# so that each player can maintain "private" information about the
# tests they have run so far.
global_vars['players'] = {}

def populate_local_user_charts ():
    "Just store the user charts in the local variable."
    global_vars['user_charts'] = flowr_web.list_user_charts()

def delete_server_user_charts ():
    "Grab information about user charts, and then run commands to delete them all from the server."
    populate_local_user_charts()
    for c in global_vars['user_charts']:
        cid = c['cid']
        print "Deleting chart #" + str(cid) + "."
        flowr_web.delete_chart(c['cid'])

"""
# Some sample data showing the eventual form of the "menu" we will build
# with populate_local_node_store().

sampled = {'ideation' : { 'scenarios': ['ScenariosGenerator',
                                        'ConceptNetChainScenarios']},
           'text' : { 'extractors': ['ListExtractor',
                                     'RegexGenerator',
                                     'RegexPhraseExtractor',
                                     'TextRankKeyphraseExtractor',
                                     'PhraseExtractor',
                                     'NamesExtractor'], 
                      'theoryFormation': ['HR3Poems',
                                          'HR3',
                                          'HR3ConceptNet']}}
"""

## helper
def ensure_three(n):
    "Turn N, like foo.bar.baz or fred.barney into ['foo', 'bar', 'baz'] or ['fred', 'null', 'barney'], respectively."
    ns = n.split(".")
    if (len(ns)==2):
        ns = [ns[0], u'null', ns[1]]
    return ns

## helper
def add_to_hierarchy(collection,triple):
    """Add TRIPLE to COLLECTION."""
    # Comments will illustrate adding the triple
    #  {'a' : {'b' : ['d']}}
    # to an existing collection with various possible features
    top=collection.get(triple[0])
    if (top):
        med=top.get(triple[1])
        # we see: {'a' : {'b' : ['c']}}
        if (med):
            # we get: {'a' : {'b' : ['c', 'd']}}
            med.append(triple[2])
        else:
            # we see: {'a' : {'p' : ['q']}}
            newb = {triple[1]:[triple[2]]}
            # we get: {'a' : {'p' : ['q'], 'b' : ['c']}}
            top.update(newb)
    # we see: {'p' : {'q' : ['r']}}
    else:
        # ... build: {'b' : ['d']}
        newm= {triple[1] : [triple[2]]}
        # ... build: {'a' : {'b' : ['d'] }}
        newt= {triple[0] : newm}
        # we get: {'p' : {'q' : ['r']}, 'a' : {'b' : ['d']}}
        collection.update(newt)
    return collection

def populate_local_node_store ():
    "Grab information about nodes from the server, and store it in a local variable."
    list_of_nodes = flowr_web.list_all_nodes()
    d = {}
    global_vars['node_store'] = reduce (add_to_hierarchy, map(ensure_three, list_of_nodes), d)


def deal_out ():
    """Randomly deal out nodes to several hands, one per retriever.
This is set up so that each player gets a retriever node, and then
all other nodes are distributed almost-equally among each player."""
    rand = Random()
    nodes = copy.deepcopy(global_vars['node_store'])
    # ensure that each "hand" includes a retriever
    retrievers = nodes['text']['retrievers']
    rand.shuffle(retrievers)
    hands = map(lambda x:['text.retrievers.'+x],retrievers)
    # now that those have been dealt with, remove them
    del nodes['text']['retrievers']
    # now iterate through the remaining leaves and linearize them
    nodelist = []
    for first_key in nodes:
        for second_key in nodes[first_key]:
            for item in nodes[first_key][second_key]:
                nodelist.append(".".join([first_key,second_key,item]))
    # now deal these out like a pack of cards
    pos = 0
    num_hands = len(hands)
    num_nodes = len(nodelist)
    while pos < num_nodes:
        hands[divmod (pos, num_hands)[1]].append(nodelist[pos])
        pos = pos + 1
        
    return hands

def pass_hands ():
    global_vars['players'] = {}
    # deal out the cards, i.e. assign each "hand" to some player
    hands = deal_out()
    num_hands = len(hands)
    for i in range(0,num_hands):
        newp = {str(i) : hands[i]}
        global_vars['players'].update(newp) 

    # in a round, each player should build a flowchart, run it, test it
    # once everyone is done broadcast a node to the other players

def broadcast(n):
    """Nth player will select a node at random from their list and add it to everyone else's list."""
    rand = Random()
    sample = rand.sample(global_vars['players'][str(n)],1)
    for key in global_vars['players']:
        if not key == str(n):
            # Actually, we don't want to store multiple copies, so we should either
            # maintain a list of already broadcast nodes so that they aren't broadcast again,
            # or else allow rebroadcast but use a smarter function than .append here.
            global_vars['players'][key].append(sample[0])


def script_to_flowchart(script):
    """Create a new flowchart on the server following the outline of nodes and connections in string SCRIPT.
Return the new chart ID number (cid)."""

    myCid = flowr_web.new_chart()
    myNodelist = []
    print "New chart: " + myCid
    stanzas=script.split("\n\n")
    for stanza in stanzas:
        lines = stanza.split("\n")
        # take off the node name, e.g.
        # text.retrievers.ConceptNet.ConceptNet_0 -> text.retrievers.ConceptNet
        nodetype = lines[0].rsplit(".",1)[0]
        #print "Node_type: " + nodetype
        # Now create a node of this type, we're likely to get the same name back again
        nodeName = flowr_web.add_node(myCid,nodetype)
        myNodelist.append(nodeName)
        print "Created: " + nodeName
        for line in lines[1::]:
            # Deal with INPUT settings
            if not (line[0]=="#"):
                # we are only interested in settings that are actually provided, not ones that are empty
                setting = filter(None,line.split(":",1))
                try: rhs = setting[1]
                except:
                    print setting[0] + " has no rhs."
                    continue
                lhs = setting[0]
                settingStatus=flowr_web.set_parameter(myCid,nodeName,lhs,rhs)
                print lhs + "->" + rhs + " " + settingStatus
            # Deal with OUTPUT settings
            else:
                # let's more or less trust people to put in syntactically correct settings
                # like #wordsOfType = answers[*]
                setting = map(str.strip,line.split("=",1))
                try: rhs = setting[1]
                except: 
                    print setting[0] + " has no rhs."
                    continue
                lhs = setting[0]
                # create a new variable, which initially has a generic name
                myNewVar = flowr_web.new_variable(myCid, nodeName)
                # rename the variable
                renamingStatus = flowr_web.rename_variable(myCid,nodeName,myNewVar,lhs)
                print "RENAMING"
                pprint (renamingStatus) 
                # set the variable
                settingStatus=flowr_web.set_variable_definition(myCid,nodeName,lhs,rhs)
                # it might be nice if it just returned "error" or "saved" like the other function
                #print lhs + "->" + rhs + " " 
                print "SETTING"
                pprint (settingStatus)
        # finished the line loop, still inside the stanza loop
        print "Check parameter settings of " + nodeName
        pprint(flowr_web.get_parameters(myCid,nodeName))
        print "Check variable settings of " + nodeName
        pprint(flowr_web.get_variables(myCid,nodeName))

    print "Check the chart itself"
    pprint(flowr_web.get_chart(myCid))
    print "Looks good, let's run it."
    flowr_web.run_chart(myCid)
    return myCid

"""
# Test code:     

script_to_flowchart("text.retrievers.ConceptNet.ConceptNet_0\ndataFile:simple_concept_net_1p0_sorted.csv\nlhsQuery:\nlhsQueries:\nrelation:IsA\nrhsQuery:profession\nrhsQueries:\nminScore:1.0\n#wordsOfType = answers[*]\n\ntext.categorisers.WordListCategoriser.WordListCategoriser_0\nwordList:\nwordListFileName:not_animals.txt\nstringsToCategorise:#wordsOfType\nstringArraysToCategorise:\nwordListArray:\npositionStringInArray:\n#filteredFacts = textsWithoutWord[*]")
"""


def testing ():
    """Test access."""
    return flowr_web.test_access()
