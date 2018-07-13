package com.ruiec.service;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class Java8Test {
    public static void main(String[] args) {
        final Dog dog = Dog.birth(Dog::new);
        final List<Dog> dogs = Arrays.asList(dog);

        dogs.forEach(Dog::eat);
        dogs.forEach(Dog::run);
        final Dog anotherDog = Dog.birth(Dog::new);
        dogs.forEach(anotherDog::sleep);

        Arrays.sort();

    }

    public static class Dog {
        public static Dog birth(final Supplier<Dog> supplier) {
            return supplier.get();
        }

        public static void eat(final Dog dog) {
            System.out.println("eat " + dog.toString());
        }

        public void sleep(final Dog anotherDog) {
            System.out.println("sleep " + anotherDog.toString());
        }

        public void run() {
            System.out.println("run " + this.toString());
        }
    }

    public class Cat {

    }

}
