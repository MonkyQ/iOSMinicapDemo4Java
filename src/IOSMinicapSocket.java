import sun.misc.BASE64Decoder;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author monky
 * IOSMinica端口转发接收端
 */
public class IOSMinicapSocket {

    private boolean ready = false;
    private boolean isClose = true;
    private byte[] finalBytes = null;
    private Socket socket;
    private int connectNum = 30;
    private int readFrameBytes = 0;
    private int frameBodyLength = 0;
    private byte[] frameBody = new byte[0];
    private static final int minicapPort = 23456;

    public void initMinicap() {
        this.isClose = false;
        this.ready = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    getImage();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    while (true) {
                        if(ready) {
                            InputStream bais = new ByteArrayInputStream(finalBytes);
//                            Image image = new Image(null, bais);
                            ready = false;
                        } else{
                            Thread.sleep(20);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void getImage(){
        InputStream stream = null;
        DataInputStream input = null;
        try {
            connectNum--;
            System.out.println("socket recive waiting...:"+connectNum);
            socket = new Socket("127.0.0.1", minicapPort);
            IOSMinicapClient.IOS_MINICAP_SOCKET_MAP.put(IOSMinicapClient.IOS_MINICAP_SOCKET, socket);
            System.out.println("Socket " + minicapPort + " connectd!");
            while (true) {
                stream = socket.getInputStream();
                input = new DataInputStream(stream);
                byte[] buffer;
                int len = 0;
                while (len == 0) {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    len = input.available();
                }
                buffer = new byte[len];
                input.read(buffer);

                byte[] currentBuffer = subByteArray(buffer, 0, buffer.length);
                for (int cursor = 0; cursor < len;) {
                    int byte10 = buffer[cursor] & 0xff;
                    if (readFrameBytes < 4) {
                        // 第二次的缓冲区中前4位数字和为frame的缓冲区大小
                        frameBodyLength += (byte10 << (readFrameBytes * 8)) >>> 0;
                        cursor += 1;
                        readFrameBytes += 1;
                    } else {
                        if (len - cursor >= frameBodyLength && frameBodyLength > 0) {
                            if (frameBodyLength != 0) {
                                byte[] subByte = subByteArray(currentBuffer, cursor, cursor + frameBodyLength);
                                frameBody = byteMerger(frameBody, subByte);

                                if ((frameBody[0] != -1) || frameBody[1] != -40) {
                                    System.out.println(String.format("Frame body does not start with JPG header"));
                                    String errorMsg = "画面传输异常，请关闭录制窗口重新执行关键字后继续录制";
                                    System.out.println(errorMsg);
                                    return;
                                }
                                System.out.println(String.format("实际图片的大小 : %d", frameBody.length));
                                finalBytes = subByteArray(frameBody, 0, frameBody.length);
                                getPNGToLocal(System.getProperty("user.dir"));
                                ready = true;
                            }
                        }
                        frameBodyLength = 0;
                        readFrameBytes = 0;
                        frameBody = new byte[0];
                        cursor = len;
                    }
                    connectNum = 30;
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    // java合并两个byte数组
    private static byte[] byteMerger(byte[] byte_1, byte[] byte_2) {
        byte[] byte_3 = new byte[byte_1.length + byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    private static byte[] subByteArray(byte[] byte1, int start, int end) {
        byte[] byte2 = new byte[end - start];
        System.arraycopy(byte1, start, byte2, 0, end - start);
        return byte2;
    }

    public void closeSocket() {
        try {
            this.isClose = true;
            if (null != socket) {
                socket.close();
                IOSMinicapClient.IOS_MINICAP_SOCKET_MAP.put(IOSMinicapClient.IOS_MINICAP_SOCKET, "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getPNGToLocal(String path) throws IOException {
        BASE64Decoder decoder = new BASE64Decoder();
//        byte[] imgbyte = decoder.decodeBuffer();
        OutputStream os = new FileOutputStream(path + "/" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + ".png");
        os.write(finalBytes, 0, finalBytes.length);
        os.flush();
        os.close();
    }

    public static void main(String[] args) {
        IOSMinicapSocket socket = new IOSMinicapSocket();
        socket.getImage();
    }
}
