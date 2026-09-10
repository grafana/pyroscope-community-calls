# Session runbook — 2026-09 community call 

Everything runs on the local stack. Pre-flight is in `notes/setup.md`; the
short version is `make up` at least 30 minutes early, `make smoke` passing, the
four tabs open, `gcx config current-context` showing `bloom-local`, and the
example trace IDs from §1 already written down.

---

## 0. Intro/Motivation [3 min]

Go to http://localhost:8080/

Today I want to talk about how Tempo and Pyroscope (traces and profiles) can be used in beautiful harmony together to solve performance issues.

We’ll start with this demo app called bloom. It’s a simple flower shop with some insidious performance bugs to help make a compelling demo. Because we have to set up a hero’s journey here.

This demo app is running locally on my machine with open source Grafana, Tempo, Pyroscope, and k6 load testing so that we see some real data in Grafana. All of the source code will be available at some point in our community calls repo so please feel free to run this at home <prompt Tiffany>

<click around, show adding flowers>

We’ve been hearing customer reports of some slow checkouts, but haven’t really been able to reproduce ourselves.

<add a few flowers, checkout, “pretty fast”>
<add a bunch of flowers, checkout, “maybe slow?”>

So let’s dive in to Grafana to see if we can tell what’s going on

## 1. The trace says *where* the issue is

Since this is technically a Tempo call, I think maybe it’s appropriate to start with traces?

http://localhost:3000/

Traces are extremely useful for finding WHERE the issue is! Let’s go to Traces > drill down to try to find our /checkout traces.

[traces drill down > duration radio button > select span.name attribute > select ‘include POST /shop/checkout’]

[if needed link]

This is live data from the last 30 min running on my local machine. So we can see here in this histogram by duration we have most checkout calls in the ~20ms range and some in the 1 - 2 sec range. Which is quite a jump. Let’s dive in.

[Click ‘slow traces’, open one in new tab]

Let’s try to get a lay of the land for what’s going on here. I see a bunch of GETs to individual product ID’s, and they’re sequential! Definitely not good. Individual DB statements are in microseconds which seems reasonable to me.

[Scroll down to show]

But if you see this bloom-pricing API call to POST /quote, this seems to be the vast majority of the time spent. Now as engineers we must ask the next question, which is WHY?? As mentioned before, traces are sometimes the *WHERE*, but profiles are often the *WHY*

## 2. The profile says *why*

If you have traces to profiles configured in Grafana [show links] then you can see the flame graph inline for the span.

[Click the POST /quote span to open the flamegraph]

What we have here is the cpu flame graph of the profile captured for this particular span. For those unfamiliar with flame graphs, it roughly resembles a stack trace where the vertical axis is showing the call tree and the horizontal axis is a representation of the percentage of time each function took on the cpu.

Some things to note here [scroll down to bottom of flame graph]:

You can see the vast majority of time in this span is spent in the discount engine building a quote, which calls bestBundleDiscount (etc) all the way down to string equals. It’s been a while since I’ve done Java, but this seems to be extremely fishy.

I want to go back to the traces drill down page, and see if we see a similar shape for the p50 low latency checkout calls.

[go back to other tab, drag small box on bottom, click one.]
[link if it doesn’t work]

What stands out to me is there are way fewer products. Still have the sequential problem. And the /quote call itself is much much faster too. Using this strong  N=2 sample, we’re starting to see some suggestion that maybe the /quote performance issues are a function of the size of items in the checkout cart. This is a good time to check out profiles heat map.

## 3. The span heatmap shows distribution

We found one slow request, let’s see if we can get some information about the distribution of the problem.

[go to profiles drill down, explain the view, go to bloom-pricing flamegraph]

This is all showing averages, let’s turn on the span heatmap, a new feature, to see the distribution. You can see the dense band at the bottom: most requests are fast. But there’s quite a long tail.

[explain exemplars, open exemplar for long request and exemplar for quick request. Show different shapes for the flame graph]

So we have a pretty good idea of exactly where in the code the issue is. What’s next? Dive in to the code of course. [prompt Tiffany to show link]

[select DiscountEngine.quote and click function details, show source code]

We can see relatively little time is spent in eligiblePromotions, let’s dive in to bestBundleDiscount. Note you can see this same info in the actual flame graph too. Looks like lot’s of discountFor calls [3 times]. I’m seeing this promotion.AppliesTo call.

Diving in to this, you see something interesting: the actual computation is simple (self ~20ms) but the total is very large. This suggests that it’s not this particular computation that’s expensive, but instead that it’s getting called WAYY too many times. Let’s zoom back out.

Going back to DiscountEngine.bestBundleDiscount you can see the inefficiencies: discountFor(a, items) being called multiple times, a synthetic refinementRounds loop to make this code super slow on purpose… etc.

## 4. Wrap up

Thank you for going on this hero’s journey with me where we were able to go from a customer report through to traces to see the *where* of the issue, and drill down (no pun intended) in to profiles to see the *why* of the issue.
