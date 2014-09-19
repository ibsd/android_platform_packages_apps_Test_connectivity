
package com.googlecode.android_scripting.facade.wifi;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import android.app.Service;

import com.googlecode.android_scripting.Log;
import com.googlecode.android_scripting.facade.FacadeManager;
import com.googlecode.android_scripting.jsonrpc.RpcReceiver;
import com.googlecode.android_scripting.rpc.Rpc;
import com.googlecode.android_scripting.rpc.RpcParameter;

/**
 * Basic http operations.
 */
public class HttpFacade extends RpcReceiver {

    private final Service mService;
    private ServerSocket mServerSocket = null;
    private int mSocketPort = 8081;
    private int mServerTimeout = -1;
    private HashMap<Integer, Socket> mSockets = null;
    private int socketCnt = 0;

    public HttpFacade(FacadeManager manager) throws IOException {
        super(manager);
        mService = manager.getService();
        mSockets = new HashMap<Integer, Socket>();
        mServerSocket = new ServerSocket(mSocketPort);
    }

    private void inputStreamToOutputStream(InputStream in, OutputStream out) throws IOException {
        if (in == null) {
            Log.e("InputStream is null.");
            return;
        }
        if (out == null) {
            Log.e("OutputStream is null.");
            return;
        }
        try {
            int read = 0;
            byte[] bytes = new byte[1024];
            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            in.close();
            out.close();
        }

    }

    private String inputStreamToString(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String str = null;
        while ((str = r.readLine()) != null) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * Send an http request and get the response.
     *
     * @param url The url to send request to.
     * @return The HttpURLConnection object.
     * @throws IOException When request failed to go through with response code 200.
     */
    private HttpURLConnection httpRequest(String url) throws IOException {
        URL targetURL = new URL(url);
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) targetURL.openConnection();
        } catch (IOException e) {
            Log.e("Failed to open a connection to " + url);
            Log.e(e.toString());
            throw e;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        int code = urlConnection.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            Log.d("Http request did not return 200.");
            Log.d("Response code: " + code);
            String respMsg = urlConnection.getResponseMessage();
            Log.d("Response message: " + respMsg);
            throw new IOException("HTTP request did not return 200. " + respMsg);
        }
        return urlConnection;
    }

    @Rpc(description = "Start waiting for a connection request on a specified port.",
            returns = "The index of the connection.")
    public Integer httpAcceptConnection(int port) throws IOException {
        mServerSocket = new ServerSocket(port);
        if (mServerTimeout > 0) {
            mServerSocket.setSoTimeout(mServerTimeout);
        }
        Socket sock = mServerSocket.accept();
        socketCnt += 1;
        mSockets.put(socketCnt, sock);
        return socketCnt;
    }

    @Rpc(description = "Download a file from specified url.")
    public void httpDownloadFile(String url) throws IOException {
        HttpURLConnection urlConnection = httpRequest(url);

        String filename = null;
        String contentDisposition = urlConnection.getHeaderField("Content-Disposition");
        // Try to figure out the name of the file being downloaded.
        // If the server returned a filename, use it.
        if (contentDisposition != null) {
            int idx = contentDisposition.toLowerCase().indexOf("filename");
            if (idx != -1) {
                filename = contentDisposition.substring(idx + 9);
                Log.d("Using name returned by server: " + filename);
            }
        }
        // If the server did not provide a filename to us, use the last part of url.
        if (filename == null) {
            int lastIdx = url.lastIndexOf('/');
            filename = url.substring(lastIdx + 1);
            Log.d("Using name from url: " + filename);
        }
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        String outPath = "/sdcard/Download/" + filename;
        OutputStream output = new FileOutputStream(new File(outPath));
        inputStreamToOutputStream(in, output);
        Log.d("Downloaded file at " + outPath);
    }

    @Rpc(description = "Make an http request and return the response message.")
    public String httpPing(@RpcParameter(name = "url") String url) throws IOException {
        HttpURLConnection urlConnection = null;
        urlConnection = httpRequest(url);
        String resp = urlConnection.getResponseMessage();
        Log.d("Fetched " + resp);
        return resp;
    }

    @Rpc(description = "Make an http request and only return the length of the response content.")
    public Integer httpRequestLength(@RpcParameter(name = "url") String url) throws IOException {
        HttpURLConnection urlConnection = httpRequest(url);
        int respSize = urlConnection.getContentLength();
        Log.d("Fetched: " + respSize);
        return respSize;
    }

    @Rpc(description = "Make an http request and return the response content as a string.")
    public String httpRequestString(@RpcParameter(name = "url") String url) throws IOException {
        HttpURLConnection urlConnection = httpRequest(url);
        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
        String result = inputStreamToString(in);
        Log.d("Fetched: " + result);
        return result;
    }

    @Rpc(description = "Set how many milliseconds to wait for an incoming connection.")
    public void httpSetServerTimeout(@RpcParameter(name = "timeout") int timeout)
            throws SocketException {
        mServerSocket.setSoTimeout(timeout);
        mServerTimeout = timeout;
    }

    @Override
    public void shutdown() {
        for (int key : mSockets.keySet()) {
            Socket sock = mSockets.get(key);
            try {
                sock.close();
            } catch (IOException e) {
                Log.e("Failed to close socket " + key + " on port " + sock.getLocalPort());
                e.printStackTrace();
            }
        }
    }
}
