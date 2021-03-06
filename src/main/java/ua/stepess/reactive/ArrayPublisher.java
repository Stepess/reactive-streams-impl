package ua.stepess.reactive;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ArrayPublisher<T> implements Publisher<T> {

    private final T[] array;

    public ArrayPublisher(T[] array) {
        this.array = array;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new Subscription() {

            int index;

            long requested;

            boolean cancelled;

            @Override
            public void request(long n) {

                if (n <= 0 && !cancelled){
                    cancel();
                    subscriber.onError(new IllegalArgumentException());
                    return;
                }

                long initialRequested = requested;

                requested += n;

                if (initialRequested != 0) {
                    return;
                }

                int sent = 0;

                for (; sent < requested && index < array.length; sent++, index++) {
                    if (cancelled) {
                        return;
                    }

                    T element = array[index];

                    if (element == null) {
                        subscriber.onError(new NullPointerException());
                        return;
                    }

                    subscriber.onNext(element);
                }

                if (cancelled) {
                    return;
                }

                if (index == array.length) {
                    subscriber.onComplete();
                    return;
                }

                requested -= sent;
            }

            @Override
            public void cancel() {
                cancelled = true;
            }
        });

    }
}
