#import "@preview/ctheorems:1.1.3": *
#show: thmrules.with(qed-symbol: $square$)

#set page(width: 16cm, height: auto, margin: 1cm)

#let proof = thmproof("proof", "Proof")

#show title: set align(center)
#set text(font: "New Computer Modern")
#set par(justify: true)

= 7.1.1

For two arbitrary intervals $X = (x_1, x_2), Y = (y_1, y_2)$ at least one of the following holds:
- $X$ and $Y$ are disjoint and $X -> Y$
- $X$ and $Y$ are disjoint and $Y -> X$
- $X$ and $Y$ overlap.

#proof[
  $x_1, x_2, y_1, y_2$ are events. Precedence is a _strict_ total order on events, so for an arbitrary interval $I = (a, b)$ we have $a -> b$ (allowing $b -> a$ only makes the definition of an interval confusing and harder to reason about, so it's irrelevant).

  If $x_2 -> y_1 or y_2 -> x_1$ is false, i.e. $y_1 -> x_2 and x_1 -> y_2$, then $X$ and $Y$ overlap.

  Let's examine the cases where $X$ and $Y$ are disjoint:
  - $x_2 -> y_1$: by definition, $X -> Y$
  - $y_2 -> x_1$: by definition, $Y -> X$.
]

= 7.1.2

Relation of precedence on intervals is
1. Irreflexive: $forall X space not(X -> X)$
2. Antisymmetric: $forall X, Y space X -> Y => not(Y -> X)$
3. Transitive: $forall X, Y, Z space (X -> Y and Y -> Z) => X -> Z$

#proof[
  1. Assume an arbitrary interval $X = (x_1, x_2)$. $X -> X$ means $x_2 -> x_1$, which contradicts with $x_1 -> x_2$ shown above, so $X -> X$ is impossible.
  2. Assume $X = (x_1, x_2)$, $Y = (y_1, y_2)$ and $X -> Y$.
    By definition of precedence on intervals we have $x_2 -> y_1$ and $y_2 -> x_1$.
    Earlier we have shown that $forall I space I=(a, b) => a -> b$, so $x_1 -> x_2$ and $y_1 -> y_2$.
    Combining these relations, we get
    $ x_2 -> y_1 -> y_2 -> x_1 -> x_2 -> ... $
    which leads to contradiction with irreflexivity of precedence on events.
  3. Assume $X = (x_1, x_2), Y = (y_1, y_2), Z = (z_1, z_2)$, $X -> Y$ and $Y -> Z$. By definition of precedence on intervals:
    $ X -> Y <=> x_2 -> y_1 quad Y -> Z <=> y_2 -> z_1 $
    Combining these relations and the fact that $y_1 -> y_2$:
    $ x_2 -> y_1 -> y_2 -> z_1 => x_2 -> z_1 $
]

Example of $X != Y$ where $not(X -> Y) and not(Y -> X)$:

Let $X = (x_1, x_2)$, $Y = (y_1, y_2)$, $x_1 -> y_1 -> x_2 -> y_2$.
$x_1 = y_1$ and $x_2 = y_2$ are impossible due to irreflexivity, so $X != Y$.
We have $x_1 -> y_2$, which disallows $y_2 -> x_1$ and $y_1 -> x_2$, which disallows $x_2 -> y_1$, leading to the conclusion that niether $X -> Y$ nor $Y -> X$ is true.
