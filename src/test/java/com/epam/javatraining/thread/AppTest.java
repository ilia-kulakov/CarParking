package com.epam.javatraining.thread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    static final Logger logger = LogManager.getLogger("TestLogger");

    @Test
    public void testCustomerProducer() {
        Store store=new Store();
        Producer producer = new Producer(store);
        Consumer consumer = new Consumer(store);

        List<Thread> threads = new ArrayList<>();

        threads.add(new Thread(producer, "Prod1"));
        threads.add(new Thread(producer, "Prod2"));
        threads.add(new Thread(producer, "Prod3"));
        threads.add(new Thread(consumer, "Cons1"));
        threads.add(new Thread(consumer, "Cons2"));
        threads.add(new Thread(consumer, "Cons3"));

        threads.forEach((t) -> t.start());

        for(Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
            }
        }
    }


    // Класс Магазин, хранящий произведенные товары
    class Store{
        private int product=0;
        private Lock locker = new ReentrantLock();
        private final Condition notEmpty = locker.newCondition();
        private final Condition notFull = locker.newCondition();

        public void get() throws InterruptedException {

            locker.lock();
            String name = Thread.currentThread().getName();
            logger.trace("Покупатель {} вошёл в магазин", name);
            try {
                while (product == 0) {
                    logger.trace("Товара нет, покупатель {} ждет.-------------------------------------------", name);
                    //Thread.currentThread().setPriority(5);
                    notEmpty.await();
                    logger.trace("Покупатель {} дождался.", name);
                }

                product--;
                logger.trace("Покупатель {} купил 1 товар", name);
                logger.trace("Товаров на складе: " + product);
                //Thread.currentThread().setPriority(1);
                notFull.signal();
            } finally {
                locker.unlock();
            }
        }

        public void put() throws InterruptedException{

            locker.lock();
            String name = Thread.currentThread().getName();
            logger.trace("Производитель {}  вошёл в магазин", name);
            try {
                while (product == 3) {
                    logger.trace("Товар не раскупили, производитель {} ждет.***************************************", name);
                    //Thread.currentThread().setPriority(5);
                    notFull.await();
                    logger.trace("Производитель {} дождался.", name);
                }

                product++;
                logger.trace("Производитель {} добавил 1 товар", name);
                logger.trace("Товаров на складе: " + product);
                //Thread.currentThread().setPriority(1);
                notEmpty.signal();
            } finally {
                locker.unlock();
            }
        }
    }
    // класс Производитель
    class Producer implements Runnable{

        Store store;
        Producer(Store store){
            this.store=store;
        }
        public void run(){
            for (int i = 1; i < 6; i++) {
                try {
                    store.put();
                } catch (InterruptedException e) {}
            }

            String name = Thread.currentThread().getName();
            logger.trace("Производитель {} свернул производство", name);
        }
    }
    // Класс Потребитель
    class Consumer implements Runnable{

        Store store;
        Consumer(Store store){
            this.store=store;
        }
        public void run(){
            for (int i = 1; i < 6; i++) {
                try {
                    store.get();
                } catch (InterruptedException e) {
                }
            }

            String name = Thread.currentThread().getName();
            logger.trace("Покупатель {} закупился", name);
        }
    }
}
