package ua.stepess.reactive;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class ArrayPublisher<T> implements Publisher<T> {

    private final T[] array;

    public ArrayPublisher(T[] array) {
        this.array = array;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        for (int i = 0; i < array.length; i++) {
            subscriber.onNext(array[i]);
        }
    }
}
