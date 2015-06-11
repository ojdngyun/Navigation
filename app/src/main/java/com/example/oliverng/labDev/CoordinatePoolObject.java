package com.example.oliverng.labDev;

/**
 * Created by OliverNg on 5/9/2015.
 */
public class CoordinatePoolObject implements PoolObject{
    float[] mCoordinates;

    public void setCoordinates(float[] coordinates){
        mCoordinates = coordinates;
    }

    public float[] getCoordinates(){
        return mCoordinates;
    }

    @Override
    public void initializePoolObject() {
        mCoordinates = null;
    }

    @Override
    public void finalizePoolObject() {
        mCoordinates = null;
    }
}
