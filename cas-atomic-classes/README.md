# Every Field Is Atomic. The Balance Is Still Negative.

Runnable code for the article on compare-and-set and the atomic classes in Java.
Every output quoted in the article comes from these programs, run five times each on
the machine described in its Environment block.

One Java file per demo. No build tool, no dependencies. JDK 21 or newer.

## Environment

```
Intel Core i7-4700HQ (4 cores / 8 threads, x86-64)
16 GB DDR3-1600
Ubuntu 26.04
Amazon Corretto 25.0.4 (also run on 21.0.11)
```

## Compile

```bash
javac -d out src/*.java
```

## Run

```bash
# Every Field Is Atomic / Compare-and-Set
java -cp out Overdraft check-then-act   # get() then addAndGet(): overdraws
java -cp out Overdraft cas              # compareAndSet loop: never does

# What One Atomic Call Buys You
java -cp out AtomicCounter              # volatile int vs AtomicInteger

# The Loop Can Run Your Code Twice
java -cp out ReappliedFunction 1
java -cp out ReappliedFunction 8

# No Lock Does Not Mean No Waiting
java -cp out CasContention 1            # also 2, 4, 8
java -cp out CasContention dump         # thread dump while 8 threads spin

# What compareAndSet Actually Compares
java -cp out IdentityTrap
java -XX:AutoBoxCacheMax=1000 -cp out IdentityTrap cache1000
java -cp out Aba

# When One Variable Is Not Enough
java -cp out TwoFields two-atomics
java -cp out TwoFields one-record
```

Every program prints one result line. If the result does not match what the program
expects, it also prints a line starting with `<<< UNEXPECTED`.

Counts vary between runs and machines. The direction does not.

## One Instruction Underneath — the machine code

`Inc` adds one in two ways: `getAndIncrement()` and a hand-written compare-and-set
loop. These JVM flags print the compiled machine code of `inc()`:

```bash
# getAndIncrement(): lock xadd, no loop
java -XX:+UnlockDiagnosticVMOptions \
     -XX:CompileCommand=print,Inc::inc -XX:CompileCommand='dontinline,Inc::*' \
     -cp out Inc

# the same with that optimization off: lock cmpxchg
java -XX:+UnlockDiagnosticVMOptions -XX:DisableIntrinsic=_getAndAddInt \
     -XX:CompileCommand=print,Inc::inc -XX:CompileCommand='dontinline,Inc::*' \
     -cp out Inc

# off, four threads competing: lock cmpxchg inside a retry loop
java -XX:+UnlockDiagnosticVMOptions -XX:DisableIntrinsic=_getAndAddInt \
     -XX:CompileCommand=print,Inc::inc -XX:CompileCommand='dontinline,Inc::*' \
     -cp out Inc contended
```

HotSpot prints readable instructions only with the `hsdis` disassembler plugin, which
is not shipped with the JDK. Without it, the JVM warns `Loading hsdis library failed`
and prints the raw bytes of the compiled code instead. The lines in the article were
read from those bytes with GNU `objdump`. Linux on x86-64 only; other CPUs use other
instructions.

The `contended` run uses four threads. With one thread the compare-and-set never
fails, and the JIT compiles the failure path as a rare exit instead of a loop.

## Notes

- `Overdraft` and `TwoFields` start all threads with a spinning gate instead of a
  `CyclicBarrier`. A barrier wakes threads one by one, microseconds apart, which hides
  the race. The code under test (`withdraw`, `tryAcquire`) is the same either way.
- `IdentityTrap` depends on the JVM's boxing cache, which is why it also runs with
  `-XX:AutoBoxCacheMax=1000`.
- `CasContention dump` runs `jstack` from the same JDK.
