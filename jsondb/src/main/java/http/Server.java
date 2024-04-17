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

package http;

import java.net.URL;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.io.File;
import java.io.FileInputStream;

import jsondb.Config;
import jsondb.JsonDB;
import org.json.JSONObject;
import java.io.FilenameFilter;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;


public class Server
{
   private static final String PORT = "port";
   private static final String SSLPORT = "ssl-port";

   private static final String QUEUE = "queue-length";
   private static final String THREADS = "worker-threads";

   private static final String TYPE = "type";
   private static final String ALIAS = "alias";
   private static final String STORE = "keystore";
   private static final String PASSWORD = "password";

   private static final String SECTION = "embedded";
   private static final String SECURITY = "security";

   private static int port;
   private static int sslport;

   private static int queue;
   private static int threads;

   private static String store;
   private static String alias;
   private static String password;
   private static String storetype;

   private static SSLContext ctx;

   public static void main(String[] args) throws Exception
   {
      InetSocketAddress addr;

      if (args.length != 1)
      {
         System.err.println("Program requires exactly one argument [instance name]");
         System.exit(-1);
      }

      String inst = args[0];
      String root = findAppHome();
      JsonDB.initialize(root,inst);

      loadServerConfig();
      createSSLContext();

      addr = new InetSocketAddress(port);
      HttpServer defsrv = HttpServer.create(addr,queue);
      defsrv.setExecutor(Executors.newFixedThreadPool(threads));
      defsrv.createContext("/",new Handler());
      defsrv.start();

      addr = new InetSocketAddress(sslport);
      HttpsServer secsrv = HttpsServer.create(addr,queue);
      secsrv.setHttpsConfigurator(new HttpsConfigurator(ctx));
      secsrv.setExecutor(Executors.newFixedThreadPool(threads));
      secsrv.createContext("/",new Handler());
      secsrv.start();
   }


   private static void createSSLContext() throws Exception
   {
      String path = Config.path(SECURITY,store);

      KeyStore ks = KeyStore.getInstance(storetype);
      FileInputStream in = new FileInputStream(path);
      ks.load(in,password.toCharArray()); in.close();

      String kalg = KeyManagerFactory.getDefaultAlgorithm();
      KeyManagerFactory kfac = KeyManagerFactory.getInstance(kalg);
      kfac.init(ks,password.toCharArray());

      String talg = TrustManagerFactory.getDefaultAlgorithm();
      TrustManagerFactory tfac = TrustManagerFactory.getInstance(talg);
      tfac.init(ks);

      ctx = SSLContext.getInstance("TLS");
      ctx.init(kfac.getKeyManagers(),tfac.getTrustManagers(),new SecureRandom());
   }


   private static void loadServerConfig()
   {
      JSONObject conf = Config.get(SECTION);

      port = Config.get(conf,PORT);
      sslport = Config.get(conf,SSLPORT);

      queue = Config.get(conf,QUEUE);
      threads = Config.get(conf,THREADS);

      store = Config.get(conf,STORE);
      alias = Config.get(conf,ALIAS);

      storetype = Config.get(conf,TYPE);
      password = Config.get(conf,PASSWORD);
   }


   private static String findAppHome()
   {
      Object obj = new Object() { };

      String cname = obj.getClass().getEnclosingClass().getName();
      cname = "/" + cname.replace('.','/') + ".class";

      URL url = obj.getClass().getResource(cname);
      String path = url.getPath();

      if (url.getProtocol().equals("jar") || url.getProtocol().equals("code-source"))
      {
         path = path.substring(5); // get rid of "file:"
         path = path.substring(0,path.indexOf("!")); // get rid of "!class"
         path = path.substring(0,path.lastIndexOf("/")); // get rid of jarname
      }

      int pos = 0;
      File folder = null;
      HasConfigFolder check = new HasConfigFolder();

      while(pos >= 0)
      {
         folder = new File(path);

         if (folder.isDirectory() && folder.listFiles(check).length == 1)
            break;

         pos = path.lastIndexOf("/");
         path = path.substring(0,pos);
      }

      return(path);
   }


   private static class HasConfigFolder implements FilenameFilter
   {
      @Override
      public boolean accept(File dir, String name)
         {return(name.equals("config"));}
   }
}
