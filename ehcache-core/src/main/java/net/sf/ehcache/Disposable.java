/**
 * Copyright Terracotta, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</a>
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.ehcache;

/**
 * A <tt>Disposable</tt> is a resource that needs to release other resources when it is no longer needed.
 * Resources registered with a CacheManager (or Cache), will have this method called whenever they are disposed themselves...
 *
 * @author Alex Snaps
 * @since 2.5.0
 */
public interface Disposable {

    /**
     * Disposes the resource and releases any system resources associated
     * with it. If the resource was already disposed of, this has no effect
     */
    void dispose();
}
