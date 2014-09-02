package com.hello.suripu.core.db.models;

public enum Gender {

    FEMALE(0),
    MALE(1),
    OTHER(2);

    private int value;

    private Gender(int value) {
        this.value = value;
    }
}
