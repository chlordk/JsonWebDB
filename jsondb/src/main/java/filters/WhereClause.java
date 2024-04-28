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

      validateFirst(filters[0]);

      return(entries);
   }


   private void validateFirst(Clause first) throws Exception
   {
      if (first.type.equals("or"))
        throw new Exception(Messages.get("WHERECLAUSE_OR_START",first.toString()));
   }


   private static class Clause
   {
      String type;
      JSONObject filter;
      JSONArray filters;

      Clause(JSONObject filter)
      {
         this.type = "and";
         this.filter = filter;

         String[] attr = JSONObject.getNames(filter);

         if (attr[0].equalsIgnoreCase("and"))
         {
            Object flt = filter.get("and");
            if (flt instanceof JSONArray) filters = (JSONArray) flt;
            else if (flt instanceof JSONObject) filter = (JSONObject) flt;
         }

         if (attr[0].equalsIgnoreCase("or"))
         {
            this.type = "or";
            Object flt = filter.get("or");
            if (flt instanceof JSONArray) filters = (JSONArray) flt;
            else if (flt instanceof JSONObject) filter = (JSONObject) flt;
         }
      }

      boolean list()
      {
         return(filters != null);
      }
   }
}
