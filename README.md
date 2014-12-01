
# Structured content as graph database

Design goals:
 * Complement versioned storage with navigable relations that 
 * Zero redundancy for content.
 * Use redundancy for relations, to make it easy to scope out features. Only model the relations that we can implement support for.

## Definition of Content

The Content Unit, a.k.a. Authoring Unit.

If the content graph contains the actual data, Content nodes would point to Model nodes.
For generic XML those would have "type" (node name) and "body" (serialized content).
Attributes would (probably) be arbitrary keys prefixes with "attr_".

### Generic XML

* Generic XML nodes are, per schema/dtd, divided into one of Structure|Content|Inline.
* Anything that isContent()===true is considered Structure, because we never traverse to Inline (i.e. below Content).
* The Content node is identified by the checksum(s) of normalized content (with serialized Inline) and output-relevant attributes. 
* Defined as above, Structure nodes don't need a checksum because they only contain an ordered list of Structure|Content nodes.

Noteworthy:
 * Tables could be of different kinds, content-wise. Content units can be rows, columns or cells. The choice depends on table semantics.

### Modelled authoring

* Allows Content nodes to have children, for example a section's content may be the title.
* The model defines the insertion points for child Content, either a fixed number and order of children or a single point for 0+ children.

## Definition of Map (or Document)

Has Revisions or Commits? See MapRevision javadoc in se.repos.se.indexing.graphdb.neo4j.GraphdbLabels

## The relationships between Structure and Content

Note that relations might be modelled without Structure nodes, as "pos" hierarchy from Map.

A relation Map|Structure -> Content is always a Has, but can be other relationship types too.

 * Has. The basic relation.
   - From Map to every Content that is a descendant.
   - From Structure to every Structure|Content that is a child.
 * Keeps. A Content kept in same position (disregarding Adds and Deletes).
 * Adds. New Content.
 * Deletes. Content no longer referenced. Pos undefined but previous pos could be relevant.
 * Reuses. Specialization of Adds.
 * Moves / Keeps-reordered. Specialization of Adds+Deletes.
 
### Reuse

The first form of reuse that we track (add relations for) is full resource copy.
 * Should be a single commit where destination may have modified properties.
 * Destination must have unchanged content.
 * Source modifications are irrelevant because reuse will be marked from previous revision, as the copy might be a move.

## Running with repos-indexing-standalone

Indexing logic should ideally add support for:
 * Let an XML node handler run for all revisions, like incremental indexing, but omit reposxml that is head only.
 * Stop at content nodes (definition per schema/dtd/document, see above)

Apply `repos-indexing-standalone.patch` for adding handlers, tweaking logging for development and adding java7 support.

## checkdata1 sample repo

Load and reindex the svndump `checkdata1.svndump`.

http://localhost:8080/solr/repositem/select?q=repo%3Acheckdata1&sort=rev+asc&rows=20&fl=id%2Crev%2Ctype%2Cpath%2Crevcomment%2Curl%2Cprop_*&wt=json&indent=true

```
#$ wget -O - http://debian.neo4j.org/neotechnology.gpg.key | sudo apt-key add - # Import our signing key
#$ cat /etc/apt/sources.list.d/neo4j.list
#deb http://debian.neo4j.org/repo stable/

sudo service neo4j-service stop
sudo rm /var/lib/neo4j/data/* -Rf
sudo service neo4j-service start
```
