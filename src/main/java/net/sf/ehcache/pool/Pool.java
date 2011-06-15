/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.pool;

/**
 * Pools are used to track shared resource consumption. Each store participating in a pool creates an accessor
 * which it uses to tell the pool about its consumption. A SizeOf engine is used to calculate the size of the
 * objects added to the pool.
 *
 * @author Ludovic Orban
 */
public interface Pool {

    /**
     * Return the used size of the pool.
     *
     * @return used size of the pool.
     */
    long getSize();

    /**
     * Return the maximum size of the pool.
     *
     * @return the maximum size of the pool.
     */
    long getMaxSize();

    /**
     * Change the maximum size of the pool.
     *
     * @param newSize the new pool size.
     */
    void setMaxSize(long newSize);

    /**
     * Return a PoolAccessor whose consumption is tracked by this pool, using a default SizeOf engine.
     *
     * @param store the store which will use the created accessor.
     * @return a PoolAccessor whose consumption is tracked by this pool.
     */
    PoolAccessor createPoolAccessor(PoolableStore store);

    /**
     * Return a PoolAccessor whose consumption is tracked by this pool, using a specific SizeOf engine.
     *
     * @param store the store which will use the created accessor.
     * @param sizeOfEngine the SizeOf engine used to measure the size of objects added through the created accessor.
     * @return a PoolAccessor whose consumption is tracked by this pool.
     */
    PoolAccessor createPoolAccessor(PoolableStore store, SizeOfEngine sizeOfEngine);

}
