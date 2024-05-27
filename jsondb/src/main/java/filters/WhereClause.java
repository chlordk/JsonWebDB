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

import jsondb.Session;
import database.SQLPart;
import messages.Messages;
import java.util.HashSet;
import java.util.HashMap;
import database.DataType;
import database.BindValue;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import sources.TableSource;
import filters.definitions.Filter;


public class WhereClause
{
   private SQLPart whcl;
   private Clause clause;
   private JSONArray filters;

   private static final String FILTER = "filter";
   private static final String CUSTOM = "custom";
   private static final String FILTERS = "filters";


   public WhereClause(Context context, JSONObject definition) throws Exception
   {
      this.whcl = new SQLPart();

      if (definition.has(FILTERS))
      {
         this.filters = definition.getJSONArray(FILTERS);
         if (this.filters.length() > 0) this.whcl = this.build(context);
      }
   }

   public boolean usesPrimaryKey(ArrayList<String> columns)
   {
      return(clause.usesPrimaryKey(columns));
   }

   public boolean exists()
   {
      return(clause != null && !clause.empty);
   }

   public SQLPart asSQL()
   {
      return(whcl);
   }


   @SuppressWarnings("unchecked")
   public SQLPart append(WhereClause append)
   {
      if (append == null)
         return(this.asSQL());

      SQLPart p1 = this.asSQL();
      SQLPart p2 = append.asSQL();

      if (!this.exists()) return(append.asSQL());
      if (!append.exists()) return(this.asSQL());

      String sql = p1.snippet() + p2.snippet().replaceFirst("where","and");
      ArrayList<BindValue> bindvalues = (ArrayList<BindValue>) p1.bindValues().clone();
      bindvalues.addAll(p2.bindValues());

      return(new SQLPart(sql,bindvalues));
   }


   private SQLPart build(Context context) throws Exception
   {
      this.clause = new Clause(context,filters);
      this.clause.root = true; this.clause.build();
      return(new SQLPart("\nwhere "+clause.sql,clause.bindvalues));
   }


   private static class Clause
   {
      String sql;
      String type;
      boolean root;
      boolean empty;
      Filter filter;
      Clause[] group;
      JSONArray definition;
      ArrayList<BindValue> bindvalues;

      Clause(Context context, JSONArray filters) throws Exception
      {
         root = false;
         empty = true;
         this.type = "and";
         this.filter = null;
         this.definition = filters;
         this.bindvalues = new ArrayList<BindValue>();

         int entries = filters.length();
         this.group = new Clause[entries];

         for (int i = 0; i < entries; i++)
         {
            this.group[i] = new Clause(context,filters.getJSONObject(i));
            if (!this.group[i].empty) this.empty = false;
            bindvalues.addAll(this.group[i].bindvalues);
            if (i == 0) StartWithAnd(this.group[i]);
         }
      }


      Clause(Context context, JSONObject filter) throws Exception
      {
         empty = true;
         this.type = "and";
         this.bindvalues = new ArrayList<BindValue>();

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
                  this.group[i] = new Clause(context,flts.getJSONObject(i));
                  if (!this.group[i].empty) this.empty = false;
                  bindvalues.addAll(this.group[i].bindvalues);
                  if (i == 0) StartWithAnd(this.group[i]);
               }
            }
            else
            {
               this.empty = false;
               this.filter = getFilter(context,(JSONObject) fltdef);
               this.bindvalues.addAll(this.filter.bindvalues());
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
                  this.group[i] = new Clause(context,flts.getJSONObject(i));
                  if (!this.group[i].empty) this.empty = false;
                  bindvalues.addAll(this.group[i].bindvalues);
                  if (i == 0) StartWithAnd(this.group[i]);
               }
            }
            else
            {
               this.empty = false;
               this.filter = getFilter(context,(JSONObject) fltdef);
               this.bindvalues.addAll(this.filter.bindvalues());
            }
         }

         else

         {
            this.empty = false;
            this.filter = getFilter(context,filter);
            this.bindvalues.addAll(this.filter.bindvalues());
         }
      }

      boolean isGroup()
      {
         return(group != null);
      }


      public boolean usesPrimaryKey(ArrayList<String> columns)
      {
         Equals test = null;
         HashSet<String> keys = new HashSet<String>();
         for(String column : columns) keys.add(column);

         for (int i = 0; i < this.group.length; i++)
         {
            String type = this.group[i].type;

            if (type != null && type.equals("or"))
               return(false);

            if (this.group[i].filter instanceof Equals)
            {
               test = (Equals) this.group[i].filter;
               keys.remove(test.column());
            }
         }

         return(keys.size() == 0);
      }


      public String build()
      {
         String sql = "";

         if (isGroup())
         {
            if (!root) sql += "(\n";
            for (int i = 0; i < group.length; i++)
            {
               if (group[i].type != null)
                  sql += "\n"+group[i].type+" ";

               if (group[i].isGroup()) sql += group[i].build();
               else
               {
                  sql += group[i].filter.sql();
               }
            }
            if (!root) sql += "\n)\n";
         }
         else
         {
            sql += filter.sql();
         }

         this.sql = sql;
         return(sql);
      }

      private void StartWithAnd(Clause clause) throws Exception
      {
         if (!clause.type.equals("and"))
            throw new Exception(Messages.get("WHERECLAUSE_OR_START",clause.definition.toString(2)));

         clause.type = null;
      }

      private static Filter getFilter(Context context, JSONObject def) throws Exception
      {
         String name = null;

         if (def.has(CUSTOM)) name = CUSTOM;
         else if (def.has(FILTER)) name = def.getString(FILTER);

         if (name == null)
            throw new Exception(Messages.get("BAD_FILTER_DEFINITION",def.toString(2)));

         return(Filter.getInstance(name,context,def));
      }
   }


   public static class Context
   {
      public final boolean query;
      public final Session session;
      public final TableSource source;
      public final HashMap<String,DataType> datatypes;

      public Context(Session session, TableSource source, boolean query)
      {
         this.query = query;
         this.source = source;
         this.session = session;
         datatypes = query ? source.qrycolumns : source.basecolumns;
      }
   }
}