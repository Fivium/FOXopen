package net.foxopen.fox.xhtml;

/**
 * A simple Name-Value Pair model implementation
 */
@Deprecated
public class NameValuePair
{
   /** The name */
   private String name;
   /** The value */
   private Object value;

   /**
    * Constructs a Name-Value pair instance.
    *
    * @param name the name part
    * @param value the value part
    */
   public NameValuePair(String name, Object value)
   {
      setName(name);
      setValue(value);
   }

   /**
    * Returns the name.
    *
    * @return the name of the name-value pair model
    */
   public String getName()
   {
      return name;
   }

   /**
    * Sets the name.
    *
    * @param name the name of the name-value pair model
    */
   public void setName(String name)
   {
      this.name = name;
   }

   /**
    * Returns the value.
    *
    * @return the value of the name-value pair model
    */
   public Object getValue()
   {
      return value;
   }

   /**
    * Sets the value.
    *
    * @param value the value of the name-value pair model
    */
   public void setValue(Object value)
   {
      this.value = value;
   }
}
