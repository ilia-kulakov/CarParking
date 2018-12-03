package com.epam.javatraining.thread.carparking;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class CarParkingTest {
    static final Logger logger = LogManager.getLogger("TestLogger");

    @Test
    void parkingTest() {
        final int placeCount = 10;
        Parking parking = new Parking(placeCount);
        List<Car> cars = new ArrayList<>();
        for (int i = 0; i < placeCount * 5; i++) {
            String code = String.valueOf(Character.toChars(65 + i % 26));
            cars.add(new Car(code, parking));
        }

        cars.forEach((c) -> c.start());

        try {
            for(Car c : cars) {
                c.join();
            }
        }catch(InterruptedException e){}
    }
}
