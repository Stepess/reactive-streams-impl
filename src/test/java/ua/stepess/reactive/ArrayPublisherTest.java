package ua.stepess.reactive;


import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ArrayPublisherTest {

    @Test
    public void everyMethodInSubscriberShouldBeExecutedInParticularOrder() throws InterruptedException {
        var latch = new CountDownLatch(1);

        var observedSignals = new ArrayList<String>();

        var arrayPublisher = new ArrayPublisher<>(generate(5));

        arrayPublisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription s) {
                observedSignals.add("onSubscribe()");
                s.request(10);
            }

            @Override
            public void onNext(Long aLong) {
                observedSignals.add("onNext(" + aLong + ")");
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
                observedSignals.add("onComplete()");
                latch.countDown();
            }
        });

        assertThat(latch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue();

        assertThat(observedSignals)
                .containsExactly(
                        "onSubscribe()",
                        "onNext(0)",
                        "onNext(1)",
                        "onNext(2)",
                        "onNext(3)",
                        "onNext(4)",
                        "onComplete()"
                );

    }

    static Long[] generate(long num) {
        return LongStream.range(0, num >= Integer.MAX_VALUE ? 1_000_000 : num)
                .boxed()
                .toArray(Long[]::new);
    }

}