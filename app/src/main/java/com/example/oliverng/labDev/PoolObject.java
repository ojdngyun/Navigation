

package com.example.oliverng.labDev;

/**
 * Interface that has to be implemented by an object that can be
 * stored in an object pool through the ObjectPool class.
 * 
 * @author Andrea Bresolin
 */
public interface PoolObject
{
	/**
	 * Initialization method. Called when an object is retrieved
	 * from the object pool or has just been created.
	 */
	public void initializePoolObject();
	
	/**
	 * Finalization method. Called when an object is stored in
	 * the object pool to mark it as free.
	 */
	public void finalizePoolObject();
}
