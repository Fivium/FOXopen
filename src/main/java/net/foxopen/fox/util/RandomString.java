package net.foxopen.fox.util;

import net.foxopen.fox.ex.ExInternal;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

/**
 * General utility class to get strings of random characters (currently base32 chars) of arbitrary lengths securely.
 *
 * Note: The resulting characters are currently 0-9a-v, not a-z2-7 that you'd get via base32 transfer encoding.
 */
public class RandomString {
  private static final int SECURE_RANDOM_REUSE_LIMIT = 5000;

  // When calling getString we convert a BigInteger to string and need to know what base to use for the string and how
  //   many bits there are per-character for that base number
  private static final int STRING_OUTPUT_BASE = 32;
  private static final int STRING_OUTPUT_BITS_PER_CHAR = 5;

  private static SecureRandom gSecureRandom = null;
  private static int gSecureRandomUseCount = 0;

  static {
    reinitialiseRandomProvider();
  }

  /**
   * Recreate the SecureRandom object that we use to generate the random string characters from
   */
  private synchronized static void reinitialiseRandomProvider() {
    try {
      //Ensure native implementation of SecureRandom is provided
      gSecureRandom = SecureRandom.getInstance("SHA1PRNG", "SUN");

      // Force use of the SecureRandom to make sure it's seeded using internal secure methods
      // see: http://www.cigital.com/justice-league-blog/2009/08/14/proper-use-of-javas-securerandom/
      byte[] lForceUseBytes = new byte[8];
      gSecureRandom.nextBytes(lForceUseBytes);

      gSecureRandomUseCount = 0;
    }
    catch (NoSuchAlgorithmException | NoSuchProviderException e) {
      throw new ExInternal("Failed to construct a SecureRandom", e);
    }
  }

  /**
   * Get a string of securely generated random characters of pStringLength
   *
   * @param pStringLength Amount of characters to generate
   * @return String of random characters
   */
  public synchronized static String getString(int pStringLength) {
    if(gSecureRandomUseCount >= SECURE_RANDOM_REUSE_LIMIT){
      reinitialiseRandomProvider();
    }

    gSecureRandomUseCount++;

    return new BigInteger(pStringLength * STRING_OUTPUT_BITS_PER_CHAR, gSecureRandom).toString(STRING_OUTPUT_BASE);
  }
}
