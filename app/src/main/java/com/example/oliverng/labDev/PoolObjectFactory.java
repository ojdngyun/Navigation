

package com.example.oliverng.labDev;

/**
 * Interface that has to be implemented by every class that allows
 * the creation of objects for an object pool through the
 * ObjectPool class.
 * 
 * @author Andrea Bresolin
 */
public interface PoolObjectFactory
{
	/**
	 * Creates a new object for the object pool.
	 * 
	 * @return new object instance for the object pool
	 */
	public PoolObject createPoolObject();
}
