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

import database.definitions.SQLTypes;


public class BindValue
{
   private int pos = 0;
   private String name = null;
   private Object value = null;
   private boolean ampersand = false;

   private Integer sqlTypeID = null;
   private String sqlTypeName = null;


   public BindValue()
   {
   }

   public BindValue(String name)
   {
      this.name = name.toLowerCase();
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

   public String sqlTypeName()
   {
      return(sqlTypeName);
   }

   public int sqlTypeID()
   {
      return(sqlTypeID);
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

   public BindValue type(String type)
   {
      this.sqlTypeName = type.toLowerCase();
      this.sqlTypeID = SQLTypes.getType(type);
      return(this);
   }

   public BindValue type(int type)
   {
      this.sqlTypeID = type;
      this.sqlTypeName = SQLTypes.getType(type);
      return(this);
   }

   public BindValue value(Object value)
   {
      this.value = value;
      return(this);
   }

   public BindValue ampersand(boolean ampersand)
   {
      this.ampersand = ampersand;
      return(this);
   }

   public String toString()
   {
      String t = ampersand ? "?" : ":";
      return(t+name+"["+sqlTypeID+"] = "+value);
   }
}