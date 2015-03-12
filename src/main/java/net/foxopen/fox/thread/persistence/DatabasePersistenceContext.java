package net.foxopen.fox.thread.persistence;


import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import net.foxopen.fox.database.UCon;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.thread.RequestContext;
import net.foxopen.fox.thread.persistence.SharedDOMManager.SharedDOMType;
import net.foxopen.fox.track.Track;
import net.foxopen.fox.track.TrackTimer;


/**
 * PersistenceContext which serialises thread data to relational database tables.
 */
public class DatabasePersistenceContext
implements PersistenceContext {

  private static class PersistenceEntry {
    Persistable mPersistable;
    PersistenceMethod mPersistenceMethod;

    PersistenceEntry(Persistable pPersistable, PersistenceMethod pMethod) {
      mPersistable = pPersistable;
      mPersistenceMethod = pMethod;
    }
  }

  //Priorities are lowest to highest so comparisons put higher priorities first in the list (i.e. lower value = higher priority)
  private static final Table<PersistableType, PersistenceMethod, Integer> gSerialisePriorities = HashBasedTable.create();
  static {
    //Thread create depends on user thread session being created first (relational integrity constraint)
    gSerialisePriorities.put(PersistableType.USER_THREAD_SESSION, PersistenceMethod.CREATE, 10);
    //Thread delete nullifies any other actions so do it first
    gSerialisePriorities.put(PersistableType.THREAD, PersistenceMethod.DELETE, 20);
    //Module call stack depends on thread being serialised first
    gSerialisePriorities.put(PersistableType.THREAD, PersistenceMethod.CREATE, 30);
    //Causes module inserts/deletes
    gSerialisePriorities.put(PersistableType.MODULE_CALL_STACK, PersistenceMethod.UPDATE, 40);
    //Delete state calls before inserting them
    gSerialisePriorities.put(PersistableType.STATE_CALL_STACK, PersistenceMethod.DELETE, 50);
    //Causes state inserts/deletes
    gSerialisePriorities.put(PersistableType.STATE_CALL_STACK, PersistenceMethod.UPDATE, 60);
    //All other operations (updates etc) are not dependent on others
  }

  private static int methodPriority(PersistenceMethod pPersistenceMethod){
    switch(pPersistenceMethod) {
      case DELETE:
        return 3;
      case CREATE:
        return 2;
      case UPDATE:
        return 1;
      default:
        return 0;
    }
  }

  private static class PersistenceEntryComparator
  implements Comparator<PersistenceEntry> {

    /**
     * Orders PersistenceEntries based on the priorities defined in the gSerialisePriorities static map. Entries with
     * a priority defined are considered to be higher than those without one. If both have a priority defined, the highest
     * wins. Creates and deletes are higher priority than updates.
     * @param pO1 Object 1
     * @param pO2 Object 2
     * @return see {@link Comparator}
     */
    @Override
    public int compare(PersistenceEntry pO1, PersistenceEntry pO2) {
      Integer lPriority1 = gSerialisePriorities.get(pO1.mPersistable.getPersistableType(), pO1.mPersistenceMethod);
      Integer lPriority2 = gSerialisePriorities.get(pO2.mPersistable.getPersistableType(), pO2.mPersistenceMethod);

      if(lPriority1 != null && lPriority2 == null) {
        return -1;
      }
      else if (lPriority1 == null && lPriority2 != null) {
        return 1;
      }
      else if(lPriority1 == null && lPriority2 == null) {
        //Priority not defined for either; use method to decide (delete > create > update)
        if(methodPriority(pO1.mPersistenceMethod) > methodPriority(pO2.mPersistenceMethod)){
          return -1;
        }
        else if (methodPriority(pO1.mPersistenceMethod) < methodPriority(pO2.mPersistenceMethod)){
          return 1;
        }
        else {
          return 0;
        }
      }
      else {
        //Compare the 2 defined priorities
        return lPriority1 - lPriority2;
      }
    }
  }
  private final List<PersistenceEntry> mRequirePersisting = new ArrayList<>();

  private final Set<ListeningPersistable> mListeningPersistables = Collections.newSetFromMap(new WeakHashMap<ListeningPersistable, Boolean>());

  private final String mThreadId;

  private Serialiser mSerialiser;

  private Deserialiser mDeserialiser = null;

  public DatabasePersistenceContext(String pThreadId) {
    mThreadId = pThreadId;
  }

  @Override
  public void startPersistenceCycle(RequestContext pRequestContext) {
    for(ListeningPersistable lPersistable : mListeningPersistables) {
      lPersistable.startPersistenceCycle();
    }
  }

  @Override
  public void registerListeningPersistable(ListeningPersistable pPersistable) {
    mListeningPersistables.add(pPersistable);
  }

  @Override
  public void requiresPersisting(Persistable pPersistable, PersistenceMethod pMethod) {
    mRequirePersisting.add(new PersistenceEntry(pPersistable, pMethod));
  }

  @Override
  public void endPersistenceCycle(RequestContext pRequestContext) {

    UCon lUCon = pRequestContext.getContextUCon().getUCon("Thread Serialise");
    try {
      mSerialiser = new DatabaseSerialiser(this, lUCon);

      //Sort the pending map so important operations happen first to avoid violating DB constraints
      Collections.sort(mRequirePersisting, new PersistenceEntryComparator());

      Track.pushInfo("ThreadSerialise", "Serialising thread to database", TrackTimer.THREAD_SERIALISE);
      try {

       Set<Persistable> lAlreadyPersisted = new HashSet<>();

        for(PersistenceEntry lEntry : mRequirePersisting) {

          Persistable lPersistable = lEntry.mPersistable;

          //Skip persistables if they are already persisted
          if(!lAlreadyPersisted.contains(lEntry.mPersistable)) {

            Collection<PersistenceResult> lImplicated;

            switch(lEntry.mPersistenceMethod) {
              case CREATE:
                lImplicated = lPersistable.create(this);
                break;
              case UPDATE:
                lImplicated = lPersistable.update(this);
                break;
              case DELETE:
                lImplicated = lPersistable.delete(this);
                break;
              default:
                throw new ExInternal("Unknown persistence method " + lEntry.mPersistenceMethod.toString()); //Shuts up compiler
            }

            //Record any implicated persistables as done so they are not serialised twice
            //(including what was just persisted)
            lAlreadyPersisted.add(lPersistable);

            for(PersistenceResult lPersistenceResult : lImplicated) {
              lAlreadyPersisted.add(lPersistenceResult.getPersistable());
              //Also remove any deletes from the listening list - might not be in here, but just in case
              if(lPersistenceResult.getMethod() == PersistenceMethod.DELETE) {
                mListeningPersistables.remove(lPersistenceResult.getPersistable());
              }
            }
          }
        }

        //Clear for next persistence cycle
        mRequirePersisting.clear();
      }
      finally {
        Track.pop("ThreadSerialise", TrackTimer.THREAD_SERIALISE);
      }
    }
    finally {
      pRequestContext.getContextUCon().returnUCon(lUCon, "Thread Serialise");
    }
  }

  @Override
  public String getThreadId() {
    return mThreadId;
  }

  @Override
  public Serialiser getSerialiser() {
    return mSerialiser;
  }

  @Override
  public Deserialiser setupDeserialiser(RequestContext pRequestContext){
    mDeserialiser = new DatabaseDeserialiser(this, pRequestContext.getContextUCon());
    return mDeserialiser;
  }

  @Override
  public Deserialiser getDeserialiser() {
    if(mDeserialiser == null){
      throw new IllegalStateException("Call setupDeserialiser first");
    }
    return mDeserialiser;
  }

  @Override
  public SharedDOMManager getSharedDOMManager(SharedDOMType pDOMType, String pDOMId) {
    return DatabaseSharedDOMManager.getOrCreateDOMManager(pDOMType, pDOMId);
  }
}
