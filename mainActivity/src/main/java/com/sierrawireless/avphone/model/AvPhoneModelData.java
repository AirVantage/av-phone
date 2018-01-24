package com.sierrawireless.avphone.model;

import android.text.TextUtils;


public class AvPhoneModelData {
    public enum Mode {
        None,
        UP,
        DOWN
    }
    private static final String TAG = "AvPhoneModelData";

    public String name;
    public String unit;
    public String defaults;
    public Mode mode;
    private Integer current=null;
    private String label;

    public AvPhoneModelData(String name, String unit, String defaults, Mode mode, String label) {
        this.name = name;
        this.unit = unit;
        this.defaults = defaults;
        if (isInteger()) {
            this.mode = mode;
            current = Integer.parseInt(defaults);
        }else{
            this.mode = Mode.None;
        }
        this.label = label;
    }

    public String toString() {
        String returned = "{";
        returned = returned + "\"name\": \"" + name + "\",";
        returned = returned + "\"unit\": \"" + unit + "\",";
        returned = returned + "\"defaults\": \"" + defaults + "\",";
        returned = returned + "\"mode\": \"" + modeToString(mode) + "\",";
        returned = returned + "\"label\": \"" + label + "\"}";
        return returned;
    }



    public Boolean isInteger() {
        return TextUtils.isDigitsOnly(defaults);
    }

    public String execMode()  {
        if (this.mode == Mode.UP) {
            return increment();
        }else if (this.mode == Mode.DOWN){
            return decrement();
        }
        return defaults;
    }

    public String modeToString(Mode mode){
        switch (mode) {
            case UP:
                return "Increase indefinitely";
            case DOWN:
                return "Decrease to zero";
            case None:
                return "None";
        }
        return "None";
    }

    private String increment(){
        if (current == null) {
            current = Integer.parseInt(defaults);
        }
        current ++;
        return current.toString();
    }

    private String decrement() {
        if (current == null) {
            current = Integer.parseInt(defaults);
        }
        if (current > 0 )
            current --;
        return current.toString();
    }



}
