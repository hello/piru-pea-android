package com.hello.suripu.core.db.models;

/**
 * Created by pangwu on 4/16/14.
 */
public enum SleepRating {
    POOR(0),
    GOOD(1),
    EXCELLENT(2);

    private int value;

    private SleepRating(int value) {
        this.value = value;
    }

    public static SleepRating fromInteger(int value){
        if(value >= 0 && value < 20){
            return POOR;
        }else if(value >= 20 && value < 80){
            return GOOD;
        }

        return EXCELLENT;
    }

    public int toScore(){
        switch (this){
            case POOR:
                return 15;
            case GOOD:
                return 75;
            case EXCELLENT:
                return 95;
            default:
                return 15;
        }
    }
}
