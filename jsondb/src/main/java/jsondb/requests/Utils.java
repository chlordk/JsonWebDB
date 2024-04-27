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

package jsondb.requests;

import database.BindValue;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;


public class Utils
{
   private static final String NAME = "name";
   private static final String VALUE = "value";
   private static final String SECTION = "bindvalues";


   public static ArrayList<BindValue> getBindValues(JSONObject def)
   {
      if (!def.has(SECTION))
         return(null);

      ArrayList<BindValue> bindvalues =
         new ArrayList<BindValue>();

      JSONArray arr = def.getJSONArray(SECTION);

      for (int i = 0; i < arr.length(); i++)
      {
         JSONObject bdef = arr.getJSONObject(i);

         String name = bdef.getString(NAME);
         Object value = bdef.getString(VALUE);

         bindvalues.add(new BindValue(name).value(value));
      }

      return(bindvalues);
   }
}
