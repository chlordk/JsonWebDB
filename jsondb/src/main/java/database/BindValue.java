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

import utils.Dates;
import jsondb.Config;
import java.util.Date;
import messages.Messages;
import utils.JSONOObject;
import org.json.JSONObject;


/**
 * Bind values are always named and defined by the colon ':' and a name, like in ":country".
 * Using the ampersand '&' form in sql indicates that this value should be passed by value.
 * With stored procedures, the ampersand form suggests that the value is an out parameter.
 */
public class BindValue
{
   private int pos = 0;
   private String name = null;
   private Object value = null;
   private Integer type = null;
   private boolean outtype = false;
   private boolean ampersand = false;


   public static BindValue from(JSONObject json)
   {
      BindValue bv = new BindValue();

      bv.value       = json.get("value");
      bv.type        = json.getInt("type");
      bv.name        = json.getString("name");
      bv.ampersand   = json.getBoolean("ampersand");

      if (SQLTypes.getType(bv.type).indexOf("DATE") >= 0)
         bv.value = new Date((long) bv.value);

      else

      if (SQLTypes.getType(bv.type).indexOf("TIME") >= 0)
         bv.value = new Date((long) bv.value);

      return(bv);
   }


   public BindValue()
   {
   }

   public BindValue(String name)
   {
      this.name = name;
   }

   public int start()
   {
      return(pos);
   }

   public int end()
   {
      return(pos+len()+1);
   }

   public int len()
   {
      return(name.length());
   }

   public String name()
   {
      return(name);
   }

   public boolean out()
   {
      return(outtype);
   }

   public boolean untyped()
   {
      return(type == null);
   }

   public Integer type()
   {
      return(type);
   }

   public Object value()
   {
      return(value);
   }

   public boolean ampersand()
   {
      return(ampersand);
   }

   public BindValue pos(int pos)
   {
      this.pos = pos;
      return(this);
   }

   public BindValue name(String name)
   {
      this.name = name.toLowerCase();
      return(this);
   }

   public BindValue type(Integer type)
   {
      this.type = type;
      return(this);
   }

   public BindValue value(Object value)
   {
      this.value = value;
      return(this);
   }

   public BindValue out(boolean out)
   {
      outtype = out;
      return(this);
   }

   public BindValue ampersand(boolean ampersand)
   {
      this.ampersand = ampersand;
      return(this);
   }

   public void validate() throws Exception
   {
      if (type == null)
      {
         type = SQLTypes.guessType(value);
         Config.logger().warning(Messages.get("NO_DATATYPE_SPECIFIED",this.name));
      }

      if (SQLTypes.isDateType(type))
         this.value = Dates.toDate(value);
   }

   public BindValue clone()
   {
      BindValue clone = new BindValue();

      clone.pos = this.pos;
      clone.name = this.name;
      clone.type = this.type;
      clone.value = this.value;
      clone.outtype = this.outtype;
      clone.ampersand = this.ampersand;

      return(clone);
   }

   public String desc()
   {
      return("sqltype: "+type+", value: "+value);
   }

   public String toString()
   {
      String t = ampersand ? "?" : ":";
      return("name: "+t+name+", sqltype: "+type+", value: "+value);
   }

   public JSONObject toJSON()
   {
      Object value = this.value;

      if (value instanceof Date)
         value = ((Date) value).getTime();

      JSONOObject json = new JSONOObject();
      json.put("name",name);
      json.put("type",type);
      json.put("ampersand",ampersand);
      json.put("value",value);
      return(json);
   }
}