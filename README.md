# Extracting event information from a page

## Problem Description

I'm not that much of a foodie and I'm usually not picky about what fun stuff to
do, but I do usually want an event of choice to be near me, or convenient in
some way for my friends.

If I am arranging to meet friends, I would look up recommendation websites like
[this](https://thesmartlocal.com/read/cheap-orchard-buffets), and then wish
that I could have all these addresses nicely mapped out.

I thought recommendation websites would follow certain html templates when
publishing, but in the process of building
[gowherene](https://gowherene.herokuapp.com/) I found that

+ It is not all that straightforward to identify addresses, due to
    + Inconsistent formatting on pages
    + Addresses that are wrongly formatted (e.g. missing postal codes)
+ And it is also not trivial to identify the "matching" header (event/food
  recommendation) for a given address

I thought there might be room to use this problem (somewhat familiar to me) as
a fun way to get practice with machine learning.

## Approach

I'll prototype in python, (and also work towards an "Individual Project" for
mlcourse.ai) and then rebuild it in clojure (a language I want to learn well)
to merge it in with [gowherene](https://gowherene.herokuapp.com/)

### Machine Learning Pipeline

1. Predict if segments of html pages are addresses in Singapore
2. Use predictions of addresses to predict the matching "header" for that address

> A *segment* of a html page would be text between two tags. We cannot assume
  that addresses fall neatly between tags, since there might be instances like
  `<div><b>123</b> <b>:</b> <b>Sesame Street</b></div>`.