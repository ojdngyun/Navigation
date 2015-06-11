package com.example.oliverng.labDev;

/**
 * Created by OliverNg on 5/9/2015.
 */
public class CoordinatePoolObjectFactory implements PoolObjectFactory {
    @Override
    public PoolObject createPoolObject() {
        return new CoordinatePoolObject();
    }
}
