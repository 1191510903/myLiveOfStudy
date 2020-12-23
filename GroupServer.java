package chatWithEachOther;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

public class GroupServer {
    private Selector selector;
    private ServerSocketChannel listChannel;
    private static final int PORT = 6667;

    public GroupServer() {
        try {
            // 得到选择器
            selector = Selector.open();
            //serverSocketChannel
            listChannel = ServerSocketChannel.open();
            // 绑定端口
            listChannel.socket().bind(new InetSocketAddress(PORT));
            // 设置非阻塞模式
            listChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        try {
            while (true) {
                int count = selector.select(2000);
                if (count > 0) {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        //取出selectionKey
                        SelectionKey key = iterator.next();
                        //监听到accept的连接
                        if (key.isAcceptable()) {
                            SocketChannel sc = listChannel.accept();
                            //将sc注册到selector
                            sc.register(selector, SelectionKey.OP_ACCEPT);
                            //提示
                            System.out.println(sc.getRemoteAddress() + "上线！");
                        }
                        if (key.isReadable()) {
                            //处理读数据
                            readData(key);
                        }
                        //当前的key 删除 防止重复处理
                        iterator.remove();
                    }
                } else {
                    System.out.println("等待客户端连接.......");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //发送异常处理
        }
    }

    //读取客户端信息
    private void readData(SelectionKey key) {
        // 取到关联的channel
        SocketChannel channel = null;
        try {
            //得到channel
            channel = (SocketChannel) key.channel();
            //创建buffer
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int count = channel.read(buffer);
            // 根据count的值做处理
            if (count > 0) {
                //把缓存区的数据转成字符串
                String msg = new String(buffer.array());
                System.out.println("form 客户端：" + msg);
                sendInfoToOtherClients(msg,channel);
            }
        } catch (Exception e) {
            try {
                System.out.println(channel.getRemoteAddress()+":离线了！");
                // 取消注册
                key.cancel();
                // 关闭通道
                channel.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }

    }

    //转发消息给其他客户（通道）
    private void sendInfoToOtherClients(String msg, SocketChannel self) throws IOException {
        System.out.println("服务器转发信息中....");
        //遍历 所以注册到selector 上的SockettChannel 并派出self
        for (SelectionKey key : selector.keys()) {
            //通过 key取出对应的socketChannel
            SelectableChannel targentChannel = key.channel();

            // 排除自己
            if (targentChannel instanceof SocketChannel && targentChannel != self) {
                //转型
                SocketChannel dest = (SocketChannel) targentChannel;
                //将 msg 存储到buffer
                ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
                // 将buffer 的数据写入通道
                dest.write(buffer);
            }

        }
    }
}
