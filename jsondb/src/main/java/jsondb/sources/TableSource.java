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

package jsondb.sources;

import database.Parser;
import database.SQLPart;
import java.util.HashMap;
import database.BindValue;
import org.json.JSONArray;
import java.util.ArrayList;
import org.json.JSONObject;


public class TableSource extends Source
{
   private static final String ID = "id";
   private static final String VPD = "vpd";
   private static final String NAME = "name";
   private static final String APPLY = "apply";
   private static final String QUERY = "query";
   private static final String OBJECT = "object";
   private static final String SORTING = "sorting";
   private static final String DERIVED = "derived";
   private static final String PRIMARY = "primary-key";
   private static final String WHCLAUSE = "where-clause";
   private static final String CUSTOMFLTS = "custom-filters";

   public final String id;
   public final String object;
   public final VPDFilter vpd;
   public final String sorting;
   public final String[] derived;
   public final QuerySource query;
   public final String[] primarykey;
   public final HashMap<String,CustomFilter> filters;


   public TableSource(JSONObject definition) throws Exception
   {
      String id = getString(definition,ID,true,true);
      String object = getString(definition,OBJECT,false);
      String sorting = getString(definition,SORTING,false);
      String[] derived = getStringArray(definition,DERIVED,false);
      String[] primarykey = getStringArray(definition,PRIMARY,false);

      VPDFilter vpd = VPDFilter.parse(definition);
      QuerySource query = QuerySource.parse(definition);
      HashMap<String,CustomFilter> filters = CustomFilter.parse(definition);

      this.id = id;
      this.vpd = vpd;
      this.query = query;
      this.object = object;
      this.sorting = sorting;
      this.derived = derived;
      this.filters = filters;
      this.primarykey = primarykey;
   }

   public SQLPart from(ArrayList<BindValue> bindvalues)
   {
      SQLPart from;

      if (query != null) from = query.from(bindvalues);
      else               from = new SQLPart("from "+object);

      return(from);
   }

   public String toString()
   {
      return(this.getClass().getSimpleName()+": "+id);
   }


   public static class QuerySource
   {
      public final SQLPart query;

      private static QuerySource parse(JSONObject def) throws Exception
      {
         if (!def.has(QUERY)) return(null);
         return(new QuerySource(Source.getString(def,QUERY)));
      }

      private QuerySource(String query)
      {
         this.query = Parser.parse(query);
      }

      private SQLPart from(ArrayList<BindValue> bindvalues)
      {
         SQLPart bound = query.clone();

         String sql = "from ("+query.sql()+")";

         for(BindValue bv : bindvalues)
            bound.bind(bv.name(),bv.type(),bv.value());

         return(bound.sql(sql).bindByValue());
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
            String name = Source.getString(flt,NAME,true,true);
            String whcl = Source.getString(flt,WHCLAUSE,true,false);
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

         String filter = Source.getString(def,WHCLAUSE,true);
         String[] applies = Source.getStringArray(def,APPLY,true,true);

         return(new VPDFilter(filter,applies));
      }

      private VPDFilter(String filter, String[] apply)
      {
         this.filter = Parser.parse(filter);
         this.apply = apply;
      }
   }
}