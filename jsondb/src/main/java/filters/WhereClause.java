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
      whcl.clause.tree();
      //String rep = whcl.toString();
      //System.out.println(rep);
   }

   private Clause clause;
   private JSONArray filters;
   private static final String FILTERS = "filters";


   public WhereClause(TableSource source, JSONObject definition) throws Exception
   {
      if (!definition.has(FILTERS)) return;
      this.filters = definition.getJSONArray(FILTERS);
   }


   public boolean isEmpty()
   {
      return(clause.empty);
   }


   public WhereClause parse() throws Exception
   {
      this.clause = new Clause(filters);
      return(this);
   }


   public String toString()
   {
      return(clause.toString());
   }


   private static class Clause
   {
      String type;
      boolean empty;
      Clause[] group;
      JSONObject filter;

      Clause(JSONArray filters) throws Exception
      {
         empty = true;
         this.type = "and";
         this.filter = null;

         int entries = filters.length();
         this.group = new Clause[entries];

         for (int i = 0; i < entries; i++)
         {
            this.group[i] = new Clause(filters.getJSONObject(i));
            if (!this.group[i].empty) this.empty = false;
         }
      }


      Clause(JSONObject filter) throws Exception
      {
         empty = true;
         this.type = "and";
         this.filter = filter;

         String[] attr = JSONObject.getNames(filter);

         if (attr[0].equalsIgnoreCase("and"))
         {
            Object fltdef = filter.get("and");

            if (fltdef instanceof JSONArray)
            {
               this.filter = null;

               JSONArray flts = (JSONArray) fltdef;
               this.group = new Clause[flts.length()];

               for (int i = 0; i < this.group.length; i++)
               {
                  this.group[i] = new Clause(flts.getJSONObject(i));
                  if (!this.group[i].empty) this.empty = false;
               }

               validateFirst();
            }
            else
            {
               this.empty = false;
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
               this.filter = null;

               JSONArray flts = (JSONArray) fltdef;
               this.group = new Clause[flts.length()];

               for (int i = 0; i < this.group.length; i++)
               {
                  this.group[i] = new Clause(flts.getJSONObject(i));
                  if (!this.group[i].empty) this.empty = false;
               }

               validateFirst();
            }
            else
            {
               this.empty = false;
               this.filter = (JSONObject) fltdef;
            }
         }
      }

      boolean isGroup()
      {
         return(group != null);
      }

      public void tree()
      {
         if (isGroup()) System.out.println("array");
         else System.out.println(filter.toString());

         if (isGroup())
         {
            for(Clause clause : group)
             clause.tree();
         }
      }

      public String toString()
      {
         return(type+" group: "+(group != null)+" filter: "+(filter != null));
         //return(toJSON().toString(2));
      }

      public JSONArray toJSON()
      {
         return(toJSON(new JSONArray()));
      }

      private JSONArray toJSON(JSONArray whcl)
      {
         if (isGroup())
         {
            for (int i = 0; i < group.length; i++)
            {
               JSONObject entry = new JSONObject();
               entry.put("type",group[i].type);
               whcl.put(group[i].toJSON());
            }
         }
         else
         {
            JSONObject entry = new JSONObject();
            entry.put(type,filter);
            whcl.put(entry);
         }

         return(whcl);
      }

      private void validateFirst() throws Exception
      {
         if (isGroup() && this.group.length > 0)
         {
            if (this.group[0].type.equals("or"))
               throw new Exception(Messages.get("WHERECLAUSE_OR_START",this.group[0].toString()));
         }
      }
   }
}
