---
author: michaela
date: 2016-02-23 10:30:05+00:00
draft: false
title: Requirements Management
type: post
tags: ["project info"]
categories:
- General
---

User requirements management is something that happens far too rarely, especially in scientific software. ([And it](http://dilbert.com/strip/2001-04-14) [can definitely](http://dilbert.com/strip/2003-03-21) [be challenging](http://dilbert.com/strip/2003-03-22).)

For our project that brings together so different worlds of science and journalism, and also different academic disciplines, it's even more important. We dedicated this a whole day on which we had Franziska and Kathrin over at SPIEGEL in Hamburg - and we proved that requirements analysis can be both, challenging and fun at the same time.

Overall,  Kathrin and Franziska interviewed four journalists from different newsrooms that showed the whole diversity of potential new/s/leak user groups .


### New Priorities


Some of the journalists' answers were interesting just because they prioritize things we thought were maybe nice from our point of view, but maybe not so important to the end user. So here is the top 3 of surprising lessons learnt:



1. **Metadata** that comes with the documents is even more important than we thought. Our software thus should not just display some selected metadata features (like time and geolocations), but rather show everything we can extract from the data, including e.g. also data types and file sizes. (One showcase for the journalistic value of metadata is [this feature about the Hacking Team Leak](https://labs.rs/en/metadata/).)
2. **Source Documents** have to be always accessible. Our initial idea was to focus on the network of entities and to show the documents just on demand - but the journalists need a direct way to the original documents in each view, and then filter the documents by selecting certain entities, entity relations, time spans or other metadata.
3. **Networks** are an utterly intuitive concept. Many concepts and figures from network theory (like centrality, connectedness, outliers...) have intuitive counterparts ("Who is in the center of all of this?","Who is best connected to whom?", "Can I see who's at the top of the communication hierarchy?"), and can provide crucial information. That's good news, and that also means that we have to be even more flexible when computing the connections in the network.

![Scribbling User Requirements](/img/2016/02/2016_02_02_cafe_05-300x225.jpg)
Drafting the next new/s/leak version after the interviews


### User-Specific needs


Some functionality needs to be highly adaptable to meet the needs of different user groups and different working styles. The focus here is on two things:


1. **Powerful tagging functionality**. We need to support free-text tagging, bookmarking and simple markers like "important" vs. "unimportant". This allows users to create their own metadata.
2. **Transparency**. Some users prefer precise results over extended functionality, other users (especially people working under time pressure) would sacrifice a bit of accuracy for more automated support to filter the data. To meet both needs, we will provide as much automated support as possible, but at the same time, we will clearly indicate what the machine generated, how confident we are about the machine's result, and which part of the information is genuine (as in: was part of the source documents).

![new/s/leak sketch](/img/2016/02/Sketch-02-300x225.png)
The scribbled wireframe (with some annotation)

The productive day at SPIEGEL was concluded with some final discussions, first drafts of wireframes, and coffee (see pictures).

Our next goal is to finish a first stand-alone prototype, with a special focus on relation extraction for the network.
