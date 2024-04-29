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

import jsondb.JsonDB;
import sources.Sources;
import messages.Messages;
import org.json.JSONArray;
import org.json.JSONObject;
import sources.TableSource;

import static utils.Misc.getJSONList;

import java.io.FileInputStream;


public class WhereClause
{
   public static void main(String[] args) throws Exception
   {
      JsonDB.initialize(args[0],args[1]);

      FileInputStream in = new FileInputStream("/Users/alhof/Repository/JsonWebDB/examples/table-select.json");
      String content = new String(in.readAllBytes()); in.close();

      JSONObject test = new JSONObject(content).getJSONObject("Table");
      String srcname = test.getString("source");

      TableSource source = Sources.get(srcname);

      test = test.getJSONObject("select()");
      WhereClause whcl = new WhereClause(source,test);
      whcl.parse();
   }

   private static final String FILTERS = "filters";

   private JSONArray filters;


   public WhereClause(TableSource source, JSONObject definition) throws Exception
   {
      if (!definition.has(FILTERS)) return;
      filters = definition.getJSONArray(FILTERS);
   }


   public boolean isEmpty()
   {
      return(filters == null || filters.length() == 0);
   }


   public WhereClause parse() throws Exception
   {
      parse(filters);
      return(this);
   }


   private int parse(JSONArray fltlist) throws Exception
   {
      int entries = fltlist.length();
      if (entries == 0) return(entries);

      Clause[] filters = new Clause[entries];

      for (int i = 0; i < entries; i++)
         filters[i] = new Clause(fltlist.getJSONObject(i));

      for (int i = 0; i < entries; i++)
         System.out.println(filters[i].toString());

      return(entries);
   }


   private static class Clause
   {
      String type;
      Clause[] group;
      JSONObject filter;

      Clause(JSONObject filter) throws Exception
      {
         this.type = "and";
         this.filter = filter;

         String[] attr = JSONObject.getNames(filter);

         if (attr[0].equalsIgnoreCase("and"))
         {
            Object fltdef = filter.get("and");

            if (fltdef instanceof JSONArray)
            {
               JSONArray flts = (JSONArray) fltdef;
               this.group = new Clause[flts.length()];

               for (int i = 0; i < this.group.length; i++)
                  this.group[i] = new Clause(flts.getJSONObject(i));

               validateFirst();
            }
            else
            {
               this.filter = (JSONObject) fltdef;
            }
         }

         else

         if (attr[0].equalsIgnoreCase("or"))
         {
            this.type = "or";
            Object fltdef = filter.get("or");

            if (fltdef instanceof JSONArray)
            {
               JSONArray flts = (JSONArray) fltdef;
               this.group = new Clause[flts.length()];

               for (int i = 0; i < this.group.length; i++)
                  this.group[i] = new Clause(flts.getJSONObject(i));

               validateFirst();
            }
            else
            {
               this.filter = (JSONObject) fltdef;
            }
         }
      }

      boolean isGroup()
      {
         return(group != null);
      }

      boolean isFilter()
      {
         return(filter != null);
      }

      private void validateFirst() throws Exception
      {
         if (isGroup() && this.group.length > 0)
         {
            if (this.group[0].type.equals("or"))
               throw new Exception(Messages.get("WHERECLAUSE_OR_START",this.group[0].toString()));
         }
      }

      public String toString()
      {
         String str = type+"\n";

         if (isFilter())
         {
            System.out.println("filter");
            str += filter.toString(2);
         }
         else
         {
            System.out.println("group");
            for(Clause clause : group)
               str += clause.toString();
         }

         return(str);
      }
   }
}
