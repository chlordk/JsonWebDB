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

package sources;

import database.Parser;
import database.Column;
import database.SQLPart;
import database.DataType;
import java.util.HashMap;
import database.BindValue;
import org.json.JSONArray;
import java.util.ArrayList;
import org.json.JSONObject;
import static utils.Misc.*;


public class TableSource extends Source
{
   private static final String ID = "id";
   private static final String SQL = "sql";
   private static final String VPD = "vpd";
   private static final String NAME = "name";
   private static final String APPLY = "apply";
   private static final String QUERY = "query";
   private static final String ORDER = "order";
   private static final String OBJECT = "object";
   private static final String DERIVED = "derived";
   private static final String PRIMARY = "primary-key";
   private static final String WHCLAUSE = "where-clause";
   private static final String CUSTOMFLTS = "custom-filters";

   public final String id;
   public final String order;
   public final String object;
   public final Access access;
   public final VPDFilter vpd;
   public final String[] derived;
   public final QuerySource query;
   public final String[] primarykey;
   public final HashMap<String,CustomFilter> filters;

   public HashMap<String,Column> columns;


   public TableSource(JSONObject definition) throws Exception
   {
      String id = getString(definition,ID,true,true);
      String order = getString(definition,ORDER,false);
      String object = getString(definition,OBJECT,false);
      String[] derived = getStringArray(definition,DERIVED,false);
      String[] primarykey = getStringArray(definition,PRIMARY,false);

      id = id.toLowerCase();
      if (object == null) object = id;
      else object = object.toLowerCase();

      Access access = new Access(definition);
      VPDFilter vpd = VPDFilter.parse(definition);
      QuerySource query = QuerySource.parse(definition,object);
      HashMap<String,CustomFilter> filters = CustomFilter.parse(definition);

      this.id = id;
      this.vpd = vpd;
      this.query = query;
      this.object = object;
      this.access = access;
      this.order = order;
      this.derived = derived;
      this.filters = filters;
      this.primarykey = primarykey;
   }

   public AccessType getAccessLimit(String operation)
   {
      return(this.access.getType(operation));
   }

   public boolean described()
   {
      return(columns != null);
   }

   public Column getColumn(String column)
   {
      return(columns.get(column.toLowerCase()));
   }

   public ArrayList<Column> getColumns()
   {
      if (columns == null) return(null);
      ArrayList<Column> columns = new ArrayList<Column>();
      columns.addAll(this.columns.values());
      return(columns);
   }

   public void setColumns(ArrayList<Column> columns)
   {
      HashMap<String,Column> index = new HashMap<String,Column>();
      for(Column column : columns) index.put(column.name.toLowerCase(),column);
      this.columns = index;
   }

   public SQLPart from(ArrayList<BindValue> bindvalues)
   {
      SQLPart from;

      if (query != null) from = query.from(bindvalues);
      else               from = new SQLPart("\nfrom "+object);

      return(from);
   }

   public String toString()
   {
      return(this.getClass().getSimpleName()+": "+id);
   }


   public static class QuerySource
   {
      public final String object;
      public final SQLPart query;
      public final HashMap<String,DataType> types;

      private static QuerySource parse(JSONObject def, String object) throws Exception
      {
         if (!def.has(QUERY))
            return(null);

         def = def.getJSONObject(QUERY);

         String sql = getString(def,SQL,true);
         HashMap<String,DataType> types = DataType.parse(def);

         return(new QuerySource(object,sql,types));
      }

      private QuerySource(String object, String query, HashMap<String,DataType> types)
      {
         this.types = types;
         this.object = object;
         this.query = Parser.parse(query);
      }

      private SQLPart from(ArrayList<BindValue> bindvalues)
      {
         SQLPart bound = query.clone();

         for(BindValue bv : bindvalues)
         {
            DataType def = this.types.get(bv.name());
            if (def != null) bv.type(def.sqlid);
            bound.bind(bv);
         }

         bound.bindByValue();
         String sql = "\nfrom ("+bound.snippet()+") "+object;

         return(bound.snippet(sql));
      }
   }


   public static class CustomFilter
   {
      public final String name;
      public final SQLPart filter;

      private static HashMap<String,CustomFilter> parse(JSONObject def) throws Exception
      {
         if (!def.has(CUSTOMFLTS))
            return(null);

         HashMap<String,CustomFilter> filters =
            new HashMap<String,CustomFilter>();

         JSONArray arr = def.getJSONArray(CUSTOMFLTS);

         for (int i = 0; i < arr.length(); i++)
         {
            JSONObject flt = arr.getJSONObject(i);
            String name = getString(flt,NAME,true,true);
            String whcl = getString(flt,WHCLAUSE,true,false);
            filters.put(name.toLowerCase(),new CustomFilter(name,whcl));
         }

         return(filters);
      }


      private CustomFilter(String name, String whcl)
      {
         this.name = name;
         this.filter = Parser.parse(whcl);
      }
   }


   public static class VPDFilter
   {
      public SQLPart filter;
      public final String[] apply;

      private static VPDFilter parse(JSONObject def) throws Exception
      {
         if (!def.has(VPD)) return(null);

         def = def.getJSONObject(VPD);

         String filter = getString(def,WHCLAUSE,true);
         String[] applies = getStringArray(def,APPLY,true,true);

         return(new VPDFilter(filter,applies));
      }

      private VPDFilter(String filter, String[] apply)
      {
         this.filter = Parser.parse(filter);
         this.apply = apply;
      }
   }

   public static class Access
   {
      public final AccessType insert;
      public final AccessType update;
      public final AccessType delete;
      public final AccessType select;

      Access(JSONObject def)
      {
         insert = getType(def,"insert");
         update = getType(def,"update");
         delete = getType(def,"delete");
         select = getType(def,"select");
      }

      public AccessType getType(String op)
      {
         op = op.toLowerCase();

         switch (op)
         {
            case "insert": return(insert);
            case "update": return(update);
            case "delete": return(delete);
            case "select": return(select);
            default: return(AccessType.denied);
         }
      }

      private AccessType getType(JSONObject def, String type)
      {
         String act = def.optString(type);
         if (act.length() == 0) act = "denied";
         act = act.replaceAll("-","");
         return(AccessType.valueOf(act));
      }
   }

   public static enum AccessType
   {
      denied,
      allowed,
      byprimarykey,
      ifwhereclause
   }
}