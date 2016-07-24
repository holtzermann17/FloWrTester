# flowrweb

Code for working with the Goldsmiths FloWr (Flowchart Writer) API.

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


## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2015 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
