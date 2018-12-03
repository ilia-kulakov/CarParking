package com.epam.javatraining.thread.carparking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Car extends Thread {

    private static final Logger logger = LogManager.getLogger("CarParkingLogger");

    private int WAITING_TIME_MS = 5000;
    private int MIN_SHOPPING_TIME_MS = 2500;
    private int MIN_PREPARE_TIME_MS = 1000;
    private int MIN_DRIVING_TIME_MS = 5000;
    private int MAX_COUNT_OF_PLACE_EXCHANGES = 3;
    private String carCode;
    private Parking parking;
    private CarPlace place;
    private boolean isEmpty;
    private int countOfPlaceExchanges;
    private CarPlace oldPlace;


    public Car(String carCode, Parking parking) {
        super("Car №" + carCode);
        this.parking = parking;
        this.carCode = carCode;
        this.place = null;
        this.isEmpty = false;
        this.countOfPlaceExchanges = 0;
        this.oldPlace = null;
    }

    public String getCarCode() {
        return carCode;
    }

    public void crush() {
        interrupt();
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    public boolean isParked() { return place != null; }

    private boolean requestParkingPermit() {
        logger.trace("Car № {} is asking parking permit.", carCode);
        return parking.requestPermit(WAITING_TIME_MS);
    }

    private void tryExchangePlace() {

        logger.trace("Car № {} try exchange place", carCode);
        boolean readyExchange = true|| (countOfPlaceExchanges < MAX_COUNT_OF_PLACE_EXCHANGES)
                && (Math.random() > 0.25);

        // response to request of place exchanging
        logger.trace("Car № {} try response to request of place exchanging", carCode);
        CarPlace newPlace = place.responseExchange(readyExchange);

        if(readyExchange) {

            boolean request = (newPlace == null);
            if(request) {
                // initiate request to exchange places
                logger.trace("Car № {} initiate request to exchange places", carCode);
                newPlace = requestExchangePlace();
            }

            if (newPlace != null) {
                // we have exchange offer
                countOfPlaceExchanges++;
                newPlace.setOwner(this, true || countOfPlaceExchanges < MAX_COUNT_OF_PLACE_EXCHANGES);
                oldPlace = place;
                place = newPlace;

                logger.trace("Car № {} exchange it's place #{} to other #{}", carCode, oldPlace, place);

                if(request) {
                    // we are initiator of place exchanging
                    try {
                        sleep(1000);
                    } catch (InterruptedException e) {
                        logger.trace(e.getMessage(), e);
                        interrupt();
                    }
                    parking.updatePlacesScheme();
                }
            }

        }
    }

    private CarPlace requestExchangePlace() {

        CarPlace newPlace = null;

        // we are ready to try request exchange car place
        CarPlace p = place.getPrevPlace();

        if (p != null && p.getOwner() != null && p != oldPlace) {
            logger.trace("Car № {} initiate request to exchange places to Car № {} ", carCode, p.getOwner().getCarCode());
            newPlace = p.requestExchange(place);
        }

        if (newPlace == null) {

            p = place.getNextPlace();

            if (p != null && p.getOwner() != null && p != oldPlace) {
                logger.trace("Car № {} initiate request to exchange places to Car № {}", carCode, p.getOwner().getCarCode());
                newPlace = p.requestExchange(place);
            }
        }

        return newPlace;
    }

    private void parking() {
        try {
            //Ищем свободное место и паркуемся
            place = parking.parkCar(this);

            logger.trace("Car № {} found place #{} and it's parking", carCode, place);
            sleep(MIN_PREPARE_TIME_MS + Math.round(MIN_PREPARE_TIME_MS * Math.random()));
            tryExchangePlace();

            logger.trace("Driver of Car № {} go shopping.", carCode);
            isEmpty = true;
            parking.updatePlacesScheme();
            sleep(MIN_SHOPPING_TIME_MS + Math.round(MIN_SHOPPING_TIME_MS * Math.random()));

            logger.trace("Driver of Car № {} come back and he's loading goods into his car.", carCode);
            isEmpty = false;
            parking.updatePlacesScheme();
            sleep(MIN_PREPARE_TIME_MS + Math.round(MIN_PREPARE_TIME_MS * Math.random()));
            tryExchangePlace();
            parking.updatePlacesScheme();
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            interrupt();
        }
    }

    private void driveOut() {
        logger.trace("Car № {} drive out.", carCode);
        try {
            sleep(MIN_DRIVING_TIME_MS + Math.round(MIN_DRIVING_TIME_MS * Math.random()));
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            interrupt();
        }
    }

    @Override
    public void run() {

        while(!isInterrupted()) {

            logger.trace("Car № {} is driving to the parking.", carCode);
            try {
                sleep(Math.round(MIN_DRIVING_TIME_MS * Math.random()));
            } catch(InterruptedException e) {
                logger.error(e.getMessage(), e);
                interrupt();
            }

            logger.trace("Car № {} drove up to the parking.", carCode);

            if (requestParkingPermit()) {
                logger.trace("Car № {} got parking permit.", carCode);
                parking();
                logger.trace("Car № {} is ready drive out.", carCode);
                parking.deParkCar(place);
                place = null;

            } else {
                logger.trace("Car № {} didn't get parking permit.", carCode);
            }

            driveOut();
        }
    }
}
