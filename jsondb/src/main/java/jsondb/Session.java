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

package jsondb;

import jsondb.state.StateHandler;
import jsondb.state.StateHandler.SessionInfo;

import java.util.concurrent.ConcurrentHashMap;


public class Session
{
   private final String guid;
   private final String user;
   private final boolean dedicated;

   private final static ConcurrentHashMap<String,Session> sessions =
      new ConcurrentHashMap<String,Session>();


   public static Session get(String guid) throws Exception
   {
      Session session = sessions.get(guid);

      if (session == null)
      {
         SessionInfo info = StateHandler.getSession(guid);

         if (info != null)
         {
            session = new Session(guid,info.user,info.dedicated);
            sessions.put(guid,session);
         }
      }

      return(session);
   }

   public static Session create(String user, boolean dedicated) throws Exception
   {
      String guid = StateHandler.createSession(user,dedicated);

      Session session = new Session(guid,user,dedicated);
      sessions.put(guid,session);

      return(session);
   }

   private Session(String guid, String user, boolean dedicated)
   {
      this.guid = guid;
      this.user = user;
      this.dedicated = dedicated;
   }

   public String getGuid()
   {
      return guid;
   }

   public String getUser()
   {
      return user;
   }

   public boolean isDedicated()
   {
      return(dedicated);
   }

   public boolean touch() throws Exception
   {
      boolean success = StateHandler.touchSession(guid);
      return(success);
   }


   public boolean remove() throws Exception
   {
      boolean success = StateHandler.removeSession(guid);
      sessions.remove(guid);
      return(success);
   }
}
