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

package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.Pool;
import net.sf.ehcache.pool.PoolAccessor;
import net.sf.ehcache.pool.PoolableStore;
import net.sf.ehcache.pool.Role;
import net.sf.ehcache.pool.SizeOfEngine;

/**
 * A no-op pool which does not enforce any resource consumption limit.
 *
 * @author Ludovic Orban
 */
public class UnboundedPool implements Pool {

    /**
     * Create an UnboundedPool instance
     */
    public UnboundedPool() {
    }

    /**
     * {@inheritDoc}
     */
    public long getSize() {
        return -1L;
    }

    /**
     * {@inheritDoc}
     */
    public long getMaxSize() {
        return -1L;
    }

    /**
     * {@inheritDoc}
     */
    public void setMaxSize(long newSize) {
    }

    /**
     * {@inheritDoc}
     */
    public PoolAccessor createPoolAccessor(PoolableStore store) {
        return new UnboundedPoolAccessor();
    }

    /**
     * {@inheritDoc}
     */
    public PoolAccessor createPoolAccessor(PoolableStore store, SizeOfEngine sizeOfEngine) {
        return new UnboundedPoolAccessor();
    }

    /**
     * The PoolAccessor class of the UnboundedPool
     */
    private final class UnboundedPoolAccessor implements PoolAccessor {

        private UnboundedPoolAccessor() {
        }

        /**
         * {@inheritDoc}
         */
        public long add(Object key, Object value, Object container, boolean force) {
            return 0L;
        }

        /**
         * {@inheritDoc}
         */
        public long delete(Object key, Object value, Object container) {
            return 0L;
        }

        /**
         * {@inheritDoc}
         */
        public long replace(Role role, Object current, Object replacement, boolean force) {
            return 0L;
        }

        /**
         * {@inheritDoc}
         */
        public long getSize() {
            return -1L;
        }

        /**
         * {@inheritDoc}
         */
        public void unlink() {
        }

        /**
         * {@inheritDoc}
         */
        public void clear() {
        }
    }

}
