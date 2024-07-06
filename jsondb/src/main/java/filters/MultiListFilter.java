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

package filters;

import database.SQLPart;
import messages.Messages;
import database.BindValue;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import jsondb.requests.Table;
import filters.definitions.Filter;
import filters.WhereClause.Context;


/**
 * This filter handles IN, NOT IN, EXISTS, NOT EXISTS for both lists and subqueries
 */
public class MultiListFilter extends Filter
{
   private final String opr;
   private final SQLPart subq;
   private static final String TABLE = Table.class.getSimpleName();


   public MultiListFilter(Context context, JSONObject definition) throws Exception
   {
      super(context,definition);

      String flt = definition.optString("filter");
      while (flt.indexOf("  ") >= 0) flt = flt.replaceAll("  "," ");

      switch (flt.toLowerCase())
      {
         case "in":
            opr = "in";
            break;

         case "exists":
            opr = "exists";
            break;

         case "not in":
            opr = "not in";
            break;

         case "not exists":
            opr = "not exists";
            break;

         default:
            throw new Exception(Messages.get("UNKNOWN_FILTER",flt));
      }

      if (this.values != null && values.length > 0) subq = null;
      else subq = Table.getSubQuery(context,definition.getJSONObject(TABLE));
   }

   @Override
   public String sql()
   {
      if (column != null)
         columns = new String[] {column};

      String sql = "(";

      for (int i = 0; i < columns.length; i++)
      {
         if (i == 0) sql += columns[i];
         else sql += ", " + columns[i];
      }

      sql += ") "+opr+" (";

      if (subq != null)
      {
         sql += subq.snippet();
      }
      else
      {
         if (values[0] instanceof JSONArray)
         {
            Object[][] list = new Object[values.length][];

            for (int i = 0; i < values.length; i++)
            {
               JSONArray vals = (JSONArray) values[i];
               list[i] = new Object[vals.length()];
               for (int j = 0; j < list.length; j++)
                  list[i][j] = vals.get(j);
            }

            boolean quote = (list[0][0] instanceof String);

            for (int i = 0; i < values.length; i++)
            {
               if (i > 0) sql += ",";

               sql += "(";

               for (int j = 0; j < list[i].length; j++)
               {
                  if (j > 0) sql += ",";
                  if (!quote) sql += list[i][j];
                  else sql += "'" + list[i][j] + "'";
               }

               sql += ")";
            }
         }
         else
         {
            boolean quote = (values[0] instanceof String);

            for (int i = 0; i < values.length; i++)
            {
               if (i > 0) sql += ",";
               if (!quote) sql += values[i];
               else sql += "'" + values[i] + "'";
            }
         }
      }

      sql += ")";

      return(sql);
   }

   @Override
   public ArrayList<BindValue> bindvalues()
   {
      if (subq == null) return(this.bindvalues);
      return(subq.bindValues());
   }
}