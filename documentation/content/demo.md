---
author: seid
date: 2017-03-08 13:04:55+00:00
draft: false
title: Demo
type: page
menu: main
weight: 30
meta: false
---

## Live demo

We prepared three publicly available demo instances of new/s/leak. All demos can be accessed with the credentials username `user`, and password `password`

* 250k English emails from the [Enron Dataset](https://ltdemos.informatik.uni-hamburg.de/newsleak-enron)
* 12k pages from German [parliamentary investigation reports on the NSU murder case](https://ltdemos.informatik.uni-hamburg.de/newsleak-nsureports)
* 27k articles from a multi-lingual collection of Wikipedia documents related to [World War II](https://ltdemos.informatik.uni-hamburg.de/newsleak-nsureports) (see also case study below)

## Case study WWII

{{< youtube 96f_4Wm5BoU >}}

Ca. 27.000 documents in our sample set are Wikipedia articles related to the topic of World War II. Articles were crawled from the encyclopedia in four languages (English:en, Spanish:es, Hungarian:hu, German:de) as a link network starting from the article "Second World War" in each respective language.

From a perspective of national history discourses and education, a certain common knowledge about WW2 can be expected. But, the topic becomes quickly a novel unexplored terrain for most people when it comes to aspects outside of the own region, e.g. the involvement of Asian powers. In our test case, we strive to fill gaps in our knowledge by identifying interesting details regarding this question. 

First, we start with a visualization of entities from the entire collection which highlights central actors of WW2 in general. In the list of extracted location entities, we can filter for ca. 2,000 articles referencing to Asia (en, es), Azsia (hu) or Asien (de). In this subselection, we find most references to China as a political power of the region followed by India and Japan. 

Further refinement of the collection by references to China highlights a central person name in the network, Chiang Kai-shek, who raises our interest. To find out more, we start the filter process all over again, subselecting all articles referencing this name. The resulting entity network reveals a close connection to the organization Kuomintang (KMT). 

Filtering for this organization, too, we can quickly identify articles centrally referencing to both entities by looking at their titles and extracted keywords. From the corresponding keyterm network and a KWIC view into the article fulltexts, we learn that KMT is the national party of China and Kai-Shek as their leader ruled the country during the period of WW2. 

A second central actor, Mao Zedong, is strongly connected with both, KMT and Chiang Kai-shek in our entity network. From articles also prominently referencing Zedong, we learn from sections highlighting both person names that Kai-shek and Zedong, also a member of KMT and later leader of the Chinese Communists, shared a complicated relationship. By filtering for both names, we can now explore the nature of this relationship in more detail and compare its display across the four languages in our dataset.