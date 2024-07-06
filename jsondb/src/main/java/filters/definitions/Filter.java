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

import filters.Like;
import filters.Equals;
import filters.Custom;
import filters.IsNull;
import filters.NotLike;
import filters.Between;
import filters.LessThan;
import filters.NotEquals;
import java.util.HashMap;
import messages.Messages;
import filters.IsNotNull;
import filters.NotBetween;
import org.json.JSONArray;
import database.BindValue;
import org.json.JSONObject;
import java.util.ArrayList;
import filters.GreaterThan;
import java.util.Comparator;
import filters.LessThanEquals;
import filters.MultiDateRange;
import filters.MultiListFilter;
import filters.GreaterThanEquals;
import filters.WhereClause.Context;


public abstract class Filter
{
   private final static String VALUE = "value";
   private final static String COLUMN = "column";
   private final static String CUSTOM = "custom";
   private final static String VALUES = "values";
   private final static String COLUMNS = "columns";

   public static void list()
   {
      ArrayList<String> flts = new ArrayList<String>();

      for(String flt : classes.keySet())
         flts.add(flt);

      flts.sort(new Comparator<String>()
      {
         @Override
         public int compare(String s1, String s2)
         {
            if (s1.length() != s2.length())
               return(s1.length() - s2.length());

            return(s1.compareTo(s2));
         }
      });

      for(String flt : flts)
         System.out.println(flt);
   }


   private static final HashMap<String,Class<?>> classes =
      new HashMap<String,Class<?>>()
      {{
         put("like",Like.class);
         put("not like",NotLike.class);

         put("=",Equals.class);
         put("!=",NotEquals.class);

         put("<",LessThan.class);
         put("<=",LessThanEquals.class);

         put(">",GreaterThan.class);
         put(">=",GreaterThanEquals.class);

         put("is null",IsNull.class);
         put("is not null",IsNotNull.class);

         put("between",Between.class);
         put("not between",NotBetween.class);

         put("in",MultiListFilter.class);
         put("not in",MultiListFilter.class);

         put("exists",MultiListFilter.class);
         put("not exists",MultiListFilter.class);

         put("@day",MultiDateRange.class);
         put("@week",MultiDateRange.class);
         put("@month",MultiDateRange.class);
         put("@year",MultiDateRange.class);

         put("custom",Custom.class);
      }};


   protected Object value = null;
   protected String column = null;
   protected String custom = null;
   protected Object[] values = null;
   protected Context context = null;
   protected String[] columns = null;
   protected JSONObject definition = null;

   protected ArrayList<BindValue> bindvalues =
      new ArrayList<BindValue>();


   public abstract String sql();
   public abstract ArrayList<BindValue> bindvalues();


   public Filter(Context context, JSONObject definition)
   {
      this.context = context;
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

      if (definition.has(VALUES))
      {
         JSONArray vals = definition.getJSONArray(VALUES);

         values = new Object[vals.length()];
         for (int i = 0; i < values.length; i++)
            values[i] = vals.get(i);
      }
   }


   @SuppressWarnings("unchecked")
   public static <T extends Filter> T getInstance(String name, Context context, JSONObject definition) throws Exception
   {
      name = name.toLowerCase();

      while (name.indexOf("  ") >= 0)
         name = name.replaceAll("  "," ");

      Class<?> clazz = classes.get(name);

      try
      {
         return((T) clazz.getConstructor(Context.class,JSONObject.class).newInstance(context,definition));
      }
      catch (Exception e)
      {
         throw new Exception(Messages.get("UNKNOWN_FILTER",name));
      }
   }
}