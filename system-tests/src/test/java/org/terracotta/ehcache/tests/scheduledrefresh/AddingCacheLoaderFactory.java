/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 *      Terracotta, Inc., a Software AG company
 */
package org.terracotta.ehcache.tests.scheduledrefresh;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.loader.CacheLoader;
import net.sf.ehcache.loader.CacheLoaderFactory;

import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: cschanck
 * Date: 5/31/13
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class AddingCacheLoaderFactory extends CacheLoaderFactory {
  @Override
  public CacheLoader createCacheLoader(Ehcache cache, Properties properties) {
    return new AddingCacheLoader();
  }
}
