package net.foxopen.fox.thread.persistence;

/**
 * Lambda for notifying a {@link PersistenceContext} that some sort of update is required. This should be passed to target
 * classes to avoid creating a dependency on the PersistenceContext itself.
 */
@FunctionalInterface
public interface PersistenceContextProxy {
  void updateRequired();
}
