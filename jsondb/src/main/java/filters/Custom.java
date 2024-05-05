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
import database.SQLPart;
import database.DataType;
import java.util.HashMap;
import database.BindValue;
import org.json.JSONObject;
import sources.TableSource;
import java.util.ArrayList;
import filters.definitions.Filter;
import sources.TableSource.CustomFilter;


public class Custom extends Filter
{
   private SQLPart parsed = null;


   public Custom(Source source, JSONObject definition)
   {
      super(source,definition);

      try
      {
         if (source instanceof TableSource)
         {
            TableSource ts = (TableSource) source;
            CustomFilter f = ts.filters.get(custom);

            HashMap<String,BindValue> bindings =
               new HashMap<String,BindValue>();

            for(DataType type : f.types.values())
            {
               Object value = definition.get(type.name);
               bindings.put(type.name.toLowerCase(),new BindValue(type.name).value(value));
            }

            this.parsed = f.bind(bindings);
         }
      }
      catch (Throwable t) {}
   }

   @Override
   public String sql()
   {
      if (parsed == null)
         return("\"NoSuchFilter@"+custom+"\"");

      return(parsed.snippet());
   }

   @Override
   public ArrayList<BindValue> bindvalues()
   {
      if (bindvalues.size() == 0 && parsed != null)
         bindvalues.addAll(parsed.bindValues());

      return(bindvalues);
   }
}