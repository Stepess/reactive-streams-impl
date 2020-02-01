package ua.stepess.reactive;


import java.util.stream.LongStream;

public class ArrayPublisherTest {

    static Long[] generate(long num) {
        return LongStream.range(0, num >= Integer.MAX_VALUE ? 1_000_000 : num)
                .boxed()
                .toArray(Long[]::new);
    }

}