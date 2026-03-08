package org.nsu.syspro.parprog.counters.benches;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class NoContentionBaseline {
    @Param({ "ThreadPrivate" })
    String counterType;

    @State(Scope.Thread)
    public static class ThreadState {
        long threadPrivateCounter;
    }

    @Benchmark
    @Threads(1)
    public void thread_private_plain_inc1(ThreadState s) {
        s.threadPrivateCounter++;
    }

    @Benchmark
    @Threads(2)
    public void thread_private_plain_inc2(ThreadState s) {
        s.threadPrivateCounter++;
    }

    @Benchmark
    @Threads(4)
    public void thread_private_plain_inc4(ThreadState s) {
        s.threadPrivateCounter++;
    }

    @Benchmark
    @Threads(Threads.MAX)
    public void thread_private_plain_incmax(ThreadState s) {
        s.threadPrivateCounter++;
    }

    @Benchmark
    @Threads(40)
    public void thread_private_plain_incmaxmax(ThreadState s) {
        s.threadPrivateCounter++;
    }

    @Benchmark
    @Threads(1)
    public long thread_private_plain_get1(ThreadState s) {
        return s.threadPrivateCounter;
    }

    @Benchmark
    @Threads(2)
    public long thread_private_plain_get2(ThreadState s) {
        return s.threadPrivateCounter;
    }

    @Benchmark
    @Threads(4)
    public long thread_private_plain_get4(ThreadState s) {
        return s.threadPrivateCounter;
    }

    @Benchmark
    @Threads(Threads.MAX)
    public long thread_private_plain_getmax(ThreadState s) {
        return s.threadPrivateCounter;
    }

    @Benchmark
    @Threads(40)
    public long thread_private_plain_getmaxmax(ThreadState s) {
        return s.threadPrivateCounter;
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(NoContentionBaseline.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
