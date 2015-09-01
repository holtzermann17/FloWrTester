# -*- coding: utf-8; -*-

# Details about the API are given at
# http://ccg.doc.gold.ac.uk/research/flowr/flowrweb/ (Click through
# "Editor" to switch to Admin mode, and then click "API".)
# See API.org in this directory for a summary of commands.

# Here is the simple sample Unix command to show the basic idea of how
# it works:

# curl --data "api_token=3pkrxjrduMASh4ZAIUbpWOmLLDkYNz9p&api_email=MYEMAIL&c=test_access" http://ccg.doc.gold.ac.uk/research/flowr/flowrweb/

# The example python call corresponding to the above is:

# python -c 'import flowr_web; print ( flowr_web.test_access() )'

# Note: before committing this code, run the hide-token.sh sed script
# or a variant to remove the tokens, before running it, run the
# show-token.sh script, or nt.sh NEWTOKEN

from hammock import Hammock as FloWrWeb
import json
import urllib
import sys
import base64
import filecmp
import re

from pprint import pprint

flowr = FloWrWeb('http://ccg.doc.gold.ac.uk/research/flowr/flowrweb/')
user = 'holtzermann17@gmail.com'           # to be replaced before/after committing!
token = 'YwlF6aiZNiStiZlMVvQHSqJUbiPhFsuR' #

standard_headers = {'Accept-Charset': "UTF-8",
                    'Content-Type': "application/x-www-form-urlencoded;charset=UTF-8"}

## Relevant functions

def test_access():
    """Used to test token and email access."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "test_access"}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # notice, there is no JSON component this time
    return resp._content

def list_all_nodes():
    """Get a list of available node types."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "list_all_nodes"}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # when it makes sense, the content is interpreted as JSON
    return json.loads(resp._content)

# Initially returns an empty list
def list_user_charts():
    """Meta-data for all owned charts."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "list_user_charts"}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    return json.loads(resp._content)

# For some reason the numbering starts at 2.
# After charts have been deleted, new charts are renumbered from the beginning.
def new_chart():
    """Creates a new chart."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "new_chart"}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # new chart id
    return resp._content

def delete_chart(cid):
    """Deletes an unlocked user chart."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "delete_chart",
          "cid": cid}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # returns "ok" if successful and "error" if unsuccessful
    return resp._content

def add_node(cid,node_type):
    """Creates a new chart."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "add_node",
          "cid": cid,
          "type": node_type,}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # returns the type of the node with a name-and-number suffix,
    # e.g. text.retrievers.Dictionary.Dictionary_0 would be the first
    # Dictionary added.  This *string* is the node's "nid" or "nodeID"
    return resp._content

def delete_node(cid,nid):
    """Deletes a node from a chart."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "delete_node",
          "cid": cid,
          "nid": nid,}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # returns updated chart in json format
    # an *empty* chart with everything deleted looks like this:
    # {u'arrows': [], u'boxes': []}
    return json.loads(resp._content)

def get_chart(cid):
    """Get current chart structure."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "get_chart",
          "cid": cid}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # json, key features of which are nodeID
    return json.loads(resp._content)

# JAC: I'm not sure what this does, presumably the chart needs to be run first.
def clear_output(cid):
    """Docstring TBA."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "clear_output",
          "cid": cid}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # no json this time, just returns 'OK'
    return resp._content

def run_chart(cid):
    """Run the chart."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "run_chart",
          "cid": cid}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # returns url to check chart run status
    return resp._content

def run_status(cid):
    """Run the chart."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "run_status",
          "cid": cid}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # current status in json, e.g. {"status":"idle"}'
    return json.loads(resp._content)

def get_parameters(cid,nid):
    """Get details of all the node's parameters"""
    
    data={"api_token": token,
          "api_email": user,
          "c": "get_parameters",
          "cid": cid,
          "nid": nid}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # parameters as json
    return json.loads(resp._content)

# Hm, what function to we use to learn which parameters there are to be set?
# I just wrote some corresponding functions in the Clojure integration layer...
def set_parameter(cid,nid,pname,pvalue):
    """Set the value of a node parameter."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "set_parameter",
          "cid": cid,
          "nid": nid,
          "pname": pname,
          "pvalue": pvalue,}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # returns "saved" or "error"
    return resp._content

def new_variable(cid, nid):
    """Creates a new output variable for the node."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "new_variable",
          "cid": cid,
          "nid": nid}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # returns something like "#newVar0", or "error" if the named
    # node does not exist
    return resp._content

def rename_variable(cid,nid,vname,nname):
    """Renames an existing variable."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "rename_variable",
          "cid": cid,
          "nid": nid,
          "vname": vname,
          "nname": nname,}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # returns a list of the variables associated with the node
    # e.g.
    # [{u'simpletype': u'TwitterOutput', u'type': u'ccg.flow.processnodes.text.retrievers.Twitter.TwitterOutput', u'name': u'#fooBar', u'defn': u''}]
    #
    # It's not totally clear how one would get this without renaming,
    # although notice that it's possible to rename a variable to the same name.
    return json.loads(resp._content)

def delete_variable(cid,nid,vname):
    """Deletes an existing variable."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "delete_variable",
          "cid": cid,
          "nid": nid,
          "vname": vname}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # returns a list of the variables associated with the node
    return json.loads(resp._content)

def get_variables(cid, nid):
    """Gets a list of the node's output variables."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "get_variables",
          "cid": cid,
          "nid": nid}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # returns a list of the output variables associated with the node
    return json.loads(resp._content)

# JAC - it will return something like {text: answers[*], type: ArrayList<String>}.
# then, if you apply a definition like *answers[f1]* in a subsequent mapping,
# you will produce something of String type...
#
#   f1 - is the first 1 element
#   r10 - is some random 10 elements
#   m3 - would be the middle 3 elements
#
# And anything where the number > 1 will be of list type.
def get_output_tree (cid, nid):
    """Get details of all the possible outputs."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "get_output_tree",
          "cid": cid,
          "nid": nid}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # output information as json
    return json.loads(resp._content)

# JAC - this is how variables get connected together with arrows
def set_variable_definition(cid,nid,vname,vdef):
    """Changes a variable definition."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "set_variable_definition",
          "cid": cid,
          "nid": nid,
          "vname": vname,
          "vdef": vdef}

    resp = flowr.POST(headers = standard_headers, data=data)
    # returns a list of the variables associated with the node
    return json.loads(resp._content)

def get_node_output(cid,nid):
    """Get the latest node output."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "get_node_output",
          "cid": cid,
          "nid": nid}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # returns JSON formatted output
    return resp._content

# JAC - Unlike to the other variable-related functions, this doesn't take an nid
# I wonder why, is that intentional?
def get_variable_output(cid,vname):
    """Get the latest node output."""
    
    data={"api_token": token,
          "api_email": user,
          "c": "get_variable_output",
          "cid": cid,
          "vname": vname}
    
    resp = flowr.POST(headers = standard_headers, data=data)
    # returns JSON formatted output
    return json.loads(resp._content)

# flowr_web.py ends here
