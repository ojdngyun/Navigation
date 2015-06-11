

package com.example.oliverng.labDev;

/**
 * Object pool implementation.
 * 
 * @author Andrea Bresolin
 */
public class ObjectPool
{
	protected final int MAX_FREE_OBJECT_INDEX;

	protected PoolObjectFactory factory;
	protected PoolObject[] freeObjects;
	protected int freeObjectIndex = -1;

	/**
	 * Constructor.
	 * 
	 * @param factory the object pool factory instance
	 * @param maxSize the maximun number of instances stored in the pool
	 */
	public ObjectPool(PoolObjectFactory factory, int maxSize)
	{
		this.factory = factory;
		this.freeObjects = new PoolObject[maxSize];
		MAX_FREE_OBJECT_INDEX = maxSize - 1;
	}
	
	/**
	 * Creates a new object or returns a free object from the pool.
	 * 
	 * @return a PoolObject instance already initialized
	 */
	public synchronized PoolObject newObject()
	{
		PoolObject obj = null;
		
		if (freeObjectIndex == -1)
		{
			// There are no free objects so I just
			// create a new object that is not in the pool.
			obj = factory.createPoolObject();
		}
		else
		{
			// Get an object from the pool
			obj = freeObjects[freeObjectIndex];
			
			freeObjectIndex--;
		}

		// Initialize the object
		obj.initializePoolObject();
		
		return obj;
	}

	/**
	 * Stores an object instance in the pool to make it available for a subsequent
	 * call to newObject() (the object is considered free).
	 * 
	 * @param obj the object to store in the pool and that will be finalized
	 */
	public synchronized void freeObject(PoolObject obj)
	{
		if (obj != null)
		{
			// Finalize the object
			obj.finalizePoolObject();
			
			// I can put an object in the pool only if there is still room for it
			if (freeObjectIndex < MAX_FREE_OBJECT_INDEX)
			{
				freeObjectIndex++;
				
				// Put the object in the pool
				freeObjects[freeObjectIndex] = obj;
			}
		}
	}
}
