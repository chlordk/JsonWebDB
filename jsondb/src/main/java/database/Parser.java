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

import java.util.ArrayList;


public class Parser
{
   public static SQL parse(String sql)
   {
      StringBuffer jdbc = new StringBuffer();
      StringBuffer stmt = new StringBuffer(sql);

      ArrayList<BindValue> bindvalues = new ArrayList<BindValue>();

      for (int i = 0; i < stmt.length(); i++)
      {
         char c = stmt.charAt(i);

         if (c == ':' || c == '&')
         {
            String bind = getBindValue(stmt,i);

            if (bind != null)
            {
               boolean amp = true;
               int pos = jdbc.length();

               if (c == '&')
               {
                  jdbc.append('&');
                  jdbc.append(bind);
               }
               else
               {
                  amp = false;
                  jdbc.append('?');
               }

               i += bind.length();
               BindValue bv = new BindValue(bind);
               bindvalues.add(bv.pos(pos).ampersand(amp));

               continue;
            }
         }

         jdbc.append(c);
      }

      return(new SQL(jdbc,bindvalues));
   }

   private static String getBindValue(StringBuffer stmt, int pos)
   {
      int start = pos + 1;

      if (pos > 0)
      {
         // Check last before start
         char pre = stmt.charAt(pos-1);
         if (wordCharacter(pre)) return(null);
         if (pre == ':' || pre == '&') return(null);
      }

      pos++;
      while(pos < stmt.length() && wordCharacter(stmt.charAt(pos))) pos++;

      char[] name = new char[pos-start];
      stmt.getChars(start,pos,name,0);

      return(new String(name).toLowerCase());
   }


   private static boolean wordCharacter(char c)
   {
      if (c == '_') return(true);

      if (c >= '0' && c <= '9') return(true);
      if (c >= 'a' && c <= 'z') return(true);
      if (c >= 'A' && c <= 'Z') return(true);

      return(false);
   }

   public static class SQL
   {
      public final String sql;
      public final ArrayList<BindValue> bindvalues;

      public SQL(StringBuffer sql, ArrayList<BindValue> bindValues)
      {
         this(new String(sql),bindValues);
      }

      public SQL(String sql, ArrayList<BindValue> bindValues)
      {
         this.sql = sql;
         this.bindvalues = bindValues;
      }

      public SQL bind(String name, Object value)
      {
         for(BindValue bv : this.bindvalues)
         {
            if (bv.name().equalsIgnoreCase(name))
               bv.value(value);
         }

         return(this);
      }

      public SQL bindByValue()
      {
         int delta = 0;
         String sql = this.sql;

         ArrayList<BindValue> bindvalues =
            new ArrayList<BindValue>();

         for(BindValue bv : this.bindvalues)
         {
            if (!bv.ampersand())
            {
               bindvalues.add(bv);
               continue;
            }

            int pe = bv.end() + delta;
            int pb = bv.start() + delta;

            String value = bv.value()+"";

            String se = sql.substring(pe);
            String sb = sql.substring(0,pb);

            sql = sb + value + se;
            delta += (value.length() - bv.len() - 1);
         }

         return(new SQL(sql,bindvalues));
      }
   }
}
