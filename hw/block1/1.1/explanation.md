# Case 1

- Thread B gets killed because of the uncaught exception
- Thread A awaits the end of execution of B, which happens with the exception
- Exception that killed B is not propagated further
- A and Main terminate gracefully

# Case 2

- Exactly as case 1, with an addition that thread C was waiting for B's termination and it already happened when it
  `join`ed

# Case 3

- Same thing as case 2. Because `join`ing means simply waiting for the target's termination, D calmly awaits A's
  termination and then terminates