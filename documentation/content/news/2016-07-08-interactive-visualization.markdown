---
author: michaela
date: 2016-07-08 12:44:09+00:00
draft: false
title: 'The Science behind new/s/leak II: Interactive Visualization'
type: post
tags: ["project info"]
categories:
- InfoViz
- Science
---

We already explained the [language-related data wrangling](http://newsleak.io/2016/04/08/the-science-behind-newsleak-i-language-technology/) happening under new/s/leak's hood. For the success of new/s/leak, our second scientific field is the game changer: interactive visualization. No matter how much accurate information we can produce - if we cannot present them to the user in an appealing way, the tool will fail its goals. So how exactly is visualization science influencing new/s/leak?


### Your daily dose of visualization science


It might seem easy to create some kind of visualization (with Excel or even pen and paper) - however, there are lots of pitfalls that you need to avoid to create good visualizations. You might take some of them for granted because you encounter them everyday when browsing the web - but they'd be painfully missed otherwise. Two examples which you can find in many applications and websites (and which we of course also consider for new/s/leak):



	  * **Animation speed:** there is a certain animation speed that is pleasant to look at, and it cannot be much faster or slower if we want to convey information. While it's intuitively clear that reaaallyyyy slooooowwww animations can be annoying (think of those endless progress bars...), going to fast can overload you, even if there is no critical information involved. For evidence, take a minute look at those two guys working out (they are doing exactly the same movement).
![www.GIFCreator.me_Io0FHw](http://newsleak.io/wp-content/uploads/2016/06/www.GIFCreator.me_Io0FHw.gif)
![www.GIFCreator.me_fVOqUg](http://newsleak.io/wp-content/uploads/2016/06/www.GIFCreator.me_fVOqUg.gif)

Now answer the question: would you pick the guy on the right as your office mate? Might become exhausting really soon (even if he's really smart).  

	  * **Colors:** If you us colors for information visualization, you cannot just use any color set you find appealing. While there is [scientific work on which color sets are good for which information types](http://colorbrewer2.org), we also need to think about color blind people. On many web pages, the color sets are already designed to be suitable for different types of color perception. See how the new/s/leak logo would look for color blind people (created with [Coblis](http://www.color-blindness.com/coblis-color-blindness-simulator/)):
[gallery columns="2" size="medium" link="file" ids="259,257,258,256"]

Those are just two examples for a whole lot of guidelines that visualization scientists have developed and that we encounter in each _good_ visualization. Of course new/s/leak has to follow all of those guidelines - which becomes harder with more complex data, and on a scale. Which brings us to the next important point:




### Accurate views on loads of complex data


![This happens with too much information displayed at once](https://i2.wp.com/eagereyes.org/wp-content/uploads/2012/02/ecoli_meta3_sm.png?w=400&ssl=1)
The largest challenge (and thus the largest need) for visualization science and language technology alike comes with the huge amounts of data we have to handle. A leak can be anything from 100 Documents to 1TB (or more). This is not only a lot for research-based software, but also enough to break many commercial applications. So, this is where the action is.
Visualizing data for investigative purposes means that the software _may not show anything untrue_ (not even shady, or too ambiguous). However, there is so much data to display (all the documents, their metadata, and all the entities we extracted) - we simply cannot display the whole truth in one screen, that would be a) impossible and b) completely unusable. Imagine a network that shows all the information it has - that quickly becomes a "hairball" like in the picture on the left.

Because new/s/leak should be _intuitively accessible_ for users with different backgrounds and without much training, ![Usability](http://newsleak.io/wp-content/uploads/2016/06/yourcompany-155x300.jpg)
we need to find easy interfaces for giant piles of complex data.  This excludes e.g. some very powerful but rather complicated interfaces used for search in scientific environments.

One extreme way to tackle this is the way Google presents the internet to us, or rather [the 60 trillion pages it has indexed](http://expandedramblings.com/index.php/by-the-numbers-a-gigantic-list-of-google-stats-and-facts/): Initially, Google doesn't display anything, but rather lets the user explore (see comic on the right). While we allow the user to explore the data on their own, new/s/leak's main purpose is to guide users through the data jungle and to provide a concise graphical summary of the core plots. In consequence, we display an initial graph as entry point that contains the most interesting entities and relations. It's a scientific question of its own to find out what _most interesting_ means, but according to our users, _frequency_ is a good indicator here. We thus show initially the most frequent people, companies and places, and let the user explore from there. (If they wish, users can also pick the least frequent entities first - this might foster serendipity.)




### Knowing our users


Coming back to the usability comic - of course we have to design new/s/leak for its users. That's what Apple and Google do, too: providing a simple one-fits-all screen as an entry point. While we have a smaller user group, we also have an application with more interaction possibilities. So the key to everything lies in knowing our users, in order to find the right mixture of the google window (with too little functionality) and an overloaded brick puzzle (which is ugly and unusable).

User studies are, in fact, an important pillar of visualization science. We already told you about[ early requirements management](http://newsleak.io/2016/02/23/requirements-management/). It might sound trivial that we ask people what they like and dislike, and then we change the system accordingly - but this is no matter of course at all (see e.g. [some excuses why companies don't do user research](http://www.uxmatters.stfi.re/mt/archives/2016/03/excuses-excuses-why-companies-dont-conduct-user-research.php?sf=dvapnv#aa)). One of the reasons for this is that it takes actually lots of experience to design a good user study, which assess all the information needed without influencing the user, and which allows to generate meaningful hypotheses for interface changes. Further, it takes simply quite some time to undertake such studies repeatedly. Fortunately, our visualization scientists are experts for user studies, and they are dedicated to testing our system frequently and in a way that allows for objective, comprehensive evaluation.
