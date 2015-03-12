package net.foxopen.fox.cache;

import net.foxopen.fox.ex.ExFoxConfiguration;

interface CacheBuilderFactory {
  FoxCacheBuilder createCacheBuilder() throws ExFoxConfiguration;
}
