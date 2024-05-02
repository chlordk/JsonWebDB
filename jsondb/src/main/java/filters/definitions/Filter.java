/*
MIT License

Copyright (c) 2024 Alex Høffner

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package filters.definitions;

import sources.Source;
import messages.Messages;
import org.json.JSONArray;
import database.BindValue;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


public abstract class Filter
{
   private final static String VALUE = "value";
   private final static String COLUMN = "column";
   private final static String CUSTOM = "custom";
   private final static String COLUMNS = "columns";

   private static final String location = location();

   private static final ConcurrentHashMap<String,Class<?>> classes =
      new ConcurrentHashMap<String,Class<?>>();


   protected Object value = null;
   protected String column = null;
   protected String custom = null;
   protected Source source = null;
   protected String[] columns = null;
   protected JSONObject definition = null;

   protected ArrayList<BindValue> bindvalues =
      new ArrayList<BindValue>();


   public abstract String sql();
   public abstract ArrayList<BindValue> bindvalues();


   public Filter(Source source, JSONObject definition)
   {
      this.source = source;
      this.definition = definition;

      if (definition.has(VALUE))
         value = definition.get(VALUE);

      if (definition.has(COLUMN))
         column = definition.getString(COLUMN);

      if (definition.has(CUSTOM))
         custom = definition.getString(CUSTOM);

      if (definition.has(COLUMNS))
      {
         JSONArray cols = definition.getJSONArray(COLUMNS);

         columns = new String[cols.length()];
         for (int i = 0; i < columns.length; i++)
            columns[i] = cols.getString(i);
      }
   }


   @SuppressWarnings("unchecked")
   public static <T extends Filter> T getInstance(String name, Source source, JSONObject definition) throws Exception
   {
      name = Character.toUpperCase(name.charAt(0)) +
               name.substring(1).toLowerCase();

      String cname = location+"."+name;
      Class<?> clazz = classes.get(cname);

      if (clazz == null)
      {
         clazz = (Class<?>) Class.forName(cname);
         classes.put(cname,clazz);
      }

      try
      {
         return((T) clazz.getConstructor(Source.class,JSONObject.class).newInstance(source,definition));
      }
      catch (Exception e)
      {
         throw new Exception(Messages.get("UNKNOWN_FILTER",name));
      }
   }


   public static String location()
   {
      String loc = Filter.class.getPackage().getName();
      int pos = loc.lastIndexOf('.');
      return(loc.substring(0,pos));
   }
}
