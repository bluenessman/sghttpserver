package cc.blueness.service;

import java.io.BufferedInputStream;  
import java.io.BufferedOutputStream;  
import java.io.BufferedReader;
import java.io.DataInputStream;  
import java.io.File;  
import java.io.FileInputStream;  
import java.io.FileOutputStream;
import java.io.IOException;  
import java.io.InputStreamReader;  
import java.io.OutputStream;  
import java.io.OutputStreamWriter;  
import java.io.Reader;  
import java.io.Writer;  
import java.net.Socket;  
import java.util.Date;  
import java.util.List;  
import java.util.LinkedList;  
import java.util.StringTokenizer;  
  
public class RequestProcessor implements Runnable {  
  
    private static List pool=new LinkedList();  
    private File documentRootDirectory;  
    private String indexFileName="index.html";  
    private int contentLength = 0;
    private String boundary = null; 
    private String requestPath;
    
    public RequestProcessor(File documentRootDirectory,String indexFileName) {  
        if (documentRootDirectory.isFile()) {  
            throw new IllegalArgumentException();  
        }  
        this.documentRootDirectory=documentRootDirectory;  
        try {  
            this.documentRootDirectory=documentRootDirectory.getCanonicalFile();  
        } catch (IOException e) {  
        }  
          
        if (indexFileName!=null) {  
            this.indexFileName=indexFileName;  
        }
    }  
      
    public static void processRequest(Socket request) {  
        synchronized (pool) {  
            pool.add(pool.size(),request);  
            pool.notifyAll();  
        }  
    }  
      
    @Override  
    public void run() {
        //安全性检测  
    	String root;
    	if(documentRootDirectory!=null){
    		root=documentRootDirectory.getPath();  
    	}
          
        while (true) {  
            Socket connection;  
            synchronized (pool) {  
                while (pool.isEmpty()) {  
                    try {  
                        pool.wait();  
                    } catch (InterruptedException e) {  
                    }  
                      
                }  
                connection=(Socket)pool.remove(0);  
            }  
              
            try {
            	DataInputStream reader = new DataInputStream((connection.getInputStream()));  
                String line = reader.readLine();  
                String method = line.substring(0, 4).trim();  
                OutputStream out = connection.getOutputStream();  
                this.requestPath = line.split(" ")[1];  
                System.out.println(method);  
                if ("GET".equalsIgnoreCase(method)) {  
                    System.out.println("do get......");  
                    this.doGet(reader, out ,requestPath);  
                } else if ("POST".equalsIgnoreCase(method)) {  
                    System.out.println("do post......");  
                    this.doPost(reader, out);  
                }  
                  
            } catch (Exception e) {
				e.printStackTrace();
			}finally{  
                try {  
                    connection.close();  
                } catch (IOException e2) {  
                }  
                  
            }  
        }  
    }  
    
