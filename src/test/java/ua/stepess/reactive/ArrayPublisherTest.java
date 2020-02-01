package ua.stepess.reactive;


import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

public class ArrayPublisherTest extends PublisherVerification<Long> {

    public ArrayPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return new ArrayPublisher<>(generate(elements));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }

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

    @Test
    public void mustSupportBackpressureControl() throws InterruptedException {
        var countDownLatch = new CountDownLatch(1);
        var collected = new ArrayList<Long>();
        var subscriptionHolder = new Subscription[1];
        long toReq = 5L;
        var array = generate(toReq);
        var arrayPublisher = new ArrayPublisher<>(array);

        arrayPublisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription s) {
                subscriptionHolder[0] = s;
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                countDownLatch.countDown();
            }
        });

        assertThat(collected).isEmpty();

        var subscription = subscriptionHolder[0];

        subscription.request(1);
        assertThat(collected).containsExactly(0L);

        subscription.request(1);
        assertThat(collected).containsExactly(0L, 1L);

        subscription.request(2);
        assertThat(collected).containsExactly(0L, 1L, 2L, 3L);

        subscription.request(20);

        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(collected).containsExactly(array);
    }

    @Test
    public void testNullability() throws InterruptedException {
        var countDownLatch = new CountDownLatch(1);
        var array = new Long[]{null};
        var error = new AtomicReference<Throwable>();
        var arrayPublisher = new ArrayPublisher<>(array);

        arrayPublisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(Long aLong) {
            }

            @Override
            public void onError(Throwable t) {
                error.set(t);
                countDownLatch.countDown();
            }

            @Override
            public void onComplete() {
            }
        });

        countDownLatch.await(1, TimeUnit.SECONDS);

        assertThat(error.get()).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testRecursion() throws InterruptedException {
        var countDownLatch = new CountDownLatch(1);
        var collected = new ArrayList<Long>();
        long toReq = 1000L;
        var array = generate(toReq);
        var arrayPublisher = new ArrayPublisher<>(array);

        arrayPublisher.subscribe(new Subscriber<Long>() {
            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                this.s = s;
                s.request(1);
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
                s.request(1);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                countDownLatch.countDown();
            }
        });


        assertThat(countDownLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(collected).containsExactly(array);
    }

    @Test
    public void cancellationShouldWork() throws InterruptedException {
        var countDownLatch = new CountDownLatch(1);
        var collected = new ArrayList<Long>();
        long toReq = 1000L;
        var array = generate(toReq);
        var arrayPublisher = new ArrayPublisher<>(array);

        arrayPublisher.subscribe(new Subscriber<Long>() {

            @Override
            public void onSubscribe(Subscription s) {
                s.cancel();
                s.request(toReq);
            }

            @Override
            public void onNext(Long aLong) {
                collected.add(aLong);
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {
                countDownLatch.countDown();
            }
        });


        assertThat(countDownLatch.await(1, TimeUnit.SECONDS)).isFalse();
        assertThat(collected).isEmpty();
    }

    static Long[] generate(long num) {
        return LongStream.range(0, num >= Integer.MAX_VALUE ? 1_000_000 : num)
                .boxed()
                .toArray(Long[]::new);
    }

}