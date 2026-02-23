# `x == 1, r_y == 0, r_z == 0`:

Possible, because

`A.1 -> A.2 -> A.3 -> A.4 -> B.1 -> B.2 -> B.3 -> B.4 -> A.5 -> B.5`

# `x == 2, r_y == 0, r_z == 1`:

Possible, because

`A.1 -> A.2 -> A.3 -> A.4 -> B.1 -> B.2 -> B.3 -> A.5 -> B.4 -> B.5`

# `x == 1, r_y == 0, r_z == 1`:

Impossible, because

If `main` observes `x == 1`, then `A.4` happened before `B.5` AND `B.4` happened before `A.5`.
But if `main` observes `r_z == 1`, then `A.5` happened before `B.4`, which contradicts the assumption above.
Q.e.d.
