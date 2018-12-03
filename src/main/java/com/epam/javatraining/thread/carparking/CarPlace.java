package com.epam.javatraining.thread.carparking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Exchanger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CarPlace implements Comparable<CarPlace>{

    private static final Logger logger = LogManager.getLogger("CarParkingLogger");

    private CarPlace prevPlace;
    private CarPlace nextPlace;

    private int number;
    private Car owner;
    private Exchanger<CarPlace> exchanger;
    private Lock locker;
    private ExchangeStatus exchangeStatus;


    public CarPlace(int number) {
        this.number = number;
        this.locker = new ReentrantLock();
        this.exchanger = new Exchanger<>();
        this.owner = null;

        // place does not have any owner
        // that's why we are not ready to
        // exchange place right now
        exchangeStatus = ExchangeStatus.UNREADY;
    }

    public CarPlace getPrevPlace() {
        return prevPlace;
    }

    public void setPrevPlace(CarPlace prevPlace) {
        this.prevPlace = prevPlace;
    }

    public CarPlace getNextPlace() {
        return nextPlace;
    }

    public void setNextPlace(CarPlace nextPlace) {
        this.nextPlace = nextPlace;
    }

    public int getNumber() {
        return number;
    }

    public Car getOwner() {
        return owner;
    }

    public void setOwner(Car owner) {
        // owner ready to exchange places
        boolean readyExchange = (owner != null);
        setOwner(owner, readyExchange);
    }

    public void setOwner(Car owner, boolean readyExchange) {
        this.owner = owner;

        if(owner == null && this.exchangeStatus == ExchangeStatus.PROCESSING) {
            try {
                exchanger.exchange(null);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                Thread.interrupted();
            }
        }

        this.exchangeStatus = (readyExchange) ? ExchangeStatus.READY : ExchangeStatus.UNREADY;
    }

    public CarPlace requestExchange(CarPlace place) {

        place.locker.lock();
        locker.lock();
        if(exchangeStatus == ExchangeStatus.READY &&
                place.exchangeStatus == ExchangeStatus.READY) {

            exchangeStatus = exchangeStatus.PROCESSING;
            place.exchangeStatus = exchangeStatus.PROCESSING;
            locker.unlock();
            place.locker.unlock();

            // we are first who request to exchange this place
            try {
                logger.trace(Thread.currentThread().getName() + " requested and wait exchange");
                return exchanger.exchange(place);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                Thread.interrupted();
            }
        } else {
            locker.unlock();
            place.locker.unlock();
        }

        return null;
    }

    public CarPlace responseExchange(boolean agree) {

        locker.lock();
        if(exchangeStatus == ExchangeStatus.PROCESSING) {
            exchangeStatus = ExchangeStatus.UNREADY;
            locker.unlock();
            // we have a request to exchange our place
            try {
                // if agree then send this place
                logger.trace(Thread.currentThread().getName() + " responsed and wait exchange");
                if(agree) {
                    this.setOwner(null);
                }
                return exchanger.exchange(agree ? this : null);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                Thread.interrupted();
            }
        } else {
            locker.unlock();
        }

        return null;
    }

    @Override
    public int compareTo(CarPlace o) {
        return this.number - o.number;
    }

    @Override
    public String toString() {
        return "" + number;
    }

    enum ExchangeStatus {
        UNREADY, READY, PROCESSING
    }
}
