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

import utils.Dates;
import java.util.Date;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.Instant;
import java.time.LocalDate;

import database.BindValue;
import java.util.ArrayList;
import org.json.JSONObject;
import java.time.ZonedDateTime;
import filters.definitions.Filter;
import filters.WhereClause.Context;
import java.time.temporal.ChronoUnit;

/**
 * This filter handles date-ranges for day, week, month and year
 */
public class MultiDateRange extends Filter
{
   private final Date date;
   private final String opr;


   public MultiDateRange(Context context, JSONObject definition) throws Exception
   {
      super(context,definition);

      String flt = definition.optString("filter");

      flt = flt.toLowerCase();
      flt = flt.replaceAll(" ","");

      opr = flt;
      date = Dates.toDate(this.value);
   }

   @Override
   public String sql()
   {
      return(column+" >= ? and "+column+" < ?");
   }

   @Override
   public ArrayList<BindValue> bindvalues()
   {
      if (bindvalues.size() == 0)
      {
         Date to = null;
         Date fr = trunc(date);

         switch (opr)
         {
            case "@day":
                  to = nextDay(fr);
               break;

            default:
               break;
         }
      }

      return(bindvalues);
   }


   private Date trunc(Date date)
   {
      Instant time = date.toInstant();
      ZonedDateTime utc = ZonedDateTime.ofInstant(time,ZoneId.systemDefault());
      utc = utc.truncatedTo(ChronoUnit.DAYS);
      date = Date.from(utc.toInstant());
      return(date);
   }


   private Date nextDay(Date date)
   {
      Instant time = date.toInstant();
      LocalDate ld = LocalDate.ofInstant(time,ZoneId.systemDefault());

      ld = ld.plusDays(1);
      date = Date.from(ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());

      return(date);
   }
}
