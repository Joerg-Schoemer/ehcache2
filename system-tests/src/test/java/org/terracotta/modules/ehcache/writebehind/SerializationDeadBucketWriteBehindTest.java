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
package org.terracotta.modules.ehcache.writebehind;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.modules.ehcache.async.AsyncCoordinatorImpl;

import com.tc.l2.L2DebugLogging.LogLevel;
import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SerializationDeadBucketWriteBehindTest extends AbstractCacheTestBase {
  private int totalWriteCount  = 0;
  private int totalDeleteCount = 0;

  public SerializationDeadBucketWriteBehindTest(TestConfig testConfig) {
    super("basic-writebehind-test.xml", testConfig, SerializationWriteBehindClient1.class,
          SerializationWriteBehindClient2.class);
    testConfig.getClientConfig().setParallelClients(false);
    configureTCLogging(AsyncCoordinatorImpl.class.getName(), LogLevel.DEBUG);
  }

  @Override
  protected void postClientVerification() {

    System.out.println("[Clients processed a total of " + totalWriteCount + " writes]");
    if (totalWriteCount < 1000 || totalWriteCount > 1900) { throw new AssertionError(totalWriteCount); }

    System.out.println("[Clients processed a total of " + totalDeleteCount + " deletes]");
    if (totalDeleteCount < 100 || totalDeleteCount > 190) { throw new AssertionError(totalDeleteCount); }
  }

  @Override
  protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
    super.evaluateClientOutput(clientName, exitCode, output);
    BufferedReader reader = null;
    FileReader fr = null;
    StringBuilder strBuilder = new StringBuilder();
    try {
      fr = new FileReader(output);
      reader = new BufferedReader(fr);
      String st = "";
      while ((st = reader.readLine()) != null) {
        strBuilder.append(st);
      }
    } catch (Exception e) {
      throw new AssertionError(e);
    } finally {
      try {
        fr.close();
        reader.close();
      } catch (Exception e) {
        //
      }
    }

    // Detect the number of writes that have happened
    int writeCount = detectLargestCount(strBuilder.toString(),
                                        Pattern.compile("\\[WriteBehindCacheWriter written (\\d+) for " + clientName
                                                        + "\\]"));
    totalWriteCount += writeCount;
    System.out.println("[" + clientName + " processed " + writeCount + " writes]");

    // Detect the number of deletes that have happened
    int deleteCount = detectLargestCount(strBuilder.toString(),
                                         Pattern.compile("\\[WriteBehindCacheWriter deleted (\\d+) for " + clientName
                                                         + "\\]"));
    totalDeleteCount += deleteCount;
    System.out.println("[" + clientName + " processed " + deleteCount + " deletes]");
  }

  private int detectLargestCount(String clientOutput, Pattern pattern) {
    Matcher matcher = pattern.matcher(clientOutput);
    int count = 0;
    while (matcher.find()) {
      int parsedCount = Integer.parseInt(matcher.group(1));
      if (parsedCount > count) {
        count = parsedCount;
      }
    }
    return count;
  }
}
