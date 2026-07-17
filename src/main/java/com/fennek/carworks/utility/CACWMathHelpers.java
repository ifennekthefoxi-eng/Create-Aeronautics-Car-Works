package com.fennek.carworks.utility;

public class CACWMathHelpers {

    public static float NormalizeToOne (float value, float min, float max)
    {
        return (value - min) / (max - min);
    }
}
