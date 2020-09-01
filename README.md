# AddDiscourse
Explicit Discourse Connectives Tagger Java Implementation 
Below is the instruction for Perl Script.

Explicit Discourse Connectives Tagger - December 14, 2009
-------------------------------------------------------------------------------

This tool is built to automatically identify explicit discourse connectives
and their sense (Expansion, Contingency, Comparison, Temporal).
It takes syntactic parse trees as input and outputs augmented
trees with tags for each discourse connective.

This tool is based on the work described in:
Emily Pitler and Ani Nenkova.  Using Syntax to Disambiguate Explicit
Discourse Connectives in Text.  Proceedings of the ACL-IJCNLP 2009
Conference Short Papers, pages 13-16.

QUICKSTART

At a terminal, while in the addDiscourse directory, type:
perl addDiscourse.pl --parses sample-parse.txt

This will read in the parse trees in sample-parse.txt and output them
augmented with discourse connective tags.  Each word or phrase which
can be a discourse connective is tagged with an ID number 
and its predicted sense (or 0 if predicted non-discourse).
The word, its id, and its sense are separated by # marks.

The possible senses are: Expansion, Contingency, Comparison, Temporal,
or 0 (non-discourse usage).  The four discourse usage tags
correspond to the top level of the sense hierarchy in the Penn
Discourse Treebank.

For example, 
as#6#Temporal should be read as 'the word as in this context has an id of 6, 
and a sense of Temporal'.

Id numbers are used to identify multi-word or long-distance connectives.
The shared id numbers for: 
In#0#Expansion 
and 
addition#0#Expansion
show that ``In addition'' is just one instance of an Expansion connective.


INSTALLING

Uncompress addDiscourse.tar.gz by typing:
gunzip < addDiscourse.tar.gz | tar -xvf -

The file addDiscourse.pl is the program which identifies each
instance of a discourse connective, classifies them,
and prints out the augmented trees.

The resources folder contains two files: connectives.info and connectives.txt.

connectives.info contain the learned feature weights.  This is the
output of Mallet's MaxEnt classifier trained on sections 2-22
of version 2 of the Penn Discourse Treebank.

connectives.txt contains a list of the words and phrases to
consider as possible connectives.  
If you wish, you may add to this list.  Words or phrases 
that were unknown at training time will be classified solely on
the basis of their syntactic context.  
Long-distance connectives (like ``On the one hand...on the
other hand") are specified using .. between the first
half and the second half (on the one hand..on the other hand).


INPUT

Input to the program is specified through the --parses command line
argument:
ie. perl addDiscourse.pl --parses sample-parse.txt

Either a directory or a file can be passed in.  If a directory is given,
it will annotate each of the files contained in that directory.

The input expected are syntactic parse trees.  The trees
can be either pretty-printed or one sentence per line, it makes
no difference.

OUTPUT

The location of the output is specified through the --output command line
argument:
ie. perl addDiscourse.pl --parses sample-parse.txt --output sample-out.txt

If a file is specified, then output is written to that file.  If a directory
is given, then individual files are placed in to that directory
in the format directory/filename.disc

If no output argument is given, then addDiscourse.pl writes to stdout.  
