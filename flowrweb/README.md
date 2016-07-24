# flowrweb

Code for working with the Goldsmiths FloWr (Flowchart Writer) API.

**Outline**

- [Instructions for using the current system](https://github.com/holtzermann17/FloWrTester/tree/master/flowrweb#usage)
- [Directions for further work](https://github.com/holtzermann17/FloWrTester/tree/master/flowrweb#some-thoughts-on-next-directions)

## Usage

### To get started:

1. Obtain a current API token from http://ccg.doc.gold.ac.uk/research/flowr/flowrweb/ `->` Admin `->` API.
2. Load the project with Cider via `./src/flowrweb/core.clj` -> `M-x cider-jack-in`.
3. Set the token obtained in Step 1 with `(def flowrweb-token <your token>)`
4. Download information from the server with `(init-local)` (but see step 6 for an alternative).
5. Cache downloaded information into a file with `(save-local)`.
6. You can restore information from the cache with `(reinit-local)`.

Under the hood: Downloaded information is stored in the variables `node-store`, `user-charts`, and `type-store`.

### Do some things with the downloaded information.

1. For interactive use you may want to readjust `*print-length*` via `(set! *print-length* nil)`.
2. List available nodes with `(all-node-names)` or `(chart-starting-nodes)`
3. Print out type information for a given node via, e.g., `(all-type-info-for-type "text.retrievers.Dictionary")`.  This simply grabs the matching entry from the `@type-store`.   Alternatively, that information can be processed further:
  - List outputs via, e.g., `(available-outputs-for-type "text.retrievers.Dictionary")`
  - List inputs via, e.g., `(available-inputs-for-type "text.retrievers.Dictionary")`
4. **Note:** The output from Step 3 is not particularly user friendly.   One of these is likely preferable:
  - Show the specific ways two node types could be connected via, e.g.,

           (potential-connections "text.retrievers.Dictionary" "text.categorisers.WordSenseCategoriser")
  - Show the node types that could be hooked up downstream of a given node type via, e.g.,

           (potential-downstream "text.retrievers.Dictionary")
  - Show the node types that could be hooked up upstream of a given node type via, e.g.,

           (potential-upstream "text.retrievers.Dictionary")

  - Show the specific fields that could be hooked up upstream of a given node type via, e.g.,

           (potential-downstream-fields "text.retrievers.Dictionary")
  - Show the specific fields that could be hooked up upstream of a given node type via, e.g.,

           (potential-upstream-fields "text.retrievers.Dictionary")


## Some thoughts on next directions

### Tests beyond Types

I've written some [tests (in Java)](https://github.com/holtzermann17/FloWrTester/tree/master/ccg/flow/tests)
that can specialise the constraints on fields, e.g. `IsWord` will enforce a string to have no spaces:

```java
package ccg.flow.tests;

public class IsWord {
    public static boolean runTest(String candidate) {
	if (candidate.contains(" ")){
	    return false;
        }
	return true;
    }
}
```

Naturally there are other ways this could be implemented.

- [ ] **Tests that are more specific than "types" should be included in the rules that check whether nodes can be connected.**

### Library of exemplar programs

One straightforward idea that I had a while ago was to take all of the available exemplar programs and try to reimplement them via a search process, or try and understand the relevant constraints (e.g., perhaps some input ranges over all *nouns*).

- [ ] **Library of exemplar programs and constraints**

### Automatic programming directly in Clojure

Supposing I wanted to implement some "nodes" locally as Clojure functions.  How difficult is
it to expose the types (and other information, see below) as an API?  Does support
for this already exist either natively or in a package?

- [ ] **Write some nodes locally as Clojure functions.**

### Programming for code generation rather than poetry generation

What about targeting the evolution of programs explicitly, rather than
the evolution of texts (like poems).  That is, rather than
poem-writing programs, we could focus on program-writing programs.
Note that *(poem-writing program)-writing programs* would then be a
special case.

- [ ] **Write a collection of processes focused on generating programs rather than focused on generating text.**

### Devise specific fitness functions for question-answering and creative ideation tasks

I was approaching automatic programming in a typical *genetic programming*
spirit, then I would have some relatively clear fitness functions to optimise
towards.  But combining FloWr nodes without a clear objective
seems aimless.  Without a clear goal, it's difficult (or impossible) to make progress.  On the other hand, if we devise a relatively
concrete goal -- e.g. to engage in question-answering tasks, or
to engage in creative ideation about some specific topic -- then we start to
see some specific fitness functions materialising.  One challenge problem
with measurable outcomes is matching Stack Exchange questions and answers.
Might it make sense to use automatic programming to generate a custom
process for searching for a match for a given piece of input?  How about
for approaching the larger-scale challenge of coming up with an original
question or answer?

*Example*: For one current state of the art program (Siri) asking
"Where is the closest gas station?" does not yield particularly convincing
results.

> "Where is the closest gas station?"
>> [list of 16 gas stations nearby]

> "Which ones of those are open?"
>> Would you like me to search the web for 'Which ones of those are open?'?

A more intelligent process might parse the initial query into a simple flowchart
that could answer the question.  Perhaps a general-purpose function like
"information gain" could be used to compute the relevance of responses in a dialogue
or a [collaborative poetry exercise](http://arxiv.org/abs/1606.07955).

- [ ] **Devise fitness functions for some specific domain(s).**

### Programs to evaluate text

Whereas most FloWr nodes that exist so far are generative, or
"poietic," it would be very useful to have a bigger collection of
"aesthetic" nodes that would be able to evaluate what's in a text.
For example, supposing someone --
[perhaps a computer program](http://arxiv.org/abs/1604.08781) -- wrote
a story with the theme "The worst Thanksgiving dish you ever had."
Could we write programs that could tell whether or not this was a
strong effort?  How about programs that could identify simple features
like a "beginning, middle, and end"?  In
[Computational Poetry Workshop: Making Sense of Work in Progress](http://ccg.doc.gold.ac.uk/papers/corneli_iccc15_poetry.pdf),
we list a bunch of questions that a human poetry expert might ask,
along with other questions that should be straightforward to operationalise
in computer programs.

- [ ] **Write a library of aesthetic functions that can evaluate a text (e.g., a generated text).**

## License

Copyright © 2015 Joseph Corneli

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