  //处理GET请求  
    private void doGet(DataInputStream reader, OutputStream out,String requestPath) throws Exception {  
    	BufferedReader bd=new BufferedReader(new InputStreamReader(reader));
    	 /**
         * 接受HTTP请求
         */
        String requestHeader;
        while((requestHeader=bd.readLine())!=null&&!requestHeader.isEmpty()){
            System.out.println(requestHeader);
            /**
             * 获得GET参数
             */
                
        }
        int begin = requestPath.indexOf("/?")+2;
        String condition=requestPath.substring(begin, requestPath.length());
        System.out.println("GET参数是："+condition);
        
      //发送回浏览器的内容  
        String response = "";  
        response += "HTTP/1.1 200 OK/n";  
        response += "Server: Sunpache 1.0/n";  
        response += "Content-Type: text/html/n";  
        response += "Last-Modified: Mon, 11 Jan 1998 13:23:42 GMT/n";  
        response += "Accept-ranges: bytes";  
        //response += "/n";  
        //String body = "<html><head><title>test server</title></head><body><p>post ok:</p>" + new String(buf, 0, size) + "</body></html>";  
        //System.out.println(body);  
        out.write(response.getBytes());  
        //out.write(body.getBytes());  
        out.flush();  
        reader.close();  
        out.close();  
        System.out.println("request complete.");  
    }  
  //处理post请求  
    private void doPost(DataInputStream reader, OutputStream out) throws Exception {
        String line = reader.readLine();  
        while (line != null) {
            System.out.println(line);  
            line = reader.readLine();  
            if ("".equals(line)) {  
                break;  
            } else if (line.indexOf("Content-Length") != -1) {  
                this.contentLength = Integer.parseInt(line.substring(line.indexOf("Content-Length") + 16));  
            }  
            //表明要上传附件， 跳转到doMultiPart方法。  
            else if(line.indexOf("multipart/form-data")!= -1){  
                //得multiltipart的分隔符  
                this.boundary = line.substring(line.indexOf("boundary") + 9);  
                this.doMultiPart(reader, out);  
                return;  
            }  
        }  
        //继续读取普通post（没有附件）提交的数据  
        System.out.println("begin reading posted data......");  
        String dataLine = null;  
        //用户发送的post数据正文  
        byte[] buf = {};  
        int size = 0;  
        if (this.contentLength != 0) {
            buf = new byte[this.contentLength];  
            while(size<this.contentLength){  
                int c = reader.read();  
                buf[size++] = (byte)c;  
                  
            }  
            System.out.println("The data user posted: " + new String(buf, 0, size));  
        }  
        //发送回浏览器的内容  
        String response = "";  
        response += "HTTP/1.1 200 OK/n";  
        response += "Server: Sunpache 1.0/n";  
        response += "Content-Type: text/html/n";  
        response += "Last-Modified: Mon, 11 Jan 1998 13:23:42 GMT/n";  
        response += "Accept-ranges: bytes";  
        response += "/n";  
        String body = "<html><head><title>test server</title></head><body><p>post ok:</p>" + new String(buf, 0, size) + "</body></html>";  
        System.out.println(body);  
        out.write(response.getBytes());  
        out.write(body.getBytes()); 
        out.flush();  
        reader.close();  
        out.close();  
        System.out.println("request complete.");  
    }  
    //处理附件  
    private void doMultiPart(DataInputStream reader, OutputStream out) throws Exception {  
        System.out.println("doMultiPart ......");  
        String line = reader.readLine();  
        while (line != null) {  
            System.out.println(line);  
            line = reader.readLine();  
            if ("".equals(line)) {  
                break;  
            } else if (line.indexOf("Content-Length") != -1) {  
                this.contentLength = Integer.parseInt(line.substring(line.indexOf("Content-Length") + 16));  
                System.out.println("contentLength: " + this.contentLength);  
            } else if (line.indexOf("boundary") != -1) {  
                //获取multipart分隔符  
                this.boundary = line.substring(line.indexOf("boundary") + 9);  
            }  
        }  
        System.out.println("begin get data......");  
        /*下面的注释是一个浏览器发送带附件的请求的全文，所有中文都是说明性的文字***** 
        <HTTP头部内容略> 
        ............ 
        Cache-Control: no-cache 
        <这里有一个空行，表明接下来的内容都是要提交的正文> 
        -----------------------------7d925134501f6<这是multipart分隔符> 
        Content-Disposition: form-data; name="myfile"; filename="mywork.doc" 
        Content-Type: text/plain 
         
        <附件正文>........................................ 
        ................................................. 
         
        -----------------------------7d925134501f6<这是multipart分隔符> 
        Content-Disposition: form-data; name="myname"<其他字段或附件> 
        <这里有一个空行> 
        <其他字段或附件的内容> 
        -----------------------------7d925134501f6--<这是multipart分隔符，最后一个分隔符多两个-> 
        ****************************************************************/  
        /** 
         * 上面的注释是一个带附件的multipart类型的POST的全文模型， 
         * 要把附件去出来，就是要找到附件正文的起始位置和结束位置 
         * **/  
        if (this.contentLength != 0) {  
            //把所有的提交的正文，包括附件和其他字段都先读到buf.  
            byte[] buf = new byte[this.contentLength];  
            int totalRead = 0;  
            int size = 0;  
            while (totalRead < this.contentLength) {  
                size = reader.read(buf, totalRead, this.contentLength - totalRead);  
                totalRead += size;  
            }  
            //用buf构造一个字符串，可以用字符串方便的计算出附件所在的位置  
            String dataString = new String(buf, 0, totalRead);  
            System.out.println("the data user posted:/n" + dataString);  
            int pos = dataString.indexOf(boundary);  
            //以下略过4行就是第一个附件的位置  
            pos = dataString.indexOf("/n", pos) + 1;  
            pos = dataString.indexOf("/n", pos) + 1;  
            pos = dataString.indexOf("/n", pos) + 1;  
            pos = dataString.indexOf("/n", pos) + 1;  
            //附件开始位置  
            int start = dataString.substring(0, pos).getBytes().length;  
            pos = dataString.indexOf(boundary, pos) - 4;  
            //附件结束位置  
            int end = dataString.substring(0, pos).getBytes().length;  
            //以下找出filename  
            int fileNameBegin = dataString.indexOf("filename") + 10;  
            int fileNameEnd = dataString.indexOf("/n", fileNameBegin);  
            String fileName = dataString.substring(fileNameBegin, fileNameEnd);  
            /** 
             * 有时候上传的文件显示完整的文件名路径,比如c:/my file/somedir/project.doc 
             * 但有时候只显示文件的名字，比如myphoto.jpg. 
             * 所以需要做一个判断。 
            */  
            if(fileName.lastIndexOf("//")!=-1){  
                fileName = fileName.substring(fileName.lastIndexOf("//") + 1);  
            }  
            fileName = fileName.substring(0, fileName.length()-2);  
            OutputStream fileOut = new FileOutputStream("c://" + fileName);  
            fileOut.write(buf, start, end-start);  
            fileOut.close();  
            fileOut.close();  
        }  
        String response = "";  
        response += "HTTP/1.1 200 OK/n";  
        response += "Server: Sunpache 1.0/n";  
        response += "Content-Type: text/html/n";  
        response += "Last-Modified: Mon, 11 Jan 1998 13:23:42 GMT/n";  
        response += "Accept-ranges: bytes";  
        response += "/n";  
        out.write("<html><head><title>test server</title></head><body><p>Post is ok</p></body></html>".getBytes());  
        out.flush();  
        reader.close();  
        System.out.println("request complete.");  
    }  
      
    public static String guessContentTypeFromName(String name) {  
        if (name.endsWith(".html")||name.endsWith(".htm")) {  
            return "text/html";  
        }else if (name.endsWith(".txt")||name.endsWith(".java")) {  
            return "text/plain";  
        }else if (name.endsWith(".gif")) {  
            return "image/gif";  
        }else if (name.endsWith(".class")) {  
            return "application/octet-stream";  
        }else if (name.endsWith(".jpg")||name.endsWith(".jpeg")) {  
            return "image/jpeg";  
        }else {  
            return "text/plain";  
        }  
    }  
  
}  
