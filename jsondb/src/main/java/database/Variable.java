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

package database;

import messages.Messages;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONObject;


public class Variable extends DataType
{
   private static final String OUT = "out";
   private static final String NAME = "name";
   private static final String TYPE = "type";
   private static final String VARIABLES = "variables";
   private static final String[] ALLOWED = new String[] {"string","integer"};


   public static HashMap<String,Variable> getVariables(JSONObject def) throws Exception
   {
      HashMap<String,Variable> types =
         new HashMap<String,Variable>();

      if (!def.has(VARIABLES)) return(types);
      JSONArray arr = def.getJSONArray(VARIABLES);

      for (int i = 0; i < arr.length(); i++)
      {
         JSONObject tdef = arr.getJSONObject(i);

         Object type = tdef.get(TYPE);
         String name = tdef.getString(NAME);
         Boolean out = tdef.optBooleanObject(OUT,false);

         if (type instanceof String)
            types.put(name.toLowerCase(),new Variable(name,(String) type,out));

         else

         if (type instanceof Integer)
            types.put(name.toLowerCase(),new Variable(name,(Integer) type, out));

         else
            throw new Exception(Messages.get("WRONG_DATA_TYPE",Messages.flatten(ALLOWED)));
      }

      return(types);
   }


   public final boolean out;

   public Variable(String name, String type, boolean out)
   {
      super(name,type);
      this.out = out;
   }

   public Variable(String name, int type, boolean out)
   {
      super(name,type);
      this.out = out;
   }
}