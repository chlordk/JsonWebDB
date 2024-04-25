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

import java.net.URI;
import java.util.Base64;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import javax.net.ssl.TrustManager;
import java.net.http.HttpResponse;
import javax.net.ssl.X509TrustManager;
import java.net.http.HttpRequest.Builder;
import java.security.cert.X509Certificate;
import java.net.http.HttpResponse.BodyHandler;
import java.security.cert.CertificateException;
import java.net.http.HttpRequest.BodyPublisher;


public class Client
{
   private Builder builder = null;


   public Client() throws Exception
   {
      this(null);
   }

   public Client(String url) throws Exception
   {
      this.builder = HttpRequest.newBuilder();
      setURL(url);
   }

   public Client setURL(String url) throws Exception
   {
      if (url != null) builder.uri(new URI(url));
      return(this);
   }

   public Client setHeader(String name, String value)
   {
      builder.header(name,value);
      return(this);
   }

   public Client setAuthorizationHeader()
   {
      String[] header = new String[2];

      String auth = Options.username() + ":" + Options.password();
      String value = Base64.getEncoder().encodeToString(auth.getBytes());

      header[0] = "Authorization";
      header[1] = "Basic "+value;

      setHeader(header[0],header[1]);
      return(this);
   }

   public byte[] get() throws Exception
   {
      HttpClient client = getClient();
      BodyHandler<byte[]> hdl = HttpResponse.BodyHandlers.ofByteArray();
      HttpResponse<byte[]> response = client.send(builder.GET().build(),hdl);
      return(response.body());
   }

   public byte[] post(String content) throws Exception
   {
      return(post(content.getBytes()));
   }

   public byte[] post(byte[] content) throws Exception
   {
      HttpClient client = getClient();
      BodyHandler<byte[]> hdl = HttpResponse.BodyHandlers.ofByteArray();
      BodyPublisher body = HttpRequest.BodyPublishers.ofByteArray(content);
      HttpResponse<byte[]> response = client.send(builder.POST(body).build(),hdl);
      return(response.body());
   }


   private static HttpClient getClient() throws Exception
   {
      SSLContext ctx = createSSLContext();
      return(HttpClient.newBuilder().sslContext(ctx).build());
   }


   private static SSLContext createSSLContext() throws Exception
   {
      SSLContext ctx = SSLContext.getInstance("TLS");

      KeyManager[] kmgrs = new KeyManager[0];
      TrustManager[] tmgrs = new TrustManager[] {new FakeTrustManager()};

      ctx.init(kmgrs,tmgrs,null);
      return(ctx);
   }


   private static class FakeTrustManager implements X509TrustManager
   {
     public FakeTrustManager()
     {
     }


     @Override
     public void checkClientTrusted(X509Certificate[] x509Certificate, String name) throws CertificateException
     {
     }


     @Override
     public void checkServerTrusted(X509Certificate[] certificates, String name) throws CertificateException
     {
     }


     @Override
     public X509Certificate[] getAcceptedIssuers()
     {
       return(null);
     }
   }}