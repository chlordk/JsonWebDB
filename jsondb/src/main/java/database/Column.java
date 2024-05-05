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

import utils.JSONOObject;
import org.json.JSONObject;


public class Column
{
   public final int sqltype;
   public final String type;
   public final String name;
   public final Integer[] precision;

   Column(String name, String type, int sqltype, int prec, int scale)
   {
      this.type = type;
      this.name = name;
      this.sqltype = sqltype;
      this.precision = new Integer[] {prec,scale};
   }

   public boolean isDateType()
   {
      return(SQLTypes.isDateType(sqltype));
   }

   public String toString()
   {
      return(name+" "+type+"["+sqltype+"] ["+precision[0]+","+precision[1]+"]");
   }

   public JSONObject toJSONObject()
   {
      JSONObject json = new JSONOObject();
      json.put("name",name);
      json.put("type",type);
      json.put("sqltype",sqltype);
      json.put("precision",precision);
      return(json);
   }
}