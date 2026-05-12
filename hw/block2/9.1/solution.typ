#import "@preview/cetz:0.5.0"
#set page(width: 18cm, height: auto, margin: 1cm)

#show title: set align(center)
#set text(font: "New Computer Modern")
#set par(justify: false)

#let pic(bars, points) = cetz.canvas({
  import cetz.draw: *
  set-style(rect: (stroke: none))

  let unit = 2
  let height = 0.5
  let padding = 0.1

  let blue = rgb("#5367aa")
  let green = rgb("#44b480")
  let red = rgb("#ec718d")

  for (i, (name, rects)) in bars.pairs().enumerate() {
    let y = -i * height - padding * i
    content((-unit / 2, y), [#name])

    let color = if calc.rem(i, 3) == 0 { blue } else if calc.rem(i, 3) == 1 { green } else { red }

    for ((start, end), name) in rects {
      start *= unit
      end *= unit
      let len = end - start
      rect((start, y - height / 2), (end, y + height / 2), fill: color)
      let mid = start + len / 2
      content((mid, y), [#name], wrap: text.with(white))
    }
  }

  for (i, points) in points.enumerate() {
    for (x, color) in points {
      x *= unit
      let y = -i * height - padding * i
      circle((x, y), radius: height / 2 - padding, fill: color, stroke: none)
    }
  }
})

= 9.1

```java
public void enq(T x) {
  int i = tail.getAndIncrement(); // E.1
  items[i].set(x);                // E.2
}
```

An example execution showing that the linearization point for `enq` cannot occur at line `E.1` (white points are `E.1`s, black points are `E.2`s):

#pic(
  (
    A: (((0, 5), "enq"),),
    B: (((1, 3), "enq"),),
    C: (((3, 4), "deq"),),
  ),
  (
    ((0.5, white), (4.5, black)),
    ((1.5, white), (2.5, black)),
  ),
)

In the example above, `deq` will return the element inserted by thread B, but thread A has completed `E.1` earlier, and if we consider it to be the linearization point, we should've got the element it tried to insert.

The "inflation" of the first `enq` might be due to thread A working slower than its siblings.

An example execution showing that the linearization point for `enq` cannot occur at line `E.2`:
#pic(
  (
    A: (((1, 3), "enq"),),
    B: (((0, 4), "enq"),),
    C: (((4, 5), "deq"),),
  ),
  (
    ((1.5, white), (2.5, black)),
    ((0.5, white), (3.5, black)),
  ),
)

In this example, even though A completed `E.2` earlier than B did, B still has put its element further to the left of the array because it finished `E.1` before A did. Thus `deq`, going from left to right, will encounter B's element first. If we consider `E.2` to be a linearization point, this should not be a valid execution.

We have shown that neither `E.1` nor `E.2` are linearization points for `enq`, yet that does not mean that `enq` is not linearizable. Definition of linearizability does not state the linearization point is fixed, it depends on the outcome and the order of operations.
