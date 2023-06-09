package nekomods.deckcontrols;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;

public class LibcIo {
    private static LibcIoInterface INSTANCE = Native.load("c", LibcIoInterface.class);

    public interface LibcIoInterface extends Library {
        int open(String pathname, int flags);
        int read(int fd, byte[] data, NativeLong len);
        int ioctl(int fd, int cmd, byte[] p);
        int ioctl(int fd, int cmd, NativeLong p);
        int close(int fd);
    }

    public static int open(String pathname, int flags) {
        return INSTANCE.open(pathname, flags);
    }

    public static int read(int fd, byte[] data, long len) {
        return INSTANCE.read(fd, data, new NativeLong(len));
    }

    public static int ioctl(int fd, int cmd, byte[] p) {
        return INSTANCE.ioctl(fd, cmd, p);
    }

    public static int ioctl(int fd, int cmd, NativeLong p) {
        return INSTANCE.ioctl(fd, cmd, p);
    }

    public static int close(int fd) {
        return INSTANCE.close(fd);
    }
}
