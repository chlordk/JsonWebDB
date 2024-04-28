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

import sources.Source;
import org.json.JSONArray;
import org.json.JSONObject;

import oracle.net.aso.f;


public class WhereClause
{
   private static final String FILTERS = "filters";

   private JSONArray filters;


   public WhereClause(Source source, JSONObject definition) throws Exception
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
      JSONObject[] filters = new JSONObject[entries];

      for (int i = 0; i < entries; i++)
         filters[i] = fltlist.getJSONObject(i);

      return(entries);
   }


   private boolean validateFirst(JSONObject first) throws Exception
   {

      String[] attr = JSONObject.getNames(filters[0]);
      if (attr[0].equalsIgnoreCase("or"))

   }
}
