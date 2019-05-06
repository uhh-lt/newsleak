---
author: michaela
date: 2016-04-08 13:12:22+00:00
draft: false
title: 'The Science behind new/s/leak I: Language Technology'
type: post
tags: ["project info"]
categories:
- General
- Language Technology
- Science
---

Because of the Easter holiday season and several conference deadlines, this blog had to take a little break. Being back, we want to give a glimpse on the science behind of new/s/leak.

We have two camps of scientists working together: computational linguists contribute software that extract semantic knowledge from texts, and visualization experts who  bring the results to smart interactive interfaces that are easy to use for journalists (after the computational linguists made the dataset even more complicated than before).

In this post, we will explain some of the semantic technology that helps to answer the questions "Who does what to whom - and when and where?". The visualization science will be covered in a later feature.


### Enriching texts with names and events


The results of the language technology software are easy to explain: we feed all the texts we want to analyze into several smart algorithms, and those algorithms find names of people, companies and places, and they spot dates. On top of those key elements (or "entities"), we finally extract the relationships between them, e.g. some person talks about a place, leaves a company, or pays another person. Finally, we are ready to put all of this into a nice network visualization.

![NE-schema](/img/2016/04/NE-schema-300x229.png)
Entity and Relation Extraction for new/s/leak

We hope that you're not ready to accept that all of this simply happens by computational magic, so let's dig a bit deeper:

_(Disclaimer: T__his is not a scientifically accurate__ explanation, but rather a vey brief high-level illustration of some science-based concepts.)_


### Identifying names - 🍎 vs. 


Identifying words that name people, organizations and so on is not as easy as it might sound. (In Computational Linguistics, this tasks is called [Named Entity Recognition](https://en.wikipedia.org/wiki/Named-entity_recognition), in short: NER).

Just looking through a big dictionary containing names works sometimes, but many names can also be things, like Stone (that can be Emma Stone or a rock) or Apple (which can be food or those people who are selling the smartphones).  Within a sentence however, it's almost always clear which one is meant (at least to humans):


<blockquote>"Apple sues Samsung."</blockquote>


..is clearly the company, whereas


<blockquote>"Apple pie is really delicious."</blockquote>


probably means the fruit. The examples also show that just checking for upper or lower case is not sufficient, either.

What the algorithms do instead is first deciding whether a word is a name at all (as in the  case), or rather some common noun (that's the 🍎 case). There are two factors that decide that: first, how likely the string "apple" is to be a name, no matter in which context. (Just to put some numbers in, say the word _apple_ has a 60% likelihood of being a company, and 40% to be a noun.) Additionally, the algorithms checks the likelihood to have a name in the given context. (Again, with exemplary numbers:  any word, no matter which one it is, in the beginning of a sentence followed by a verb, has a likelihood of 12% to be a name; followed by a noun, the likelihood is 8%, and so on).

With this kind of information, the NER algorithm decides whether, in the given sentence, _Apple_ is most likely to be a name (or something else).

In the final step, the algorithm uses similar methods to decide whether the name is more likely to belong to a person, a company or a place.

There are many different tools for named entity recognition; new/s/leak uses the [Epic system](https://github.com/dlwh/epic).


### Timing!


In principle, extracting dates (like "April 1st" or "2015-04-01") works very similar to extracting names. But often dates are incomplete - then we need more information: If we only find "April 1st" with no year given, we need some indicator which year could be meant. In our case, the algorithm checks the publishing date of the document (which we almost always have for journalistic leaks) and defaults all missing years with the publishing year.

The extraction of time expressions in new/s/leak is done with the [Heideltime tool](http://dbs.ifi.uni-heidelberg.de/index.php?id=129).


### Finding relations (or _events_)


Now that we found that somewhere in our text collection are  Apple and Samsung, and both are companies, we want to know whether or not they actually have some business together, and if so, how they are connected. The algorithms behind this do a very human-like thing: they read  all the texts and check whether or not they find Apple and Samsung (as companies) in the same document, and if so, they try to find out whether there is some event (like "suing" in the sentence above) that connects the two directly. There might also be multiple such relations, or they might change over time - then we try to find the most relevant ones. _Relevant_ events in our example are things mentioned frequently for Apple and Samsung, but rarely in other contexts. E.g. if we find additionally the sentence "Apple talks about Samsung" somewhere, _talking_ would probably be less relevant than _suing_ (from  "Apple sues Samsung"), because talking shows up more often than suing and is not very specific for the Apple / Samsung story.

To find relations between entities, we use the same system employed in the [Network of Days](http://tagesnetzwerk.de), together with relevance information computed by [JTopia](https://github.com/srijiths/jtopia).

Now that we have all this information about people, organizations, times and places, the software of our visualization scientists can display them together into one interactive graph. This visualization science part will be covered in one of the next entries.
