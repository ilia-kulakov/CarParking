package com.epam.javatraining.thread.carparking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Parking {
    private static final Logger logger = LogManager.getLogger("CarParkingLogger");

    private CarPlace carPlacesChain;     // linked list of CarPlaces
    private Semaphore semaphore;
    private Lock locker;
    private StringBuilder placesScheme;


    Parking(int placeCount) {
        semaphore = new Semaphore(placeCount);
        locker = new ReentrantLock();

        CarPlace current = null;
        int i = 0;
        while(i < placeCount) {

            CarPlace place = new CarPlace(i + 1);
            place.setPrevPlace(current);

            if(current == null) {
                carPlacesChain = place;
            } else {
                current.setNextPlace(place);
            }
            current = place;
            i++;
        }
        current.setNextPlace(null);

        updatePlacesScheme();
    }

    public CarPlace parkCar(Car car) {

        locker.lock();

        CarPlace place = carPlacesChain;
        do {
            if (place.getOwner() == null) {
                place.setOwner(car);
                break;
            }
            place = place.getNextPlace();
        }while(place != null);

        unlockUpdatePlacesScheme();
        locker.unlock();

        return place;
    }

    public void deParkCar(CarPlace place) {

        locker.lock();
        place.setOwner(null);
        unlockUpdatePlacesScheme();
        locker.unlock();
        semaphore.release();
    }

    public boolean requestPermit(int waitingTimeMs) {
        try {
            boolean result = semaphore.tryAcquire(waitingTimeMs, TimeUnit.MILLISECONDS);
            if(!result) {
                updatePlacesScheme();
            }
            return result;
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            return false;
        }
    }

    public void updatePlacesScheme() {

        locker.lock();
        unlockUpdatePlacesScheme();
        locker.unlock();
    }

    private void unlockUpdatePlacesScheme() {

        StringBuilder scheme = new StringBuilder();
        CarPlace place = carPlacesChain;
        do {
            Car c = place.getOwner();
            String symbol;
            if(c == null) {
                symbol = "| |";
            } else {
                symbol = "|" + ((c.isEmpty()) ? c.getCarCode().toLowerCase() : c.getCarCode().toUpperCase()) + "|";
            }
            scheme.append(symbol);
            place = place.getNextPlace();
        } while (place != null);

        placesScheme = scheme;

        String queueSize = String.format("<%02d>", semaphore.getQueueLength() );
        logger.info(queueSize + placesScheme);
    }

}