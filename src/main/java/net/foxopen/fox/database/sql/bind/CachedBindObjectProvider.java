package net.foxopen.fox.database.sql.bind;

/**
 * Marker interface. Implementors of this represent a BindObjectProvider which is safe to cache and can be used to execute
 * a statement repeatedly, with new BindObject instances created each time.
 */
public interface CachedBindObjectProvider
extends BindObjectProvider {
}
